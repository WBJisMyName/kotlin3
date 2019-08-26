package com.transcend.otg

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.transcend.otg.databinding.ActivityMainBinding
import com.transcend.otg.utilities.BackpressCallback
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
        binding.navigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id) {
//                R.id.EULAFragment -> Toast.makeText(this@MainActivity, "EULA", Toast.LENGTH_SHORT)
//                R.id.helpFragment -> Toast.makeText(this@MainActivity, "Help", Toast.LENGTH_SHORT)
//                R.id.feedbackFragment -> Toast.makeText(this@MainActivity, "Feedback", Toast.LENGTH_SHORT)
//                R.id.statementFragment -> Toast.makeText(this@MainActivity, "Statement", Toast.LENGTH_SHORT)
                R.id.browserFragment -> {
                    arguments?.putString("root", Constant.LOCAL_ROOT)   //讀取本地路徑
                }
                R.id.sdFragment -> {
                    arguments?.putString("root", SystemUtil().getSDLocation(this@MainActivity))   //讀取sd路徑
                }
            }
        }
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

    override fun onPause() {
        super.onPause()

    }
}
