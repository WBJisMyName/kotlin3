package com.transcend.otg.viewmodels

import android.view.View
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel;
import com.transcend.otg.utilities.Constant

class MainActivityViewModel : ViewModel() {
    var dropdownVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0

}
