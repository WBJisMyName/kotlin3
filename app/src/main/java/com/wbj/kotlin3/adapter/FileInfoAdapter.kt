package com.wbj.kotlin3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wbj.kotlin3.R
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.utilities.RecyclerViewClickCallback

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

        fun bind(fileInfo: FileInfo) {
            titleTv.text = fileInfo.name
            subTitleTv.text = fileInfo.path

            itemView.setOnClickListener {
                mRecyclerViewClickCallback?.onClick(fileInfo)
            }
        }
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