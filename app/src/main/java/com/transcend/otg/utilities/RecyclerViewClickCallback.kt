package com.transcend.otg.utilities

import com.transcend.otg.data.FileInfo


interface RecyclerViewClickCallback {
    fun onClick(fileInfo: FileInfo, position: Int)
    fun onLongClick(fileInfo: FileInfo, position: Int)
}
