package com.transcend.otg.browser

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.Loader
import com.transcend.otg.R
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.dialog.FileActionNewFolderDialog
import com.transcend.otg.action.loader.FolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.ActionLocateViewModel
import java.io.File
import kotlin.concurrent.thread

class FileActionLocateFragment: BrowserFragment(Constant.Storage_Device_Root){

    lateinit var mainViewModel: ActionLocateViewModel
    var mMenu: Menu? = null

    override fun getFragmentActicity(): AppCompatActivity {
        return activity as FileActionLocateActivity
    }

    override fun setFileActionManager() {
        mFileActionManager = FileActionManager(mContext!!, FileActionManager.FileActionServiceType.PHONE, this)   //action manager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)     //設定選單
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun refreshView() {
        //更新下拉式選單
        setDropdownList(viewModel.mPath)
    }

    override fun setDropdownList(path: String){
        if (activity == null)
            return
        var path = path
        mainViewModel = ViewModelProviders.of(activity as FileActionLocateActivity).get(ActionLocateViewModel::class.java)   //取得activity的viewmodel，用以更新Dropdown
        val localMainTitle = Constant.PhoneName
        val sdMainTitle = getString(R.string.nav_sd)
        val sdcardRoot = Constant.SD_ROOT
        if (path.startsWith(Constant.LOCAL_ROOT))   //置換本地根目錄的名稱
            path = path.replace(Constant.LOCAL_ROOT, localMainTitle)
        else if (sdcardRoot != null && path.startsWith(sdcardRoot)) //置換SD根目錄名稱
            path = path.replace(sdcardRoot, sdMainTitle)
        else
            path = path.replaceFirst("/", getString(R.string.nav_otg) + "/")

        val list = path.split("/").reversed().filter {
            !it.equals("")  //過濾空字串
        }

        //根目錄時隱藏下拉箭頭
        val arrowVisibility = if(list.size == 1) View.GONE else View.VISIBLE

        mainViewModel.dropdownArrowVisibility.set(arrowVisibility)
        mainViewModel.mDropdownList.set(list)
        mainViewModel.dropdownVisibility.set(View.VISIBLE)

        //監控Dropdown item click
        mainViewModel.mDropdownAdapter.setOnDropdownItemSelectedListener(object: DropDownAdapter.OnDropdownItemSelectedListener{
            override fun onDropdownItemSelected(path: String) {
                var selected_path = path
                if (viewModel.mPath.startsWith(Constant.LOCAL_ROOT))
                    selected_path = selected_path.replace(Constant.PhoneName, Constant.LOCAL_ROOT)
                else if (sdcardRoot != null && viewModel.mPath.startsWith(sdcardRoot))
                    selected_path = selected_path.replace(getString(R.string.nav_sd), sdcardRoot)
                else
                    selected_path = selected_path.replace(getString(R.string.nav_otg), "/")
                doLoadFiles(selected_path)
            }
        })
    }

    override fun doLoadFiles(path: String){
        if (path.equals(Constant.Storage_Device_Root)){
            mainViewModel.confirmBtnVisibility.set(View.GONE)
            val list = buildStorageDeviceRoot()
            if (list.size > 1) {    //超過一個才顯示，顯示各裝置列表
                viewModel.items.postValue(list)
                viewModel.mPath = Constant.Storage_Device_Root
                setDropdownList(Constant.Storage_Device_Root)
                mMenu?.findItem(R.id.action_new_folder)?.setVisible(false)
                return
            } else {    //只有一個則直接讀取本地路徑
                mainViewModel.confirmBtnVisibility.set(View.VISIBLE)
                viewModel.doLoadFolders(Constant.LOCAL_ROOT)
                thread {
                    Thread.sleep(100)   //睡0.1秒，避免黑畫面發生
                    setDropdownList(Constant.LOCAL_ROOT)
                }
            }
        } else {
            mainViewModel.confirmBtnVisibility.set(View.VISIBLE)
            viewModel.doLoadFolders(path)
            thread {
                Thread.sleep(100)   //睡0.1秒，避免黑畫面發生
                setDropdownList(path)
            }
        }
        mMenu?.findItem(R.id.action_new_folder)?.setVisible(true)
    }

    override fun doRefresh() {
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doRefresh(true)
    }

    fun buildStorageDeviceRoot(): ArrayList<FileInfo>{
        val list = ArrayList<FileInfo>()
        val localInfo = FileInfo()
        localInfo.path = Constant.LOCAL_ROOT
        localInfo.title = SystemUtil().getDeviceName()
        localInfo.fileType = Constant.TYPE_DIR
        localInfo.defaultIcon =
            R.drawable.ic_drawer_myphone_grey
        localInfo.infoIcon =
            R.drawable.ic_brower_listview_filearrow
        list.add(localInfo)
        val sdPath = Constant.SD_ROOT
        if (sdPath != null){
            val sdInfo = FileInfo()
            sdInfo.path = sdPath
            sdInfo.title = getString(R.string.nav_sd)
            sdInfo.fileType = Constant.TYPE_DIR
            sdInfo.defaultIcon =
                R.drawable.ic_drawer_microsd_grey
            sdInfo.infoIcon =
                R.drawable.ic_brower_listview_filearrow
            list.add(sdInfo)
        }
        if (Constant.OTG_ROOT != null){
            val otgInfo = FileInfo()
            otgInfo.path = "/"
            otgInfo.title = getString(R.string.nav_otg)
            otgInfo.fileType = Constant.TYPE_DIR
            otgInfo.defaultIcon = R.drawable.ic_drawer_otg_grey
            otgInfo.infoIcon =
                R.drawable.ic_brower_listview_filearrow
            list.add(otgInfo)
        }
        return list
    }

    override var mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            if (fileInfo.title.equals(getString(R.string.nav_otg))){    //檢查OTG權限
                val fragmentActivity = (activity as FileActionLocateActivity)
                if (!UsbUtils.hasUSBPermission() || isDetached) {
                    UsbUtils.doUSBRequestPermission(fragmentActivity)
                    return
                }
            } else if(fileInfo.title.equals(getString(R.string.nav_sd))){
                val fragmentActivity = (activity as FileActionLocateActivity)
                if (!fragmentActivity.checkPermission())
                    return
            }
            doLoadFiles(fileInfo.path)
        }

