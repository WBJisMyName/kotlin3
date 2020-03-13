package com.transcend.otg.utilities

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.transcend.otg.bitmap.ThumbnailCache
import com.transcend.otg.browser.DropDownAdapter
import java.io.IOException

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

    fun getAddress(latitude: Double, longitude: Double): String?{
        try {
            val gc = Geocoder(mContext)
            val lstAddress: List<Address>? = gc.getFromLocation(latitude, longitude, 1)
            if (lstAddress != null && lstAddress.size != 0) { //取得部分地址
                //                lstAddress.get(0).getCountryName();  //台灣省
                //                lstAddress.get(0).getAdminArea();  //台北市
                //                lstAddress.get(0).getLocality();  //中正區
                //                lstAddress.get(0).getThoroughfare();  //信陽街 (包含路巷弄)
                //                lstAddress.get(0).getFeatureName();  //33 (號)
                //                lstAddress.get(0).getPostalCode();  //100 (郵遞區號)
                //取得全部地址
                var resultAddress = lstAddress[0].getAddressLine(0)
                if (lstAddress[0].postalCode != null) resultAddress =
                    resultAddress.replace(
                        lstAddress[0].postalCode,
                        ""
                    ) //過濾"郵遞區號"
                return resultAddress
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
