package com.transcend.otg.data

class UrlRepository private constructor(private val urlDao: UrlDao){

    fun addUrl(url:Url){
        urlDao.addUrl(url)
    }

    fun getUrls() = urlDao.getUrls()


    companion object{
        @Volatile private var instance : UrlRepository?=null

        fun getInstance(urlDao: UrlDao) =
            instance ?: synchronized(this){
                instance ?: UrlRepository(urlDao).also { instance = it }
            }
    }
}