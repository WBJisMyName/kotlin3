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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.RecyclerviewGriditemBinding
import com.transcend.otg.databinding.RecyclerviewListitemBinding
import com.transcend.otg.task.ImageLoaderTask
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FileInfoAdapter(recyclerViewClickCallback: RecyclerViewClickCallback, val viewModel: BrowserViewModel) : ListAdapter<FileInfo, FileInfoAdapter.ViewHolder>(FileInfoDiffCallback()) {

    companion object {
        val List = 1
        val Grid = 2
    }

    var mViewType: Int = List
    var mRecyclerViewClickCallback: RecyclerViewClickCallback? = recyclerViewClickCallback

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewType = getItemViewType(position)
        return when (viewType) {
            Grid -> ViewHolderGrid(RecyclerviewGriditemBinding.inflate(inflater))
            else -> ViewHolderList(RecyclerviewListitemBinding.inflate(inflater))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ViewHolderList)
            holder.bind(position)
        else if (holder is ViewHolderGrid)
            holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int = mViewType

    fun setViewType(viewType: Int){
        mViewType = viewType
        notifyDataSetChanged()
    }

    open inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view)
    inner class ViewHolderList(var listItemBinding: RecyclerviewListitemBinding) : ViewHolder(listItemBinding.root) {
        fun bind(position: Int){
            val item = getItem(position)

            listItemBinding.recyclerModel = item
            listItemBinding.recyclerViewModel = viewModel

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(item, position)
            }

            itemView.setOnLongClickListener{
                mRecyclerViewClickCallback?.onLongClick(item, position)
                true
            }

            listItemBinding.itemIcon.setImageResource(item.defaultIcon) //載入預設圖片
            listItemBinding.itemIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE  //設定顯示格式
            if (item.fileType == Constant.TYPE_IMAGE || item.fileType == Constant.TYPE_VIDEO) {
                Glide.with(itemView)
                    .load(File(item.path))
                    .placeholder(item.defaultIcon)
                    .transform(CenterInside(), CenterCrop())
                    .into(listItemBinding.itemIcon)
            } else if (item.fileType == Constant.TYPE_MUSIC) {
                //TODO 專輯圖片
                loadAlbumThumbnail(listItemBinding.itemIcon, item)
            } else {
                Glide.with(itemView)
                    .load(item.defaultIcon)
                    .centerInside()
                    .into(listItemBinding.itemIcon)
            }
        }
    }

    inner class ViewHolderGrid(var gridItemBinding: RecyclerviewGriditemBinding) : ViewHolder(gridItemBinding.root) {
        fun bind(position: Int){
            val item = getItem(position)

            gridItemBinding.recyclerModel = item
            gridItemBinding.recyclerViewModel = viewModel

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(item, position)
            }

            itemView.setOnLongClickListener{
                mRecyclerViewClickCallback?.onLongClick(item, position)
                true
            }

            if (item.fileType == Constant.TYPE_IMAGE || item.fileType == Constant.TYPE_VIDEO) {
                Glide.with(itemView)
                    .load(File(item.path))
                    .placeholder(item.defaultIcon)
                    .transform(CenterInside(), CenterCrop())
                    .into(gridItemBinding.itemIcon)
            } else if (item.fileType == Constant.TYPE_MUSIC) {
                //TODO 專輯圖片
                loadAlbumThumbnail(gridItemBinding.itemIcon, item)
            } else {
                Glide.with(itemView)
                    .load(item.defaultIcon)
                    .centerInside()
                    .into(gridItemBinding.itemIcon)
            }
        }
    }

    fun getTime(time: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(Date(time))
    }

    fun loadAlbumThumbnail(imageView: ImageView, fileInfo: FileInfo){
        imageView.setImageResource(fileInfo.defaultIcon) //載入預設圖片
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE  //設定顯示格式
        val cache = MainApplication.thumbnailsCache?.get(fileInfo.path + Constant.thumbnailCacheTail)
        if (cache != null) {
            imageView.setImageBitmap(cache)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            val thumbSize = Point(180, 180)
            object : ImageLoaderTask(fileInfo.path, fileInfo.fileType, thumbSize) {
                override fun onFinished(bitmap: Bitmap?) {
                    if (bitmap != null) {
                        MainApplication.thumbnailsCache?.put(
                            fileInfo.path + Constant.thumbnailCacheTail,
                            bitmap
                        )  //將thumbnail記錄於Cache
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            }.execute()
        }
    }

    fun getSelectedFiles(): List<FileInfo>{
        val mList = ArrayList<FileInfo>()
        if (itemCount > 0){
            for (file: FileInfo in currentList){
                if (file.isSelected)
                    mList.add(file)
            }
        }
        return mList
    }

    fun deselectAll(){
        if (itemCount > 0){
            for (file: FileInfo in currentList){
                if (file.isSelected) {
                    file.isSelected = false
                    notifyItemChanged(currentList.indexOf(file))
                }
            }

        }
    }

    fun selectAll(){
        if (itemCount > 0){
            for (file: FileInfo in currentList){
                if (file.isSelected.not()) {
                    file.isSelected = true
                    notifyItemChanged(currentList.indexOf(file))
                }
            }
        }
    }
}

private class FileInfoDiffCallback : DiffUtil.ItemCallback<FileInfo>() {
    override fun areItemsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem.path.equals(newItem.path)
    }

    override fun areContentsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem == newItem
    }
}