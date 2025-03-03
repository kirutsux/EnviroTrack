package com.ecocp.capstoneenvirotrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.emb.EMB_Dashboard

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load EMB_Dashboard Fragment on Startup
        if (savedInstanceState == null) {
            replaceFragment(EMB_Dashboard())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
