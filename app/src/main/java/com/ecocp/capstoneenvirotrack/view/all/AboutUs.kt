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

        // Open Facebook profile link automatically
        val facebookUrl = "https://www.facebook.com/kurtzyyy"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))

        // Try to open it in Facebook app if installed, else fallback to browser
        intent.setPackage("com.facebook.katana")
        if (intent.resolveActivity(requireActivity().packageManager) == null) {
            // Facebook app not installed, open in browser instead
            intent.setPackage(null)
        }

        startActivity(intent)

        // Optionally, you can close the fragment afterward
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
