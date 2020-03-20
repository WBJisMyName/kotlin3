package com.transcend.otg.action.loader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.transcend.otg.R
import com.transcend.otg.task.ScanFolderFilesTask
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.io.IOException

class DecryptLoader(val activity: Activity, srcs: List<String>, dest: String, val decryptPassword: String): NotificationAbstractLoader(activity, srcs, dest){

    init{
        TAG = DecryptLoader::class.java.simpleName
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
            return decrypt()
        } catch (e: IOException) {
            updateResult(context.getString(R.string.decrypt), context.getString(R.string.error))
            val file = File(mDest)
            if (file.exists())
                file.delete()
            closeProgressWatcher()
            e.printStackTrace()
        } catch (e: ZipException) {
            updateResult(context.getString(R.string.decrypt), context.getString(R.string.incorrect_password))
            val file = File(mDest)
            if (file.exists())
                file.delete()
            closeProgressWatcher()
            e.printStackTrace()
        }
        return false
    }

    @Throws(IOException::class, ZipException::class)
    private fun decrypt(): Boolean {
        if (mSrcs.size == 0)    return false
        updateProgress(context.getString(R.string.decrypt), context.getString(R.string.loading))
        if (mDest.startsWith(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && mDest.startsWith(Constant.SD_ROOT!!))){
            val zipFile = ZipFile(mSrcs.get(0))
            if (zipFile.isEncrypted)
                zipFile.setPassword(decryptPassword)
            val extractFile = File(mDest)
            var b_mkdir = false
            if (!extractFile.exists()) b_mkdir = extractFile.mkdir()
            if (b_mkdir) {
                startProgressWatcher(context.getString(R.string.decrypt), context.getString(R.string.decrypting))
                zipFile.extractAll(extractFile.path)
            }
            mActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(extractFile)))
            val broadcastFiles: Array<File> = extractFile.listFiles()
            if (broadcastFiles != null) {
                for (file in broadcastFiles) {
                    if (file.isHidden) continue
                    if (file.isDirectory) checkDirectory(file) else mActivity.sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)
                        )
                    )
                }
            }
            closeProgressWatcher()
        }
        scanFolder()
        updateResult(context.getString(R.string.decrypt), context.getString(R.string.done))
        return true
    }

    fun scanFolder(){
        val isLocal = (destRoot == Root.Local || destRoot == Root.SD)
        ScanFolderFilesTask(isLocal, mDest).execute()
    }
}