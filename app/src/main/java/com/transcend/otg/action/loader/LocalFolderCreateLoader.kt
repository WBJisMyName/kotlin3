package com.transcend.otg.action.loader

import android.content.Context
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
                insertFile(dir)
            }
        } else //檔案已存在
            isSuccess = true

        return isSuccess
    }
}
