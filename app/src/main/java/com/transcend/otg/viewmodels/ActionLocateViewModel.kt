package com.transcend.otg.viewmodels

import android.app.Application
import com.transcend.otg.task.FileLoaderTask
import com.transcend.otg.utilities.Constant
import kotlin.concurrent.thread

class ActionLocateViewModel(application: Application) : BrowserViewModel(application) {

    override fun doLoadFiles(path: String){
        mPath = path
        livePath.postValue(mPath)
        isLoading.set(true)

        thread {
            val list =
                repository.getFiles(path, Constant.TYPE_DIR)
            if (list.size == 0){    //無資料就進task處理
                val task = FileLoaderTask(this)
                task.execute(path)
            } else { //有資料則直接post上去
                items.postValue(list)
                isEmpty.set(false)
            }
        }
    }
}