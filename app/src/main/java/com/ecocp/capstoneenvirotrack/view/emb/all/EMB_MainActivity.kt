package com.ecocp.capstoneenvirotrack.view.emb.all

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class EMB_MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emb_main)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_emb) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        bottomNav = findViewById(R.id.bottom_nav_emb)
        bottomNav.setupWithNavController(navController)

        // Keep correct item highlighted when navigating
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.embDashboard -> bottomNav.selectedItemId = R.id.nav_emb_dashboard
                R.id.checklistFragment -> bottomNav.selectedItemId = R.id.nav_checklist
                R.id.embProfile -> bottomNav.selectedItemId = R.id.nav_emb_profile
                else -> bottomNav.selectedItemId = R.id.nav_emb_dashboard
            }
        }

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // No bottom padding
            insets
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}