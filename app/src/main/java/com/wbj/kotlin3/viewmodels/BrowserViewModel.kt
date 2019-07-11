package com.wbj.kotlin3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel;
import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.data.FileRepository


class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val fileInfos = repository.getAllFileInfos()


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
