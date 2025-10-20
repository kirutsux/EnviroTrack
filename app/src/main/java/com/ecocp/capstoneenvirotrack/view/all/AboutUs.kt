package com.ecocp.capstoneenvirotrack.view.all

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class AboutUs : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        return inflater.inflate(R.layout.fragment_about_us, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🌐 Open EnviroTrack website automatically
        val websiteUrl = "https://envirotrackph.onhercules.app/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))

        // Open in browser (no need for package check like Facebook)
        intent.setPackage(null)

        startActivity(intent)

        // Optionally close the fragment after opening
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
