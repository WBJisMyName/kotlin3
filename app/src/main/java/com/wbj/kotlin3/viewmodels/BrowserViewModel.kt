package com.wbj.kotlin3.viewmodels

import android.app.Application
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.data.FileRepository
import com.wbj.kotlin3.task.FileLoaderTask
import com.wbj.kotlin3.utilities.Constant
import java.util.Comparator
import kotlin.concurrent.thread


class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    var mPath = Constant.LOCAL_ROOT
    var isLoading = ObservableBoolean(false)
    var isEmpty = ObservableBoolean(true)

    private val repository = FileRepository(application)
    var items = MutableLiveData<List<FileInfo>>().apply {
        this.value = ArrayList<FileInfo>()
    }
    var progress = ObservableInt(View.GONE)

    var isOnSelectMode = ObservableBoolean(false)

    fun getAllFileInfos(): LiveData<List<FileInfo>> {
        return repository.getAllFileInfos()
    }

    fun getAllFileInfos(parent: String){
        thread {
            var originalList = repository.getAllFileInfos(parent)
            val list = sort(originalList)
            items.postValue(list)
            isEmpty.set(list.size == 0)
        }
    }

    fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    fun delete(fileInfo: FileInfo) {
        repository.delete(fileInfo)
    }

    fun deleteAll(){
        repository.deleteAll()
    }

    fun insertAll(fileInfos:List<FileInfo>){
        repository.insertAll(fileInfos)
    }

    fun doLoadFiles(path: String){
        mPath = path
        isLoading.set(true)

        thread {
            val list =  sort(repository.getAllFileInfos(path))
            if (list.size == 0){    //無資料就進task處理
                val task = FileLoaderTask(this)
                task.execute(path)
            } else { //有資料則直接post上去
                items.postValue(list)
                isEmpty.set(false)
            }
        }
    }

    fun sort(list: List<FileInfo>): List<FileInfo>{
        return list.sortedWith(compareBy({it.fileType != 0}, {it.title}))   //先排資料夾，再照字母排
    }
}
