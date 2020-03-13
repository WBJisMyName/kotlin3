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
import java.io.File
import java.io.IOException
import java.io.InputStream

class MoveLoader(activity: Activity, srcs: List<String>, dest: String): NotificationAbstractLoader(activity, srcs, dest){

    init {
        TAG = MoveLoader::class.java.simpleName
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
            checkTotalFileCount(context.getString(R.string.move))
            return move()
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
        updateResult(context.getString(R.string.move), context.getString(R.string.error))
    }

    @Throws(IOException::class)
    private fun move(): Boolean {
        itemCount = 0
        when(srcRoot){
            Root.Local, Root.SD -> {
                for (path in mSrcs) {
                    val source = File(path)
                    if (source.parent == mDest)
                        continue
                    if (source.isDirectory)
                        moveDirectory(source, mDest)
                    else
                        moveFile(source, mDest)
                }
            }
            Root.OTG -> {
                for (path in mSrcs) {
                    val source = UsbUtils.usbFileSystem?.rootDirectory?.search(path)
                    if (source != null) {
                        if (source.parent == null || source.parent!!.absolutePath.equals(mDest))
                            continue
                        if (source.isDirectory)
                            moveDirectory(source, mDest)
                        else
                            moveFile(source, mDest)
                    }
                }
            }
        }
        updateResult(context.getString(R.string.move), context.getString(R.string.done))
        return true
    }

    private fun moveDirectory(source: File, destination: String) {
        val name = createUniqueName(source, destination)

        var path: String? = null
        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                target.mkdirs()
                path = target.path
                insertFile(target)  //插入新檔案資料
                mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createDirectory(name)
                if (target == null) return
                path = target.absolutePath
                insertFile(target)  //插入新檔案資料
            }
        }
        if (path == null)   return

        val files = source.listFiles()
        for (file in files!!) {
            if (file.isHidden)
                continue
            if (file.isDirectory)
                moveDirectory(file, path)
            else
                moveFile(file, path)
        }
        source.delete()
        deleteFile(source)  //資料庫中移除該筆資料

    }

    private fun moveFile(source: File, destination: String) {
        val name = createUniqueName(source, destination)
        val total = source.length()

        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                current_file_path = target.path
                startProgressWatcher(target, total, context.getString(R.string.move))

                FileUtils.copyFile(source, target)
                source.delete()
                deleteFile(source)  //資料庫中移除該筆資料
                insertFile(target)  //插入新檔案資料
                mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
                updateProgress(context.getString(R.string.move), target.name, total, total)
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createFile(name)
                current_file_path = target?.absolutePath
                if (target == null) return
                startProgressWatcher(target, total, context.getString(R.string.move))
                val outputStream = UsbFileStreamFactory.createBufferedOutputStream(target, UsbUtils.usbFileSystem!!)
                FileUtils.copyFile(source, outputStream)
                outputStream.flush()
                outputStream.close()
                source.delete()
                insertFile(target)  //insert file
                deleteFile(source)  //資料庫中移除該筆資料
                updateProgress(context.getString(R.string.move), target.name, total, total)
            }
        }

        closeProgressWatcher()
        itemCount++
    }

    private fun moveDirectory(source: UsbFile, destination: String) {
        val name = createUniqueName(source, destination)

        var path: String? = null
        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                target.mkdirs()
                path = target.path
                insertFile(target)  //插入新檔案資料
                mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))
            }
            Root.OTG -> {
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)?.createDirectory(name)
                if (target == null) return
                path = target.absolutePath
                insertFile(target)  //插入新檔案資料
            }
        }
        if (path == null)   return

        val files = source.listFiles()
        for (file in files!!) {
            if (file.isDirectory)
                moveDirectory(file, path)
            else
                moveFile(file, path)
        }
        source.delete()
        deleteFile(source)  //資料庫中移除該筆資料

    }

    private fun moveFile(source: UsbFile, destination: String) {
        val name = createUniqueName(source, destination)
        val total = source.length

        when(destRoot) {
            Root.Local, Root.SD -> {
                val target = File(destination, name)
                current_file_path = target.path
                startProgressWatcher(target, total, context.getString(R.string.move))

                val inputStream: InputStream = UsbFileStreamFactory.createBufferedInputStream(source, UsbUtils.usbFileSystem!!)
                FileUtils.copyInputStreamToFile(inputStream, target)
                inputStream.close()
                source.delete()
                deleteFile(source)  //資料庫中移除該筆資料
                insertFile(target)  //insert file
                (mActivity).sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)))    //呼叫系統掃描該檔案
                updateProgress(context.getString(R.string.move), target.name, total, total)
            }
            Root.OTG -> {
                deleteFile(source)  //資料庫中移除該筆資料
                val target = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)
                current_file_path = target?.absolutePath
                if (target == null) return
                source.moveTo(target)
                insertFile(target)  //insert file
                updateProgress(context.getString(R.string.move), target.name, total, total)
            }
        }

        closeProgressWatcher()
        itemCount++
    }
}