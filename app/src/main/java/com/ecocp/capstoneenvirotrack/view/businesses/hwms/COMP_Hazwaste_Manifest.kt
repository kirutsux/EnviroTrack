package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.ecocp.capstoneenvirotrack.R

class COMP_Hazwaste_Manifest : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comp__hazwaste__manifest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // child NavHostFragment is declared in XML (see layout below)
        val navHostFragment = childFragmentManager.findFragmentById(R.id.hwms_nav_host_fragment) as? NavHostFragment
        // navHostFragment?.navController  // not needed now but kept for reference
    }
}
