package com.ecocp.capstoneenvirotrack.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class SP_RegistrationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp__registration, container, false)

        // Initialize Spinner
        val spinner: Spinner = view.findViewById(R.id.spinnerServiceCategory)
        val categories = resources.getStringArray(R.array.service_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        return view
    }
}
