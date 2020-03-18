package com.transcend.otg.singleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.task.ImageLoaderTask
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
        val progress = view.findViewById<ProgressBar>(R.id.image_progress)
        val item = mList.get(position)

        if (FileFactory().isLocalPath(item.path) || FileFactory().isSDCardPath(mContext, item.path)) {
            val file = File(item.path)
            try{
                Glide.with(view)
                    .load(file)
                    .listener(object: RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            progress.setVisibility(View.GONE);
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            progress.setVisibility(View.GONE);
                            return false
                        }
                    })
                    .error(R.drawable.ic_filelist_pic_grey)
                    .into(view.viewer_image)
            } catch(e: IllegalArgumentException) {
                Log.e("Glide-tag", view.viewer_image.tag.toString())
            }
        } else {
            val cache = MainApplication.thumbnailsCache?.get(item.path)
            if (cache != null) {
                imageview.scaleType = ImageView.ScaleType.FIT_CENTER  //設定顯示格式
                imageview.setImageBitmap(cache)
            } else {
                val displaymetrics = DisplayMetrics()
                (mContext as ImageActivity).windowManager.defaultDisplay.getMetrics(displaymetrics)
                val mScreenW = displaymetrics.widthPixels
                val mScreenH = displaymetrics.heightPixels
                val thumbSize = Point(mScreenW, mScreenH)
                val task = object:ImageLoaderTask(item.path, imageview, item.fileType, thumbSize){
                    override fun onPostExecute(result: Bitmap?) {
                        progress.visibility = View.GONE
                        if (result != null) {
                            MainApplication.thumbnailsCache?.put(mPath, result)  //將thumbnail記錄於Cache
                            mImageView.scaleType = ImageView.ScaleType.FIT_CENTER  //設定顯示格式
                            mImageView.setImageBitmap(result)
                        } else {
                            mImageView.setImageResource(R.drawable.ic_filelist_pic_grey)
                        }
                    }
                }
                imageview.tag = task
                task.execute()
            }
        }
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        if (`object` is View) {
            container.removeView(`object`)
            val imageView = `object`.findViewById<ImageView>(R.id.viewer_image)
            if (imageView != null) {
                if ((imageView.tag is ImageLoaderTask)) {
                    val task = (imageView.tag as ImageLoaderTask)
                    task.cancel(true)
                }
            }
        }
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