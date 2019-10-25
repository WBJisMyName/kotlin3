package com.transcend.otg.browser

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class PagerSwipeRefreshLayout : SwipeRefreshLayout {

    private var mPrevx: Float = 0.toFloat()
    private var mPrevy: Float = 0.toFloat()

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mPrevx = MotionEvent.obtain(ev).x
                mPrevy = MotionEvent.obtain(ev).y
            }
            MotionEvent.ACTION_MOVE -> {
                val evX = ev.x
                val evy = ev.y
                val xDiff = Math.abs(evX - mPrevx)
                val yDiff = Math.abs(evy - mPrevy)

                if (xDiff > yDiff) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
