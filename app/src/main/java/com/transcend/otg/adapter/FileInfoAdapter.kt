package com.transcend.otg.adapter

import android.graphics.Bitmap
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.RecyclerviewFootitemBinding
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

open class FileInfoAdapter(recyclerViewClickCallback: RecyclerViewClickCallback, val viewModel: BrowserViewModel) : RecyclerView.Adapter<FileInfoAdapter.ViewHolder>() {

    companion object {
        val Footer = 0
        val List = 1
        val Grid = 2
    }

    var mViewType: Int = List
    var mRecyclerViewClickCallback: RecyclerViewClickCallback? = recyclerViewClickCallback
    var mList = ArrayList<FileInfo>()

    var mLazyLoadCount = 30

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewType = getItemViewType(position)
        return when (viewType) {
            Grid -> ViewHolderGrid(RecyclerviewGriditemBinding.inflate(inflater))
            List -> ViewHolderList(RecyclerviewListitemBinding.inflate(inflater))
            else -> ViewHolderFooter(RecyclerviewFootitemBinding.inflate(inflater))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (isFooter(position))
            return
        if (holder is ViewHolderList)
            holder.bind(mList.get(position))
        else if (holder is ViewHolderGrid)
            holder.bind(mList.get(position))
    }

    fun submitList(list: List<FileInfo>){
        mList = ArrayList(list)
        notifyDataSetChanged()
    }

    fun isFooter(position: Int): Boolean {
        if (hasFooter()) {
            var count = 0
            if (mList.size > mLazyLoadCount)
                count = mLazyLoadCount
            else
                count = mList.size

            return position == count
        }
        return false
    }

    fun hasFooter(): Boolean {
        if (viewModel.mMediaType == -1)  //全瀏覽時不使用lazy load
            return false
        return mList.size > 0
    }

    override fun getItemCount(): Int {
        var count = mList.size
        if (hasFooter()) {
            if (mList.size > mLazyLoadCount)  //總檔案數大於懶加載數，則顯示懶加載數
                count = mLazyLoadCount + 1  //footer
            else    //否則顯示總檔案數
                count = mList.size     //footer
        }

        return count
    }

    fun lazyLoad(){
        if (hasFooter()) {
            val lastCount = mLazyLoadCount
            mLazyLoadCount += 30
            if (mLazyLoadCount > mList.size)
                mLazyLoadCount = mList.size
            if (lastCount != mLazyLoadCount && mLazyLoadCount > lastCount)    //懶加載數有變才作更新
                notifyItemChanged(lastCount, mLazyLoadCount)
        }
    }

    fun loadAllFiles(){
        val lastCount = mLazyLoadCount
        mLazyLoadCount = mList.size
        notifyItemChanged(lastCount, mLazyLoadCount)
    }

    override fun getItemViewType(position: Int): Int{
        if (isFooter(position))
            return Footer
        return mViewType
    }

    fun setViewType(viewType: Int){
        mViewType = viewType
        notifyDataSetChanged()
    }

    open inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view)
    inner class ViewHolderList(var listItemBinding: RecyclerviewListitemBinding) : ViewHolder(listItemBinding.root) {
        fun bind(item: FileInfo){
            listItemBinding.recyclerModel = item
            listItemBinding.recyclerViewModel = viewModel

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(item)
            }

            itemView.setOnLongClickListener{
                mRecyclerViewClickCallback?.onLongClick(item)
                true
            }

            itemView.isSelected = item.isSelected

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
        fun bind(item: FileInfo){

            gridItemBinding.recyclerModel = item
            gridItemBinding.recyclerViewModel = viewModel

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(item)
            }

            itemView.setOnLongClickListener{
                mRecyclerViewClickCallback?.onLongClick(item)
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

    inner class ViewHolderFooter(val itemBinding: RecyclerviewFootitemBinding): ViewHolder(itemBinding.root){

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

    //TODO
    fun getSelectedFiles(): List<FileInfo>{
        val list = ArrayList<FileInfo>()
        if (itemCount > 0){
            for (file: FileInfo in mList){
                if (file.isSelected)
                    list.add(file)
            }
        }
        return list
    }

    //TODO
    fun getSelectedFilesPath(): ArrayList<String>{
        val list = ArrayList<String>()
        if (itemCount > 0){
            for (file: FileInfo in mList){
                if (file.isSelected)
                    list.add(file.path)
            }
        }
        return list
    }

    fun deselectAll(){
        if (itemCount > 0){
            for (file: FileInfo in mList){
                if (file.isSelected) {
                    file.isSelected = false
                    notifyItemChanged(mList.indexOf(file))
                }
            }

        }
    }

    fun selectAll(){
        if (itemCount > 0){
            loadAllFiles()
            for (file: FileInfo in mList){
                if (file.isSelected.not()) {
                    file.isSelected = true
                    notifyItemChanged(mList.indexOf(file))
                }
            }
        }
    }
}