package com.transcend.otg.viewmodels

import android.app.Application
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt

class ActionLocateViewModel(application: Application) : BrowserViewModel(application) {

    var dropdownVisibility = ObservableInt(View.VISIBLE)//GONE = 8, Invisible = 4, visible = 0
    var dropdownArrowVisibility = ObservableInt(View.GONE)//GONE = 8, Invisible = 4, visible = 0

    var mDropdownList = ObservableField<List<String>>(ArrayList<String>())

    override fun doLoadFiles(path: String){
        mPath = path
        isLoading.set(true)

//        thread {
//            val list =
//                repository.getFiles(path, Constant.TYPE_DIR)
//            if (list.size == 0){    //無資料就進task處理
//                val task = FileLoaderTask(this)
//                task.execute(path)
//            } else { //有資料則直接post上去
//                items.postValue(list)
//                isEmpty.set(false)
//            }
//        }
    }
}