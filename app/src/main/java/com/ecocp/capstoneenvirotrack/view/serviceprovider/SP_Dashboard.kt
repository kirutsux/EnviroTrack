package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class SP_Dashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sp_dashboard, container, false)

        // find views
        val serviceRequestCard = view.findViewById<View>(R.id.cnc_card)
        val activeTasksCard = view.findViewById<View>(R.id.smr_card)
        val completedCard = view.findViewById<View>(R.id.hazewaste_card)
        val paymentsCard = view.findViewById<CardView?>(R.id.opms_card)

        // click listeners
        activeTasksCard?.setOnClickListener {
            findNavController().navigate(R.id.SP_ActiveTasks)
        }

        serviceRequestCard?.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_Servicerequest)
        }

        completedCard?.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_CompletedServices)
        }

        paymentsCard?.setOnClickListener {
            // prevent double taps
            it.isEnabled = false
            // Use the navigation action you added in nav_graph_sp.xml
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_Payments)
            // re-enable shortly after
            it.postDelayed({ it.isEnabled = true }, 400)
        }

        return view
    }
}
