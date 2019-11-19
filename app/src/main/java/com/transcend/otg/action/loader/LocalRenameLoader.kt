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

        if (target.exists()){
            if (target.renameTo(rename)) {
                repository.updateFileName(mPath, mName)
                return true
            }
        }

        return false
    }
}
