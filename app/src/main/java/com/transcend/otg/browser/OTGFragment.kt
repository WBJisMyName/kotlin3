package com.transcend.otg.browser

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.server.http.UsbFileHttpServerService
import com.github.mjdev.libaums.server.http.UsbFileHttpServerService.ServiceBinder
import com.github.mjdev.libaums.server.http.server.AsyncHttpServer
import com.transcend.otg.FileActionLocateActivity
import com.transcend.otg.MainActivity
import com.transcend.otg.R
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.dialog.FileActionNewFolderDialog
import com.transcend.otg.action.dialog.FileActionRenameDialog
import com.transcend.otg.action.loader.*
import com.transcend.otg.adapter.RecyclerViewAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.singleview.ImageActivity
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_browser.*
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread


class OTGFragment: BrowserFragment("/"){

    lateinit var mMenu: Menu
    val TAG = OTGFragment::class.java.simpleName
    private var serviceIntent: Intent? = null
    private var serverService: UsbFileHttpServerService? = null

    override fun getFragmentActicity(): AppCompatActivity {
        return activity as MainActivity
    }

    override fun setFileActionManager() {
        mFileActionManager = FileActionManager(mContext!!, FileActionManager.FileActionServiceType.OTG, this)   //action manager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)     //設定選單
        serviceIntent = Intent(context, UsbFileHttpServerService::class.java)   //USB Server
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun refreshView() {
        if (activity is MainActivity)   //更新toolbar
            (activity as MainActivity).setToolbarMode(MainActivityViewModel.TabMode.Browser)
        //更新下拉式選單
        setDropdownList("/")
    }

    override fun setDropdownList(path: String){
        if (activity == null)
            return
        var path = path
        val mainViewModel: MainActivityViewModel = ViewModelProviders.of(activity as MainActivity).get(MainActivityViewModel::class.java)   //取得activity的viewmodel，用以更新Dropdown

        path = path.replaceFirst("/", getString(R.string.nav_otg) + "/")
        val list = path.split("/").reversed().filter {
            !it.equals("")  //過濾空字串
        }

        //根目錄時隱藏下拉箭頭
        val arrowVisibility = if(list.size == 1) View.GONE else View.VISIBLE
        mainViewModel.dropdownArrowVisibility.set(arrowVisibility)
        mainViewModel.mDropdownList.set(list)

        //監控Dropdown item click
        mainViewModel.mDropdownAdapter.setOnDropdownItemSelectedListener(object: DropDownAdapter.OnDropdownItemSelectedListener{
            override fun onDropdownItemSelected(path: String) {
                var selected_path = path
                if (selected_path.startsWith(getString(R.string.nav_otg)))
                    selected_path = selected_path.replace(getString(R.string.nav_otg), "/")
                doLoadFiles(selected_path)
            }
        })
    }

    fun doRefresh() {
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doRefresh(false)
    }

    override fun doLoadFiles(path: String) {
        if (UsbUtils.usbDevice == null) return
        viewModel.doLoadFiles(path)
        thread {
            Thread.sleep(100)   //睡0.1秒，避免黑畫面發生
            setDropdownList(path)
        }
    }

    override var mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            if (mActionMode != null) {
                fileInfo.isSelected = fileInfo.isSelected.not()
                adapter?.notifyItemChanged(adapter?.mList?.indexOf(fileInfo)!!)
                updateActionTitle()
            } else {
                when(fileInfo.fileType){
                    Constant.TYPE_IMAGE -> {
                        val intent = Intent(activity, ImageActivity::class.java)
                        if (viewModel.mMediaType == -1) //-1為全瀏覽頁面，此時才季路目前路徑
                            intent.putExtra("folderPath", viewModel.mPath)
                        intent.putExtra("path", fileInfo.path)
                        intent.putExtra("title", fileInfo.title)
                        activity?.startActivity(intent)
                    }
                    Constant.TYPE_DIR -> doLoadFiles(fileInfo.path)
                    else -> {
                        val file: UsbFile? = UsbUtils.usbFileSystem?.rootDirectory?.search(fileInfo.path)
                        if(file != null) {
                            startHttpServer(file)
                        }
                    }
                }
            }
        }

