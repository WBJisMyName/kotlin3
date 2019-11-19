package com.transcend.otg.action.loader

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.MimeUtil
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*

abstract class LocalAbstractLoader(context: Context) : AsyncTaskLoader<Boolean>(context) {

    var TAG = LocalAbstractLoader::class.java.simpleName
    val repository = FileRepository(MainApplication())
    var itemCount: Int = 0
    var itemTotal: Int = 0

    protected fun createUniqueName(source: File, destination: String): String {
        val isDirectory = source.isDirectory
        val dir = File(destination)
        val files = dir.listFiles { pathname -> pathname.isDirectory == isDirectory }
        val names = ArrayList<String>()
        for (file in files) names.add(file.name)
        val origin = source.name
        var unique = origin
        val ext = FilenameUtils.getExtension(origin)
        val prefix = FilenameUtils.getBaseName(origin)
        val suffix = if (ext.isEmpty()) "" else String.format(".%s", ext)
        var index = 1
        while (names.contains(unique)) {
            unique = String.format("$prefix (%d)$suffix", index++)
        }
        return unique
    }

    protected fun updateProgress(mActivity: Activity, notification_id: Int, name: String, count: Long, total: Long) {
        val channelId = notification_id.toString()

        val max = if (count == total) 0 else 100
        var progress = 0
        if (total > 100 && count > 0)
            progress = if (total > 0) (count / (total / 100)).toInt() else 0
        val indeterminate = total == 0L
        val icon = R.drawable.icon_elite_logo

        val type = context.resources.getString(R.string.copy)
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
        ntfMgr.notify(notification_id, builder.build())
    }

    protected fun updateResult(mActivity: Activity, notification_id: Int, result: String) {
        val channelId = notification_id.toString()
        val icon = R.drawable.icon_elite_logo
        val name = context.resources.getString(R.string.app_name)
        val type = context.resources.getString(R.string.copy)
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
        ntfMgr.notify(notification_id, builder.build())
        FileFactory().getInstance().releaseNotificationID(notification_id)
    }

    protected fun insertFile(file: File){
        val info = FileInfo()
        info.title = file.name
        info.path = file.path
        info.lastModifyTime = file.lastModified()
        info.size = file.length()
        if (file.parent != null)
            info.parent = file.parent
        else
            info.parent = ""

        info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.path)
        when(info.fileType){
            Constant.TYPE_DIR -> {
                info.defaultIcon = R.drawable.ic_filelist_folder_grey
                info.infoIcon = R.drawable.ic_brower_listview_filearrow
            }
            Constant.TYPE_IMAGE -> {
                info.defaultIcon = R.drawable.ic_filelist_pic_grey
            }
            Constant.TYPE_MUSIC -> {
                info.defaultIcon = R.drawable.ic_filelist_mp3_grey
            }
            Constant.TYPE_VIDEO -> {
                info.defaultIcon = R.drawable.ic_filelist_video_grey
            }
            else -> {
                info.defaultIcon = R.drawable.ic_filelist_others_grey
            }
        }
        repository.insert(info)
    }
}
