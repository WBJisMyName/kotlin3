package com.transcend.otg.action

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.action.loader.FileActionService
import com.transcend.otg.action.loader.PhoneActionService
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.Constant
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
        PHONE, SD, OTG
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
                FileActionServiceType.OTG -> service = OTGActionService()
            }
            mFileActionServicePool!![type] = service
        }

        mFileActionServiceType = type
        fileActionService = service
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

    fun encrypt(dest: String, paths: ArrayList<String>, password: String){
        createLoader(FileActionService.FileAction.ENCRYPT, password, dest, paths)
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
}
