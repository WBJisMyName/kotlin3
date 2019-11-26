package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.transcend.otg.R
import com.transcend.otg.utilities.FileFactory
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class LocalCopyLoader(val activity: Activity, val srcs: List<String>, val dest: String): LocalActionWithNotificationLoader(activity, srcs, dest) {

    init {
        TAG = LocalCopyLoader::class.java.simpleName
        itemCount = 0
        itemTotal = mSrcs.size
        mNotificationID = FileFactory().getInstance().getNotificationID()
    }

    override fun loadInBackground(): Boolean? {
        try {
            checkTotalFileCount(context.getString(R.string.copy))
            return copy()
        } catch (e: IOException) {
            e.printStackTrace()
            closeProgressWatcher()
            updateResult(context.getString(R.string.copy), context.getString(R.string.error))
        }

        return false
    }

    private fun copy(): Boolean {
        itemCount = 0
        for (path in mSrcs) {
            val source = File(path)
            if (source.isDirectory)
                copyDirectory(source, mDest)
            else
                copyFile(source, mDest)
        }
        updateResult(context.getString(R.string.copy), context.getString(R.string.done))
        return true
    }

    private fun copyDirectory(source: File, destination: String) {
        val name = createUniqueName(source, destination)
        val target = File(destination, name)
        target.mkdirs()
        insertFile(target)  //insert directory

        (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))

        val files = source.listFiles()
        val path = target.path
        for (file in files!!) {
            if (file.isHidden)
                continue
            if (file.isDirectory)
                copyDirectory(file, path)
            else
                copyFile(file, path)
        }
    }

    private fun copyFile(source: File, destination: String) {
        val name = createUniqueName(source, destination)
        val target = File(destination, name)
        val total = source.length()
        startProgressWatcher(target, total)
        FileUtils.copyFile(source, target)
        insertFile(target)  //insert file

        closeProgressWatcher()
        (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))    //呼叫系統掃描該檔案
        itemCount++
        updateProgress(context.getString(R.string.copy), target.name, total, total)
    }
}