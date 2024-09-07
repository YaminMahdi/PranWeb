package com.mlab.pran

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.mlab.pran.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController : NavController
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListener()
    }

    private fun setupUI() {
        binding.apply {
            setSupportActionBar(contentMain.toolbar)
            supportActionBar?.apply {
                drawerToggle = ActionBarDrawerToggle(this@MainActivity, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
                drawerLayout.addDrawerListener(drawerToggle)
                root.post {
                    drawerToggle.syncState()
                }
            }

            navView.menu.apply {
                Website.entries.forEach {
                    add(0, it.ordinal, Menu.NONE, it.pageName).setIcon(it.icon)
                }
                setGroupCheckable(0, true, true)
                getItem(0).isChecked = true
            }

            navController =  (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
                .findNavController()

            setupActionBarWithNavController(navController, drawerLayout)
        }
    }

    private fun setupListener() {
        binding.apply {
            navController.addOnDestinationChangedListener { _, _, arguments ->
                arguments?.getString("url").website.also {
                    if (!navView.menu.getItem(it.ordinal).isChecked) {
                        navView.menu.getItem(it.ordinal).isChecked = true
                    }
                }
            }
            navView.setNavigationItemSelectedListener {
                drawerLayout.closeDrawers()
                val currentWebsite = Website.entries[it.itemId]
                viewModel.url = currentWebsite.url
                viewModel.currentIndex = it.itemId
                navController.navigate(R.id.nav_web,
                    bundleOf(
                        "url" to currentWebsite.url,
                        "index" to it.itemId
                    ),
                    NavOptions.Builder().build().apply {
                        shouldPopUpToSaveState()
                        shouldRestoreState()
                        shouldLaunchSingleTop()
                    }
                )
                true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item))
            return true
        // Handle other menu items here
        return when (item.itemId) {
            R.id.action_settings -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, viewModel.url)
                    putExtra(Intent.EXTRA_TITLE, viewModel.url)
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
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}



