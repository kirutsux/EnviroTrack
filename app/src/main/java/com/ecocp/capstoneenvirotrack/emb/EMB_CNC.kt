package com.ecocp.capstoneenvirotrack.emb

import android.os.Bundle
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ecocp.capstoneenvirotrack.R

class EMB_CNC : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emb_cnc)

        val businessList = listOf(
            Business("Dunkin' Donuts", R.drawable.ic_launcher_background, 75),
            Business("McDonald's", R.drawable.ic_launcher_background, 78)
        )

        val listView: ListView = findViewById(R.id.business_list)
        listView.adapter = BusinessAdapter(this, businessList)
    }
}