package com.transcend.otg

import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.transcend.otg.browser.HomeFragment
import com.transcend.otg.browser.OTGFragment
import com.transcend.otg.browser.LocalTabFragment
import com.transcend.otg.databinding.ActivityMainBinding
import com.transcend.otg.receiver.SDCardReceiver
import com.transcend.otg.sdcard.ViewerPagerAdapterSD
import com.transcend.otg.settings.EULAFragment
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.MainActivityViewModel
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(),
    SDCardReceiver.SDCardObserver,
    EULAFragment.OnEulaClickListener {
    private val TAG = MainActivity::class.java.simpleName

    override fun onEulaAgreeClick(v: View) {
        Toast.makeText(this, "按了EULA", Toast.LENGTH_SHORT).show()
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainActivityViewModel

    private var oneSecond = true    //Permission 等待時間
    private val mSDPermission = 1009
    private val mSDQPermission = 1007

    override fun onResume() {
        super.onResume()
        initBroadcast()
        SDCardReceiver.instance.registerObserver(this) //監測SD卡插拔
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(usbReceiver)
            SDCardReceiver.instance.unregisterObserver(this)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        binding.viewModel = viewModel

        EULAFragment.setOnEulaClickListener(this)
        UiHelper.setSystemBarTranslucent(this)

        //設置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)    // enable overriding the default toolbar layout
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)  // show or hide the default home button

        //設置Drawer
        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.container)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)

        //設置下拉式選單箭頭行為
        binding.dropdownArrow.setOnClickListener {
            binding.mainDropdown.performClick()
        }

        //點選Drawer物件後，切換畫面後的UI設置
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.toolbar.setNavigationIcon(R.drawable.ic_toggle_menu)    //TODO toggle 圖示暫時固定為menu樣式，待修改
            binding.toolbar.setNavigationOnClickListener {
                    v: View? ->
                drawerLayout.openDrawer(GravityCompat.START)
            }

            when(destination.id) {
                //不須帶參數的Fragment切換讓他自動實作
                R.id.homeFragment -> {
                    setToolbarMode(MainActivityViewModel.TabMode.Mid_Title_Only, 0, getString(R.string.homeTitle))
                }
                R.id.helpFragment -> {
                    setToolbarMode(MainActivityViewModel.TabMode.Mid_Title_Only, 0, getString(R.string.helpTitle))
                }
                R.id.feedbackFragment -> {
                    setToolbarMode(MainActivityViewModel.TabMode.Mid_Title_Only, 0, getString(R.string.feedbackTitle))
                }
                R.id.settingsFragment -> {
                    setToolbarMode(MainActivityViewModel.TabMode.Mid_Title_Only, 0, getString(R.string.settingsTitle))
                }
                R.id.browserFragment -> {
                    arguments?.putString("root", Constant.LOCAL_ROOT)   //讀取本地路徑
                }
                R.id.sdFragment -> {
                    if(checkSDPermission())
                        arguments?.putString("root", SystemUtil().getSDLocation(this@MainActivity))   //讀取sd路徑
                    else
                        navController.popBackStack()
                }
                R.id.otgFragment -> {
                    arguments?.putString("root", "/")   //讀取本地路徑
                    if (!UsbUtils.hasUSBPermission()) {
                        UsbUtils.doUSBRequestPermission(this)
                        navController.popBackStack()
                    }
                }
            }
        }

        checkLocalPermission()

        //確認SD卡狀態
        val sdPath = SystemUtil().getSDLocation(this)
        if (sdPath != null) {
            Constant.SD_ROOT = sdPath
            binding.navigationView.menu.findItem(R.id.sdFragment).setVisible(true)
        } else
            binding.navigationView.menu.findItem(R.id.sdFragment).setVisible(false)

        //初始化資料庫
        viewModel.deleteAll()
    }

    fun setToggleAction(iconId: Int){
        binding.toolbar.setNavigationIcon(iconId)
        when(iconId){
            R.drawable.ic_toggle_menu -> {
                binding.toolbar.setNavigationOnClickListener {
                        v: View? ->
                    drawerLayout.openDrawer(Gravity.LEFT)
                }
            }
            R.drawable.ic_navi_back_white -> {
                //TODO
            }
        }
    }

    fun goToBrowser(browser_id: Int){
        setToggleAction(R.drawable.ic_toggle_menu)

        val arguments: Bundle? = Bundle()
        when(browser_id) {
            R.id.browserFragment -> {
                arguments?.putString("root", Constant.LOCAL_ROOT)   //讀取本地路徑
                navController.navigate(browser_id, arguments)
            }
            R.id.sdFragment -> {
                arguments?.putString("root", SystemUtil().getSDLocation(this@MainActivity))   //讀取sd路徑
                navController.navigate(browser_id, arguments)
            }
            R.id.otgFragment -> {
                arguments?.putString("root", "/")   //讀取本地路徑
                if (!UsbUtils.hasUSBPermission())
                    UsbUtils.doUSBRequestPermission(this)
                else
                    navController.navigate(browser_id, arguments)
            }
        }
    }

    fun goToMediaTab(mediaType: Int){
        setToggleAction(R.drawable.ic_toggle_menu)

        val arguments: Bundle? = Bundle()
        arguments?.putString("root", Constant.LOCAL_ROOT)   //讀取本地路徑
        arguments?.putInt("media_type", mediaType)
        navController.navigate(R.id.browserFragment, arguments)
    }

    fun setMidTitle(title: String){
        viewModel.setMidTitleText(title)
    }

    fun setToolbarMode(mode: MainActivityViewModel.TabMode, menuCount: Int, mid_title: String){
        viewModel.updateTabMode(mode)
        viewModel.updateSystemMenuIconCount(menuCount)
        setMidTitle(mid_title)
    }

    fun setToolbarMode(mode: MainActivityViewModel.TabMode){
        viewModel.updateTabMode(mode)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        val fragment = this.supportFragmentManager.findFragmentById(R.id.container)
        if(fragment?.childFragmentManager?.fragments?.get(0) is BackpressCallback){
            (fragment.childFragmentManager.fragments.get(0)as? BackpressCallback)?.onBackPressed()?.let {
                if(it) super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mSDPermission){
            if (resultCode == RESULT_OK) {
                if (data != null && data.data != null) {
                    val uriTree = data.data
                    val dFile = DocumentFile.fromTreeUri(this, uriTree!!)
                    if (dFile?.exists() ?: false) {
                        contentResolver.takePersistableUriPermission(uriTree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        AppPref.setSDKey(this, uriTree.toString())
                        goToBrowser(R.id.sdFragment)
                    }
                }
            }
        } else if (requestCode == mSDQPermission){
            if (resultCode == RESULT_OK) {
                if (data != null && data.data != null) {
                    val uriTree = data.data
                    if(checkSD(uriTree)){
                        val dFile = DocumentFile.fromTreeUri(this, uriTree!!)
                        if (dFile?.exists() ?: false) {
                            contentResolver.takePersistableUriPermission(uriTree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            AppPref.setSDKey(this, uriTree.toString())
                            goToBrowser(R.id.sdFragment)
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            showSelectWrongDialog()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            Constant.onHOMEPERMISSIONS_REQUEST_WRITE_STORAGE ->
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSION, FirebaseAnalyticsFactory.EVENT.ACCESSSUCCESS, null)
                    checkLocalPermission()
                } else if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                    FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSION, FirebaseAnalyticsFactory.EVENT.ACCESSCANCEL, null)
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
//                        val snackbar = Snackbar.make(container, resources.getString(R.string.runtimepermission), Snackbar.LENGTH_LONG)
//                        snackbar.setAction(resources.getString(R.string.drawer_setting)) {
//                            val intent = Intent()
//                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                            val uri = Uri.fromParts("package", getPackageName(), null)
//                            intent.data = uri
//                            startActivity(intent)
//                        }
//                        snackbar.show()
                    }
                }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkLocalPermission() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            navController.navigate(R.id.homeFragment)
            supportActionBar?.show()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) //開啟滑出Drawer手勢
            viewModel.updateTabMode(MainActivityViewModel.TabMode.Mid_Title_Only)
            viewModel.setMidTitleText("Home")
            viewModel.updateSystemMenuIconCount(0)  //首頁沒有menu
            navController.popBackStack(R.id.startPermissionFragment, true)  //已取得權限，移除該fragment
        } else {
            navController.navigate(R.id.startPermissionFragment)
            supportActionBar?.hide()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)    //鎖住滑出Drawer手勢
        }
    }

    override fun notifyMounted() {  //sd card mount event
        Constant.SD_ROOT = SystemUtil().getSDLocation(this)
        binding.navigationView.menu.findItem(R.id.sdFragment).setVisible(true)
        initHome()
    }

    override fun notifyUnmounted() {    //sd card unmount event
        Constant.SD_ROOT = SystemUtil().getSDLocation(this)
        binding.navigationView.menu.findItem(R.id.sdFragment).setVisible(false)
        viewModel.deleteAllFromRoot(Constant.STORAGEMODE_SD)
        initHome()
    }

    fun initHome(){
        //SD卡安裝、移除後都需重新讀取檔案，故在此初始化Scan狀態
        Constant.sdMediaScanState[1] = Constant.ScanState.NONE
        Constant.sdMediaScanState[2] = Constant.ScanState.NONE
        Constant.sdMediaScanState[3] = Constant.ScanState.NONE
        Constant.sdMediaScanState[4] = Constant.ScanState.NONE

        val fragment = this.supportFragmentManager.findFragmentById(R.id.container)
        if(fragment?.childFragmentManager?.fragments?.get(0) is HomeFragment){
            (fragment.childFragmentManager.fragments.get(0) as? HomeFragment)?.initHome()
        } else if (fragment?.childFragmentManager?.fragments?.get(0) is OTGFragment){
            if (UsbUtils.usbDevice == null || !UsbUtils.hasUSBPermission()) {
                navController.popBackStack()    //pop出該層fragment(即返回上一頁)
            }
        }
    }

    //更新Fragment頁面
    fun actionFinishedReminder(resId: Int){
        val fragment = this.supportFragmentManager.findFragmentById(R.id.container)
        if (fragment != null && fragment.view != null)
            Snackbar.make(fragment.view!!, resId, Snackbar.LENGTH_LONG).setAction("Action", null).show()
        val childFragment = fragment?.childFragmentManager?.fragments?.get(0)
        if (childFragment is LocalTabFragment){
            childFragment.doRefresh()
        } else if (childFragment is OTGFragment){
            childFragment.doRefresh()
        }
    }

    fun checkSDPermission(): Boolean {
        val sdKey = AppPref.getSDKey(this)
        Log.d(TAG, "sdKey: $sdKey")
        if (!sdKey.equals("")) {
            val uriSDKey = Uri.parse(sdKey)
            val f = DocumentFile.fromTreeUri(this, uriSDKey)
            if (f != null && f.exists()) {
                return true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestSDCardQPermissionDialog(true)
            return false
        } else if (MainApplication().OSisAfterNougat()) {
            Log.d(TAG, ">N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            requestSDCardPermission()
            oneSecond = false
            Thread(Runnable {
                try {
                    Thread.sleep(100)
                    oneSecond = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }).start()
            return false
        } else {
            Log.d(TAG, "<N, SDK_INT: " + android.os.Build.VERSION.SDK_INT)
            requestSDCardPermissionDialog()
            return false
        }
    }

    private fun requestSDCardPermission() {
        if (MainApplication().OSisAfterNougat()) {
            val sdPath = SystemUtil().getSDLocation(this)
            if (sdPath == null) {
                Log.d(TAG, "sdPath is null")
                return
            }
            val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.getStorageVolume(File(sdPath))
            val intent = storageVolume!!.createAccessIntent(null)
            try {
                Log.d(TAG, "startActivityForResult, mSDPermission:$mSDPermission")
                startActivityForResult(intent, mSDPermission)
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "ActivityNotFoundException")
                requestSDCardPermissionDialog()
            }
        }
    }

    private fun requestSDCardPermissionDialog() {
        //*索取權限
        val sdKey = AppPref.getSDKey(this)
        if (!sdKey.equals("")) {
            //釋放已記錄的權限
            contentResolver.releasePersistableUriPermission(
                Uri.parse(sdKey),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            AppPref.setSDKey(this, "")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.nav_sd)
        builder.setIcon(R.drawable.ic_drawer_microsd_grey)
        builder.setView(R.layout.dialog_connect_sd)
        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.setPositiveButton(getString(R.string.confirm), null)
        builder.setCancelable(false)
        val dialog = builder.show()
        val posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        posBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, mSDPermission)
            dialog.dismiss()
        }
        posBtn.textSize = 18f
        val negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negBtn.setOnClickListener {
            dialog.dismiss()
        }
        negBtn.textSize = 18f

        val viewerPager = dialog.findViewById(R.id.viewer_pager_sd) as ViewPagerZoomFixed?
        viewerPager?.setAdapter(ViewerPagerAdapterSD(this))
        viewerPager?.setCurrentItem(0)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun requestSDCardQPermissionDialog(isShowRemindDialog: Boolean) {
        //*索取權限
        val sdKey = AppPref.getSDKey(this)
        if (!sdKey.equals("")) {
            //釋放已記錄的權限
            contentResolver.releasePersistableUriPermission(Uri.parse(sdKey), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            AppPref.setSDKey(this, "")
        }

        if (!isShowRemindDialog){
            startSDQPermission()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.reminder)
        builder.setIcon(R.drawable.icon_elite_logo)
        builder.setView(R.layout.dialog_ask_exit)
        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.setPositiveButton(getString(R.string.confirm), null)
        builder.setCancelable(false)
        val dialog = builder.show()
        val tv = dialog.findViewById<TextView>(R.id.message)
        tv?.setText(getString(R.string.q_sd_permission_remind))
        val posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        posBtn.setOnClickListener {
            startSDQPermission()
        }
        posBtn.textSize = 18f
        val negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negBtn.setOnClickListener {
            dialog.dismiss()
        }
        negBtn.textSize = 18f
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun showSelectWrongDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.reminder)
        builder.setIcon(R.drawable.icon_elite_logo)
        builder.setView(R.layout.dialog_ask_exit)
        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.setPositiveButton(getString(R.string.permission_tryagain), null)
        builder.setCancelable(false)
        val dialog = builder.show()
        val tv = dialog.findViewById<TextView>(R.id.message)
        tv?.setText(getString(R.string.q_sd_permission_selectwrong))
        val posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        posBtn.setOnClickListener {
            requestSDCardQPermissionDialog(false)
        }
        posBtn.textSize = 18f
        val negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negBtn.setOnClickListener {
            dialog.dismiss()
        }
        negBtn.textSize = 18f
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun startSDQPermission(){
        val sdPath = FileFactory.getInstance().getSdCardPath(this@MainActivity)
        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.getStorageVolume(File(sdPath))
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent = storageVolume!!.createOpenDocumentTreeIntent()
        }
        try {
            startActivityForResult(intent, mSDQPermission)
        } catch (e: ActivityNotFoundException) {

        }
    }

    private fun checkSD(uri: Uri?): Boolean {
        if (!uri.toString().contains("primary")) {
            if (uri != null) {
                if (uri.path.toString().split(":").toTypedArray().size > 1) {
//                    FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSIONSD, FirebaseAnalyticsFactory.EVENT.PERMISSION_FAIL, null)
                    snackBarShow(R.string.snackbar_plz_select_top)
                } else {
                    val rootDir = DocumentFile.fromTreeUri(this, uri) //sd root path
                    if (rootDir != null && rootDir.exists()) {
                        var bSDCard = false
                        if (FileFactory().isSamsungStyle(this)) {
                            val smSDPath: String? = FileFactory().getSamsungStyleOuterStoragePath(this)
                            val rootName: String? = rootDir.getName()
                            if (smSDPath != null && rootName != null) {
                                if (smSDPath.contains(rootName)) bSDCard = true
                            }
                        } else {
                            val sdCardFileName: ArrayList<String>? = FileFactory().getSDCardFileName(this)
                            if (sdCardFileName == null)
                                bSDCard = false
                            else
                                bSDCard = FileFactory.getInstance().doFileNameCompare(rootDir.listFiles(), sdCardFileName)
                        }
                        if (bSDCard) {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            AppPref.setSDKey(this, uri.toString())
//                            FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSIONSD, FirebaseAnalyticsFactory.EVENT.PERMISSION_SUCCESS, null)
                            return true
                        } else {
//                            FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSIONSD, FirebaseAnalyticsFactory.EVENT.PERMISSION_FAIL, null)
                            snackBarShow(R.string.snackbar_plz_select_sd)
                        }
                    } else {
                        snackBarShow(R.string.no_sd)
                    }
                }
            }
        } else {
//            FirebaseAnalyticsFactory.getInstance(mContext).sendEvent(FirebaseAnalyticsFactory.FRAGMENT.PERMISSIONSD, FirebaseAnalyticsFactory.EVENT.PERMISSION_FAIL, null)
            snackBarShow(R.string.snackbar_plz_select_sd)
        }
        return false
    }

    private fun initBroadcast() {
        val filter = IntentFilter()
        filter.addAction(UsbUtils.ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED)
        registerReceiver(usbReceiver, filter)
        getScreenSize()
        SDCardReceiver.instance.registerObserver(this)
        checkOtgDevice()
    }

    private fun getScreenSize(){
        val displaymetrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics)
        Constant.mPortraitScreenWidth = displaymetrics.widthPixels
        Constant.mPortraitScreenHeight = displaymetrics.heightPixels
    }

    fun checkOtgDevice() {
        val isFindDevice = UsbUtils.isOtgDeviceExist(this)
        binding.navigationView.menu.findItem(R.id.otgFragment).setVisible(isFindDevice)
        initHome()
    }

    val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbUtils.ACTION_USB_PERMISSION == action) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                    UsbUtils.discoverDevices(this@MainActivity)
                    val arguments = Bundle()
                    arguments.putString("root", "/")   //讀取本地路徑
                    navController.navigate(R.id.otgFragment, arguments)
                } else {
                    Log.e("Permission", "False")
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action){
                checkOtgDevice()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action){
                checkOtgDevice()
                viewModel.deleteAllFromRoot(Constant.STORAGEMODE_OTG)
            }
        }
    }

    private fun snackBarShow(resId: Int){
        val fragment = this.supportFragmentManager.findFragmentById(R.id.container)
        if (fragment != null && fragment.view != null)
            Snackbar.make(fragment.view!!, resId, Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }
}