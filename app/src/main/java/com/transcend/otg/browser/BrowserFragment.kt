package com.transcend.otg.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transcend.otg.FileActionLocateActivity
import com.transcend.otg.R
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.adapter.RecyclerViewAdapter
import com.transcend.otg.databinding.FragmentBrowserBinding
import com.transcend.otg.information.InfoActivity
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

abstract class BrowserFragment(val mRoot: String): Fragment(),
    BackpressCallback,
    ActionMode.Callback,
    LoaderManager.LoaderCallbacks<Boolean>{

    //action mode controller
    var mActionMode: ActionMode? = null
    var mActionModeView: RelativeLayout? = null
    lateinit var mActionModeTitle: TextView

    var mContext: Context? = null
    lateinit var mFileActionManager: FileActionManager  //action manager

    lateinit var viewModel: BrowserViewModel
    var adapter: RecyclerViewAdapter? = null
    var mBinding: FragmentBrowserBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDetach() {
        super.onDetach()
        mContext = null
    }

    override fun onResume() {
        super.onResume()
        //刷新整個頁面
        refreshView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        if (mContext == null)   //避免未進入onAttach而造成的Null
            mContext = activity as Context

        setFileActionManager()
        setDropdownList(mRoot)

        mBinding = FragmentBrowserBinding.inflate(inflater, container, false)
        mBinding!!.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        adapter?.lazyLoad()
                }
            }
        })

        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.mPath = mRoot
        viewModel.mRoot = mRoot
        viewModel.items.observe(this, Observer {
                fileList ->
            adapter?.submitList(fileList)
            viewModel.isLoading.set(false)
            viewModel.isEmpty.set(fileList.size == 0)
        })

        doLoadFiles(mRoot)    //讀取根目錄
        mBinding?.viewModel = viewModel  //Bind view and view model

        val lm = LinearLayoutManager(context)
        adapter = RecyclerViewAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)
    }

    fun startActionMode(){
        if (mActionMode == null) {
            getFragmentActicity().startSupportActionMode(this)
        }
    }

    fun destroyActionMode(){
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mActionMode = mode
        mode?.getMenuInflater()?.inflate(R.menu.menu_actionmode, menu)
        mActionModeView = LayoutInflater.from(activity).inflate(R.layout.action_mode_custom, null) as RelativeLayout
        mActionModeTitle = (mActionModeView as RelativeLayout).findViewById(R.id.action_mode_custom_title) as TextView
        mActionMode?.setCustomView(mActionModeView)
        viewModel.isOnSelectMode.set(true)
        updateActionTitle()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        mActionMode = null
        viewModel.isOnSelectMode.set(false)
        adapter?.deselectAll()
        mode?.finish()
    }

    fun updateActionTitle(){
        val count = adapter?.getSelectedFiles()?.size!!
        val format = resources.getString(if (count <= 1) R.string.msg_file_selected else R.string.msg_files_selected)
        mActionModeTitle.setText(String.format(format, count))
    }

    //開啟資訊頁面
    protected fun startInfoActivity(path: String) {
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)
        val args = Bundle()
        args.putString("path", path)
        val intent = Intent()
        intent.setClass(getFragmentActicity(), InfoActivity::class.java)
        intent.putExtras(args)
        startActivityForResult(intent,
            FileActionLocateActivity.REQUEST_CODE
        )
    }

    //開起目的地選擇頁面
    protected fun startLocateActivity(actionID: Int) {
        val args = Bundle()
        args.putString("path", viewModel.mPath)
        args.putInt("action_id", actionID)
        val intent = Intent()
        intent.setClass(getFragmentActicity(), FileActionLocateActivity::class.java)
        intent.putExtras(args)
        startActivityForResult(intent,
            FileActionLocateActivity.REQUEST_CODE
        )
    }

    //重讀路徑，直接從資料庫取出
    fun doReload(){
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doReload()
    }

    //全選
    fun selectAll(){
        startActionMode()
        adapter?.selectAll()
        updateActionTitle()
    }

    fun getPath(): String{
        return viewModel.mPath
    }

    abstract fun refreshView()
    abstract fun setFileActionManager()
    abstract fun setDropdownList(path: String)
    abstract fun doLoadFiles(path: String)
    abstract fun getFragmentActicity(): AppCompatActivity
    abstract var mRecyclerViewClickCallback: RecyclerViewClickCallback
}