package com.transcend.otg.information

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class InfoViewModel(application: Application): AndroidViewModel(application)  {

    var midTitle: String = "Information"
    val repository = MediaInfoRepository(application)
    var app = application

    fun getImageInfo(path: String): ImageInfo?{
        return repository.getImageInfo(path)
    }
}