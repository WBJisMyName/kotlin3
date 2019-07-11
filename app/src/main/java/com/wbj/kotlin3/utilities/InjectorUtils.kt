package com.wbj.kotlin3.utilities

import com.wbj.kotlin3.data.UrlDatabase
import com.wbj.kotlin3.data.UrlRepository
import com.wbj.kotlin3.viewmodels.StatementViewModelFactory

object InjectorUtils {

    fun provideStatementViewModelFactory():StatementViewModelFactory{
        val urlRepository = UrlRepository.getInstance(UrlDatabase.getInstance().urlDao)
        return StatementViewModelFactory(urlRepository)
    }
}