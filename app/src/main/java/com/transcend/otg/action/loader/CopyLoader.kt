package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileStreamFactory
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.utilities.UsbUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class CopyLoader(val activity: Activity, srcs: List<String>, dest: String): NotificationAbstractLoader(activity, srcs, dest) {

    init {
        TAG = CopyLoader::class.java.simpleName
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
            checkTotalFileCount(context.getString(R.string.copy))
            return copy()
        } catch (e: IOException) {
            e.printStackTrace()
            errorHandling()
        } catch (e: KotlinNullPointerException){
            e.printStackTrace()
            errorHandling()
        }
        return false
    }

    private fun errorHandling(){
        deleteFailedFile()
        closeProgressWatcher()
        updateResult(context.getString(R.string.copy), context.getString(R.string.error))
    }

    private fun copy(): Boolean {
        itemCount = 0
        when(srcRoot){
            Root.Local, Root.SD -> {
                for (path in mSrcs) {
                    val source = File(path)
                    if (source.isDirectory)
                        copyDirectory(source, mDest)
                    else
                        copyFile(source, mDest)
                }
            }
            Root.OTG -> {
                for (path in mSrcs) {
                    val source = UsbUtils.usbFileSystem?.rootDirectory?.search(path)
                    if (source != null) {
                        if (source.isDirectory)
                            copyDirectory(source, mDest)
                        else
                            copyFile(source, mDest)
                    }
                }
            }
        }
        updateResult(context.getString(R.string.copy), context.getString(R.string.done))
        return true
    }

    private fun copyDirectory(source: File, destination: String) {
        val name = createUniqueName(source, destination)

        var path: String? = null
        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                target.mkdirs()
                path = target.path
                insertFile(target)  //insert directory
                (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createDirectory(name)
                if (target == null) return
                path = target.absolutePath
                insertFile(target)  //insert directory
            }
        }
        if (path == null)   return

        val files = source.listFiles()
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
        val total = source.length()

        when(destRoot){
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                current_file_path = target.path
                startProgressWatcher(target, total, context.getString(R.string.copy))
                FileUtils.copyFile(source, target)
                insertFile(target)  //insert file
                (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))    //呼叫系統掃描該檔案
                updateProgress(context.getString(R.string.copy), target.name, total, total)
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createFile(name)
                current_file_path = target?.absolutePath
                if (target == null) return
                startProgressWatcher(target, total, context.getString(R.string.copy))
                val outputStream = UsbFileStreamFactory.createBufferedOutputStream(target, UsbUtils.usbFileSystem!!)
                FileUtils.copyFile(source, outputStream)
                outputStream.flush()
                outputStream.close()
                insertFile(target)  //insert file
                updateProgress(context.getString(R.string.copy), target.name, total, total)
            }
        }

        closeProgressWatcher()
        itemCount++
    }

    private fun copyDirectory(source: UsbFile, destination: String) {
        val name = createUniqueName(source, destination)

        var path: String? = null
        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                target.mkdirs()
                path = target.path
                insertFile(target)  //insert directory
                (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createDirectory(name)
                if (target == null) return
                path = target.absolutePath
                insertFile(target)  //insert directory
            }
        }
        if (path == null)   return

        val files = source.listFiles()
        for (file in files) {
            if (file.isDirectory)
                copyDirectory(file, path)
            else
                copyFile(file, path)
        }
    }

    private fun copyFile(source: UsbFile, destination: String) {
        val name = createUniqueName(source, destination)
        val total = source.length

        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                current_file_path = target.path
                startProgressWatcher(target, total, context.getString(R.string.copy))

                val inputStream: InputStream = UsbFileStreamFactory.createBufferedInputStream(source, UsbUtils.usbFileSystem!!)
                FileUtils.copyInputStreamToFile(inputStream, target)
                inputStream.close()
                insertFile(target)  //insert file
                (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))    //呼叫系統掃描該檔案
                updateProgress(context.getString(R.string.copy), target.name, total, total)
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createFile(name)
                current_file_path = target?.absolutePath
                if (target == null) return

                startProgressWatcher(target, total, context.getString(R.string.copy))
                val inputStream: InputStream = UsbFileStreamFactory.createBufferedInputStream(source, UsbUtils.usbFileSystem!!)
                val outputStream: OutputStream = UsbFileStreamFactory.createBufferedOutputStream(target, UsbUtils.usbFileSystem!!)
                IOUtils.copy(inputStream, outputStream)
                inputStream.close()
                outputStream.flush()
                outputStream.close()
                insertFile(target)  //insert file
                updateProgress(context.getString(R.string.copy), target.name, total, total)
            }
        }

        closeProgressWatcher()
        itemCount++
    }
}