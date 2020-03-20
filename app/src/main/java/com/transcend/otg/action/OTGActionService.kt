package com.transcend.otg.action

import android.content.Context
import com.transcend.otg.action.loader.PhoneActionService
import com.transcend.otg.utilities.Constant

class OTGActionService: PhoneActionService(){
    init {
        TAG = OTGActionService::class.java.simpleName
        mRoot = Constant.OTG_ROOT
        mPath = Constant.OTG_ROOT
    }

    override fun getRootPath(context: Context): String? {
        if (mRoot == null)
            return "/"
        return mRoot
    }
}