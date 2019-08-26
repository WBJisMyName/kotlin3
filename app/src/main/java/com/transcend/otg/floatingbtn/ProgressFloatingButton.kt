package com.transcend.otg.floatingbtn

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

@CoordinatorLayout.DefaultBehavior(ProgressFloatingButton.Behavior::class)
class ProgressFloatingButton(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val TAG = "ProgressFloatingButton"

    private var mProgressBar: ProgressBar? = null
    private var mFab: FloatingActionButton? = null
    private var mTextView: TextView? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (childCount == 3){   //progress & fab btn
            var i = 0
            while (i < childCount){
                val view = getChildAt(i)
                if (view is ProgressBar) {  //progress
                    mProgressBar = view
                } else if (view is FloatingActionButton) {
                    mFab = view
                } else if (view is TextView) {
                    mTextView = view
                }
                i++
            }

            if (mFab != null && mProgressBar != null) {
                resize()
            } else
                Log.e(TAG, "Fab or ProgressBar is null!")
        } else
            Log.e(TAG, "Item count error")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (mFab != null && mProgressBar != null) {
            resize()
        }
    }

    private fun resize() {
//        val translationZpx =
//            resources.displayMetrics.density * 6 // 6 is needed for progress bar to be visible, 5 doesn't work
//        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
//            mProgressBar!!.translationZ = translationZpx
//
//        val mFabParams = mFab!!.layoutParams as FrameLayout.LayoutParams
//        val mProgressParams = mProgressBar!!.layoutParams as FrameLayout.LayoutParams
//
//        val additionSize = resources.getDimensionPixelSize(R.dimen.progress_bar_size)
//        mProgressBar!!.layoutParams.height = mFab!!.height + additionSize
//        mProgressBar!!.layoutParams.width = mFab!!.width + additionSize
//
//        mFabParams.gravity = Gravity.CENTER
//        mProgressParams.gravity = Gravity.CENTER
    }

    fun setText(text: String){
        mTextView?.setText(text)
    }

    fun setProgressMax(max: Int){
        mProgressBar?.max = max
    }

    fun setProgressValue(value: Int){
        mProgressBar?.progress = value
    }

    /**
     * 隨著Snackbar的高度作位移
     * source: https://lab.getbase.com/introduction-to-coordinator-layout-on-android/
     */
    class Behavior : CoordinatorLayout.Behavior<ProgressFloatingButton> {
        constructor() : super() {}

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

        override fun layoutDependsOn(parent: CoordinatorLayout, child: ProgressFloatingButton, dependency: View): Boolean {
            return dependency is Snackbar.SnackbarLayout
        }

        override fun onDependentViewChanged(parent: CoordinatorLayout, child: ProgressFloatingButton, dependency: View): Boolean {
            val translationY = Math.min(0f, (dependency.getTranslationY() - dependency.getHeight()))
            if (child.bottom > dependency.getTop()) {
                child.setTranslationY(translationY)
            }
            return true
        }
    }
}