package com.transcend.otg.action.loader

import android.content.Context
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.Constant
import java.io.File

class LocalFolderCreateLoader(context: Context, private val mPath: String) : LocalAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        return createNewFolder()
    }

    private fun createNewFolder(): Boolean {
        val dir = File(mPath)
        var isSuccess = false
        if (!dir.exists()) {
            isSuccess = dir.mkdirs()
            if (isSuccess){
                var info = FileInfo()
                info.title = dir.name
                info.path = dir.path
                info.lastModifyTime = dir.lastModified()
                info.size = dir.length()
                info.fileType = Constant.TYPE_DIR
                if (dir.parent != null)
                    info.parent = dir.parent
                else
                    info.parent = ""

                info.defaultIcon = R.drawable.ic_filelist_folder_grey
                info.infoIcon = R.drawable.ic_brower_listview_filearrow

                repository.insert(info)
            }
        } else //檔案已存在
            isSuccess = true

        return isSuccess
    }
}
