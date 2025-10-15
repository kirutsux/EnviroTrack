package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class COMP_CNCRenewalForm : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.pco_cnc_renewal_form, container, false)
    }

}