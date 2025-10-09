package com.ecocp.capstoneenvirotrack.view.emb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class EMB_Dashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ✅ Inflate the new LinearLayout-based layout
        return inflater.inflate(R.layout.emb_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Set up click listeners for each icon
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
}
