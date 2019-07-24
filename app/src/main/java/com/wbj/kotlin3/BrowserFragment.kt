package com.wbj.kotlin3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.wbj.kotlin3.adapter.FileInfoAdapter
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.databinding.BrowserFragmentBinding
import com.wbj.kotlin3.utilities.BackpressCallback
import com.wbj.kotlin3.utilities.Constant
import com.wbj.kotlin3.utilities.RecyclerViewClickCallback
import com.wbj.kotlin3.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.browser_fragment.*
import java.io.File


class BrowserFragment : Fragment(), BackpressCallback {
    override fun onBackPressed(): Boolean {
        if(viewModel.mPath.equals(Constant.LOCAL_ROOT))
            return true
        else
            afterBackpress(File(viewModel.mPath).absolutePath)
        return false
    }

    fun afterBackpress(currentPath:String?){
        if(currentPath != null){
            val currentFile = File(currentPath)
            val currentFileParentString = currentFile.parent
//            getFolderFolderFile(currentFileParentString)
            viewModel.doLoadFiles(currentFileParentString)
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
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)

        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.items.observe(this, Observer { fileInfo->
            adapter.submitList(fileInfo)
            viewModel.isLoading.set(false)
        })

        viewModel.doLoadFiles(Constant.LOCAL_ROOT)
        mBinding?.viewModel = viewModel  //Bind view and view model

        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);

//        getLocalFile()
    }

//    fun getLocalFile(){
//        progressView.visibility = View.VISIBLE
//        val localPath = Environment.getExternalStorageDirectory().getAbsolutePath()
//        val localFile = File(localPath)
//        var list = localFile.listFiles()
//        for (file in list) {
//            if(file.name.toString().startsWith("."))
//                continue
//            var info = FileInfo()
//            info.title = file.name
//            info.path = file.path
//            info.lastModifyTime = file.lastModified()
//            info.size = file.length()
//            info.fileType = if(file.isDirectory) 0 else 1
//            if(file.parent != null)
//                info.parent = file.parent
//            else
//                info.parent = ""
//            viewModel.insert(info)
//        }
//    }
//
//    fun getFolderFolderFile(folderString:String){
//        progressView.visibility = View.VISIBLE
//        val localFile = File(folderString)
//        var list =localFile.listFiles()
//        if(list!=null){
//            var fileInfoList : MutableList<FileInfo> = mutableListOf()
//            for (file in list) {
//                if(file.name.toString().startsWith("."))
//                    continue
//                var info = FileInfo()
//                info.title = file.name
//                info.path = file.path
//                info.lastModifyTime = file.lastModified()
//                info.size = file.length()
//                info.fileType = if(file.isDirectory) 0 else 1
//                if(file.parent != null)
//                    info.parent = file.parent
//                else
//                    info.parent = ""
//                fileInfoList.add(info)
//            }
//            if(fileInfoList.size == 0){
//                var info = FileInfo()
//                info.title = "空"
//                info.path = "空"
//                info.parent = folderString
//                fileInfoList.add(info)
//            }
//            viewModel.insertAll(fileInfoList)
//        }
//    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
//            getFolderFolderFile(fileInfo.path)
            viewModel.doLoadFiles(fileInfo.path)
        }
    }


}
