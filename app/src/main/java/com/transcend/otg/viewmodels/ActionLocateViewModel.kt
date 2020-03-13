package com.transcend.otg.viewmodels

import android.app.Application
import android.view.View
import androidx.databinding.ObservableInt

class ActionLocateViewModel(application: Application) : MainActivityViewModel(application) {
    var confirmBtnVisibility = ObservableInt(View.INVISIBLE)//GONE = 8, Invisible = 4, visible = 0
}