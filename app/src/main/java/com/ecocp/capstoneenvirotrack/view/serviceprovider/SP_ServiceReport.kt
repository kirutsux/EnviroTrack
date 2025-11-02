package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

class SP_ServiceReport : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_service_report, container, false)

        val companyName = arguments?.getString("companyName") ?: "Unknown"
        view.findViewById<TextView>(R.id.txtCompanyName).text = companyName

        view.findViewById<Button>(R.id.btnDownload).setOnClickListener {
            Toast.makeText(requireContext(), "Downloading report...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnShareReport).setOnClickListener {
            Toast.makeText(requireContext(), "Sharing report...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnBackToList).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return view
    }
}
