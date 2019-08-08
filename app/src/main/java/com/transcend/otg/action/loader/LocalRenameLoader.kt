package com.transcend.otg.action.loader

import android.content.Context
import java.io.File

class LocalRenameLoader(context: Context, private val mPath: String, private val mName: String) : LocalAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        return rename()
    }

    private fun rename(): Boolean {
        val target = File(mPath)
        val parent = target.getParentFile()
        val rename = File(parent, mName)
        var isSuccess =  if (target.exists()) target.renameTo(rename) else false

        if (isSuccess){
            repository.updateFileName(mPath, mName)
        }

        return isSuccess
    }
}
