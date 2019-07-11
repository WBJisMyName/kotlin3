package com.wbj.kotlin3

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        mBinding!!.hasFiles = true
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.getAllFileInfos().observe(this, Observer { fileInfo->
            if(fileInfo.size > 0){
                mBinding?.hasFiles = !(fileInfo[0].name == "空" && fileInfo[0].path == "空")
            }

            adapter.submitList(fileInfo)

        })
    }



    fun getLocalFile(){
        val localPath = Environment.getExternalStorageDirectory().getAbsolutePath()
        val localFile = File(localPath)
        var list =localFile.listFiles()
        for (file in list) {
            if(file.name.toString().startsWith("."))
                continue
            var info = FileInfo()
            info.name = file.name
            info.path = file.path
            if(file.parent != null)
                info.parent = file.parent
            else
                info.parent = ""
            viewModel.insert(info)
        }
    }

    fun getFolderFolderFile(folderString:String){
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
