package com.lasallecollegevancouver.gameinventoryapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.lasallecollegevancouver.gameinventoryapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.priceCheckFragment,
                R.id.collectionListFragment,
                R.id.wishlistListFragment
            )
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isOnboarding = destination.id == R.id.onboardingFragment
            binding.appBar.visibility = if (isOnboarding) View.GONE else View.VISIBLE
            binding.bottomNavigation.visibility = if (isOnboarding) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
