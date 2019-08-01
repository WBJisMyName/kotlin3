package com.transcend.otg.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcend.otg.data.Url

class UrlDao {
    private val urls = MutableLiveData<Url>()


    fun addUrl(url: Url){
        urls.value = url
    }

    fun getUrls() = urls as LiveData<Url>

}