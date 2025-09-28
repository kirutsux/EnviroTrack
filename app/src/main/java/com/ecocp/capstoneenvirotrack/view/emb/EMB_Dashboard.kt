package com.ecocp.capstoneenvirotrack.view.emb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.android.material.navigation.NavigationView
import androidx.activity.OnBackPressedCallback

class EMB_Dashboard : Fragment() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.emb_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize DrawerLayout
        drawerLayout = view.findViewById(R.id.drawer_layout)

        // Set up menu icon to toggle drawer
        view.findViewById<View>(R.id.menu_icon).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Set up exit button to close drawer
        view.findViewById<View>(R.id.exit_button).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Set up fragment navigation for each icon
        view.findViewById<LinearLayout>(R.id.CNC_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embCNCFragment)
        }
        view.findViewById<LinearLayout>(R.id.SMR_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embSMRFragment)
        }
        view.findViewById<LinearLayout>(R.id.OPMS_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embOPMSFragment)
        }
        view.findViewById<LinearLayout>(R.id.HMS_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embHMSFragment)
        }
        view.findViewById<LinearLayout>(R.id.CRS_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embCRSFragment)
        }
        view.findViewById<LinearLayout>(R.id.PCO_icon).setOnClickListener {
            findNavController().navigate(R.id.action_embDashboard_to_embPCOFragment)
        }
    }

    // Handle back press to close drawer if open
    override fun onStart() {
        super.onStart()
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }
}