package com.ecocp.capstoneenvirotrack.businesses

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.commit
import com.ecocp.capstoneenvirotrack.R

class COMP_Dashboard : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_comp__dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get references to CardViews
        val cncCard = view.findViewById<CardView>(R.id.cnc_card)
        val smrCard = view.findViewById<CardView>(R.id.smr_card)
        val opmsCard = view.findViewById<CardView>(R.id.opms_card)
        val hazewasteCard = view.findViewById<CardView>(R.id.hazewaste_card)

        // Set OnClickListeners
        cncCard.setOnClickListener {
            navigateToFragment(COMP_CNC())
        }

        smrCard.setOnClickListener {
            navigateToFragment(COMP_SMR())
        }

        opmsCard.setOnClickListener {
            navigateToFragment(COMP_OPMS())
        }

        hazewasteCard.setOnClickListener {
            navigateToFragment(COMP_Hazwaste_Manifest())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, fragment)
            addToBackStack(null)
        }
    }
}