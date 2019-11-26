package com.transcend.otg

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.transcend.otg.action.dialog.FileActionNewFolderDialog
import com.transcend.otg.action.loader.LocalFolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.ActivityFileLocateBinding
import com.transcend.otg.permission.PermissionHandle
import com.transcend.otg.permission.SdPermission
import com.transcend.otg.sdcard.ExternalStorageController
import com.transcend.otg.sdcard.ExternalStorageLollipop
import com.transcend.otg.sdcard.ViewerPagerAdapterSD
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.ActionLocateViewModel
import java.io.File
import java.util.*

//TODO  暫時僅作Local端
class FileActionLocateActivity : AppCompatActivity(),
    LoaderManager.LoaderCallbacks<Boolean>{

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

    init {
        mPath = Constant.LOCAL_ROOT
        mActionID = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_file_locate)
        val viewModel = ViewModelProviders.of(this).get(ActionLocateViewModel::class.java)
        mBinding.viewModel = viewModel

        val path = intent.getStringExtra("path")
        if (path != null && !path.equals(""))
            mPath = path

        mActionID = intent.getIntExtra("action_id", -1)
        when(mActionID){
            R.id.action_copy -> Toast.makeText(this, R.string.title_copy_to, Toast.LENGTH_LONG).show()
            R.id.action_move -> Toast.makeText(this, R.string.title_move_to, Toast.LENGTH_LONG).show()
        }

        mBinding.toggle.setOnClickListener {
            this@FileActionLocateActivity.finish()
        }

        initFragment()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_actionmode, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_new_folder -> doNewFolder()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mSDPermission) {
            if (resultCode == RESULT_OK) {
                if (oneSecond && data != null && data.data != null) {
                    val uriTree = data.data
                    contentResolver.takePersistableUriPermission(
                        uriTree!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    AppPref.setSDKey(this, uriTree!!.toString())

                    val isSelectedFolderValid =
                        ExternalStorageLollipop(this).checkSelectedFolder(data)
                    if (isSelectedFolderValid) {
                        if (mFileInfo != null) {
                            mPath = mFileInfo.path
                            mFragment?.doLoad(mPath)
                        }
                    }

                    return
                }
            } else {
                mFragment?.doLoad(Constant.Storage_Root_Path)
            }

            requestPermissionDialog()
        } else if (requestCode == mSDQPermission) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.data != null) {
                    val uriTree = data.data
                    contentResolver.takePersistableUriPermission(
                        uriTree!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    AppPref.setSDKey(this, uriTree!!.toString())

                    val isSelectedFolderValid =
                        ExternalStorageLollipop(this).checkSelectedFolder(data)
                    if (isSelectedFolderValid) {
                        if (mFileInfo != null) {
                            mPath = mFileInfo.path
                            mFragment?.doLoad(mPath)
                        }
                    }
                    return
                } else
                    mFragment?.doLoad(Constant.Storage_Root_Path)

                checkPermission()
            }
        }
    }

    private fun initFragment() {
        var showConfirm = true
        val stgList = SystemUtil().getStorageList(this)
//        if (stgList.size > 1) {
//            mPath = Constant.Storage_Root_Path
//            showConfirm = false
//        }
        mFragment = FileActionLocateFragment()
        mFragment?.arguments?.putString("root", mPath)   //讀取本地路徑

        confirmBtn = findViewById(R.id.action_confirm) as Button
        confirmBtn?.visibility = if (showConfirm) View.VISIBLE else View.GONE
        confirmBtn?.setOnClickListener(View.OnClickListener { popupConfirmDialog() })

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
            requestPermissionDialog()
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

    private fun checkPermission(): Boolean {
        val sdKey = AppPref.getSdKey(this)
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
            requestPermissionDialog()
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
                requestPermissionDialog()
            }

        }
    }

    private fun popupConfirmDialog() {
        if (mFragment == null)
            return

        var title: String = getString(R.string.app_name)
        when(mActionID){
            R.id.action_copy -> title = getString(R.string.copyitemsto)
            R.id.action_move -> title = getString(R.string.moveitemsto)
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

    private fun requestPermissionDialog() {
        //*索取權限
        if (!PermissionHandle.requestReadPermission(this)) {
            return
        } else {
            val sdKey = AppPref.getSdKey(this)
            if (sdKey != null && sdKey != "") {
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
                //                    activity.toggleDrawerCheckedItem();
                dialog.dismiss()
            }
            negBtn.textSize = 18f

            val viewerPager = dialog.findViewById(R.id.viewer_pager_sd) as ViewPagerZoomFixed?
            viewerPager?.setAdapter(ViewerPagerAdapterSD(this))
            viewerPager?.setCurrentItem(0)
        }
    }

    private fun doNewFolder() {
        if (mFragment != null)
            mPath = mFragment?.getPath() ?: Constant.LOCAL_ROOT
        else
            return

        if (!SdPermission.isEditable(this, mPath)) {
            Toast.makeText(this, "Please select a root folder to grant write permission", Toast.LENGTH_SHORT).show()
            requestPermissionDialog()
            return
        }

        val folderNames = ArrayList<String>()
        val fileList = mFragment?.getFileList()
        if (fileList == null || fileList.size == 0)
            return
        for (file in fileList) {    //儲存資料夾名稱
            if (file.fileType == Constant.TYPE_DIR)
                folderNames.add(file.title)
        }
        object : FileActionNewFolderDialog(this, folderNames) {
            override fun onConfirm(newName: String) {
                val storageController = ExternalStorageController(this@FileActionLocateActivity)
                val folderPath = mFragment?.getPath()
                val id = if (folderPath?.startsWith(Constant.LOCAL_ROOT) ?: false || folderPath?.startsWith(SystemUtil().getSDLocation(this@FileActionLocateActivity)!!) ?: false)
                    LoaderID.LOCAL_NEW_FOLDER
                else
                    LoaderID.OTG_LOCAL_NEW_FOLDER

                val builder = StringBuilder(mPath)
                if (!mPath.endsWith("/"))
                    builder.append("/")
                if (!storageController.isWritePermissionRequired(mPath)) {
                    builder.append(newName)
                }
                val path = builder.toString()
                val args = Bundle()
                args.putString("path", path)
                LoaderManager.getInstance(this@FileActionLocateActivity).restartLoader(id, args, this@FileActionLocateActivity).forceLoad()
                Log.w(TAG, "doNewFolder: $path")
            }
        }
    }



    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
//        mFragment.setLoadingProgressVisibility(View.VISIBLE)
        val path = args?.getString("path")
        when (id) {
            LoaderID.LOCAL_NEW_FOLDER -> return LocalFolderCreateLoader(this, path!!)
        }
        return NullLoader(this)
    }

    override fun onLoadFinished(loader: Loader<Boolean>, data: Boolean?) {
//        mFragment.setLoadingProgressVisibility(View.GONE)
        mFragment?.doReload()
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
