package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.utilities.LoaderID
import java.util.*

open class PhoneActionService : FileActionService() {
    override fun onLoadFinished(
        context: Context,
        progress: RelativeLayout?,
        loader: AsyncTaskLoader<*>,
        success: Boolean?
    ): Boolean {
        return false    //待覆寫
    }

    override fun initLoaderID(ids: HashMap<FileActionService.FileAction, Int>) {    //初始化Loader
        ids[FileAction.CreateFOLDER] = LoaderID.NEW_FOLDER
        ids[FileAction.RENAME] = LoaderID.FILE_RENAME
        ids[FileAction.COPY] = LoaderID.FILE_COPY
        ids[FileAction.MOVE] = LoaderID.FILE_MOVE
        ids[FileAction.DELETE] = LoaderID.FILE_DELETE
        ids[FileAction.ENCRYPT] = LoaderID.FILE_ENCRYPT
        ids[FileAction.DECRYPT] = LoaderID.FILE_DECRYPT
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

    override fun encrypt(context: Context, list: List<String>, dest: String, password: String): AsyncTaskLoader<*>? {
        return EncryptLoader(context as Activity, list, dest, password)
    }

    override fun decrypt(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>? {
        return CopyLoader(context as Activity, list, dest)
    }
}
