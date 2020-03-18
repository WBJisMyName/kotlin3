package com.transcend.otg.task

import android.os.AsyncTask
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.MimeUtil
import com.transcend.otg.utilities.UsbUtils

open class ScanOTGFilesTask: AsyncTask<Unit, Unit, Boolean>() {

    val repository = FileRepository(MainApplication.getInstance()!!) //存取資料庫用

    override fun doInBackground(vararg p0: Unit?): Boolean {
        Constant.otgMediaScanState = Constant.ScanState.SCANNING
        val root = UsbUtils.usbFileSystem?.rootDirectory
        if (root == null)
            return false
        scanOTGFolder(root)
        return true
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result ?: false){
            Constant.otgMediaScanState = Constant.ScanState.SCANNED
        }
    }

    private fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    fun scanOTGFolder(folder: UsbFile){
        insertFile(folder)
        val list = folder.listFiles()
        for (file in list){
            if (file.isDirectory)
                scanOTGFolder(file)
            else{
                if (file.name.toString().startsWith("."))
                    continue
                insertFile(file)
            }
        }
        repository.setFolderScanned(folder.absolutePath, true)
    }

    private fun insertFile(file: UsbFile){
        if (file.isRoot) {
            recordRootPath()
            return
        }
        val info = FileInfo()
        info.title = file.name
        info.path = file.absolutePath
        info.rootType = Constant.STORAGEMODE_OTG
        info.lastModifyTime = file.lastModified()
        info.size = if (!file.isDirectory) file.length else 0
        info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.absolutePath)
        if (file.parent != null)
            info.parent = (file.parent as UsbFile).absolutePath
        else
            info.parent = ""

        when(info.fileType) {
            Constant.TYPE_DIR -> {
                info.defaultIcon = R.drawable.ic_filelist_folder_grey
                info.infoIcon = R.drawable.ic_brower_listview_filearrow
            }
            Constant.TYPE_IMAGE -> {
                info.defaultIcon = R.drawable.ic_filelist_pic_grey
                info.smallMediaIconResId = 1
            }
            Constant.TYPE_MUSIC -> {
                info.defaultIcon = R.drawable.ic_filelist_mp3_grey
                info.smallMediaIconResId = R.drawable.ic_browser_lable_music
            }
            Constant.TYPE_VIDEO -> {
                info.defaultIcon = R.drawable.ic_filelist_video_grey
                info.smallMediaIconResId = R.drawable.ic_cameraroll_video
            }
            else -> {
                info.defaultIcon = R.drawable.ic_filelist_others_grey
            }
        }
        insert(info)
    }

    private fun recordRootPath(){
        val fileInfo = FileInfo()
        fileInfo.path = "/"
        fileInfo.title = "Root"
        fileInfo.rootType = Constant.STORAGEMODE_OTG
        fileInfo.fileType = Constant.TYPE_DIR
        fileInfo.defaultIcon = R.drawable.ic_filelist_folder_grey
        fileInfo.infoIcon = R.drawable.ic_brower_listview_filearrow
        insert(fileInfo)
    }
}