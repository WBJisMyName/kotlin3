package com.transcend.otg.action.loader

import android.content.Context
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.loader.content.AsyncTaskLoader
import java.util.*

abstract class FileActionService {
    var TAG = FileActionService::class.java.simpleName
    protected var mRoot: String? = null
    protected var mPath: String? = null
    protected var mFileActionIDs: HashMap<FileAction, Int>

    enum class FileAction {
        RENAME, COPY, MOVE, DELETE, CreateFOLDER, SHARE
    }

    init {
        mFileActionIDs = HashMap()
        initLoaderID(mFileActionIDs)
    }

    abstract fun initLoaderID(ids: HashMap<FileAction, Int>)

    open fun getRootPath(context: Context): String? {
        return mRoot
    }

    fun setCurrentPath(path: String) {
        mPath = path
    }

    fun getFileAction(action: Int): FileAction? {
        for (type in mFileActionIDs.keys) {
            val id = mFileActionIDs[type]!!
            if (id > 0 && id == action)
                return type
        }
        return null
    }

    fun getLoaderID(action: FileAction): Int {
        return mFileActionIDs[action]!!
    }

    fun onCreateLoader(context: Context, id: FileAction, args: Bundle): AsyncTaskLoader<*>? {
        val paths = args.getStringArrayList("paths")!!
        val path = args.getString("path")!!
        val name = args.getString("name")!!
        when (id) {
            FileAction.RENAME -> return rename(context, path, name)
            FileAction.COPY -> return copy(context, paths, path)
            FileAction.MOVE -> return move(context, paths, path)
            FileAction.DELETE -> return delete(context, paths)
            FileAction.CreateFOLDER -> return createFolder(context, path)
            FileAction.SHARE -> return share(context, paths, path)
        }
        return null
    }

    fun onLoadFinished(context: Context, loader: AsyncTaskLoader<*>, success: Boolean?): Boolean {
        return onLoadFinished(context, null, loader, success)
    }

    abstract fun onLoadFinished(
        context: Context,
        progress: RelativeLayout?,
        loader: AsyncTaskLoader<*>,
        success: Boolean?
    ): Boolean


    protected abstract fun rename(context: Context, path: String, name: String): AsyncTaskLoader<*>?

    protected abstract fun copy(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>?

    protected abstract fun move(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>?

    protected abstract fun delete(context: Context, list: List<String>): AsyncTaskLoader<*>?

    protected abstract fun createFolder(context: Context, path: String): AsyncTaskLoader<*>?

    protected abstract fun share(context: Context, paths: ArrayList<String>, dest: String): AsyncTaskLoader<*>?
}
