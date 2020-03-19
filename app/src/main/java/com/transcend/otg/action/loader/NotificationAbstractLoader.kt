package com.transcend.otg.action.loader

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.core.app.NotificationCompat
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.R
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.utilities.UsbUtils
import java.io.File

abstract class NotificationAbstractLoader(val mActivity: Activity, val mSrcs: List<String>, val mDest: String): ActionAbstractLoader(mActivity){
    var mThread: HandlerThread? = null
    var mHandler: Handler? = null
    var mWatcher: Runnable? = null

    var itemCount: Int = 0
    var itemTotal: Int = 0
    var mNotificationID: Int = -1

    //失敗時方便刪除用
    var current_file_path: String? = null

    protected fun deleteFailedFile(){
        if (current_file_path == null)
            return
        when(destRoot){
            Root.Local, Root.SD -> {
                val file = File(current_file_path)
                if (file != null && file.exists())
                    file.delete()
            }
            Root.OTG -> {
                val file = UsbUtils.usbFileSystem?.rootDirectory?.search(current_file_path!!)
                if (file != null)
                    file.delete()
            }
        }
    }

    protected fun checkTotalFileCount(action: String) {
        updateProgress(action, context.getString(R.string.loading), 0, 0) //顯示讀取中
        itemTotal = 0
        when(srcRoot){
            Root.Local, Root.SD -> {
                for (path in mSrcs) {
                    val source = File(path)
                    if (source.isDirectory)
                        checkDirectory(source)
                    else
                        itemTotal++
                }
            }
            Root.OTG -> {
                for (path in mSrcs) {
                    val source = UsbUtils.usbFileSystem?.rootDirectory?.search(path)
                    if (source?.isDirectory ?: false)
                        checkDirectory(source)
                    else
                        itemTotal++
                }
            }
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

    protected fun checkDirectory(source: UsbFile?) {
        val files = source?.listFiles()
        if (files == null || files.size==0)
            return
        for (file in files) {
            if (file.isDirectory)
                checkDirectory(file)
            else
                itemTotal++
        }
    }

    protected fun startProgressWatcher(target: File, total: Long, action: String) {
        mThread = HandlerThread(TAG)
        mThread!!.start()
        mHandler = Handler(mThread!!.getLooper())

        mWatcher = Runnable {
            val count = target.length()
            if (mHandler != null) {
                mHandler!!.postDelayed(mWatcher, 500)    //每一秒更新progress
                updateProgress(action, target.name, count, total)
            }
        }

        mHandler!!.post(mWatcher)
    }

    protected fun startProgressWatcher(target: UsbFile, total: Long, action: String) {
        mThread = HandlerThread(TAG)
        mThread!!.start()
        mHandler = Handler(mThread!!.getLooper())

        mWatcher = Runnable {
            val count = target.length
            if (mHandler != null) {
                mHandler!!.postDelayed(mWatcher, 500)    //每一秒更新progress
                updateProgress(action, target.name, count, total)
            }
        }

        mHandler!!.post(mWatcher)
    }

    var waiting_count = 0
    protected fun startProgressWatcher(name: String, text: String) {
        mThread = HandlerThread(TAG)
        mThread!!.start()
        mHandler = Handler(mThread!!.getLooper())

        mWatcher = Runnable {
            if (mHandler != null) {
                mHandler!!.postDelayed(mWatcher, 1000)    //每一秒更新progress
                var dot: String = ""
                for (i in 0 .. (waiting_count)){
                    dot = dot + "."
                }
                waiting_count = 3 % waiting_count++
                updateProgress(name, text + dot)
            }
        }

        mHandler!!.post(mWatcher)
    }

    protected fun closeProgressWatcher() {
        if (mHandler != null) {
            mHandler!!.removeCallbacks(mWatcher)
            Thread.sleep(500)
            mHandler = null
        }
        if (mThread != null) {
            mThread!!.quit()
            Thread.sleep(500)
            mThread = null
        }
    }

    protected fun updateProgress(action: String, name: String, count: Long, total: Long) {
        val channelId = mNotificationID.toString()

        val max = 100
        var progress = 0
        if (total > 100 && count > 0)
            progress = if (total > 0) (count / (total / 100)).toInt() else 0
        val indeterminate = total == 0L
        val icon = R.drawable.icon_elite_logo

        val type = action
        //        String stat = String.format("%s / %s", MathUtils.getBytes(count), MathUtils.getBytes(total));
        val stat = String.format("%s / %s", itemCount, itemTotal)
        val text = String.format("%s - %s", type, stat)
        val info = String.format("%d%%", progress)

        val ntfMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = mActivity.getIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(icon)
        builder.setContentTitle(name)
        builder.setContentText(text)
        builder.setContentInfo(info)
        builder.setProgress(max, progress, indeterminate)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                TAG,
                NotificationManager.IMPORTANCE_LOW
            )
            ntfMgr.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }
        ntfMgr.notify(mNotificationID, builder.build())
    }

    protected fun updateProgress(name: String, text: String) {
        val channelId = mNotificationID.toString()

        val icon = R.drawable.icon_elite_logo

        val ntfMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = mActivity.getIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(icon)
        builder.setContentTitle(name)
        builder.setContentText(text)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                TAG,
                NotificationManager.IMPORTANCE_LOW
            )
            ntfMgr.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }
        ntfMgr.notify(mNotificationID, builder.build())
    }

    protected fun updateResult(action: String, result: String) {
        val channelId = mNotificationID.toString()
        val icon = R.drawable.icon_elite_logo
        val name = context.resources.getString(R.string.app_name)
        val type = action
        val text = String.format("%s - %s", type, result)

        val ntfMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = mActivity.getIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(icon)
        builder.setContentTitle(name)
        builder.setContentText(text)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                TAG,
                NotificationManager.IMPORTANCE_LOW
            )
            ntfMgr.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }
        ntfMgr.notify(mNotificationID, builder.build())
        FileFactory.getInstance().releaseNotificationID(mNotificationID)
    }
}