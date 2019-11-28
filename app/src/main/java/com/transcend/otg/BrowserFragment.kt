package com.transcend.otg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.loader.*
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.browser.DropDownAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.FragmentBrowserBinding
import com.transcend.otg.floatingbtn.BottomSheetFragment
import com.transcend.otg.floatingbtn.ProgressFloatingButton
import com.transcend.otg.singleview.ImageActivity
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.BrowserViewModel
import com.transcend.otg.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_browser.*
import java.io.File
import kotlin.concurrent.thread



open class BrowserFragment(val mRoot: String) : Fragment(),
    BackpressCallback,
    ActionMode.Callback,
    LoaderCallbacks<Boolean>{

    //action mode controller
    var mActionMode: ActionMode? = null
    var mActionModeView: RelativeLayout? = null
    lateinit var mActionModeTitle: TextView

    lateinit var mContext: Context
    lateinit var mFileActionManager: FileActionManager  //action manager
    lateinit var mBottomSheetFragment: BottomSheetFragment   //底部進度視窗
    lateinit var mFloatingBtn: ProgressFloatingButton   //Custom floating btn

    lateinit var viewModel: BrowserViewModel
    var adapter: FileInfoAdapter? = null
    var mBinding: FragmentBrowserBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity)
            (activity as MainActivity).setToolbarMode(MainActivityViewModel.TabMode.Browser)
        refreshView()   //切換tab時更新dropdown list，全檔案瀏覽時更新目前路徑；媒體瀏覽則顯示手機名稱
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        if (mContext == null)   //避免未進入onAttach而造成的Null
            mContext = activity as Context

