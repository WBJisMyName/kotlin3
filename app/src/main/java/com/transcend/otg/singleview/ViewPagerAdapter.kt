package com.transcend.otg.singleview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
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
        val item = mList.get(position)

        val file = File(item.path)
        Glide.with(view)
            .load(file)
            .placeholder(item.defaultIcon)
            .into(view.viewer_image)

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