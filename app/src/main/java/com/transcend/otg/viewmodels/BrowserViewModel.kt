package com.transcend.otg.viewmodels

import android.app.Application
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MimeUtil
import java.io.File

open class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    var mPath = Constant.LOCAL_ROOT //記錄當前路徑
    var mMediaType = -1
    var isLoading = ObservableBoolean(false)
    var isEmpty = ObservableBoolean(false)
    var isOnSelectMode = ObservableBoolean(false)

    var isCancelScanTask = false

    val repository = FileRepository(application)
    var progress = ObservableInt(View.GONE)

    var items = MutableLiveData<List<FileInfo>>()
    var searchItems = MutableLiveData<List<FileInfo>>()

    var imageItems =  MutableLiveData<List<FileInfo>>()
    var musicItems =  MutableLiveData<List<FileInfo>>()
    var videoItems =  MutableLiveData<List<FileInfo>>()
    var docItems =  MutableLiveData<List<FileInfo>>()

    fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    fun delete(fileInfo: FileInfo) {
        repository.delete(fileInfo)
    }

    fun deleteFilesFromParentPath(parent: String) {
        repository.deleteFilesFromParentPath(parent)
    }

    fun deleteAll(){
        repository.deleteAll()
    }

    fun deleteAll(type: Int){
        repository.deleteAll(type)
    }

    fun insertAll(fileInfos:List<FileInfo>){
        repository.insertAll(fileInfos)
    }

    fun setFolderScanned(path: String){
        repository.setFolderScanned(path, true)
    }

    fun getFileInfo(path: String): FileInfo{
        val file: FileInfo? = repository.getFileInfo(path)  //可能為空
        return file ?: FileInfo()
    }

    fun doLoadFiles(path: String){
        mPath = path
        isLoading.set(true)

        if (!mPath.equals(Constant.Storage_Device_Root)){   //選擇本地或SD卡路徑的根頁面
            val thread = Thread(Runnable {
                if (!getFileInfo(mPath).hasScanned)
                    scanFolderFiles(path)
                else
                    items.postValue(sort(repository.getAllFileInfos(path)))
            })
            thread.start()
        }
    }

    fun doRefresh(){
        val thread = Thread(Runnable {
            deleteFilesFromParentPath(mPath)
            scanFolderFiles(mPath)
        })
        thread.start()
    }

    fun doSearch(searchText: String, type: Int){
        isLoading.set(true)
        val thread = Thread(Runnable {
            if (type == Constant.TYPE_IMAGE || type == Constant.TYPE_MUSIC || type == Constant.TYPE_VIDEO || type == Constant.TYPE_DOC) {
                val list = sort(repository.getSearchFiles(searchText, type))
                searchItems.postValue(list)
            } else {
                val list = sort(repository.getSearchFiles(searchText, mPath))
                items.postValue(list)
            }
        })
        thread.start()
    }

    fun sort(list: List<FileInfo>): List<FileInfo>{
        return list.sortedWith(compareBy({it.fileType != 0}, {it.title}))   //先排資料夾，再照字母排
    }

    //撈資料夾擋按列表，撈完會post給liveData
    fun scanFolderFiles(parent: String){
        isLoading.set(true)

        if (parent.equals(Constant.LOCAL_ROOT) || parent.equals(Constant.SD_ROOT)){
            val fileInfo = FileInfo()
            fileInfo.path = parent
            fileInfo.title = if (parent.equals(Constant.LOCAL_ROOT)) Constant.LocalBrowserMainPageTitle else Constant.SDBrowserMainPageTitle
            fileInfo.hasScanned = true
            fileInfo.fileType = Constant.TYPE_DIR
            fileInfo.defaultIcon = R.drawable.ic_filelist_folder_grey
            fileInfo.infoIcon = R.drawable.ic_brower_listview_filearrow
            insert(fileInfo)
        }

        val localFile = File(parent)
        var insert_count = 0
        if (localFile.exists()) {
            val list = localFile.listFiles()
            if (list==null)
                return
            for (file in list) {
                if (isCancelScanTask){
                    isCancelScanTask = false
                    //直接撈資料可能造成檔案不完全
                    var list = repository.getAllFileInfos(parent)
                    var count = 0 //count表示撈幾次才正確(微秒)
                    while (insert_count != list.size && count < 30) {    //此處檢查撈到的資料跟insert的資料數量是否有一致，或3秒後跳出
                        list = repository.getAllFileInfos(parent)
                        Thread.sleep(100)
                        count++
                    }
                    items.postValue(sort(list))
                    setFolderScanned(parent)
                }

                if (file.name.toString().startsWith("."))
                    continue
                val info = FileInfo()
                info.title = file.name
                info.path = file.path
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
                insert(info)
                insert_count++
            }
        }

        //scan完直接撈資料，可能造成檔案不完全
        var list = repository.getAllFileInfos(parent)
        var count = 0 //count表示撈幾次才正確(微秒)
        while (insert_count != list.size && count < 30) {    //此處檢查撈到的資料跟insert的資料數量是否有一致，或3秒後跳出
            list = repository.getAllFileInfos(parent)
            Thread.sleep(100)
            count++
        }
        items.postValue(sort(list))
        setFolderScanned(parent)
    }
}