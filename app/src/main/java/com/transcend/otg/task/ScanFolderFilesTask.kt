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
import java.io.File

open class ScanFolderFilesTask(val isLocal: Boolean, val parent: String): AsyncTask<Int, Unit, List<FileInfo>>(){

    val repository = FileRepository(MainApplication.getInstance()!!) //存取資料庫用
    var mType : Int = -1    //判斷是否只取出單一類型的檔案，-1表示全取

    override fun doInBackground(vararg type: Int?): List<FileInfo> {
        if (type.size > 0)
            mType = type[0] ?: -1

        //根目錄判斷 & 處理
        recordRootPath()

        val fileList = ArrayList<FileInfo>()
        if (isLocal){
            val localFile = File(parent)
            if (localFile.exists()) {
                val list = localFile.listFiles()
                if (list==null)
                    return ArrayList()
                for (file in list) {
                    if (file.name.toString().startsWith("."))
                        continue
                    val info = FileInfo()
                    info.title = file.name
                    info.path = file.path
                    info.rootType = if (file.path.startsWith(Constant.LOCAL_ROOT)) Constant.STORAGEMODE_LOCAL else Constant.STORAGEMODE_SD
                    info.lastModifyTime = file.lastModified()
                    info.size = file.length()
                    info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.path)
                    if (file.parent != null)
                        info.parent = file.parent
                    else
                        info.parent = ""

                    when(info.fileType){
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
                    if (info.fileType == mType || mType == -1)
                        fileList.add(info)
                }
            }
        } else {
            val target = UsbUtils.usbFileSystem?.rootDirectory?.search(parent)
            if (target != null){
                if (target.isDirectory){
                    val list = target.listFiles()
                    for (file in list){
                        if (file.name.toString().startsWith("."))
                            continue
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

                        when(info.fileType){
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
                        if (info.fileType == mType || mType == -1)
                            fileList.add(info)
                    }
                }
            }
        }
        return fileList
    }

    private fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    private fun recordRootPath(){
        //若掃描路徑為根目錄，則將insert進資料庫(若不如此，則無法知道根目錄是否掃描過了)
        if (parent.equals(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && parent.equals(Constant.SD_ROOT)) || parent.equals("/")){
            val fileInfo = FileInfo()
            fileInfo.path = parent
            fileInfo.title = "Root"
            if (parent.equals(Constant.LOCAL_ROOT))
                fileInfo.rootType = Constant.STORAGEMODE_LOCAL
            else if((Constant.SD_ROOT != null && parent.equals(Constant.SD_ROOT)))
                fileInfo.rootType = Constant.STORAGEMODE_SD
            else
                fileInfo.rootType = Constant.STORAGEMODE_OTG
            fileInfo.fileType = Constant.TYPE_DIR
            fileInfo.defaultIcon = R.drawable.ic_filelist_folder_grey
            fileInfo.infoIcon = R.drawable.ic_brower_listview_filearrow
            insert(fileInfo)
        }
    }

    override fun onPostExecute(result: List<FileInfo>?) {
        super.onPostExecute(result)
        repository.setFolderScanned(parent, true)    //順利結束任務，更新為已掃描
    }
}