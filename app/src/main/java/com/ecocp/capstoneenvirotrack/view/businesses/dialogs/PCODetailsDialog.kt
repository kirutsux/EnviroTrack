package com.ecocp.capstoneenvirotrack.view.businesses.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ecocp.capstoneenvirotrack.R
import java.net.URLDecoder

class PCODetailsDialog : DialogFragment() {

    companion object {
        fun newInstance(
            fullName: String,
            position: String,
            accreditationNumber: String,
            company: String,
            educationBackground: String,
            experienceEnvManagement: String,
            governmentIdUrl: String?,
            certificateUrl: String?,
            trainingCertificateUrl: String?
        ): PCODetailsDialog {
            val fragment = PCODetailsDialog()
            val args = Bundle().apply {
                putString("fullName", fullName)
                putString("position", position)
                putString("accreditationNumber", accreditationNumber)
                putString("company", company)
                putString("educationBackground", educationBackground)
                putString("experienceEnvManagement", experienceEnvManagement)
                putString("governmentIdUrl", governmentIdUrl)
                putString("certificateUrl", certificateUrl)
                putString("trainingCertificateUrl", trainingCertificateUrl)
            }
            fragment.arguments = args
            return fragment
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pco_details, null)
        val args = requireArguments()

        val tvFullName = view.findViewById<TextView>(R.id.tvFullName)
        val tvPosition = view.findViewById<TextView>(R.id.tvPosition)
        val tvAccreditationNo = view.findViewById<TextView>(R.id.tvAccreditationNo)
        val tvCompany = view.findViewById<TextView>(R.id.tvCompany)
        val tvEducation = view.findViewById<TextView>(R.id.tvEducation)
        val tvExperience = view.findViewById<TextView>(R.id.tvExperience)
        val governmentId = view.findViewById<TextView>(R.id.governmentId)
        val tvDutyCert = view.findViewById<TextView>(R.id.tvDutyCert)
        val tvTrainingCert = view.findViewById<TextView>(R.id.tvTrainingCert)

        tvFullName.text = "Full Name: ${args.getString("fullName")}"
        tvPosition.text = "Position: ${args.getString("position")}"
        tvAccreditationNo.text = "Accreditation #: ${args.getString("accreditationNumber")}"
        tvCompany.text = "Company: ${args.getString("company")}"
        tvEducation.text = "Educational Background: ${args.getString("educationBackground")}"
        tvExperience.text = "Experience: ${args.getString("experienceEnvManagement")}"

        // Extract and format filenames from URLs
        val governmentIdUrl = args.getString("governmentIdUrl")
        val certificateUrl = args.getString("certificateUrl")
        val trainingCertificateUrl = args.getString("trainingCertificateUrl")

        governmentId.text = "${getFileName(governmentIdUrl)}"
        tvDutyCert.text = "${getFileName(certificateUrl)}"
        tvTrainingCert.text = "${getFileName(trainingCertificateUrl)}"

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    // Function to get only the file name from Firebase URL
    private fun getFileName(url: String?): String {
        if (url.isNullOrBlank()) return "No file submitted"
        return try {
            val decodedUrl = URLDecoder.decode(url, "UTF-8")
            decodedUrl.substringAfterLast("/").substringBefore("?")
        } catch (e: Exception) {
            "Invalid file"
        }
    }
}