//        setBottomSheetFragment()    //設定底部進度視窗

        mFileActionManager = FileActionManager(mContext, FileActionManager.FileActionServiceType.PHONE, this)   //action manager

        setDropdownList(mRoot)

        mBinding = FragmentBrowserBinding.inflate(inflater, container, false)
        mBinding!!.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    if (newState == SCROLL_STATE_IDLE)
                        adapter?.lazyLoad()
                }
            }
        })

        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)

        viewModel.items.observe(this, Observer {
                fileList ->
            adapter?.submitList(null)
            adapter?.submitList(fileList)
            viewModel.isLoading.set(false)
            viewModel.isEmpty.set(fileList.size == 0)
        })

        doLoadFiles(mRoot)    //讀取根目錄
        mBinding?.viewModel = viewModel  //Bind view and view model

        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileActionLocateActivity.REQUEST_CODE){
            if (resultCode == AppCompatActivity.RESULT_OK){
                val destPath = data?.getStringExtra("path")!!
                val action_id = data?.getIntExtra("action_id", -1)
                when(action_id){
                    R.id.action_copy -> mFileActionManager.copy(destPath, adapter?.getSelectedFilesPath()!!)
                    R.id.action_move -> mFileActionManager.move(destPath, adapter?.getSelectedFilesPath()!!)
                }
            }
        }
    }

    open fun refreshView(){
        setDropdownList(viewModel.mPath)
    }

    open fun setDropdownList(path: String){
        if (activity == null)
            return
        var path = path
        val mainViewModel: MainActivityViewModel = ViewModelProviders.of(activity as MainActivity).get(MainActivityViewModel::class.java)   //取得activity的viewmodel
        val localMainTitle = Constant.LocalBrowserMainPageTitle
        val sdMainTitle = Constant.SDBrowserMainPageTitle
        val sdcardRoot = Constant.SD_ROOT
        if (path.startsWith(Constant.LOCAL_ROOT))   //置換本地根目錄的名稱
            path = path.replace(Constant.LOCAL_ROOT, localMainTitle)
        else if (sdcardRoot != null && path.startsWith(sdcardRoot)) //置換SD根目錄名稱
            path = path.replace(sdcardRoot, sdMainTitle)

        val list = path.split("/").reversed().filter {
            !it.equals("")  //過濾空字串
        }

        //根目錄時隱藏下拉箭頭
        val arrowVisibility = if(list.size == 1) View.GONE else View.VISIBLE
        mainViewModel.dropdownArrowVisibility.set(arrowVisibility)

        mainViewModel.mDropdownList.set(list)

        //監控Dropdown item click
        MainApplication.getInstance()?.getDropdownAdapter()?.setOnDropdownItemSelectedListener(object: DropDownAdapter.OnDropdownItemSelectedListener{
            override fun onDropdownItemSelected(path: String) {
                var selected_path = path
                if (getPath().startsWith(Constant.LOCAL_ROOT))
                    selected_path = selected_path.replace(Constant.LocalBrowserMainPageTitle, Constant.LOCAL_ROOT)
                else if (sdcardRoot != null && getPath().startsWith(sdcardRoot))
                    selected_path = selected_path.replace(Constant.SDBrowserMainPageTitle, sdcardRoot)
                doLoadFiles(selected_path)
            }
        })
    }

    open fun doRefresh(){
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doRefresh()
    }

    open fun doReload(){
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doReload()
    }

    protected fun startLocateActivity(actionID: Int) {
        val args = Bundle()
        args.putString("path", getPath())
        args.putInt("action_id", actionID)
        val intent = Intent()
        intent.setClass(activity as MainActivity, FileActionLocateActivity::class.java)
        intent.putExtras(args)
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE)
    }

    fun setBottomSheetFragment(){
        mBottomSheetFragment = BottomSheetFragment()
        mFloatingBtn = (activity as MainActivity).findViewById(R.id.progress_floating_btn)
        mFloatingBtn.visibility = View.GONE
        mFloatingBtn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                if (activity != null) {
                    mBottomSheetFragment.show((activity as MainActivity).supportFragmentManager, "TAG")
                    if (count >= max) {
                        Toast.makeText(activity, "讀取完成!", Toast.LENGTH_SHORT)
                        mFloatingBtn.visibility = View.GONE
                    }
                }
            }
        })
    }

    //TODO 測試底部進度視窗
    private val handler = Handler()
    private var count = 0
    private var max = 100
    inner private class ProgressTest : Runnable {
        override fun run() {
            count += 20
            if (count <= 100) {
                mFloatingBtn.setText("$count / $max")
                mFloatingBtn.setProgressValue(count)
                mBottomSheetFragment.setProcessText("$count / $max")
                mBottomSheetFragment.setProgressValue(count)
                handler.postDelayed(this, 1000)
            } else if (count > 100){
                mFloatingBtn.setProgressValue(0)
                mBottomSheetFragment.setProgressValue(0)
                mFloatingBtn.setText("Finished")
            }
        }
    }

    open val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            if (mActionMode != null) {
                fileInfo.isSelected = fileInfo.isSelected.not()
                adapter?.notifyItemChanged(adapter?.currentList?.indexOf(fileInfo)!!)
                updateActionTitle()
            } else {
                when(fileInfo.fileType){
                    Constant.TYPE_IMAGE -> {
                        val intent = Intent(activity, ImageActivity::class.java)
                        if (viewModel.mMediaType == -1) //-1為全瀏覽頁面，此時才季路目前路徑
                            intent.putExtra("folderPath", getPath())
                        intent.putExtra("path", fileInfo.path)
                        intent.putExtra("title", fileInfo.title)
                        activity?.startActivity(intent)
                    }
                    Constant.TYPE_DIR -> doLoadFiles(fileInfo.path)
                }
            }
        }

        override fun onLongClick(fileInfo: FileInfo) {
            startActionMode()
            fileInfo.isSelected = fileInfo.isSelected.not()
            adapter?.notifyItemChanged(adapter?.currentList?.indexOf(fileInfo)!!)
            updateActionTitle()
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val id = item?.itemId
        when(id){
            R.id.action_delete -> {
                // setup dialog builder
                AlertDialog.Builder(mContext)
                    .setTitle("Delete")
                    .setPositiveButton("Confirm",{ dialog, whichButton ->
                        mFileActionManager.delete(adapter?.getSelectedFilesPath()!!)
                    })
                    .setNegativeButton("Cancel", { dialog, whichButton ->
                        println("cancel")
                    })
                    .show()
            }
            R.id.action_rename -> {
                if (adapter?.getSelectedFiles()?.size == 1) {
                    val fileInfo = adapter?.getSelectedFiles()!![0]

                    val view = View.inflate(mContext, R.layout.dialog_folder_create, null)
                    val textLayout = view.findViewById<TextInputLayout>(R.id.dialog_folder_create_name)
                    textLayout.editText?.setText(fileInfo.title)
                    AlertDialog.Builder(mContext)
                        .setTitle("Rename")
                        .setIcon(R.drawable.ic_tab_rename_grey)
                        .setView(view)
                        .setPositiveButton("Confirm", { dialog, whichButton ->
                            val name = textLayout.editText?.text.toString()
                            mFileActionManager.rename(fileInfo.path, name)
                        })
                        .setNegativeButton("Cancel", { dialog, whichButton ->
                            println("cancel")
                        })
                        .setCancelable(true)
                        .show()
                }
            }
            R.id.action_selectAll -> {
                selectAll()
            }
            R.id.action_copy, R.id.action_move -> {
                startLocateActivity(id)
            }

        }
        return false
    }

    fun startActionMode(){
        if (mActionMode == null) {
            (activity as AppCompatActivity).startSupportActionMode(this@BrowserFragment)
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

    fun getPath(): String{
        return viewModel.mPath
    }

    fun scrollToTop(){
        activity?.runOnUiThread{
            mBinding?.recyclerView?.scrollToPosition(0)
        }
    }

    fun selectAll(){
        startActionMode()
        adapter?.selectAll()
        updateActionTitle()
    }

    fun updateActionTitle(){
        val count = adapter?.getSelectedFiles()?.size!!
        val format = resources.getString(if (count <= 1) R.string.msg_file_selected else R.string.msg_files_selected)
        mActionModeTitle.setText(String.format(format, count))
    }

    open fun doLoadFiles(path: String){
        viewModel.doLoadFiles(path)
        thread {
            Thread.sleep(100)   //睡0.1秒，避免黑畫面發生
            setDropdownList(path)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.LOCAL_FILE_DELETE -> return LocalFileDeleteLoader(mContext, args?.getStringArrayList("paths")!!)
            LoaderID.LOCAL_FILE_RENAME -> return LocalRenameLoader(mContext, args?.getString("path")!!, args.getString("name")!!)
            LoaderID.LOCAL_FILE_COPY -> return LocalCopyLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            LoaderID.LOCAL_FILE_MOVE -> return LocalMoveLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            else -> return NullLoader(mContext)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)

        if (loader is LocalFileDeleteLoader){
            viewModel.doRefresh()
            adapter?.notifyDataSetChanged()
        } else if (loader is LocalRenameLoader || loader is LocalCopyLoader || loader is LocalMoveLoader){
            viewModel.doRefresh()
        }
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.isLoading.get()) {    //如果檔案讀取中，則中斷讀取檔案
            viewModel.isCancelScanTask = true
            return false
        }

        if(getPath().equals(mRoot))   //到了根目錄，回傳true
            return true
        else
            doLoadFiles(File(getPath()).parent)  //讀取parent路徑
        return false
    }
}
