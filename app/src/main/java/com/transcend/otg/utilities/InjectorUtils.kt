package com.transcend.otg.utilities

import androidx.fragment.app.FragmentActivity
import com.transcend.otg.data.UrlDatabase
import com.transcend.otg.data.UrlRepository
import com.transcend.otg.viewmodels.StartPermissionViewModelFactory
import com.transcend.otg.viewmodels.StatementViewModelFactory

object InjectorUtils {

    fun provideStatementViewModelFactory():StatementViewModelFactory{
        val urlRepository = UrlRepository.getInstance(UrlDatabase.getInstance().urlDao)
        return StatementViewModelFactory(urlRepository)
    }

    fun provideStartPermissionViewModelFactory(activity: FragmentActivity): StartPermissionViewModelFactory{
        return StartPermissionViewModelFactory(activity)
    }
}