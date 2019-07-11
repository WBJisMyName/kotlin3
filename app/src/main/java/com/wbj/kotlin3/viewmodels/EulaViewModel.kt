package com.wbj.kotlin3.viewmodels

import android.view.View
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;

class EulaViewModel : ViewModel() {
    var buttonVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0
    var progressVisibility = ObservableInt(View.GONE)
    var url = MutableLiveData<String>()

}
