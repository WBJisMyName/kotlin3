package com.transcend.otg.action.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader

class NullLoader(context: Context) : AsyncTaskLoader<Boolean>(context) {

    override fun loadInBackground(): Boolean? {
        return true
    }
}
