package com.transcend.otg.action.loader

import android.content.Context
import java.io.File
import java.io.IOException

class LocalFileDeleteLoader(context: Context, private val mPaths: List<String>) : LocalAbstractLoader(context) {

    override fun loadInBackground(): Boolean? {
        try {
            return delete()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    @Throws(IOException::class)
    private fun delete(): Boolean {
        var isSuccess: Boolean = true
        for (path in mPaths) {
            val target = File(path)
            if (target.isDirectory) {
                isSuccess = deleteDirectory(target)
                if (isSuccess)  //資料夾刪除完成，呼叫DB刪除該路徑底下所有資料
                    repository.deleteAllFilesUnderFolder(path)
            } else
                isSuccess = target.delete()

            if (isSuccess)  //檔案刪除完成，呼叫DB刪除該檔案
                repository.delete(path)
        }
        return isSuccess
    }

    private fun deleteDirectory(dir: File): Boolean {
        for (target in dir.listFiles()) {
            var isSuccess = false
            if (target.isDirectory)
                deleteDirectory(target)
            else {
                target.delete()
                if (isSuccess)  //檔案刪除完成，呼叫DB刪除該檔案
                    repository.delete(target.absolutePath)
            }
        }
        return dir.delete()
    }
}
