package com.transcend.otg.viewmodels

import android.app.Application
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.*
import java.io.File

open class BrowserViewModel(application: Application) : AbstractViewModel(application) {

    var mPath = Constant.LOCAL_ROOT //記錄當前路徑
    var mRoot = Constant.LOCAL_ROOT
    var mMediaType = -1
    var isLoading = ObservableBoolean(false)
    var isEmpty = ObservableBoolean(false)
    var isOnSelectMode = ObservableBoolean(false)

    var isCancelScanTask = false    //判斷是否取消掃描檔案任務

    var items = MutableLiveData<List<FileInfo>>()   //檔案列表

    fun getFileInfo(path: String): FileInfo{
        val file: FileInfo? = repository.getFileInfo(path)  //可能為空
        return file ?: FileInfo()
    }

    fun getCurrentPathFiles(): List<FileInfo>{
        return getAllFiles(mPath)
    }

    fun doLoadFiles(path: String){  //讀取路徑
        mPath = path
        isLoading.set(true)

        //判斷來源
        var isLocal = false
        if (path.startsWith(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && path.startsWith(Constant.SD_ROOT!!)))
            isLocal = true

        if (!mPath.equals(Constant.Storage_Device_Root)){   //選擇本地或SD卡路徑的根頁面
            val thread = Thread(Runnable {
                if (!getFileInfo(mPath).hasScanned) //若未掃瞄過則開始掃描開路路徑
                    scanFolderFiles(isLocal, path)
                else    //已掃描過則直接post資料上去
                    items.postValue(sort(repository.getAllFileInfos(path)))
            })
            thread.start()
        }
    }

    //讀取資料夾
    fun doLoadFolders(path: String){
        mPath = path
        isLoading.set(true)

        //判斷來源
        var isLocal = false
        if (path.startsWith(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && path.startsWith(Constant.SD_ROOT!!)))
            isLocal = true

        if (!mPath.equals(Constant.Storage_Device_Root)){   //選擇本地或SD卡路徑的根頁面
            val thread = Thread(Runnable {
                if (!getFileInfo(mPath).hasScanned) //若未掃瞄過則開始掃描開路路徑
                    scanFolderFiles(isLocal, path, Constant.TYPE_DIR)
                else    //已掃描過則直接post資料上去
                    items.postValue(sort(repository.getFiles(path, Constant.TYPE_DIR)))
            })
            thread.start()
        }
    }

    //讀取媒體檔案，目前OTG不支援
    fun doLoadMediaFiles(type: Int, root: String){
        if (items.value?.isNotEmpty() ?: false) //空的才進行加載
            return

        isLoading.set(true)
        val thread = Thread(Runnable {
            if (Constant.SD_ROOT != null && root.equals(Constant.SD_ROOT)) { //若未掃瞄過則開始掃描開路路徑
                if (Constant.sdMediaScanState[type] == Constant.ScanState.NONE)
                    scanMediaFiles(type, root)
                else    //已掃描過則直接post資料上去
                    items.postValue(sort(repository.getAllFilesByTypeFromSrc(type, root)))
            } else if (root.equals(Constant.LOCAL_ROOT)){
                if (Constant.localMediaScanState[type] == Constant.ScanState.NONE)
                    scanMediaFiles(type, root)
                else    //已掃描過則直接post資料上去
                    items.postValue(sort(repository.getAllFilesByTypeFromSrc(type, root)))
            }
        })
        thread.start()
    }

    fun doReload(){ //重新讀取路徑
        val thread = Thread(Runnable {
            doLoadFiles(mPath)
        })
        thread.start()
    }

    fun doRefresh(isLocal: Boolean){    //重新整理路徑，刪除該資料夾下的檔案列表並重新掃描
        isLoading.set(true)
        val thread = Thread(Runnable {
            deleteFilesFromParentPath(mPath)    //刪除檔案

            //等待刪除完成
            var count = 0
            var list = repository.getAllFileInfos(mPath)
            while (list.size > 0 && count < 300){   //最久等待300秒
                list = repository.getAllFileInfos(mPath)
                Thread.sleep(100)
                count++
            }

            scanFolderFiles(isLocal, mPath)
        })
        thread.start()
    }

