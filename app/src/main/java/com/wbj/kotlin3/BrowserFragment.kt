package com.wbj.kotlin3

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.databinding.BrowserFragmentBinding
import com.wbj.kotlin3.utilities.BackpressCallback
import com.wbj.kotlin3.adapter.FileInfoAdapter
import com.wbj.kotlin3.utilities.RecyclerViewClickCallback
import com.wbj.kotlin3.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.browser_fragment.*
import java.io.File


class BrowserFragment : Fragment(), BackpressCallback {
    override fun onBackPressed(): Boolean {
        if(adapter.getCurrenItemParent() == Environment.getExternalStorageDirectory().getAbsolutePath())
            return true
        else
            afterBackpress(adapter.getCurrenItemParent())
        return false
    }

    fun afterBackpress(currentPath:String?){
        if(currentPath != null){
            val currentFile = File(currentPath)
            val currentFileParentString = currentFile.parent
            getFolderFolderFile(currentFileParentString)
        }
    }

    companion object {
        fun newInstance() = BrowserFragment()
    }
    private lateinit var progressView: RelativeLayout
    private lateinit var viewModel: BrowserViewModel
    private lateinit var adapter: FileInfoAdapter
    var mBinding: BrowserFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        mBinding = BrowserFragmentBinding.inflate(inflater, container, false)
        mBinding!!.buttonLocal.setOnClickListener{
            getLocalFile()
        }
        mBinding!!.buttonDelete.setOnClickListener{
            viewModel.deleteAll()
        }
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressView = progress_view
        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.getAllFileInfos().observe(this, Observer { fileInfo->
            progressView.visibility = View.GONE
            adapter.submitList(fileInfo)
        })
        viewModel.deleteAll()
        getLocalFile()
    }



    fun getLocalFile(){
        progressView.visibility = View.VISIBLE
        val localPath = Environment.getExternalStorageDirectory().getAbsolutePath()
        val localFile = File(localPath)
        var list = localFile.listFiles()
        for (file in list) {
            if(file.name.toString().startsWith("."))
                continue
            var info = FileInfo()
            info.name = file.name
            info.path = file.path
            info.lastModifyTime = file.lastModified()
            info.size = file.length()
            info.fileType = if(file.isDirectory) 0 else 1
            if(file.parent != null)
                info.parent = file.parent
            else
                info.parent = ""
            viewModel.insert(info)
        }
    }

    fun getFolderFolderFile(folderString:String){
        progressView.visibility = View.VISIBLE
        val localFile = File(folderString)
        var list =localFile.listFiles()
        if(list!=null){
            var fileInfoList : MutableList<FileInfo> = mutableListOf()
            for (file in list) {
                if(file.name.toString().startsWith("."))
                    continue
                var info = FileInfo()
                info.name = file.name
                info.path = file.path
                info.lastModifyTime = file.lastModified()
                info.size = file.length()
                info.fileType = if(file.isDirectory) 0 else 1
                if(file.parent != null)
                    info.parent = file.parent
                else
                    info.parent = ""
                fileInfoList.add(info)
            }
            if(fileInfoList.size == 0){
                var info = FileInfo()
                info.name = "空"
                info.path = "空"
                info.parent = folderString
                fileInfoList.add(info)
            }
            viewModel.insertAll(fileInfoList)
        }
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            getFolderFolderFile(fileInfo.path)
        }
    }


}
