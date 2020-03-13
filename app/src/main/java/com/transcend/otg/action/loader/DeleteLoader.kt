package com.transcend.otg.action.loader

import android.content.Context
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.UsbUtils
import java.io.File
import java.io.IOException

class DeleteLoader(context: Context, private val mPaths: List<String>) : ActionAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        if (mPaths.size == 0)
            return true

        if (mPaths[0].startsWith(Constant.LOCAL_ROOT))
            srcRoot = Root.Local
        else if ((Constant.SD_ROOT != null && mPaths[0].startsWith(Constant.SD_ROOT!!)))
            srcRoot = Root.SD
        else
            srcRoot = Root.OTG

        try {
            if (srcRoot == Root.Local || srcRoot == Root.SD)
                return deleteLocal()
            else
                return deleteOTG()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun deleteLocal(): Boolean {
        var isSuccess: Boolean = true
        for (path in mPaths) {
            val target = File(path)
            if (target.isDirectory) {
                isSuccess = deleteLocalDirectory(target)
                if (isSuccess)  //資料夾刪除完成，呼叫DB刪除該路徑底下所有資料
                    repository.deleteAllFilesUnderFolder(path)
            } else
                isSuccess = target.delete()
        }
        return isSuccess
    }

    private fun deleteLocalDirectory(dir: File): Boolean {
        if (dir.listFiles() == null)
            return true
        for (target in dir.listFiles()) {
            if (target.isDirectory) {
                deleteLocalDirectory(target)
            } else {
                target.delete()
            }
        }
        return dir.delete()
    }

    private fun deleteOTG(): Boolean {
        if (UsbUtils.usbDevice == null)
            return false
        var isSuccess: Boolean = true
        for (path in mPaths) {
            val target = UsbUtils.usbFileSystem?.rootDirectory?.search(path)
            if (target == null)
                continue
            if (target.isDirectory) {
                deleteOTGDirectory(target)
            } else
                target.delete()

            isSuccess = (UsbUtils.usbFileSystem?.rootDirectory?.search(path) == null)
            if (isSuccess)  //檔案刪除完成，呼叫DB刪除該檔案
                repository.deleteAllFilesUnderFolder(path)
        }
        return isSuccess
    }

    private fun deleteOTGDirectory(dir: UsbFile) {
        for (target in dir.listFiles()) {
            if (target.isDirectory)
                deleteOTGDirectory(target)
            else {
                target.delete()
            }
        }
        dir.delete()
    }
}
