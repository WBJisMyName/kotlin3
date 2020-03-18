package com.transcend.otg.browser

import android.content.*
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProviders
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.ActivityFileLocateBinding
import com.transcend.otg.permission.SdPermission
import com.transcend.otg.receiver.SDCardReceiver
import com.transcend.otg.sdcard.ExternalStorageLollipop
import com.transcend.otg.sdcard.ViewerPagerAdapterSD
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.ActionLocateViewModel
import java.io.File

class FileActionLocateActivity : AppCompatActivity(),
    SDCardReceiver.SDCardObserver{

    companion object {
        val REQUEST_CODE = FileActionLocateActivity::class.java.hashCode() and 0xFFFF
        private val TAG = FileActionLocateActivity::class.java.simpleName
    }

    private var mFragment: FileActionLocateFragment? = null
    private var confirmBtn: Button? = null
    private var mPath: String
    private var mActionID: Int

    private val mSDPermission = 1005
    private val mSDQPermission = 1006
    private val mFileInfo: FileInfo? = null
    private var oneSecond = true

    lateinit var mBinding: ActivityFileLocateBinding
    lateinit var viewModel: ActionLocateViewModel

    init {
        mPath = Constant.LOCAL_ROOT
        mActionID = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_file_locate
        )
        viewModel = ViewModelProviders.of(this).get(ActionLocateViewModel::class.java)
        mBinding.viewModel = viewModel

        val path = intent.getStringExtra("path")
        if (path != null && !path.equals(""))
            mPath = path

        mActionID = intent.getIntExtra("action_id", -1)
        when(mActionID){
            R.id.action_copy -> Toast.makeText(this,
                R.string.copy_items_to, Toast.LENGTH_LONG).show()
            R.id.action_move -> Toast.makeText(this,
                R.string.move_items_to, Toast.LENGTH_LONG).show()
        }

        mBinding.toggle.setOnClickListener {
            this@FileActionLocateActivity.finish()
        }

        //設置Toolbar
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)    // enable overriding the default toolbar layout
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)  // show or hide the default home button

        initFragment()
    }

    override fun onResume() {
        super.onResume()
        initBroadcast()
        SDCardReceiver.instance.registerObserver(this) //監測SD卡插拔
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mSDPermission) {
            if (resultCode == RESULT_OK) {
                if (oneSecond && data != null && data.data != null) {
                    val uriTree = data.data
                    contentResolver.takePersistableUriPermission(uriTree!!, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                    AppPref.setSDKey(this, uriTree.toString())

                    val isSelectedFolderValid = ExternalStorageLollipop(this).checkSelectedFolder(data)
                    if (isSelectedFolderValid) {
                        if (mFileInfo != null) {
                            mPath = mFileInfo.path
                            mFragment?.doLoadFiles(mPath)
                        }
                    }
                    return
                }
            } else {
                mFragment?.doLoadFiles(Constant.Storage_Device_Root)
            }

            checkSDPermission()
        } else if (requestCode == mSDQPermission) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.data != null) {
                    val uriTree = data.data
                    contentResolver.takePersistableUriPermission(
                        uriTree!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    AppPref.setSDKey(this, uriTree.toString())

                    val isSelectedFolderValid =
                        ExternalStorageLollipop(this).checkSelectedFolder(data)
                    if (isSelectedFolderValid) {
                        if (mFileInfo != null) {
                            mPath = mFileInfo.path
                            mFragment?.doLoadFiles(mPath)
                        }
                    }
                    return
                } else
                    mFragment?.doLoadFiles(Constant.Storage_Device_Root)

                checkPermission()
            }
        }
    }

    private fun initFragment() {
        mFragment = FileActionLocateFragment()
        mFragment?.arguments?.putString("root", mPath)   //讀取本地路徑

        confirmBtn = findViewById(R.id.action_confirm) as Button
        confirmBtn?.setOnClickListener(View.OnClickListener { popupConfirmDialog() })
        viewModel.confirmBtnVisibility.set(View.GONE)

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        //transaction.setCustomAnimations(R.anim.appear, 0);
        transaction.replace(R.id.fragment_container, mFragment!!)
        transaction.commit()
    }

    private fun backToMainActivity() {
        if (mFragment != null)
            mPath = mFragment?.getPath() ?: Constant.LOCAL_ROOT

        if (!SdPermission.isEditable(this, mPath)) {
            Toast.makeText(this, "Please select a root folder to grant write permission", Toast.LENGTH_SHORT).show()
            checkSDPermission()
        } else {
            val bundle = Bundle()
            bundle.putString("path", mPath)
            bundle.putInt("action_id", mActionID)
            val intent = Intent()
            intent.putExtras(bundle)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    fun checkPermission(): Boolean {
        val sdKey = AppPref.getSDKey(this)
        Log.d(TAG, "sdKey: $sdKey")
        if (sdKey != "") {
            val uriSDKey = Uri.parse(sdKey)
            val f = DocumentFile.fromTreeUri(this, uriSDKey)
            if (f != null && f.exists()) {
                return true
            }
        }

        if (MainApplication().OSisAfterNougat()) {
            Log.d(TAG, ">N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            requestSDCardPermission()
            oneSecond = false
            Thread(Runnable {
                try {
                    Thread.sleep(100)
                    oneSecond = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }).start()
            return false
        } else {
            Log.d(TAG, "<N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            checkSDPermission()
            return false
        }
    }

    private fun popupConfirmDialog() {
        if (mFragment == null)
            return

        var title: String = getString(R.string.app_name)
        when(mActionID){
            R.id.action_copy -> title = getString(
                R.string.copy_items_to
            )
            R.id.action_move -> title = getString(
                R.string.move_items_to
            )
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(mFragment?.getPath())
        builder.setNegativeButton(R.string.cancel, null)
        builder.setPositiveButton(R.string.confirm, null)
        builder.setCancelable(true)
        val dialog = builder.show()
        val bnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        bnPos.setOnClickListener {
            dialog.dismiss()
            backToMainActivity()
        }
    }

    override fun onBackPressed() {
        if(mFragment is BackpressCallback){
            (mFragment as? BackpressCallback)?.onBackPressed()?.let {
                if(it) super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun checkSDPermission(): Boolean {
        val sdKey = AppPref.getSDKey(this)
        Log.d(TAG, "sdKey: $sdKey")
        if (!sdKey.equals("")) {
            val uriSDKey = Uri.parse(sdKey)
            val f = DocumentFile.fromTreeUri(this, uriSDKey)
            if (f != null && f.exists()) {
                return true
            }
        }

        //TODO Permission of Android Q
        if (MainApplication().OSisAfterNougat()) {
            Log.d(TAG, ">N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            requestSDCardPermission()
            oneSecond = false
            Thread(Runnable {
                try {
                    Thread.sleep(100)
                    oneSecond = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }).start()
            return false
        } else {
            Log.d(TAG, "<N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            requestSDCardPermissionDialog()
            return false
        }
    }

    private fun requestSDCardPermission() {
        if (MainApplication().OSisAfterNougat()) {
            val sdPath = SystemUtil().getSDLocation(this)
            if (sdPath == null) {
                Log.d(TAG, "sdPath is null")
                return
            }
            val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.getStorageVolume(File(sdPath))
            val intent = storageVolume!!.createAccessIntent(null)
            try {
                Log.d(TAG, "startActivityForResult, mSDPermission:$mSDPermission")
                startActivityForResult(intent, mSDPermission)
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "ActivityNotFoundException")
                requestSDCardPermissionDialog()
            }
        }
    }

    private fun requestSDCardPermissionDialog() {
        //*索取權限
        val sdKey = AppPref.getSDKey(this)
        if (!sdKey.equals("")) {
            //釋放已記錄的權限
            contentResolver.releasePersistableUriPermission(
                Uri.parse(sdKey),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            AppPref.setSDKey(this, "")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("SD Card")
        builder.setIcon(R.drawable.ic_drawer_microsd_grey)
        builder.setView(R.layout.dialog_connect_sd)
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("Confirm", null)
        builder.setCancelable(false)
        val dialog = builder.show()
        val posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        posBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, mSDPermission)
            dialog.dismiss()
        }
        posBtn.textSize = 18f
        val negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negBtn.setOnClickListener {
            dialog.dismiss()
        }
        negBtn.textSize = 18f

        val viewerPager = dialog.findViewById(R.id.viewer_pager_sd) as ViewPagerZoomFixed?
        viewerPager?.setAdapter(ViewerPagerAdapterSD(this))
        viewerPager?.setCurrentItem(0)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(usbReceiver)
            SDCardReceiver.instance.unregisterObserver(this)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun notifyMounted() {
        Constant.SD_ROOT = SystemUtil().getSDLocation(this)
        mFragment?.doLoadFiles(Constant.Storage_Device_Root)
    }

    override fun notifyUnmounted() {
        Constant.SD_ROOT = SystemUtil().getSDLocation(this)
        mFragment?.doLoadFiles(Constant.Storage_Device_Root)
        viewModel.deleteAllFromRoot(Constant.STORAGEMODE_SD)
    }

    private fun initBroadcast() {
        val filter = IntentFilter()
        filter.addAction(UsbUtils.ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED)
        registerReceiver(usbReceiver, filter)

        SDCardReceiver.instance.registerObserver(this)
        checkOtgDevice()
    }

    fun checkOtgDevice() {
        UsbUtils.isOtgDeviceExist(this)
        mFragment?.doLoadFiles(Constant.Storage_Device_Root)
    }

    val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbUtils.ACTION_USB_PERMISSION == action) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                    UsbUtils.discoverDevices(this@FileActionLocateActivity)
                    mFragment?.doLoadFiles("/")
                } else {
                    Log.e("Permission", "False")
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action){
                checkOtgDevice()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action){
                checkOtgDevice()
                viewModel.deleteAllFromRoot(Constant.STORAGEMODE_OTG)
            }
        }
    }
}
