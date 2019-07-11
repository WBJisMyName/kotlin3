package com.wbj.kotlin3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.wbj.kotlin3.databinding.ActivityMainBinding
import com.wbj.kotlin3.utilities.BackpressCallback
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity(), EULAFragment.OnEulaClickListener {
    override fun onEulaAgreeClick(v: View) {
        Toast.makeText(this, "按了EULA", Toast.LENGTH_SHORT).show()
    }

    val eulaPath:String = "file:///android_asset/EULA.html"
    val statementPath = "file:///android_asset/Statement.html"
    val helpPath = "https://help.transcendcloud.com/Elite/Android/TW"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.buttonEULA.setOnClickListener{
            replaceFragment(binding.buttonEULA.text.toString())
        }
        binding.buttonStatement.setOnClickListener{
            replaceFragment(binding.buttonStatement.text.toString())
        }
        binding.buttonHelp.setOnClickListener{
            replaceFragment(binding.buttonHelp.text.toString())
        }
        binding.buttonFeedback.setOnClickListener{
            replaceFragment(binding.buttonFeedback.text.toString())
        }
        binding.buttonBrowser.setOnClickListener{
            replaceFragment(binding.buttonBrowser.text.toString())
        }

        EULAFragment.setOnEulaClickListener(this)

        drawerLayout = binding.drawerLayout

        navController = findNavController(R.id.container)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun replaceFragment(fragment:String){
        var f:Fragment
        when(fragment){
            getString(R.string.eulaTitle) -> f = EULAFragment.newInstance(true, eulaPath)
            getString(R.string.statementTitle) -> f = StatementFragment.newInstance(statementPath)
            getString(R.string.helpTitle) -> f = HelpFragment.newInstance(helpPath)
            getString(R.string.feedbackTitle) -> f = FeedbackFragment.newInstance()
            getString(R.string.browserTitle) -> f = BrowserFragment.newInstance()
            else -> f = EULAFragment.newInstance(true, eulaPath)
        }

        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }



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
