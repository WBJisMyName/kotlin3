package com.transcend.otg.viewmodels

import android.app.Application
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.transcend.otg.browser.DropDownAdapter

open class MainActivityViewModel(application: Application) : AbstractViewModel(application) {
    var dropdownVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0
    var dropdownArrowVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0
    var midTitleVisibility = ObservableInt(View.VISIBLE)//GONE = 8, Invisible = 4, visible = 0
    var midTitle = ObservableField<String>("Home")

    var midTitlePaddingLeft = ObservableInt(0)
    var midTitlePaddingRight = ObservableInt(0)

    var mDropdownList = ObservableField<List<String>>(ArrayList<String>())
    var mDropdownAdapter = DropDownAdapter()

    var mTabMode: TabMode = TabMode.Mid_Title_Only
    enum class TabMode{
        Mid_Title_Only, Browser
    }

    fun updateTabMode(mode: TabMode){
        when(mode){
            TabMode.Mid_Title_Only -> {
                dropdownVisibility.set(View.GONE)
                dropdownArrowVisibility.set(View.GONE)
                midTitleVisibility.set(View.VISIBLE)
            }
            TabMode.Browser -> {
                dropdownVisibility.set(View.VISIBLE)
                dropdownArrowVisibility.set(View.VISIBLE)
                midTitleVisibility.set(View.GONE)
            }
        }
        mTabMode = mode
    }

    fun setMidTitleText(text: String){
        midTitle.set(text)
    }

    //根據toolbar系統自建圖示來偏移標題以達至中效果
    fun updateSystemMenuIconCount(count: Int){
        if(count == 0){
            midTitlePaddingLeft.set(0)
            midTitlePaddingRight.set(175)
        } else {
            midTitlePaddingLeft.set((count - 1) * 175)
            midTitlePaddingRight.set(0)
        }
    }

}
