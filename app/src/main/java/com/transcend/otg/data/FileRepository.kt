package com.transcend.otg.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.transcend.otg.utilities.Constant
import java.io.File

class FileRepository(application: Application) {

    private val fileInfoDatabase = FileInfoDatabase.getInstance(application)!!
    private val fileInfoDao: FileInfoDao = fileInfoDatabase.fileInfoDao()

    fun getAllFilesByType(type: Int): LiveData<List<FileInfo>> {
        return fileInfoDao.getAllFilesByType(type)
    }

    fun getSearchFiles(searchText: String, type: Int): List<FileInfo>{
        if (type == Constant.TYPE_IMAGE || type == Constant.TYPE_MUSIC || type == Constant.TYPE_VIDEO || type == Constant.TYPE_DOC)
            return fileInfoDao.getSearchFilesByType("%"+searchText+"%", type)
        else
            return fileInfoDao.getSearchFiles("%"+searchText+"%")   //不管前後文，有符合的就撈出來
    }

    fun getAllFileInfos(parent: String): List<FileInfo> {
        return fileInfoDao.getAllFileInfos(parent)
    }

    fun getFiles(parent: String, type: Int): List<FileInfo>{
        return fileInfoDao.getFiles(parent, type)
    }

    fun insert(fileInfo: FileInfo) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.insert(fileInfo) })
            thread.start()
        } catch (e: Exception) {
            Log.e("FileRepository", e.toString())
        }
    }

    fun delete(fileInfo: FileInfo) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.delete(fileInfo)
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun delete(path: String) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.delete(path)
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun deleteFilesUnderFolderPath(path: String) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.deleteFilesUnderFolderPath(path+"%")    //尾端加%表示後面無論為何多長都算進去
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun deleteAll() {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.deleteAll()
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun deleteAll(type: Int) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.deleteAll(type)
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun updateFileName(oldPath: String, newName: String){
        try {
            val thread = Thread(Runnable {
                var fileInfo = fileInfoDao.getFile(oldPath)
                val newPath = File(oldPath).parent + "/" + newName
                fileInfo.title = newName
                fileInfo.path = newPath
                fileInfoDao.delete(oldPath)
                fileInfoDao.insert(fileInfo)
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun insertAll(fileInfos :List<FileInfo>){
        try {
            val thread = Thread(Runnable {
                fileInfoDao.deleteAll()//jerry
                fileInfoDao.insertAll(fileInfos)
            })
            thread.start()
        } catch (e: Exception) { }
    }

}