    fun doSearch(searchText: String, type: Int){
        isLoading.set(true)
        val thread = Thread(Runnable {
            var root: String = "/"
            if(mPath.startsWith(Constant.LOCAL_ROOT))
                root = Constant.LOCAL_ROOT
            else if (Constant.SD_ROOT != null && mPath.startsWith(Constant.SD_ROOT!!))
                root = Constant.SD_ROOT!!

            if (!root.equals("/")){   //Local & SD
                if (type == Constant.TYPE_IMAGE || type == Constant.TYPE_MUSIC || type == Constant.TYPE_VIDEO || type == Constant.TYPE_DOC) {
                    val list = sort(repository.getSearchFiles(searchText, type, root))
                    items.postValue(list)
                } else {
                    val list = sort(repository.getSearchFiles(searchText, mPath))
                    items.postValue(list)
                }
            } else {    //OTG
                val list = sort(repository.getSearchFiles(searchText, mPath))
                items.postValue(list)
            }
        })
        thread.start()
    }

    fun sort(list: List<FileInfo>): List<FileInfo>{
        return list.sortedWith(compareBy({it.fileType != 0}, {it.title}))   //先排資料夾，再照字母排
    }

    fun scanFolderFiles(isLocal: Boolean, parent: String){
        scanFolderFiles(isLocal, parent, -1)
    }

    //撈資料夾檔案列表，撈完會post給liveData
    fun scanFolderFiles(isLocal: Boolean, parent: String, type: Int){
        isLoading.set(true)

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

        var insert_count = 0
        if (isLocal){
            val localFile = File(parent)
            if (localFile.exists()) {
                val list = localFile.listFiles()
                if (list==null)
                    return
                for (file in list) {
                    if (isCancelScanTask){  //使用者點擊了返回鍵，flag升起
                        isCancelScanTask = false
                        postDataList(parent, insert_count, type)
                        //由於取消任務，此處不更新為已掃描
                    }

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
                    if (info.fileType == type || type == -1)
                        insert_count++
                }
            }
        } else {
            val target = UsbUtils.usbFileSystem?.rootDirectory?.search(parent)
            if (target != null){
                if (target.isDirectory){
                    val list = target.listFiles()
                    for (file in list){
                        if (isCancelScanTask){  //使用者點擊了返回鍵，flag升起
                            isCancelScanTask = false
                            postDataList(parent, insert_count, type)
                            //由於取消任務，此處不更新為已掃描
                        }

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
                        if (info.fileType == type || type == -1)
                            insert_count++
                    }
                }
            }
        }

        postDataList(parent, insert_count, type)
        setFolderScanned(parent)    //順利結束任務，更新為已掃描
    }

    fun scanMediaFiles(type: Int, root: String){
        val scanTask = object: ScanMediaFiles(MainApplication.getInstance()!!){
            override fun onFinished(list: List<FileInfo>) {
                val finalList = sort(list)
                items.postValue(finalList)
                if (root.equals(Constant.LOCAL_ROOT))
                    Constant.localMediaScanState[type] = Constant.ScanState.SCANNED
                else if (root.equals(Constant.SD_ROOT))
                    Constant.sdMediaScanState[type] = Constant.ScanState.SCANNED
            }
        }
        scanTask.scanFileList(type, root)
    }

    fun postDataList(path: String, insert_count: Int, type: Int){
        //scan完直接撈資料，可能造成檔案不完全
        var list = run {
            if(type == -1)
                repository.getAllFileInfos(path)
            else
                repository.getFiles(path, type)
        }
        var count = 0 //count表示撈幾次才正確(微秒)
        while (insert_count != list.size && count < 30) {    //此處檢查撈到的資料跟insert的資料數量是否有一致，或3秒後跳出
            if(type == -1)
                list = repository.getAllFileInfos(path)
            else
                list = repository.getFiles(path, type)
            Thread.sleep(100)
            count++
        }
        items.postValue(sort(list))
    }
}