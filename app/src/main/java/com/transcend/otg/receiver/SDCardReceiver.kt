package com.transcend.otg.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.*

class SDCardReceiver: BroadcastReceiver(){
    private val TAG = SDCardReceiver::class.java.simpleName

    companion object {
        private val mObserver = ArrayList<SDCardObserver>()
        val instance by lazy(LazyThreadSafetyMode.NONE) { SDCardReceiver() }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val filter = IntentFilter(Intent.ACTION_MEDIA_MOUNTED)
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        filter.addDataScheme("file")

        when (intent.action) {
            Intent.ACTION_MEDIA_MOUNTED -> for (o in mObserver) {
                o.notifyMounted()
            }
            Intent.ACTION_MEDIA_UNMOUNTED -> for (o in mObserver) {
                o.notifyUnmounted()
            }
        }
    }

    interface SDCardObserver {
        fun notifyMounted()
        fun notifyUnmounted()
    }

    fun registerObserver(observer: SDCardObserver) {
        mObserver.add(observer)
    }

    fun unregisterObserver(observer: SDCardObserver) {
        mObserver.remove(observer)
    }
}