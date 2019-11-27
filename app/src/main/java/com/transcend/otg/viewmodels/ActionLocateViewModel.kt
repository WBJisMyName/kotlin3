package com.transcend.otg.viewmodels

import android.app.Application
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt

class ActionLocateViewModel(application: Application) : BrowserViewModel(application) {

    var dropdownVisibility = ObservableInt(View.VISIBLE)//GONE = 8, Invisible = 4, visible = 0
    var dropdownArrowVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0

    var mDropdownList = ObservableField<List<String>>(ArrayList<String>())
}