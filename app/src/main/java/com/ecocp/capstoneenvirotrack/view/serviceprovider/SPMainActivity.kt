package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class SPMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spmain)

        // Get the NavHostFragment using its ID
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.sp_nav_host_fragment) as NavHostFragment

        // Get the NavController from the NavHostFragment
        val navController = navHostFragment.navController

        // Hook up BottomNavigationView with NavController
        val bottomNav = findViewById<BottomNavigationView>(R.id.spBottomNavigation)
        bottomNav.setupWithNavController(navController)
    }
}
