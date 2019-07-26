package com.transcend.otg.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.transcend.otg.data.UrlRepository

class StatementViewModelFactory(private val urlRepository: UrlRepository)
    :ViewModelProvider.NewInstanceFactory(){

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return StatementViewModel(urlRepository) as T
    }

}