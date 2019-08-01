package com.transcend.otg.viewmodels

import android.view.View
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;

class HelpViewModel : ViewModel() {
    var progressVisibility = ObservableInt(View.GONE)
    var webViewUrl = MutableLiveData<String>()
}
