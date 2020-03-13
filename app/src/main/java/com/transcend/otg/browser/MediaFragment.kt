package com.transcend.otg.browser

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.MainActivity
import com.transcend.otg.adapter.RecyclerViewAdapter
import com.transcend.otg.viewmodels.BrowserViewModel
import com.transcend.otg.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

class MediaFragment(val mType: Int, val mediaRoot: String): LocalFragment(mediaRoot){

    override fun onResume() {
        viewModel.doLoadMediaFiles(mType, mediaRoot)
        super.onResume()
    }

    override fun refreshView(){
        if (activity is MainActivity)   //更新toolbar
            (activity as MainActivity).setToolbarMode(MainActivityViewModel.TabMode.Browser)
        setDropdownList(mediaRoot)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.mPath = mRoot
        viewModel.isLoading.set(true)

        val lm = LinearLayoutManager(context)
        adapter = RecyclerViewAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)

        startLoadingView()
    }

    fun startLoadingView() {
        viewModel.mMediaType = mType
        viewModel.items.observe(this@MediaFragment, Observer { fileList ->
            adapter?.submitList(fileList)
            viewModel.isLoading.set(false)
            viewModel.isEmpty.set(fileList.size == 0)
        })
        mBinding?.viewModel = viewModel  //Bind view and view model
    }

    override fun doRefresh() {
        viewModel.isLoading.set(true)
        destroyActionMode()
        val thread = Thread(Runnable {
            viewModel.scanMediaFiles(mType, mediaRoot)
            Thread.sleep(200)
        })
        thread.start()
    }
}