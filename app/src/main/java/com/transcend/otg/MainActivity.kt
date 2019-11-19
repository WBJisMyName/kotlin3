package com.transcend.otg

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.transcend.otg.databinding.ActivityMainBinding
import com.transcend.otg.sdcard.ViewerPagerAdapterSD
import com.transcend.otg.singleview.ViewPagerZoomFixed
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.SystemUtil
import com.transcend.otg.viewmodels.MainActivityViewModel
import java.io.File

class MainActivity : AppCompatActivity(), EULAFragment.OnEulaClickListener {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        binding.viewModel = viewModel

        EULAFragment.setOnEulaClickListener(this)

        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.container)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)    // enable overriding the default toolbar layout
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)  // show or hide the default home button

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)

        binding.dropdownArrow.setOnClickListener {
            view ->
            binding.mainDropdown.performClick()
        }

        navController.addOnDestinationChangedListener { controller, destination, arguments ->

            binding.toolbar.setNavigationIcon(R.drawable.ic_toggle_menu)    //TODO toggle 圖示暫時固定為menu樣式，待修改
            binding.toolbar.setNavigationOnClickListener {
                    v: View? ->
                drawerLayout.openDrawer(Gravity.LEFT)
            }

            when(destination.id) {
                //不須帶參數的Fragment切換讓他自動實作
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
                    setToolbarMode(MainActivityViewModel.TabMode.Browser)
                }
                R.id.sdFragment -> {
                    arguments?.putString("root", SystemUtil().getSDLocation(this@MainActivity))   //讀取sd路徑
                    setToolbarMode(MainActivityViewModel.TabMode.Browser)
                }
            }
        }

        checkLocalPermission()
    }

    fun setMidTitle(title: String){
        viewModel.setMidTitleText(title)
    }

    private fun setToolbarMode(mode: MainActivityViewModel.TabMode, menuCount: Int, mid_title: String){
        viewModel.updateTabMode(mode)
        viewModel.updateSystemMenuIconCount(menuCount)
        setMidTitle(mid_title)
    }

    private fun setToolbarMode(mode: MainActivityViewModel.TabMode){
        viewModel.updateTabMode(mode)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
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
            if (requestCode == RESULT_OK) {
                if (data != null && data.data != null) {
                    val uriTree = data.data
                    val dFile = DocumentFile.fromTreeUri(this, uriTree!!)
                    if (dFile!!.exists()) {

                        contentResolver.takePersistableUriPermission(
                            uriTree,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        AppPref.setSDKey(this, uriTree.toString())
                    } else {

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
        } else {
            navController.navigate(R.id.startPermissionFragment)
            supportActionBar?.hide()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)    //鎖住滑出Drawer手勢
        }
    }

    private fun checkSDPermission(): Boolean {
        val sdKey = AppPref.getSdKey(this)
        Log.d(TAG, "sdKey: $sdKey")
        if (!sdKey.equals("")) {
            val uriSDKey = Uri.parse(sdKey)
            val f = DocumentFile.fromTreeUri(this, uriSDKey)
            if (f != null && f.exists()) {
                return true
            }
        }

        //TODO Permission of Android Q
        if (MainApplication().OSisAfterNougat()) {
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
        val sdKey = AppPref.getSdKey(this)
        if (!sdKey.equals("")) {
            //釋放已記錄的權限
            contentResolver.releasePersistableUriPermission(
                Uri.parse(sdKey),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            AppPref.setSDKey(this, "")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("SD Card")
        builder.setIcon(R.drawable.ic_drawer_microsd_grey)
        builder.setView(R.layout.dialog_connect_sd)
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("Confirm", null)
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
}
