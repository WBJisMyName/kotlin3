package com.transcend.otg.utilities

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.transcend.otg.bitmap.ThumbnailCache

class MainApplication: Application() {
    private var mThumbnails: ThumbnailCache? = null

    override fun onCreate() {
        super.onCreate()

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClassBytes = am.memoryClass * 1024 * 1024   //app內存限制大小
        mThumbnails = ThumbnailCache(memoryClassBytes / 4)

        mContext = applicationContext
    }

    //應用程序在不同的情況下進行自身的內存釋放，以避免被系統直接殺掉
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            mThumbnails!!.evictAll()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            mThumbnails!!.trimToSize(mThumbnails!!.size() / 2)
        }


    }

    companion object {
        lateinit var mContext: Context

        //取得Thumbnail cache，以便快速顯示
        val thumbnailsCache: ThumbnailCache?
            get() {
                val app = mContext.applicationContext as MainApplication
                return app.mThumbnails
            }
    }

    fun isPad() : Boolean{
        return mContext.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}