        override fun onLongClick(fileInfo: FileInfo) {

        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.NEW_FOLDER -> return FolderCreateLoader(MainApplication.getInstance()!!.getContext(), args?.getString("path")!!)
            else -> return NullLoader(MainApplication.getInstance()!!.getContext())
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        if (loader is FolderCreateLoader){
            viewModel.doRefresh(true)
        }
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        mMenu = menu
        inflater.inflate(R.menu.action_locate_menu, menu)
        if (getPath().equals(Constant.Storage_Device_Root))
            menu.findItem(R.id.action_new_folder).setVisible(false)
        else
            menu.findItem(R.id.action_new_folder).setVisible(true)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
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
        }
        return super.onOptionsItemSelected(item)
    }

    fun getFileList(): List<FileInfo>?{
        return viewModel.items.value
    }

    override fun onBackPressed(): Boolean {
        val path = getPath()
        if (path.equals(Constant.Storage_Device_Root))
            return true
        val rootArray = buildStorageDeviceRoot()
        if (rootArray.size > 1){
            if(path.equals(Constant.LOCAL_ROOT) ||
                (Constant.SD_ROOT != null && path.equals(Constant.SD_ROOT)) ||
                (Constant.OTG_ROOT != null && path.equals(Constant.OTG_ROOT)))   //到了根目錄，讀取裝置目錄
                doLoadFiles(Constant.Storage_Device_Root)
            else
                doLoadFiles(File(getPath()).parent)  //讀取parent路徑
        } else {    //只有一個表示只有本地端
            if(path.equals(Constant.LOCAL_ROOT))   //到了根目錄，回傳true
                return true
            else
                doLoadFiles(File(getPath()).parent)  //讀取parent路徑
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        //理論上不會進到這裡
        return false
    }
}
