package com.ecocp.capstoneenvirotrack.view.all

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.serviceprovider.SPMainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Firebase App Check setup
        FirebaseApp.initializeApp(this)
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())

        // ✅ Navigation setup
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // ✅ Bottom nav item handling
        bottomNavigationView.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.pcoDashboard -> R.id.pcoDashboard
                R.id.inboxFragment -> R.id.inboxFragment
                R.id.aiFaqBotFragment -> R.id.aiFaqBotFragment
                R.id.comp_Profile -> R.id.comp_Profile
                else -> return@setOnItemSelectedListener false
            }

            safeNavigate(destinationId)
            true
        }

        // ✅ Show bottom nav only on certain fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.pcoDashboard,
                R.id.chatFragment,
                R.id.aiFaqBotFragment,
                R.id.comp_Profile -> bottomNavigationView.visibility = View.VISIBLE
                else -> bottomNavigationView.visibility = View.GONE
            }

            logCurrentDestination(destination)
        }
    }

    // ✅ Lifecycle-safe navigation with retry
    private fun safeNavigate(destinationId: Int) {
        lifecycleScope.launch {
            try {
                val current = navController.currentDestination?.id
                Log.d("MainActivity", "Navigating from $current to $destinationId")

                if (current != destinationId) {
                    val navOptions = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(
                            R.id.pcoDashboard, // Keeps dashboard as base
                            inclusive = false
                        )
                        .build()

                    navController.navigate(destinationId, null, navOptions)
                } else {
                    Log.d("MainActivity", "Already on destination: $destinationId")
                }

            } catch (e: IllegalStateException) {
                Log.w("MainActivity", "Navigation failed: ${e.message}. Retrying...")
                delay(200)
                retryNavigate(destinationId)
            }
        }
    }

    // ✅ Retry with extra safety
    private fun retryNavigate(destinationId: Int) {
        lifecycleScope.launchWhenResumed {
            try {
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.pcoDashboard, false)
                    .build()
                navController.navigate(destinationId, null, navOptions)
                Log.d("MainActivity", "Retry navigation successful: $destinationId")
            } catch (e: Exception) {
                Log.e("MainActivity", "Retry navigation failed: ${e.message}")
            }
        }
    }

    // ✅ Log current destination only (instead of backstack)
    private fun logCurrentDestination(destination: NavDestination) {
        val destName = try {
            resources.getResourceEntryName(destination.id)
        } catch (e: Exception) {
            destination.id.toString()
        }
        Log.d("MainActivity", "Now on destination: $destName (${destination.id})")
    }
    override fun onStart() {
        super.onStart()

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val userType = prefs.getString("userType", null)

        val user = FirebaseAuth.getInstance().currentUser
        if (isLoggedIn && user != null) {
            when (userType) {
                "service_provider" -> {
                    startActivity(Intent(this, SPMainActivity::class.java))
                    finish()
                }

            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
