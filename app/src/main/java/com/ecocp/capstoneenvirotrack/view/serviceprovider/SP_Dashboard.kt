package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class SP_Dashboard : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sp_dashboard, container, false)

        // 🟢 Find the Service Request Card by its ID (from XML)
        val serviceRequestCard = view.findViewById<View>(R.id.cnc_card)
        val activeTasksCard = view.findViewById<View>(R.id.smr_card)
        val completedCard = view.findViewById<View>(R.id.hazewaste_card)


        activeTasksCard.setOnClickListener {
            findNavController().navigate(R.id.SP_ActiveTasks)
        }
        // 🟢 Set click listener to navigate to Service Request fragment
        serviceRequestCard.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_Servicerequest)
        }
        // ✅ Set Click Listener
        completedCard.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_CompletedServices)
        }

        return view
    }
}
