package com.transcend.otg.action

import android.content.Context
import com.transcend.otg.action.loader.PhoneActionService
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.SystemUtil

class SdcardActionService : PhoneActionService() {
    init {
        TAG = SdcardActionService::class.java.simpleName
        mRoot = Constant.SD_ROOT
        mPath = Constant.SD_ROOT
    }

    override fun getRootPath(context: Context): String? {
        if (null == mRoot || "" == mRoot)
            mRoot = SystemUtil().getSDLocation(context)
        return mRoot
    }

}
