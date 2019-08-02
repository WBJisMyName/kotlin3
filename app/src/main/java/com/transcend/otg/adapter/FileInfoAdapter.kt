package com.transcend.otg.adapter

import android.graphics.Bitmap
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.RecyclerviewListitemBinding
import com.transcend.otg.task.ImageLoaderTask
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.*

class FileInfoAdapter(recyclerViewClickCallback: RecyclerViewClickCallback, val viewModel: BrowserViewModel) : ListAdapter<FileInfo, FileInfoAdapter.ViewHolder>(FileInfoDiffCallback()) {

    companion object {
        val List = 1
        val Grid = 2
    }

    var mRecyclerViewClickCallback: RecyclerViewClickCallback? = recyclerViewClickCallback

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolderList(RecyclerviewListitemBinding.inflate(inflater))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ViewHolderList)
            holder.bind(getItem(position))
    }

    open inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view)
    inner class ViewHolderList(var listItemBinding: RecyclerviewListitemBinding) : ViewHolder(listItemBinding.root) {
        fun bind(item : FileInfo){
            listItemBinding.recyclerModel = item
            listItemBinding.recyclerViewModel = viewModel

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(item)
            }

            listItemBinding.itemIcon.setImageResource(item.defaultIcon) //載入預設圖片
            listItemBinding.itemIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE  //設定顯示格式
            if (isMedia(item.fileType)) {
                val cache = MainApplication.thumbnailsCache?.get(item.path + Constant.thumbnailCacheTail)
                if (cache != null) {
                    listItemBinding.itemIcon.setImageBitmap(cache)
                    listItemBinding.itemIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    val thumbSize = Point(180, 180)
                    object : ImageLoaderTask(item.path, item.fileType, thumbSize) {
                        override fun onFinished(bitmap: Bitmap?) {
                            if (bitmap != null) {
                                MainApplication.thumbnailsCache?.put(
                                    item.path + Constant.thumbnailCacheTail,
                                    bitmap
                                )  //將thumbnail記錄於Cache
                                listItemBinding.itemIcon.setImageBitmap(bitmap)
                                listItemBinding.itemIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        }
                    }.execute()
                }
            }
        }
    }

    fun getTime(time: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(Date(time))
    }

    fun isMedia(type: Int): Boolean{
        return type == Constant.TYPE_IMAGE || type == Constant.TYPE_VIDEO || type == Constant.TYPE_MUSIC
    }
}

private class FileInfoDiffCallback : DiffUtil.ItemCallback<FileInfo>() {
    override fun areItemsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem?.path.equals(newItem?.path)
    }

    override fun areContentsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem == newItem
    }
}