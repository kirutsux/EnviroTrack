package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class SP_ServiceRequestDetails : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_sp_service_request_details, container, false)

        // 🟢 Get data passed from previous fragment
        val companyName = arguments?.getString("companyName") ?: "Unknown"
        val serviceType = arguments?.getString("serviceType") ?: "Unknown"
        val location = arguments?.getString("location") ?: "N/A"
        val dateRequested = arguments?.getString("dateRequested") ?: "N/A"
        val status = arguments?.getString("status") ?: "N/A"

        // 🟢 Bind data to TextViews
        view.findViewById<TextView>(R.id.txtCompanyName).text = companyName
        view.findViewById<TextView>(R.id.txtServiceType).text = serviceType
        view.findViewById<TextView>(R.id.txtLocation).text = location
        view.findViewById<TextView>(R.id.txtDateRequested).text = dateRequested
        view.findViewById<TextView>(R.id.txtStatus).text = status

        // 🟢 Handle buttons
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        btnAccept.setOnClickListener {
            Toast.makeText(requireContext(), "✅ Request Accepted", Toast.LENGTH_SHORT).show()
            // TODO: Update Firestore status to "Accepted" here if connected
        }

        btnReject.setOnClickListener {
            Toast.makeText(requireContext(), "❌ Request Rejected", Toast.LENGTH_SHORT).show()
            // TODO: Update Firestore status to "Rejected" here if connected
        }

        return view
    }
}
