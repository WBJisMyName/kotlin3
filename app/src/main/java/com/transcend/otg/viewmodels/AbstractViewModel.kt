package com.transcend.otg.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant

open class AbstractViewModel(application: Application) : AndroidViewModel(application) {
    val repository = FileRepository(application)    //資料庫存取用

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

    fun deleteAllFromRoot(rootType: Int){   //From Constant.kt, 0 → Local；1 → SD；2 → OTG
        repository.deleteAllFromRoot(rootType)
    }

    fun insertAll(fileInfos:List<FileInfo>){
        repository.insertAll(fileInfos)
    }

    fun setFolderScanned(path: String){
        repository.setFolderScanned(path, true)
    }

    fun getAllFiles(parent: String): List<FileInfo>{
        return repository.getAllFileInfos(parent)
    }

    fun getFolderNames(parent: String): ArrayList<String>{
        val files = repository.getFiles(parent, Constant.TYPE_DIR)
        val names = ArrayList<String>()
        for (file in files){
            names.add(file.title)
        }
        return names
    }
}