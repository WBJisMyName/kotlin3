package com.transcend.otg.action.loader

import android.content.Context
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.UsbUtils
import java.io.File

class FolderCreateLoader(context: Context, private val mPath: String) : ActionAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        if (mPath.startsWith(Constant.LOCAL_ROOT))
            srcRoot = Root.Local
        else if ((Constant.SD_ROOT != null && mPath.startsWith(Constant.SD_ROOT!!)))
            srcRoot = Root.SD
        else
            srcRoot = Root.OTG

        if (srcRoot == Root.Local || srcRoot == Root.SD)
            return createLocalNewFolder()
        else
            return createOTGNewFolder()
    }

    private fun createLocalNewFolder(): Boolean {
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

    private fun createOTGNewFolder(): Boolean {
        val file = File(mPath)
        val name = file.name
        val parent = file.parentFile?.absolutePath ?: "/"
        val target = UsbUtils.usbFileSystem?.rootDirectory?.search(parent)
        if (target != null){
            val folder = target.createDirectory(name)
            return (folder != null)
        }
        return false
    }
}
