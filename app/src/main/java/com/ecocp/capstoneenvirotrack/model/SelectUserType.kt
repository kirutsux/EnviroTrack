package com.ecocp.capstoneenvirotrack.model

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.MainActivity

class SelectUserType : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user_type)

        val btnPCO: Button = findViewById(R.id.btnPCO)
        val btnServiceProvider: Button = findViewById(R.id.btnServiceProvider)

        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        btnPCO.setOnClickListener {
            sharedPreferences.edit().putString("selectedUserType", "PCO").apply()
            navigateToLogin()
        }

        btnServiceProvider.setOnClickListener {
            sharedPreferences.edit().putString("selectedUserType", "Service Provider").apply()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Prevent going back to this screen
    }
}