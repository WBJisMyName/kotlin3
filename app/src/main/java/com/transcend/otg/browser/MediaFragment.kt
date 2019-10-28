package com.transcend.otg.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.BrowserFragment
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.utilities.Constant
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

class MediaFragment(val mType: Int): BrowserFragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)

        if (!Constant.hasLoadedTab[mType])  //初次讀取檔案
            viewModel.scanFileList(mType)

        when(mType){
            Constant.TYPE_IMAGE -> {
                viewModel.imageItems.observe(this, Observer { fileList ->
                    adapter.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_MUSIC -> {
                viewModel.musicItems.observe(this, Observer { fileList ->
                    adapter.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_VIDEO -> {
                viewModel.videoItems.observe(this, Observer { fileList ->
                    adapter.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_DOC -> {
                viewModel.docItems.observe(this, Observer { fileList ->
                    adapter.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
        }

        mBinding?.viewModel = viewModel  //Bind view and view model

        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)
    }

    fun doRefresh(type: Int){
        viewModel.isLoading.set(true)
        val thread = Thread(Runnable {
            viewModel.deleteAll(type)
            Thread.sleep(200)
            viewModel.scanFileList(type)
            Thread.sleep(200)
        })
        thread.start()
    }
}