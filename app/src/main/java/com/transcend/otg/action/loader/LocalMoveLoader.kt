package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.transcend.otg.R
import com.transcend.otg.utilities.FileFactory
import java.io.File
import java.io.IOException

class LocalMoveLoader(activity: Activity, srcs: List<String>, dest: String): LocalActionWithNotificationLoader(activity, srcs, dest){

    init {
        TAG = LocalMoveLoader::class.java.simpleName
        itemCount = 0
        itemTotal = mSrcs.size
        mNotificationID = FileFactory().getInstance().getNotificationID()
    }

    override fun loadInBackground(): Boolean? {
        try {
            checkTotalFileCount(context.getString(R.string.move))
            return move()
        } catch (e: IOException) {
            e.printStackTrace()
            closeProgressWatcher()
            updateResult(context.getString(R.string.move), context.getString(R.string.error))
        }

        return false
    }

    @Throws(IOException::class)
    private fun move(): Boolean {
        itemCount = 0
        for (path in mSrcs) {
            val source = File(path)
            if (source.parent == mDest)
                continue
            if (source.isDirectory)
                moveDirectory(source, mDest)
            else
                moveFile(source, mDest)
        }
        updateResult(context.getString(R.string.move), context.getString(R.string.done))
        return true
    }

    @Throws(IOException::class)
    private fun moveDirectory(source: File, destination: String) {
        val name = createUniqueName(source, destination)
        val target = File(destination, name)
        target.mkdirs()
        mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))

        val files = source.listFiles()
        val path = target.path
        for (file in files!!) {
            if (file.isHidden)
                continue
            if (file.isDirectory)
                moveDirectory(file, path)
            else
                moveFile(file, path)
        }
        source.delete()

        deleteFile(source)
        insertFile(target)
    }

    @Throws(IOException::class)
    private fun moveFile(source: File, destination: String) {
        val name = createUniqueName(source, destination)
        val target = File(destination, name)
        val total = source.length().toInt().toLong()
        startProgressWatcher(target, total)

        deleteFile(source)  //資料庫中移除該筆資料
        source.renameTo(target)
        insertFile(target)  //插入新檔案資料

        closeProgressWatcher()
        mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
        itemCount++
        updateProgress(context.getString(R.string.copy), target.name, total, total)
    }
}