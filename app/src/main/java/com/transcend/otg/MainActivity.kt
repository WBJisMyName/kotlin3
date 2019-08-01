package com.transcend.otg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.transcend.otg.utilities.BackpressCallback
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.transcend.otg.databinding.ActivityMainBinding
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.SystemUtil

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

        EULAFragment.setOnEulaClickListener(this)

        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.container)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
//        binding.navigationView.setupWithNavController(navController)

        binding.navigationView.setNavigationItemSelectedListener(
            object: NavigationView.OnNavigationItemSelectedListener{
                override fun onNavigationItemSelected(item: MenuItem): Boolean {
                    val id = item.itemId
                    when(id){
                        R.id.EULAFragment -> navController.navigate(R.id.EULAFragment)
                        R.id.helpFragment -> navController.navigate(R.id.helpFragment)
                        R.id.feedbackFragment -> navController.navigate(R.id.feedbackFragment)
                        R.id.statementFragment -> navController.navigate(R.id.statementFragment)
                        R.id.browserFragment -> {
                            var bundle: Bundle = bundleOf("root" to Constant.LOCAL_ROOT)
                            navController.navigate(R.id.browserFragment, bundle)
                        }
                        R.id.sdFragment -> {
                            var bundle: Bundle = bundleOf("root" to SystemUtil().getSDLocation(this@MainActivity))
                            navController.navigate(R.id.browserFragment, bundle)
                        }
                    }
                    drawerLayout.closeDrawers()
                    return false
                }
            }
        )
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
//
//        (fragment as? BackpressCallback)?.onBackPressed()?.let {
//            if(it) super.onBackPressed()
//        }
//        (fragment as? BackpressCallback)?.onBackPressed()?.not().let {
//            super.onBackPressed()
//        }
    }

    override fun onPause() {
        super.onPause()

    }
}
