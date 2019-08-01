package com.transcend.otg.viewmodels

import android.view.View
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.transcend.otg.data.Url
import com.transcend.otg.data.UrlRepository

class StatementViewModel(private val urlRepository: UrlRepository) : ViewModel() {
    var progressVisibility = ObservableInt(View.GONE)
    var webViewUrl = MutableLiveData<String>()


    fun getUrls() = urlRepository.getUrls()

    fun addUrl(url: Url){
        urlRepository.addUrl(url)
    }
}
