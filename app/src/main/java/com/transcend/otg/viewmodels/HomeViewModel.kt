package com.transcend.otg.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel

class HomeViewModel: ViewModel(){
    var localProgressThousandth = ObservableInt(0)  //千分之幾
    var localCapabilityText = ObservableField<String>("Loading...")
    var sdProgressThousandth = ObservableInt(0) //千分之幾
    var sdCapabilityText = ObservableField<String>("Loading...")
    var sdLayoutVisible = ObservableBoolean(false)
}