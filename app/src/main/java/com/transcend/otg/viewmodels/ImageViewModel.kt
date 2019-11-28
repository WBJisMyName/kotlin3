package com.transcend.otg.viewmodels

import android.app.Application
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant
import kotlin.concurrent.thread

class ImageViewModel(application: Application) : AndroidViewModel(application){
    private val repository = FileRepository(application)
    val items: LiveData<List<FileInfo>> = MutableLiveData<List<FileInfo>>().apply {
        this.value = ArrayList<FileInfo>()
    }

    var title = ObservableField<String>()
    var isLoading = ObservableBoolean()

    fun loadImageList(folderPath: String){
        thread {
            var list = repository.getFiles(folderPath, Constant.TYPE_IMAGE)
            (items as MutableLiveData).postValue(list)
        }
    }

    fun loadAllImageList(){
        try {
            val thread = Thread(Runnable {
                var list = repository.getAllFilesByType(Constant.TYPE_IMAGE)
                (items as MutableLiveData).postValue(list)
            })
            thread.start()
        } catch (e: Exception) {
            Log.e("FileRepository", e.toString())
        }
    }
}