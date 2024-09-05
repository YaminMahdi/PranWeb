package com.mlab.pran

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.mlab.pran.databinding.ActivityMainBinding
import com.mlab.pran.ui.WebViewFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController : NavController
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
////            binding.navView.layoutParams = (binding.navView.layoutParams as ViewGroup.MarginLayoutParams).apply {
////                setMargins(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
////            }
//            binding.navView.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.apply {
            drawerToggle =  ActionBarDrawerToggle(this@MainActivity, binding.drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            binding.drawerLayout.addDrawerListener(drawerToggle)
            binding.root.post{
                drawerToggle.syncState()
            }
        }
        binding.navView.menu.apply {
            clear()
            add(0, 0, Menu.NONE, "Home").setIcon(R.drawable.home)
            Website.entries.forEach {
                add(0, it.ordinal+1, Menu.NONE, it.pageName).setIcon(R.drawable.web)
            }
            setGroupCheckable(0, true, true)
            getItem(0).isChecked = true
        }

        navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Website.entries.find { it.url == arguments?.getString("url") }?.also {
                if(!binding.navView.menu.getItem(it.ordinal+1).isChecked) {
                    binding.navView.menu.getItem(it.ordinal + 1).isChecked = true
                    supportActionBar?.title = it.pageName
                }
            } ?: run {
                if(!binding.navView.menu.getItem(0).isChecked)
                    binding.navView.menu.getItem(0).isChecked = true
            }
        }
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                0 -> navController.navigate(R.id.nav_home,null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_home, true).build())
                else -> navController.navigate(R.id.nav_web, bundleOf("url" to Website.entries[it.itemId-1].url))
            }
            binding.drawerLayout.closeDrawers()
            supportActionBar?.title = if(it.itemId == 0) "" else Website.entries[it.itemId-1].pageName
            true
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(binding.navView.menu, binding.drawerLayout)
        setupActionBarWithNavController(navController, binding.drawerLayout)
//        binding.navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val url = if(!WebViewFragment.currentUrl.isNullOrEmpty())
            WebViewFragment.currentUrl ?: ""
        else
            "https://www.pranfoods.net"
        if (drawerToggle.onOptionsItemSelected(item))
            return true
        // Handle other menu items here
        return when (item.itemId) {
            R.id.action_settings -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    putExtra(Intent.EXTRA_TITLE, url)
                    type = "text/x-uri"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}



