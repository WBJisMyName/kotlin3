package com.transcend.otg

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.action.loader.LocalFolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.browser.DropDownAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.ActionLocateViewModel
import kotlinx.android.synthetic.main.dialog_folder_create.*
import java.io.File

class FileActionLocateFragment : BrowserFragment(Constant.Storage_Device_Root),
    BackpressCallback,
    LoaderCallbacks<Boolean>{

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_locate_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
            R.id.action_new_folder -> {
                val view = View.inflate(mContext, R.layout.dialog_folder_create, null)
                val textLayout = view.findViewById<TextInputLayout>(R.id.dialog_folder_create_name)

                 AlertDialog.Builder(mContext)
                    .setTitle("New Folder")
                    .setIcon(R.drawable.ic_tab_newfolder_grey)
                    .setView(view)
                    .setPositiveButton("Confirm",{ dialog, whichButton ->
                        dialog_folder_create_name
                        val tmp = textLayout.editText?.text.toString()
                        mFileActionManager.createFolder(viewModel.mPath, tmp)   //通知action manager執行createFolder
                    })
                    .setNegativeButton("Cancel", { dialog, whichButton ->
                        println("cancel")
                    })
                    .setCancelable(true)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setDropdownList(path: String){
        var path = path
        val mainViewModel: ActionLocateViewModel = ViewModelProviders.of(activity as FileActionLocateActivity).get(
            ActionLocateViewModel::class.java)   //取得activity的viewmodel
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
                if (viewModel.mPath.startsWith(Constant.LOCAL_ROOT))
                    selected_path = selected_path.replace(Constant.LocalBrowserMainPageTitle, Constant.LOCAL_ROOT)
                else if (sdcardRoot != null && viewModel.mPath.startsWith(sdcardRoot))
                    selected_path = selected_path.replace(Constant.SDBrowserMainPageTitle, sdcardRoot)
                doLoadFiles(selected_path)
            }
        })
    }

    override val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            doLoadFiles(fileInfo.path)
        }

        override fun onLongClick(fileInfo: FileInfo) {
            onClick(fileInfo)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.LOCAL_NEW_FOLDER -> return LocalFolderCreateLoader(mContext, args?.getString("path")!!)
            else -> return NullLoader(mContext)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        if (loader is LocalFolderCreateLoader){
            viewModel.doRefresh()
        }
    }

    fun buildStorageDeviceRoot(): ArrayList<FileInfo>{
        val list = ArrayList<FileInfo>()
        val localInfo = FileInfo()
        localInfo.path = Constant.LOCAL_ROOT
        localInfo.title = SystemUtil().getDeviceName()
        localInfo.fileType = Constant.TYPE_DIR
        localInfo.defaultIcon = R.drawable.ic_drawer_myphone_grey
        localInfo.infoIcon = R.drawable.ic_brower_listview_filearrow
        list.add(localInfo)
        val sdPath = Constant.SD_ROOT
        if (sdPath != null){
            val sdInfo = FileInfo()
            sdInfo.path = sdPath
            sdInfo.title = getString(R.string.nav_sd)
            sdInfo.fileType = Constant.TYPE_DIR
            sdInfo.defaultIcon = R.drawable.ic_drawer_microsd_grey
            sdInfo.infoIcon = R.drawable.ic_brower_listview_filearrow
            list.add(sdInfo)
        }
        return list
    }

    override fun doLoadFiles(path: String){
        if (path.equals(Constant.Storage_Device_Root)){
            val list = buildStorageDeviceRoot()
            if (list.size > 1) {    //超過一個才顯示，否則加載本地資料
                viewModel.items.postValue(list)
                return
            }
        }
        super.doLoadFiles(path)
    }

    fun getFileList(): List<FileInfo>?{
        return viewModel.items.value
    }

    override fun doReload(){
        viewModel.doReload()
    }

    override fun onBackPressed(): Boolean {
        if(getPath().equals(mRoot))   //到了根目錄，回傳true
            return true
        else
            doLoadFiles(File(getPath()).parent)  //讀取parent路徑
        return false
    }
}
