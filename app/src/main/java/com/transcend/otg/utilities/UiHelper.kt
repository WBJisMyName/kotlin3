package com.transcend.otg.utilities

import android.app.Activity
import android.view.WindowManager

object UiHelper {
    fun setSystemBarTranslucent(mActivity: Activity) {
        val window = mActivity.window
        // Translucent status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )
        // Translucent navigation bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
    }
}