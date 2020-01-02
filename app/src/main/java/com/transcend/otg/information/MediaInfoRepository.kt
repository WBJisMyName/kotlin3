package com.transcend.otg.information

import android.app.Application
import android.util.Log

class MediaInfoRepository(application: Application) {

    private val mediaInfoDatabase = MediaInfoDatabase.getInstance(application)!!
    private val mediaInfoDao: MediaInfoDao = mediaInfoDatabase.mediaInfoDao()

    private val imageInfoDatabase = ImageInfoDatabase.getInstance(application)!!
    private val imageInfoDao: ImageInfoDao = imageInfoDatabase.imageInfoDao()

    fun getMediaInfo(path: String): MediaInfo?{
        return mediaInfoDao.getFile(path)
    }
    fun getImageInfo(path: String): ImageInfo?{
        return imageInfoDao.getFile(path)
    }

    fun insert(fileInfo: MediaInfo) {
        try {
            val thread = Thread(Runnable {
                mediaInfoDao.insert(fileInfo)
            })
            thread.start()
        } catch (e: Exception) {
            Log.e("FileRepository", e.toString())
        }
    }

    fun insert(fileInfo: ImageInfo) {
        try {
            val thread = Thread(Runnable {
                imageInfoDao.insert(fileInfo)
            })
            thread.start()
        } catch (e: Exception) {
            Log.e("FileRepository", e.toString())
        }
    }

    fun deleteMediaInfo(path: String) {
        try {
            val thread = Thread(Runnable {
                mediaInfoDao.delete(path)
            })
            thread.start()
        } catch (e: Exception) { }
    }

    fun deleteImageInfo(path: String) {
        try {
            val thread = Thread(Runnable {
                imageInfoDao.delete(path)
            })
            thread.start()
        } catch (e: Exception) { }
    }
}