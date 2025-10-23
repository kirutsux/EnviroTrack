package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class SP_Profile : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_profile, container, false)

        // Navigate to SP_Account when "Account" button is tapped
        view.findViewById<View>(R.id.btnAccount).setOnClickListener {
            findNavController().navigate(R.id.SP_Account)
        }

        return view
    }
}
