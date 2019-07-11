package com.wbj.kotlin3.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class UrlDao {
    private val urls = MutableLiveData<Url>()


    fun addUrl(url: Url){
        urls.value = url
    }

    fun getUrls() = urls as LiveData<Url>

}