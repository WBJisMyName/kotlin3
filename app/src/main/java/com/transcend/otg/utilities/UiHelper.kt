package com.transcend.otg.utilities

import android.app.Activity
import android.content.res.Configuration
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

    //此處特別注意，如果長寬都為0會無法處理
    fun calculateGridItemWidth(): Int {
        val context = MainApplication.getInstance()!!

        val columnCount: Int
        val screenWidth: Int
        if (isPad()) {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenWidth =
                    if (Constant.mPortraitScreenWidth > Constant.mPortraitScreenHeight)
                        Constant.mPortraitScreenWidth
                    else
                        Constant.mPortraitScreenHeight
                columnCount = 8
            } else {
                screenWidth =
                    if (Constant.mPortraitScreenWidth < Constant.mPortraitScreenHeight)
                        Constant.mPortraitScreenWidth
                    else
                        Constant.mPortraitScreenHeight
                columnCount = 6
            }
        } else {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenWidth =
                    if (Constant.mPortraitScreenWidth > Constant.mPortraitScreenHeight)
                        Constant.mPortraitScreenWidth
                    else
                        Constant.mPortraitScreenHeight
                columnCount = 6
            } else {
                screenWidth =
                    if (Constant.mPortraitScreenWidth < Constant.mPortraitScreenHeight)
                        Constant.mPortraitScreenWidth
                    else
                        Constant.mPortraitScreenHeight
                columnCount = 3
            }
        }
        return screenWidth / columnCount
    }

    //回傳true就是平板、回傳false就是手機
    fun isPad(): Boolean {
        return (MainApplication.getInstance()!!.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}