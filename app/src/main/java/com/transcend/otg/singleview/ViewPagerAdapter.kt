package com.transcend.otg.singleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.task.ImageLoaderTask
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.utilities.MainApplication
import kotlinx.android.synthetic.main.single_view_image_item.view.*
import java.io.File

class ViewPagerAdapter(val mContext : Context) : PagerAdapter(){

    var mList : List<FileInfo> = emptyList()

    fun update(items : List<FileInfo>){
        mList = items
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var view = LayoutInflater.from(mContext).inflate(R.layout.single_view_image_item, container, false)
        val imageview = view.findViewById<ImageView>(R.id.viewer_image)
        val item = mList.get(position)

        if (FileFactory().isLocalPath(item.path) || FileFactory().isSDCardPath(mContext, item.path)) {
            val file = File(item.path)
            Glide.with(view)
                .load(file)
                .placeholder(item.defaultIcon)
                .into(view.viewer_image)
        } else {
            val cache = MainApplication.thumbnailsCache?.get(item.path + Constant.thumbnailCacheTail)
            if (cache != null) {
                imageview.scaleType = ImageView.ScaleType.FIT_CENTER  //設定顯示格式
                imageview.setImageBitmap(cache)
            } else {
                val displaymetrics = DisplayMetrics()
                (mContext as ImageActivity).windowManager.defaultDisplay.getMetrics(displaymetrics)
                val mScreenW = displaymetrics.widthPixels
                val mScreenH = displaymetrics.heightPixels
                val thumbSize = Point(mScreenW, mScreenH)
                object : ImageLoaderTask(item.path, item.fileType, thumbSize) {
                    override fun onFinished(bitmap: Bitmap?) {
                        if (bitmap != null) {
                            MainApplication.thumbnailsCache?.put(item.path + Constant.thumbnailCacheTail, bitmap)  //將thumbnail記錄於Cache
                            imageview.scaleType = ImageView.ScaleType.FIT_CENTER  //設定顯示格式
                            imageview.setImageBitmap(bitmap)
                        }
                    }
                }.execute()
            }
        }
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        if (`object` is View)
            container.removeView(`object`)
    }

    override fun getItemPosition(`object`: Any): Int {
        return if (mList.contains(`object`)) {
            mList.indexOf(`object`)
        } else {
            POSITION_NONE
        }
    }

    fun getItemTitle(position: Int): String{
        if (position < mList.size)
            return mList[position].title
        return "No title"
    }
}