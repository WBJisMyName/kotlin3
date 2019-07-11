package com.wbj.kotlin3.data

import android.app.Application
import androidx.lifecycle.LiveData
import android.os.AsyncTask

class FileRepository(application: Application) {

    private val fileInfoDatabase = FileInfoDatabase.getInstance(application)!!
    private val fileInfoDao: FileInfoDao = fileInfoDatabase.fileInfoDao()
    private val fileInfos: LiveData<List<FileInfo>> = fileInfoDao.getAllFileInfos()



    fun getAllFileInfos(): LiveData<List<FileInfo>> {
        return fileInfos
    }

    fun insert(fileInfo: FileInfo) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.insert(fileInfo) })
            thread.start()
        } catch (e: Exception) { }
    }

    fun delete(fileInfo: FileInfo) {
        try {
            val thread = Thread(Runnable {
                fileInfoDao.delete(fileInfo)
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