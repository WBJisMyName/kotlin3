package com.transcend.otg.utilities

import com.transcend.otg.data.UrlDatabase
import com.transcend.otg.data.UrlRepository
import com.transcend.otg.viewmodels.StatementViewModelFactory

object InjectorUtils {

    fun provideStatementViewModelFactory(): StatementViewModelFactory {
        val urlRepository = UrlRepository.getInstance(UrlDatabase.getInstance().urlDao)
        return StatementViewModelFactory(urlRepository)
    }
}