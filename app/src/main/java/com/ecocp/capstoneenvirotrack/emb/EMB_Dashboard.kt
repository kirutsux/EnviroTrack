package com.ecocp.capstoneenvirotrack.emb

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ecocp.capstoneenvirotrack.R

class EMB_Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.emb_dashboard)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle click for CNC_icon to navigate to CNCActivity
        val cncIcon = findViewById<LinearLayout>(R.id.CNC_icon)
        cncIcon.setOnClickListener {
            val intent = Intent(this, EMB_CNC::class.java)
            startActivity(intent)
        }
    }
}
