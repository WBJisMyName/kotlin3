package com.transcend.otg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.BrowserFragmentBinding
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
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

        viewModel = ViewModelProviders.of(activity as MainActivity).get(BrowserViewModel::class.java)
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
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            viewModel.doLoadFiles(fileInfo.path)
        }
    }


}
