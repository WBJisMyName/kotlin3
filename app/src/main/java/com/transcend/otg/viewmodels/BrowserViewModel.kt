package com.transcend.otg.viewmodels

import android.app.Application
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.transcend.otg.data.FileInfo
import com.transcend.otg.task.ScanFolderFilesTask
import com.transcend.otg.task.ScanMediaFiles
import com.transcend.otg.task.ScanOTGFilesTask
import com.transcend.otg.utilities.Constant

open class BrowserViewModel(application: Application) : AbstractViewModel(application) {

    var mPath = Constant.LOCAL_ROOT //記錄當前路徑
    var mRoot = Constant.LOCAL_ROOT
    var mMediaType = -1
    var isLoading = ObservableBoolean(false)
    var isEmpty = ObservableBoolean(false)
    var isOnSelectMode = ObservableBoolean(false)

    var items = MutableLiveData<List<FileInfo>>()   //檔案列表
    var scanTask: ScanFolderFilesTask? = null
    var scanOTGTask: ScanOTGFilesTask? = null

    fun getFileInfo(path: String): FileInfo{
        val file: FileInfo? = repository.getFileInfo(path)  //可能為空
        return file ?: FileInfo()
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
                    items.postValue(sort(repository.getMediaFiles(type, Constant.STORAGEMODE_SD)))
            } else if (root.equals(Constant.LOCAL_ROOT)){
                if (Constant.localMediaScanState[type] == Constant.ScanState.NONE)
                    scanMediaFiles(type, root)
                else    //已掃描過則直接post資料上去
                    items.postValue(sort(repository.getMediaFiles(type, Constant.STORAGEMODE_LOCAL)))
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
        scanTask = object: ScanFolderFilesTask(isLocal, parent){
            override fun onPostExecute(result: List<FileInfo>?) {
                super.onPostExecute(result)
                if (result == null)
                    items.postValue(ArrayList<FileInfo>())
                else
                    items.postValue(sort(result))
            }
        }
        scanTask!!.execute(type)
    }

    fun cancelScanTask(){
        scanTask?.cancel(true)
        scanTask = null
        isLoading.set(false)
    }

    fun scanMediaFiles(type: Int, root: String){
        val scanTask = object: ScanMediaFiles(){
            override fun onFinished(list: List<FileInfo>) {
                val finalList = sort(list)
                items.postValue(finalList)
                if (root.equals(Constant.LOCAL_ROOT))
                    Constant.localMediaScanState[type] = Constant.ScanState.SCANNED
                else if (root.equals(Constant.SD_ROOT))
                    Constant.sdMediaScanState[type] = Constant.ScanState.SCANNED
            }
        }
        scanTask.scanMediaFiles(type, root)
    }

    fun scanOTGFiles(){
        if (Constant.otgMediaScanState == Constant.ScanState.NONE){
            scanOTGTask = object:ScanOTGFilesTask(){
                override fun onPostExecute(result: Boolean?) {
                    super.onPostExecute(result)
                    postItemValue()
                }
            }
            scanOTGTask!!.execute()
        } else{
            val thread = Thread(Runnable {
                while(Constant.otgMediaScanState != Constant.ScanState.SCANNED){

                }
                postItemValue()
            })
            thread.start()
        }
    }

    fun postItemValue(){
        val thread = Thread(Runnable {
            if (items.value == null)
                items.postValue(sort(repository.getMediaFiles(mMediaType, Constant.STORAGEMODE_OTG)))
        })
        thread.start()

    }
}