package com.wbj.kotlin3.viewmodels

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.data.FileRepository


class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val fileInfos = repository.getAllFileInfos()
    var progress = ObservableInt(View.GONE)


    fun getAllFileInfos(): LiveData<List<FileInfo>> {
        return this.fileInfos
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
}
