package com.transcend.otg.utilities

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.transcend.otg.bitmap.ThumbnailCache
import com.transcend.otg.browser.DropDownAdapter

class MainApplication: Application() {
    private var mThumbnails: ThumbnailCache? = null
    var mDropdownAdapter: DropDownAdapter? = null


    override fun onCreate() {
        super.onCreate()

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClassBytes = am.memoryClass * 1024 * 1024   //app內存限制大小
        mThumbnails = ThumbnailCache(memoryClassBytes / 4)
        INSTANCE = this
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

        private var INSTANCE: MainApplication? = null
        fun getInstance(): MainApplication? {
            if (INSTANCE == null) {
                synchronized(MainApplication::class) {
                    INSTANCE = MainApplication()
                }
            }
            return INSTANCE
        }
    }

    fun getContext(): Context{
        return mContext
    }

    fun isPad() : Boolean{
        return mContext.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun OSisAfterNougat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    //設在這裡的原因為，binding後每次讀取都要新建一個adapter，導致無法監聽item click
    fun getDropdownAdapter(): DropDownAdapter{
        if (mDropdownAdapter == null)
            mDropdownAdapter = DropDownAdapter()
        return mDropdownAdapter as DropDownAdapter
    }
}
