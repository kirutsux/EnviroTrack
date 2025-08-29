package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ecocp.capstoneenvirotrack.R

class ServiceProviderDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_service_provider_dashboard, container, false)

        // Add a temporary TextView for testing
        val testText = TextView(requireContext())
        testText.text = "Service Provider Dashboard (Under Development)"
        testText.textSize = 18f
        testText.setPadding(16, 16, 16, 16)

        // Add the TextView to the layout (since the XML is blank)
        (view as ViewGroup).addView(testText)

        return view
    }
}