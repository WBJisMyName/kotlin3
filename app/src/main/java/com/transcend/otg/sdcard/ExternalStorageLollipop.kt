package com.transcend.otg.sdcard

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.transcend.otg.R
import com.transcend.otg.permission.PermissionHandle
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.SystemUtil
import java.io.File
import java.util.*

class ExternalStorageLollipop(context: Context) : AbstractExternalStorage(context) {
    private val mContext: Context

    override val isWritePermissionNotGranted: Boolean
        get() {
            val uri = sdLocationUri
            if (uri != null) {
                val storagePaths = SystemUtil().getStoragePathsWithoutLocal(mContext)
                for (path in storagePaths) {
                    val f = File(path)
                    val name = f.getName()
                    if (uri.contains(name))
                        return false
                }
            }
            return true
        }

    private val rootFolderSD: DocumentFile?
        @TargetApi(19)
        get() {
            var uriTree = PreferenceManager.getDefaultSharedPreferences(mContext).all[PREF_DEFAULT_URISD] as String?
            if (uriTree == null)
                uriTree = SystemUtil().getSDLocation(mContext)
            mContext.contentResolver.takePersistableUriPermission(
                Uri.parse(uriTree),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            return DocumentFile.fromTreeUri(mContext, Uri.parse(uriTree))
        }

    private val sdLocationUri: String?
        get() = PreferenceManager.getDefaultSharedPreferences(mContext).all[PREF_DEFAULT_URISD] as String?

    init {
        mContext = context
    }

    override fun onActivityResult(activity: AppCompatActivity, data: Intent) {
        if (activity == null || data == null) {
            return
        }

        val isValid = checkSelectedFolder(data)
        if (isValid) {
            val treeUri = Uri.parse(sdLocationUri)
            Log.d(TAG, "treeUri: $treeUri")
            Log.d(TAG, "getSDLocation(): " + SystemUtil().getSDLocation(mContext)!!)
            //            activity.startActivity(MainActivity.class, R.id.nav_downloads, StorageUtil.getSDLocation(mContext));
        } else {
            Toast.makeText(mContext, "Selected folder is not the root folder of SD card", Toast.LENGTH_LONG).show()
            requestPermissionDialog(activity)
        }
    }

    override fun isWritePermissionRequired(vararg path: String): Boolean {
        for (p in path) {
            if (SystemUtil().isSDCardPath(mContext, p))
                return true
        }
        return false
    }

    override fun getSDFileUri(path: String): Uri {
        val file = getSDFileLocation(path)
        return file!!.uri
    }

    @TargetApi(19)
    fun checkSelectedFolder(data: Intent): Boolean {
        var isValid = false
        val uriTree = data.data
        Log.d(TAG, "uriTree.toString(): " + uriTree!!.toString())
        if (!uriTree.toString().contains("primary")) {
            if (isRootFolder(uriTree)) {
                mContext.contentResolver.takePersistableUriPermission(
                    uriTree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val pickedDir = DocumentFile.fromTreeUri(mContext, uriTree)
                //jerry
                val isSDFile = isSDFile(pickedDir)
                Log.d(TAG, "isSDFile: $isSDFile")
                if (isSDFile) {
                    storeSDLocationUri(uriTree)
                    isValid = true
                }
            }
        }
        return isValid
    }

    fun getDestination(destPath: String): DocumentFile? {
        val file = File(destPath)
        if (!file.exists()) {
            return null
        }
        val pickedDir: DocumentFile?
        if (SystemUtil().isSDCardPath(mContext, destPath)) {
            pickedDir = getSDFileLocation(destPath)
        } else {
            pickedDir = DocumentFile.fromFile(file)
        }
        return pickedDir
    }

    fun getSDFileLocation(destPath: String): DocumentFile? {
        var sdFile = rootFolderSD
        if (destPath == SystemUtil().getSDLocation(context)) {//root path
            return sdFile
        } else {
            val splitPath = destPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (splitPath.size > 3) {
                for (index in 3 until splitPath.size) {
                    sdFile = sdFile!!.findFile(splitPath[index])
                }
                return sdFile

            } else {
                return sdFile
            }
        }
    }

    /**
     * example of splitURI:  (root folder: /BE8D-1108)         content://com.android.externalstorage.documents/tree/BE8D-1108%3A
     * example of splitURI:  (subfolder: /BE8D-1108/Android)   content://com.android.externalstorage.documents/tree/BE8D-1108%3AAndroid
     *
     * @param uriTree
     * @return
     */
    private fun isRootFolder(uriTree: Uri?): Boolean {
        var splitURI = arrayOfNulls<String>(0)
        if (uriTree != null) {
            splitURI = uriTree.toString().split("%".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        return splitURI.size == 2 && splitURI[1]!!.length <= 2
    }

    private fun isSDFile(sdDir: DocumentFile?): Boolean {
        val stgList = SystemUtil().getStorageList(mContext)
        var isSDFile = false
        if (stgList.size > 1) {//has sd card
            for (sd in stgList) {
                if (!sd.getAbsolutePath().toLowerCase().contains("usb")) {
                    try {
                        val tmpFile = getSDCardFileName(SystemUtil().getSDLocation(mContext))
                        val tmpDFile = sdDir!!.listFiles()
                        isSDFile = doFileNameCompare(tmpDFile, tmpFile)
                    } catch (e: Exception) {
                        isSDFile = false
                    }

                    break
                }
            }
        }
        return isSDFile
    }

    private fun getSDCardFileName(mPath: String?): ArrayList<String> {
        val sdName = ArrayList<String>()
        val dir = File(mPath)

        val files = dir.listFiles()
        for (file in files) {
            if (file.isHidden)
                continue
            val name = file.name
            sdName.add(name)
        }
        return sdName
    }

    private fun doFileNameCompare(tmpDFile: Array<DocumentFile>, tmpFile: ArrayList<String>): Boolean {
        var fileCount = 0
        for (fi in tmpFile.indices) {
            val name = tmpFile[fi]
            for (df in tmpDFile.indices) {
                if (name == tmpDFile[df].name) {
                    fileCount++
                    break
                }
            }
        }
        return if (fileCount == tmpFile.size) {
            true
        } else {
            false
        }
    }

    private fun storeSDLocationUri(uriTree: Uri): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(mContext).edit()
            .putString(PREF_DEFAULT_URISD, uriTree.toString()).commit()
    }

    private fun requestPermissionDialog(activity: AppCompatActivity) {
        //*索取權限
        if (!PermissionHandle.requestReadPermission(activity)) {
            return
        } else {
            val builder = AlertDialog.Builder(mContext)
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
                activity.startActivityForResult(intent, REQUEST_CODE)
                dialog.dismiss()
            }
            posBtn.textSize = 18f
            val negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            negBtn.setOnClickListener {
                //                    activity.toggleDrawerCheckedItem();
                dialog.dismiss()
            }
            negBtn.textSize = 18f

            val viewerPager = dialog.findViewById<View>(R.id.viewer_pager_sd) as ViewPagerZoomFixed?
            viewerPager!!.adapter = ViewerPagerAdapterSD(mContext)
            viewerPager.currentItem = 0
        }
    }

    companion object {
        private val TAG = ExternalStorageLollipop::class.java.simpleName
        val PREF_DEFAULT_URISD = "PREF_DEFAULT_URISD"
        val REQUEST_CODE = ExternalStorageLollipop::class.java.hashCode() and 0xFFFF
    }
}
