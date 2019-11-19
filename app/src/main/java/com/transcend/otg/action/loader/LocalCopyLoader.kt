package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import com.transcend.otg.R
import com.transcend.otg.utilities.FileFactory
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class LocalCopyLoader(val mActivity: Activity, val mSrcs: List<String>, val mDest: String): LocalAbstractLoader(mActivity) {

    private var mThread: HandlerThread? = null
    private var mHandler: Handler? = null
    private var mWatcher: Runnable? = null

    private var mNotificationID: Int

    init {
        TAG = LocalCopyLoader::class.java.simpleName
        itemCount = 0
        itemTotal = mSrcs.size
        mNotificationID = FileFactory().getInstance().getNotificationID()
    }

    override fun loadInBackground(): Boolean? {
        try {
            checkTotalFileCount()
            return copy()
        } catch (e: IOException) {
            e.printStackTrace()
            closeProgressWatcher()
            updateResult(mActivity, mNotificationID, context.getString(R.string.error))
        }

        return false
    }

    protected fun checkTotalFileCount() {
        updateProgress(mActivity, mNotificationID, context.resources.getString(R.string.loading), 0, 0) //顯示讀取中
        itemTotal = 0
        for (path in mSrcs) {
            val source = File(path)
            if (source.isDirectory)
                checkDirectory(source)
            else
                itemTotal++
        }
    }

    protected fun checkDirectory(source: File) {
        val files = source.listFiles()
        for (file in files!!) {
            if (file.isHidden)
                continue
            if (file.isDirectory)
                checkDirectory(file)
            else
                itemTotal++
        }
    }

    private fun startProgressWatcher(target: File, total: Long) {
        mThread = HandlerThread(TAG)
        mThread!!.start()
        mHandler = Handler(mThread!!.getLooper())

        mWatcher = Runnable {
            val count = target.length()
            if (mHandler != null) {
                mHandler!!.postDelayed(mWatcher, 1000)    //每一秒更新progress
                updateProgress(mActivity, mNotificationID, target.name, count, total)
            }
        }

        mHandler!!.post(mWatcher)
    }

    private fun closeProgressWatcher() {
        if (mHandler != null) {
            mHandler!!.removeCallbacks(mWatcher)
            mHandler = null
        }
        if (mThread != null) {
            mThread!!.quit()
            mThread = null
        }
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
        updateResult(mActivity, mNotificationID, context.getString(R.string.done))
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
        updateProgress(mActivity, mNotificationID, target.name, total, total)
    }
}