        override fun onLongClick(fileInfo: FileInfo) {
            startActionMode()
            fileInfo.isSelected = fileInfo.isSelected.not()
            adapter?.notifyItemChanged(adapter?.mList?.indexOf(fileInfo)!!)
            updateActionTitle()
        }
    }


    //UsbServer for open with
    var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "on service connected $name")
            val binder = service as ServiceBinder
            serverService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "on service disconnected $name")
            serverService = null
        }
    }

    override fun onStart() {
        super.onStart()
        context?.startService(serviceIntent);
        context?.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    override fun onStop() {
        super.onStop()
        context?.unbindService(serviceConnection);
    }

    private fun startHttpServer(file: UsbFile) {
        Log.d(TAG, "starting HTTP server")
        if (serverService == null) {
//            Toast.makeText(context, "serverService == null!", Toast.LENGTH_LONG).show()
            return
        }
        if (serverService!!.isServerRunning) {
            Log.d(TAG, "Stopping existing server service")
            serverService!!.stopServer()
        }
        // now start the server
        try {
            serverService!!.startServer(file, AsyncHttpServer(8000))
        } catch (e: IOException) {
            Log.e(TAG, "Error starting HTTP server", e)
            return
        }
        val myIntent = Intent(Intent.ACTION_VIEW)
        myIntent.data = Uri.parse(serverService!!.server!!.baseUrl + file.name)
        try {
            startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Could no find an app for that file!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        mMenu = menu
        inflater.inflate(R.menu.main_menu, menu)

        val searchView: SearchView = menu.findItem(R.id.action_search).actionView as SearchView
        val search_editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        search_editText.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorWhite))
        search_editText.setTextColor(ContextCompat.getColor(context!!, R.color.c_02))
        search_editText.setHintTextColor(ContextCompat.getColor(context!!, R.color.c_04))
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText == null)
                    return false
                if (newText.equals("")){
                    viewModel.doReload()
                } else {
                    viewModel.doSearch(newText, Constant.TYPE_DIR)
                }
                return true
            }
        })

        menu.findItem(R.id.action_search).setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                menu.findItem(R.id.action_more).setVisible(false)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                menu.findItem(R.id.action_more).setVisible(true)
                doRefresh()
                return true
            }
        })

        menu.findItem(R.id.action_more).setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
            override fun onMenuItemClick(p0: MenuItem?): Boolean {
                //設定顯示模式 list or grid
                if (adapter?.mViewType == RecyclerViewAdapter.List)
                    menu.findItem(R.id.action_view_type).setTitle(R.string.view_by_icons)
                else
                    menu.findItem(R.id.action_view_type).setTitle(R.string.view_by_list)
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
            R.id.action_view_type -> {  //List or Grid
                changeViewType()
            }
            R.id.action_select_mode ->{
                startActionMode()
            }
            R.id.action_new_folder -> {
                val fileList = adapter?.mList
                if (fileList == null)
                    return false
                val nameList: MutableList<String> = ArrayList<String>()
                for (fileInfo in fileList){
                    nameList.add(fileInfo.title.toLowerCase())
                }

                val newFolderDialog = object: FileActionNewFolderDialog(context!!, nameList){
                    override fun onConfirm(newName: String) {
                        mFileActionManager.createFolder(getPath(), newName)   //通知action manager執行createFolder
                    }
                }
            }
            R.id.action_selectAll -> {
                selectAll()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeViewType(){
        if (adapter?.itemCount ?: 0 > 0){
            val currentItemType = adapter?.getItemViewType(0)
            when(currentItemType){
                RecyclerViewAdapter.Grid -> {
                    val listLayoutManager = LinearLayoutManager(context)
                    recyclerView.layoutManager = listLayoutManager
                    adapter?.setViewType(RecyclerViewAdapter.List)
                    mMenu.findItem(R.id.action_view_type).setTitle(R.string.view_by_icons)
                }
                RecyclerViewAdapter.List -> {
                    val gridLayoutManager = GridLayoutManager(context, 3)
                    recyclerView.layoutManager = gridLayoutManager
                    adapter?.setViewType(RecyclerViewAdapter.Grid)
                    mMenu.findItem(R.id.action_view_type).setTitle(R.string.view_by_list)
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        //如果檔案讀取中，則中斷讀取檔案
        if (viewModel.isLoading.get()) {
            viewModel.isCancelScanTask = true
            return false
        }

        if(viewModel.mPath.equals(mRoot))   //到了根目錄，回傳true
            return true
        else
            doLoadFiles(File(viewModel.mPath).parent ?: Constant.LOCAL_ROOT)  //讀取parent路徑
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileActionLocateActivity.REQUEST_CODE){
            if (resultCode == AppCompatActivity.RESULT_OK && data != null){
                val destPath = data.getStringExtra("path")!!
                val action_id = data.getIntExtra("action_id", -1)
                when(action_id){
                    R.id.action_copy -> mFileActionManager.copy(destPath, adapter?.getSelectedFilesPath()!!)
                    R.id.action_move -> mFileActionManager.move(destPath, adapter?.getSelectedFilesPath()!!)
                }
            }
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val id = item?.itemId
        when(id){
            R.id.action_delete -> {
                // setup dialog builder
                if(mContext != null) {
                    AlertDialog.Builder(mContext!!)
                        .setTitle("Delete")
                        .setPositiveButton("Confirm", { dialog, whichButton ->
                            mFileActionManager.delete(adapter?.getSelectedFilesPath()!!)
                        })
                        .setNegativeButton("Cancel", { dialog, whichButton ->
                            println("cancel")
                        })
                        .show()
                }
            }
            R.id.action_rename -> {
                if (adapter?.getSelectedFiles()?.size == 1) {
                    val fileInfo = adapter?.getSelectedFiles()!![0]
                    val fileList = adapter?.mList
                    if (fileList == null)
                        return false
                    val nameList: MutableList<String> = ArrayList<String>()
                    for (file in fileList) {
                        nameList.add(file.title.toLowerCase())
                    }

                    val isDirectory = (fileInfo.fileType == Constant.TYPE_DIR)
                    val newFolderDialog = object : FileActionRenameDialog(context!!, isDirectory, fileInfo.title, nameList) {
                        override fun onConfirm(newName: String) {
                            mFileActionManager.rename(fileInfo.path, newName)   //通知action manager執行createFolder
                        }
                    }
                }
            }
            R.id.action_selectAll -> {
                selectAll()
            }
            R.id.action_copy, R.id.action_move -> {
                startLocateActivity(id)
            }
            R.id.action_info -> {
                val list = adapter?.getSelectedFilesPath()
                if (list == null || list.size != 1)
                    return false
                else
                    startInfoActivity(list.get(0))
            }
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)
        when(id){
            LoaderID.FILE_DELETE -> return DeleteLoader(context!!, args?.getStringArrayList("paths")!!)
            LoaderID.NEW_FOLDER -> return FolderCreateLoader(context!!, args?.getString("path")!!)
            LoaderID.FILE_RENAME -> return RenameLoader(MainApplication.getInstance()!!.getContext(), args?.getString("path")!!, args.getString("name")!!)
            LoaderID.FILE_COPY -> return CopyLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            LoaderID.FILE_MOVE -> return MoveLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            else -> return NullLoader(MainApplication.getInstance()!!.getContext())
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        (activity as MainActivity).actionFinishedReminder(R.string.done)
        doRefresh()
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {

    }
}