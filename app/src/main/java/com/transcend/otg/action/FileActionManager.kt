package com.transcend.otg.action

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.action.loader.FileActionService
import com.transcend.otg.action.loader.PhoneActionService
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.SystemUtil
import java.io.File
import java.util.*

class FileActionManager @JvmOverloads constructor(
    var context: Context,
    private var mFileActionServiceType: FileActionServiceType,
    callbacks: LoaderManager.LoaderCallbacks<Boolean>
) : AbstractActionManager(context, callbacks) {

    var fileActionService: FileActionService? = null    //public getter and private (only internally modifiable) setter
        private set

    private var mFileActionServicePool: HashMap<FileActionServiceType, FileActionService>? = null
    private var isLockType = false

    val serviceRootPath: String
        get() {
            var root = Constant.LOCAL_ROOT
            if (fileActionService != null)
                root = fileActionService!!.getRootPath(context)
            return root
        }

    enum class FileActionServiceType {  //action 執行類別
        PHONE, SD
    }

    init {
        setServiceType(mFileActionServiceType)
        isLockType = false
    }

    fun setServiceType(type: FileActionServiceType) {   //判斷現在是哪種裝置
        if (fileActionService != null && mFileActionServiceType == type)
            return

        if (null == mFileActionServicePool)
            mFileActionServicePool = HashMap<FileActionServiceType, FileActionService>()

        var service = mFileActionServicePool!![type]
        if (null == service) {
            when (type) {
                FileActionServiceType.SD -> service = SdcardActionService()
                FileActionServiceType.PHONE -> service = PhoneActionService()
            }
            mFileActionServicePool!![type] = service
        }

        mFileActionServiceType = type
        fileActionService = service
    }

    fun setServiceType(path: String?) {
        if (isLockType)
            return

        if (path == null)
            return

        if (path.startsWith("/storage")) {
            if (SystemUtil().isSDCardPath(context, path))
                setServiceType(FileActionManager.FileActionServiceType.SD)
            else
                setServiceType(FileActionManager.FileActionServiceType.PHONE)
        }
    }

    fun setCurrentPath(path: String) {
        if (fileActionService != null)
            fileActionService!!.setCurrentPath(path)
    }

    fun rename(path: String, name: String) {
        createLoader(FileActionService.FileAction.RENAME, name, path, null)
    }

    fun copy(dest: String, paths: ArrayList<String>) {
        createLoader(FileActionService.FileAction.COPY, null, dest, paths)
    }

    fun move(dest: String, paths: ArrayList<String>) {
        createLoader(FileActionService.FileAction.MOVE, null, dest, paths)
    }

    fun delete(paths: ArrayList<String>) {
        createLoader(FileActionService.FileAction.DELETE, null, null, paths)
    }

    //建立資料夾
    fun createFolder(dest: String, newName: String) {
        val builder = StringBuilder(dest)
        if (!dest.endsWith("/"))
        //因為要加上新資料夾的名稱，故要有 " / "
            builder.append("/")
        builder.append(newName)
        val path = builder.toString()

        createLoader(FileActionService.FileAction.CreateFOLDER, null, path, null)
    }

    fun share(dest: String, files: ArrayList<FileInfo>) {
//        if (isRemoteAction(files[0].path)) {
//            val paths = ArrayList<String>()
//            for (file in files) {
//                paths.add(file.path)
//            }
//            createLoader(FileActionService.FileAction.SHARE, null, dest, paths)
//        } else {
//            SystemUtil.shareLocalFile(context, files)
//        }
    }

    private fun createLoader(
        type: FileActionService.FileAction,
        name: String?,
        dest: String?,
        paths: ArrayList<String>?
    ) {
        val id = fileActionService!!.getLoaderID(type)
        val args = Bundle()
        if (name != null)
            args.putString("name", name)
        if (dest != null)
            args.putString("path", dest)
        if (paths != null)
            args.putStringArrayList("paths", paths)

        args.putInt("actionType", id)

        createLoader(id, args)
    }

    override fun onCreateLoader(id: Int, args: Bundle): AsyncTaskLoader<*>? {
        var loader: AsyncTaskLoader<*>? = null
        if (fileActionService != null) {
            val type = args.getInt("actionType", -1)
            if (type > 0) {
                val action = fileActionService!!.getFileAction(type)
                if (action != null) {
                    loader = fileActionService!!.onCreateLoader(context, action, args)
                    if (loader != null) {
//                        when (action) {
//                            LIST, RENAME, DELETE, CreateFOLDER, SHARE, ShareLINK, OPEN -> showProgress()
//                            else -> hideProgress()
//                        }
                    }
                }
            }
        }

        return loader
    }

    override fun onLoadFinished(loader: AsyncTaskLoader<*>, success: Boolean?): Boolean {
        return if (fileActionService != null) {
            fileActionService!!.onLoadFinished(context, loader, success)
        } else false

    }

    override fun onLoaderReset(loader: AsyncTaskLoader<*>) {

    }

    fun isTopDirectory(path: String?): Boolean {
        if (path == null)
            return false

        val root = serviceRootPath
        when (mFileActionServiceType) {
            FileActionServiceType.SD, FileActionServiceType.PHONE -> {
                val base = File(root)
                val file = File(path)
                return file == base
            }
            else -> return path == root
        }
    }

    fun isSubDirectory(dest: String, paths: ArrayList<String>): Boolean {
        for (path in paths) {
            if (dest.startsWith(path)) {
                return true
            }
        }
        return false
    }

    fun isDirectorySupportFileAction(path: String): Boolean {
        return isTopDirectory(path)
    }

    fun doLockActionType() {
        isLockType = true
    }

    fun doUnLockActionType() {
        isLockType = false
    }

}
