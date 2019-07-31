package com.transcend.otg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import com.transcend.otg.databinding.ActivityMainBinding
import com.transcend.otg.utilities.BackpressCallback
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.transcend.otg.adapter.DropDownAdapter
import com.transcend.otg.utilities.Constant
import com.transcend.otg.viewmodels.BrowserViewModel
import com.transcend.otg.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), EULAFragment.OnEulaClickListener, DropDownAdapter.OnDropdownItemSelectedListener {
    override fun onDropdownItemSelected(position: Int) {
        if(position > 0){
            var nowPath = browserViewModel.livePath.value
            var nowFile = File(nowPath)
            if(nowFile.exists()){
                for(parentCount in 0 until position){
                    nowFile = nowFile.parentFile
                }
            }
            adapter.updateDropDownList(nowFile.absolutePath)
            browserViewModel.doLoadFiles(nowFile.absolutePath)
        }
    }

    override fun onEulaAgreeClick(v: View) {
        Toast.makeText(this, "按了EULA", Toast.LENGTH_SHORT).show()
    }

    val eulaPath:String = "file:///android_asset/EULA.html"
    val statementPath = "file:///android_asset/Statement.html"
    val helpPath = "https://help.transcendcloud.com/Elite/Android/TW"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var adapter: DropDownAdapter
    private lateinit var spinner: AppCompatSpinner
    private lateinit var browserViewModel: BrowserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        browserViewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)

        binding.viewModel = viewModel
        binding.browserViewModel = browserViewModel
        EULAFragment.setOnEulaClickListener(this)
        drawerLayout = binding.drawerLayout

        navController = findNavController(R.id.container)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.browserFragment) {
                viewModel.dropdownVisibility.set(View.VISIBLE)
                Constant.DropDownMainTitle = Constant.LOCAL_ROOT
                adapter.updateDropDownList(Constant.LOCAL_ROOT)
                adapter.setMainPage(Constant.getDeviceName())
            } else {
                viewModel.dropdownVisibility.set(View.GONE)
            }
        }

        spinner = main_dropdown
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
        adapter = DropDownAdapter(this, dropdown_arrow)
        spinner.adapter = adapter

        adapter.setOnDropdownItemSelectedListener(this)

        browserViewModel.livePath.observe(this, Observer<String> {path->
            if(path != null){
                adapter.updateDropDownList(path)
            }
        })

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

//    private fun replaceFragment(fragment:String){
//        var f:Fragment
//        when(fragment){
//            getString(R.string.eulaTitle) -> f = EULAFragment.newInstance(true, eulaPath)
//            getString(R.string.statementTitle) -> f = StatementFragment.newInstance(statementPath)
//            getString(R.string.helpTitle) -> f = HelpFragment.newInstance(helpPath)
//            getString(R.string.feedbackTitle) -> f = FeedbackFragment.newInstance()
//            getString(R.string.browserTitle) -> f = BrowserFragment.newInstance()
//            else -> f = EULAFragment.newInstance(true, eulaPath)
//        }
//
//        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
//    }



    override fun onBackPressed() {
        val fragment = this.supportFragmentManager.findFragmentById(R.id.container)

        if(fragment?.childFragmentManager?.fragments?.get(0) is BackpressCallback){
            (fragment?.childFragmentManager?.fragments?.get(0)as? BackpressCallback)?.onBackPressed()?.let {
                if(it) super.onBackPressed()
            }
        }else{
            super.onBackPressed()
        }
//
//        (fragment as? BackpressCallback)?.onBackPressed()?.let {
//            if(it) super.onBackPressed()
//        }
//        (fragment as? BackpressCallback)?.onBackPressed()?.not().let {
//            super.onBackPressed()
//        }
    }
}
