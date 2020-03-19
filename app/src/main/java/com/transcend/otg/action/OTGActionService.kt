package com.transcend.otg.action

import android.app.Activity
import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.action.loader.*
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.LoaderID
import java.util.*

class OTGActionService: PhoneActionService(){
    init {
        TAG = OTGActionService::class.java.simpleName
        mRoot = Constant.OTG_ROOT
        mPath = Constant.OTG_ROOT
    }

    override fun initLoaderID(ids: HashMap<FileAction, Int>) {    //初始化Loader
        ids[FileAction.CreateFOLDER] = LoaderID.NEW_FOLDER
        ids[FileAction.RENAME] = LoaderID.FILE_RENAME
        ids[FileAction.COPY] = LoaderID.FILE_COPY
        ids[FileAction.MOVE] = LoaderID.FILE_MOVE
        ids[FileAction.DELETE] = LoaderID.FILE_DELETE
        ids[FileAction.ENCRYPT] = LoaderID.FILE_ENCRYPT
        ids[FileAction.DECRYPT] = LoaderID.FILE_DECRYPT
    }

    override fun getRootPath(context: Context): String? {
        if (mRoot == null)
            return "/"
        return mRoot
    }

    override fun rename(context: Context, path: String, name: String): AsyncTaskLoader<*>? {
        return null
    }

    override fun copy(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>? {
        return CopyLoader(context as Activity, list, dest)
    }

    override fun move(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>? {
        return MoveLoader(context as Activity, list, dest)
    }

    override fun delete(context: Context, list: List<String>): AsyncTaskLoader<*>? {
        return DeleteLoader(context, list)
    }

    override fun createFolder(context: Context, path: String): AsyncTaskLoader<*>? {
        return FolderCreateLoader(context, path)
    }

    override fun share(context: Context, paths: ArrayList<String>, dest: String): AsyncTaskLoader<*>? {
        return null
    }
}