package com.transcend.otg.action.loader

import android.content.Context
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.UsbUtils
import java.io.File

class RenameLoader(context: Context, private val mPath: String, private val mName: String) : ActionAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        if (mPath.startsWith(Constant.LOCAL_ROOT))
            srcRoot = Root.Local
        else if ((Constant.SD_ROOT != null && mPath.startsWith(Constant.SD_ROOT!!)))
            srcRoot = Root.SD
        else
            srcRoot = Root.OTG

        if (srcRoot == Root.Local || srcRoot == Root.SD)
            return renameLocal()
        else if (srcRoot == Root.OTG)
            return renameOTG()
        return false
    }

    private fun renameLocal(): Boolean {
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

    private fun renameOTG(): Boolean {
        val target = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
        if (target != null){
            target.name = mName
            repository.updateFileName(mPath, mName)
            return true
        }
        return false
    }
}
