package com.ecocp.capstoneenvirotrack.emb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class EMB_Dashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.emb_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up fragment navigation for each icon
        view.findViewById<LinearLayout>(R.id.CNC_icon).setOnClickListener { replaceFragment(EMB_CNC()) }
        view.findViewById<LinearLayout>(R.id.SMR_icon).setOnClickListener { replaceFragment(EMB_SMR()) }
        view.findViewById<LinearLayout>(R.id.OPMS_icon).setOnClickListener { replaceFragment(EMB_OPMS()) }
        view.findViewById<LinearLayout>(R.id.HMS_icon).setOnClickListener { replaceFragment(EMB_HMS()) }
        view.findViewById<LinearLayout>(R.id.CRS_icon).setOnClickListener { replaceFragment(EMB_CRS()) }
        view.findViewById<LinearLayout>(R.id.PCO_icon).setOnClickListener { replaceFragment(EMB_PCO()) }
    }

    // Function to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}
