package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ecocp.capstoneenvirotrack.R

class COMP_Hazwaste_Manifest : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comp__hazwaste__manifest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGenerator = view.findViewById<CardView>(R.id.btn_generator)
        btnGenerator?.setOnClickListener {
            navigateToFragment(GeneratorDashboardFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true) // ✅ optional, improves performance and state handling
            replace(R.id.nav_host_fragment, fragment)
            addToBackStack(null)
        }
    }
}
