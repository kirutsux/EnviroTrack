package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.all.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class SPMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login session
        val auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

        if (!isLoggedIn || auth.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_spmain)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.sp_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.spBottomNavigation)
        bottomNav.setupWithNavController(navController)

        // Fragments where bottom nav should be hidden
        val hideBottomNavOn = setOf(
            R.id.SP_Servicerequest,         // Service Requests list
            R.id.SP_ServiceRequestDetails   // Service Request details
        )

        // Toggle bottom nav visibility on destination change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = if (destination.id in hideBottomNavOn) View.GONE else View.VISIBLE
        }
    }
}
