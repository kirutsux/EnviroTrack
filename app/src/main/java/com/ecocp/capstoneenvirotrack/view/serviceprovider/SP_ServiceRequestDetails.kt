package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class SP_ServiceRequestDetails : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_sp_service_request_details, container, false)

        // 🟢 Receive complete arguments from ServiceRequest list
        val companyName     = arguments?.getString("companyName")     ?: "Unknown"
        val serviceType     = arguments?.getString("serviceTitle")    ?: "Unknown"
        val location        = arguments?.getString("origin")          ?: "N/A"
        val dateRequested   = arguments?.getString("dateRequested")   ?: "N/A"
        val providerContact = arguments?.getString("providerContact") ?: "N/A"
        val providerName    = arguments?.getString("providerName")    ?: ""
        val status          = arguments?.getString("status")          ?: "Pending"
        val notes           = arguments?.getString("notes")           ?: "No additional notes"
        val attachment      = arguments?.getString("attachment")      ?: "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

        // 🟢 Bind basic fields
        view.findViewById<TextView>(R.id.txtCompanyName).text = companyName
        view.findViewById<TextView>(R.id.txtServiceType).text = serviceType
        view.findViewById<TextView>(R.id.txtLocation).text = location
        view.findViewById<TextView>(R.id.txtDateRequested).text = dateRequested

        // 🟢 Contact person (providerName + providerContact)
        view.findViewById<TextView>(R.id.txtContactPerson).text =
            if (providerName.isNotEmpty()) "$providerName — $providerContact"
            else providerContact

        // 🟢 Status Pill
        view.findViewById<TextView>(R.id.txtStatusPill).text = status

        // 🟢 Notes / Special Instructions
        view.findViewById<TextView>(R.id.txtNotes).text = notes

        // 🟢 Attachments
        val txtAttach = view.findViewById<TextView>(R.id.txtAttachments)
        txtAttach.text = attachment.substringAfterLast("/")
        txtAttach.tag = attachment

        txtAttach.setOnClickListener {
            openAttachment(attachment)
        }

        // (Future) Company Logo
        val imgLogo = view.findViewById<ImageView>(R.id.imgCompanyLogo)
        // If later attachment becomes a URL: Glide.with(this).load(attachment).into(imgLogo)

        // 🟢 Buttons: Accept & Reject
        view.findViewById<Button>(R.id.btnAccept).setOnClickListener {
            Toast.makeText(requireContext(), "✅ Request Accepted", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnReject).setOnClickListener {
            Toast.makeText(requireContext(), "❌ Request Rejected", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun openAttachment(path: String) {
        if (path.startsWith("http")) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(path)))
        } else {
            // DEV: your file is a local path (uploaded via ChatGPT)
            Toast.makeText(requireContext(), "Attachment: $path", Toast.LENGTH_LONG).show()
        }
    }
}
