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
import com.transcend.otg.R
import com.transcend.otg.utilities.FileFactory
import java.io.File

abstract class LocalActionWithNotificationLoader(val mActivity: Activity, val mSrcs: List<String>, val mDest: String): LocalAbstractLoader(mActivity){
    var mThread: HandlerThread? = null
    var mHandler: Handler? = null
    var mWatcher: Runnable? = null

    var itemCount: Int = 0
    var itemTotal: Int = 0
    var mNotificationID: Int = -1

    protected fun checkTotalFileCount(action: String) {
        updateProgress(action, context.getString(R.string.loading), 0, 0) //顯示讀取中
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

    protected fun startProgressWatcher(target: File, total: Long) {
        mThread = HandlerThread(TAG)
        mThread!!.start()
        mHandler = Handler(mThread!!.getLooper())

        mWatcher = Runnable {
            val count = target.length()
            if (mHandler != null) {
                mHandler!!.postDelayed(mWatcher, 500)    //每一秒更新progress
                updateProgress(context.getString(R.string.copy), target.name, count, total)
            }
        }

        mHandler!!.post(mWatcher)
    }

    protected fun closeProgressWatcher() {
        if (mHandler != null) {
            mHandler!!.removeCallbacks(mWatcher)
            mHandler = null
        }
        if (mThread != null) {
            mThread!!.quit()
            mThread = null
        }
    }

    protected fun updateProgress(action: String, name: String, count: Long, total: Long) {
        val channelId = mNotificationID.toString()

        val max = if (count == total) 0 else 100
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
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
        FileFactory().getInstance().releaseNotificationID(mNotificationID)
    }
}