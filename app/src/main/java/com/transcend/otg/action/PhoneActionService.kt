package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import androidx.documentfile.provider.DocumentFile
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.utilities.LoaderID
import org.apache.commons.io.FilenameUtils
import java.io.File
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
        ids[FileAction.CreateFOLDER] = LoaderID.LOCAL_NEW_FOLDER
        ids[FileAction.RENAME] = LoaderID.LOCAL_FILE_RENAME
        ids[FileAction.COPY] = LoaderID.LOCAL_FILE_COPY
        ids[FileAction.MOVE] = LoaderID.LOCAL_FILE_MOVE
        ids[FileAction.DELETE] = LoaderID.LOCAL_FILE_DELETE
    }


    override fun rename(context: Context, path: String, name: String): AsyncTaskLoader<*>? {
        return null
    }

    override fun copy(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>? {
        return LocalCopyLoader(context as Activity, list, dest)
    }

    override fun move(context: Context, list: List<String>, dest: String): AsyncTaskLoader<*>? {
        return null
    }

    override fun delete(context: Context, list: List<String>): AsyncTaskLoader<*>? {
        return LocalFileDeleteLoader(context, list)
    }

    override fun createFolder(context: Context, path: String): AsyncTaskLoader<*>? {
        return LocalFolderCreateLoader(context, path)
    }

    override fun share(context: Context, paths: ArrayList<String>, dest: String): AsyncTaskLoader<*>? {
        return null
    }


//    private fun getSelectedDocumentFiles(
//        context: Context,
//        path: String?,
//        list: List<String>?
//    ): ArrayList<DocumentFile> {
//        var files = ArrayList<DocumentFile>()
//        if (SystemUtil.isSDCardPath(context, path)) {
//            val pickedDir = ExternalStorageLollipop(context).getSDFileLocation(path)
//            for (name in list!!) {
//                files.add(pickedDir.findFile(FilenameUtils.getName(name)))
//            }
//        } else {
//            files = extractDocumentFiles(path, list)
//        }
//        return files
//    }

    private fun extractDocumentFiles(path: String?, selectedFileNames: List<String>): ArrayList<DocumentFile> {
        val sourceFiles = ArrayList<DocumentFile>()
        val file = File(path)

        if (file.exists()) {
            val document = DocumentFile.fromFile(file)
            for (s in selectedFileNames) {
                val d = document.findFile(FilenameUtils.getName(s))
                if (d != null)
                    sourceFiles.add(d)
            }
        }
        return sourceFiles
    }
}
