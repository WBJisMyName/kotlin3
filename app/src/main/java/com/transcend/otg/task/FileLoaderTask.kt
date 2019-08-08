package com.transcend.otg.task

import android.os.AsyncTask
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MimeUtil
import com.transcend.otg.viewmodels.BrowserViewModel
import java.io.File

class FileLoaderTask(val viewModel: BrowserViewModel) : AsyncTask<String, Unit, Unit>(){

    lateinit var parent: String

    override fun doInBackground(vararg params: String?) {
        parent = params[0]!!

        if (parent != null) {
            val localFile = File(parent)
            if (localFile.exists()) {
                var list = localFile.listFiles()
                if (list==null)
                    return
                for (file in list) {
                    if (file.name.toString().startsWith("."))
                        continue
                    var info = FileInfo()
                    info.title = file.name
                    info.path = file.path
                    info.lastModifyTime = file.lastModified()
                    info.size = file.length()
                    info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.path)
                    if (file.parent != null)
                        info.parent = file.parent
                    else
                        info.parent = ""

                    when(info.fileType){
                        Constant.TYPE_DIR -> {
                            info.defaultIcon = R.drawable.ic_filelist_folder_grey
                            info.infoIcon = R.drawable.ic_brower_listview_filearrow
                        }
                        Constant.TYPE_IMAGE -> {
                            info.defaultIcon = R.drawable.ic_filelist_pic_grey
                        }
                        Constant.TYPE_MUSIC -> {
                            info.defaultIcon = R.drawable.ic_filelist_mp3_grey
                        }
                        Constant.TYPE_VIDEO -> {
                            info.defaultIcon = R.drawable.ic_filelist_video_grey
                        }
                    }

                    viewModel.insert(info)
                }
            }
        }
    }

    override fun onPostExecute(result: Unit?) {
        viewModel.getAllFileInfos(parent)
    }
}