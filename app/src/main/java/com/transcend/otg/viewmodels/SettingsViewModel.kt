package com.transcend.otg.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class SettingsViewModel: ViewModel(){
    var cacheValue = ObservableField<String>("0 Byte")
    val cacheProgressVisible = ObservableBoolean(false)
}