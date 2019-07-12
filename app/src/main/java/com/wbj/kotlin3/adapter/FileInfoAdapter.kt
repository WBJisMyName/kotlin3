package com.wbj.kotlin3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wbj.kotlin3.R
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.utilities.RecyclerViewClickCallback
import java.text.SimpleDateFormat
import java.util.*

class FileInfoAdapter(recyclerViewClickCallback: RecyclerViewClickCallback) : ListAdapter<FileInfo, FileInfoAdapter.ViewHolder>(
    FileInfoDiffCallback()
) {

    var mRecyclerViewClickCallback: RecyclerViewClickCallback? = recyclerViewClickCallback

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_listitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getCurrenItemParent(): String? {
        if(currentList.size > 0 )
            return currentList.get(0).parent
        else
            return ""
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTv = itemView.findViewById<TextView>(R.id.item_title)
        private val subTitleTv = itemView.findViewById<TextView>(R.id.item_subtitle)
        private val layout = itemView.findViewById<RelativeLayout>(R.id.item_layout)
        private val noFile = itemView.findViewById<TextView>(R.id.noFile_tv)
        private val fileTypeIv = itemView.findViewById<ImageView>(R.id.item_icon)
        private val arrowIv = itemView.findViewById<ImageView>(R.id.item_info)
        fun bind(fileInfo: FileInfo) {
            titleTv.text = fileInfo.name
            subTitleTv.text = getTime(fileInfo.lastModifyTime)
            if(fileInfo.fileType == 0){
                arrowIv.visibility = View.VISIBLE
                fileTypeIv.setImageResource(R.mipmap.ic_filelist_folder_grey)
            }
            else{
                arrowIv.visibility = View.GONE
                fileTypeIv.setImageResource(R.mipmap.ic_brower_listview_filearrow)
            }


            if(fileInfo.name == "空" && fileInfo.path == "空"){
                layout.visibility = View.GONE
                noFile.visibility = View.VISIBLE
            }else{
                layout.visibility = View.VISIBLE
                noFile.visibility = View.GONE
            }


            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(fileInfo)
            }
        }
    }

    fun getTime(time: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(Date(time))
    }
}

private class FileInfoDiffCallback : DiffUtil.ItemCallback<FileInfo>() {
    override fun areItemsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem?.id == newItem?.id
    }

    override fun areContentsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
        return oldItem == newItem
    }
}