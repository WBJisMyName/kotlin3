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
import androidx.documentfile.provider.DocumentFile
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.transcend.otg.action.dialog.FileActionNewFolderDialog
import com.transcend.otg.action.loader.LocalFolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.data.FileInfo
import com.transcend.otg.permission.PermissionHandle
import com.transcend.otg.permission.SdPermission
import com.transcend.otg.sdcard.ExternalStorageController
import com.transcend.otg.sdcard.ViewerPagerAdapterSD
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.LoaderID
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.SystemUtil
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
    private lateinit var mPath: String

    private val mSDPermission = 1005
    private val mFileInfo: FileInfo? = null
    private var oneSecond = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_locate)

        mPath = Constant.LOCAL_ROOT
        mPath = intent.getStringExtra("path")

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
        //        if (requestCode == mSDPermission){
        //            if(resultCode == RESULT_OK) {
        //                if (oneSecond && (data != null && data.getData() != null)) {
        //                    Uri uriTree = data.getData();
        //                    getContentResolver().takePersistableUriPermission(uriTree,
        //                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        //
        //                    AppPref.setSDKey(this, uriTree.toString());
        //
        //                    boolean isSelectedFolderValid = new ExternalStorageLollipop(this).checkSelectedFolder(data);
        //                    if (isSelectedFolderValid) {
        //                        if (mFileInfo != null) {
        //                            mPath = mFileInfo.path;
        //                            mFragment.doLoad(mPath);
        //                        }
        //                    }
        //
        //                    return;
        //                }
        //            } else {
        //                mFragment.doLoad(AppConst.Storage_Root_Path);
        //            }
        //
        //            requestPermissionDialog();
        //        }
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

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select")
        //        builder.setMessage(mFragment.getPath());
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("Confirm", null)
        builder.setCancelable(true)
        val dialog = builder.show()
        val bnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        bnPos.setOnClickListener {
            dialog.dismiss()
            backToMainActivity()
        }
    }

    override fun onBackPressed() {

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
                val id = if (mFragment?.getPath()?.startsWith(Constant.LOCAL_ROOT) ?: false)
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
                args.putString("name", newName)
                supportLoaderManager.restartLoader(id, args, this@FileActionLocateActivity).forceLoad()
                Log.w(TAG, "doNewFolder: $path")
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
//        mFragment.setLoadingProgressVisibility(View.VISIBLE)
        val path = args?.getString("path")
        val name = args?.getString("name")
        when (id) {
            LoaderID.LOCAL_NEW_FOLDER -> return LocalFolderCreateLoader(this, path!!)
//            LoaderID.OTG_LOCAL_NEW_FOLDER -> return OTGLocalFolderCreateLoader(
//                this,
//                ExternalStorageLollipop(this).getSDFileLocation(path),
//                name
//            )
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
