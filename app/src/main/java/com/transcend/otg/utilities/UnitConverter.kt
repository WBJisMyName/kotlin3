package com.transcend.otg.utilities

import android.content.Context
import java.lang.ref.WeakReference

class UnitConverter(context: Context?) {
    companion object {

        private lateinit var mContext: WeakReference<Context?>

        /**
         * Covert px to dp
         * @param px
         * @return dp
         */

    }

    init {
        mContext = WeakReference(context)
    }

    fun convertPixelToDp(px: Float): Float {
        var context: Context? = mContext.get()
        if (context != null) {//判斷有無被系統GC
            context = mContext.get()
            //可以執行到這，就表示 context 還未被系統回收，可繼續做接下來的任務
        } else
            return px

        return px * 160 / 320 * getDensity(context!!)  //微調數據
    }

    /**
     * Covert pt to sp
     * @param pt
     * @return sp
     */
    fun convertPtToSp(pt: Float): Float {
        return pt / 100 * 45    //微調數據
    }

    /**
     * 取得螢幕密度
     * 120dpi = 0.75
     * 160dpi = 1 (default)
     * 240dpi = 1.5
     * @param context
     * @return
     */
    fun getDensity(context: Context): Float {
        val metrics = context.resources.displayMetrics
        return metrics.density
    }


}
