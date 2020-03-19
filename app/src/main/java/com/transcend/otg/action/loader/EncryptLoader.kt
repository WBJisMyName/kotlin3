package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.UsbUtils
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EncryptLoader(activity: Activity, srcs: List<String>, dest: String, val encryptPassword: String): CopyLoader(activity, srcs, dest){
//傳來的dest為已包含.enc的完整路徑

    private var cacheDirDest = dest
    init {
        TAG = EncryptLoader::class.java.simpleName
        itemCount = 0
        itemTotal = mSrcs.size
        mNotificationID = FileFactory.getInstance().getNotificationID()

        if (dest.startsWith(Constant.LOCAL_ROOT))
            destRoot = Root.Local
        else if((Constant.SD_ROOT != null) && dest.startsWith(Constant.SD_ROOT!!))
            destRoot = Root.SD
        else
            destRoot = Root.OTG

        if (srcs.size != 0) {
            if (srcs[0].startsWith(Constant.LOCAL_ROOT))
                srcRoot = Root.Local
            else if ((Constant.SD_ROOT != null) && srcs[0].startsWith(Constant.SD_ROOT!!))
                srcRoot = Root.SD
            else
                srcRoot = Root.OTG
        }
    }

    override fun loadInBackground(): Boolean? {
        try {
            val isCreateCacheFolder = createCacheFolder()
            val isCopyToCacheFolder = copyToCacheDir()
            val isEncrypt = encrypt()

            return (isCreateCacheFolder && isCopyToCacheFolder && isEncrypt)
        } catch (e: IOException) {
            e.printStackTrace()
            updateResult(mActivity.getString(R.string.encrypt), mActivity.getString(R.string.error))
        } catch (e: ZipException) {
            e.printStackTrace()
            updateResult(mActivity.getString(R.string.encrypt), mActivity.getString(R.string.error))
        }
        //任務結束，刪除快取檔案
        val cacheFile = File(cacheDirDest)
        if (cacheFile.exists())
            cacheFile.delete()
        return false
    }

    override fun startProgressWatcher(file: File, total: Long) {
        startProgressWatcher(file, total, context.getString(R.string.encrypt))
    }

    override fun startProgressWatcher(file: UsbFile, total: Long) {
        startProgressWatcher(file, total, context.getString(R.string.encrypt))
    }

    override fun updateProgress(name: String, count: Long, total: Long) {
        updateProgress(context.getString(R.string.encrypt), name, count, total)
    }

    override fun updateResult(message: String) {
        updateResult(context.getString(R.string.encrypt), message)
    }

    private fun createCacheFolder(): Boolean{
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH_mm_ss")
        val currentDateTimeString = sdf.format(calendar.time)
        val cacheDir = MainApplication.getInstance()!!.getCacheDir()
        val target = File(cacheDir, currentDateTimeString)
        cacheDirDest = target.absolutePath
        return target.mkdirs()
    }

    private fun copyToCacheDir(): Boolean{
        checkTotalFileCount(context.getString(R.string.encrypting))
        itemCount = 0
        when(srcRoot){
            Root.Local, Root.SD -> {
                for (path in mSrcs) {
                    val source = File(path)
                    if (source.isDirectory)
                        copyDirectory(source, cacheDirDest)
                    else
                        copyFile(source, cacheDirDest)
                }
            }
            Root.OTG -> {
                for (path in mSrcs) {
                    val source = UsbUtils.usbFileSystem?.rootDirectory?.search(path)
                    if (source != null) {
                        if (source.isDirectory)
                            copyDirectory(source, cacheDirDest)
                        else
                            copyFile(source, cacheDirDest)
                    }
                }
            }
        }
        updateResult(context.getString(R.string.done))
        return true
    }

    private fun encrypt(): Boolean{
        updateProgress(context.getString(R.string.loading), 0, 0)

        try {
            val zipFile = ZipFile(mDest)
            val parameters = ZipParameters()
            parameters.compressionMethod = Zip4jConstants.COMP_DEFLATE
            parameters.compressionLevel = Zip4jConstants.DEFLATE_LEVEL_NORMAL
            parameters.isEncryptFiles = true
            parameters.encryptionMethod = Zip4jConstants.ENC_METHOD_STANDARD
            parameters.setPassword(encryptPassword)
            startProgressWatcher(context.getString(R.string.encrypt), context.getString(R.string.encrypting))
            zipFile.addFolder(cacheDirDest, parameters)
        } catch (e: Exception) {
            return false
        }
        val target = File(mDest)
        mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
        updateResult(context.getString(R.string.done))
        closeProgressWatcher()
        return true
    }
}