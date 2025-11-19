package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServiceReportBinding
import com.google.android.material.button.MaterialButton
import java.io.File

class SP_ServiceReport : Fragment() {

    private var _binding: FragmentSpServiceReportBinding? = null
    private val binding get() = _binding!!

    // Optional: track download state so UI can show / hide progress
    private var isDownloading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServiceReportBinding.inflate(inflater, container, false)
        val view = binding.root

        // ---- safe wiring using binding (no casting mistakes) ----
        binding.btnBack.setOnClickListener {
            // navigate back
            findNavController().popBackStack()
        }

        // Buttons in your XML are ImageButton for download/share and MaterialButtons for footer:
        binding.btnDownload.setOnClickListener { startDownload() }
        binding.btnShare.setOnClickListener { shareReport() }

        binding.btnShareReport.setOnClickListener { shareReport() }
        binding.btnBackToList.setOnClickListener { findNavController().popBackStack() }

        // Populate fields from bundle arguments (if any)
        populateFromArgs()

        return view
    }

    private fun populateFromArgs() {
        // Read the bundle passed from CompletedServices (if any)
        val args = arguments
        args?.let {
            val company = it.getString("companyName") ?: ""
            val serviceTitle = it.getString("serviceTitle") ?: ""
            val status = it.getString("status") ?: ""
            val compliance = it.getString("compliance") ?: ""
            val clientName = it.getString("clientName") ?: ""
            val requestId = it.getString("requestId") ?: ""

            // Set UI values (IDs must match your XML)
            binding.txtCompanyName.text = company
            binding.txtServiceType.text = serviceTitle
            binding.txtRemarks.text = status + if (compliance.isNotBlank()) " • $compliance" else ""
            // optionally use other textviews
            if (requestId.isNotBlank()) binding.txtReportRef.text = "Ref: $requestId"

            // If you have a file name passed in bundle:
            val fileName = it.getString("fileName")
            if (!fileName.isNullOrBlank()) {
                binding.txtFileName.text = fileName
                // optionally set file size text if passed
                val fileSize = it.getString("fileSize")
                if (!fileSize.isNullOrBlank()) binding.txtFileSize.text = fileSize
            }
        }
    }

    private fun startDownload() {
        // Basic stub: show progress bar while downloading, then hide it.
        // Replace this with your real download logic (WorkManager / Retrofit / OkHttp).
        if (isDownloading) return

        isDownloading = true
        binding.progressDownload.visibility = View.VISIBLE
        binding.progressDownload.progress = 0

        // Simulate progress quickly on UI thread (remove when using real downloader)
        binding.progressDownload.postDelayed({
            binding.progressDownload.progress = 100
            binding.progressDownload.visibility = View.GONE
            isDownloading = false
            // after download, set txtFileName/txtFileSize to the actual downloaded file if needed
        }, 1200)
    }

    private fun shareReport() {
        // If you already downloaded the file and have a File reference, share via FileProvider.
        // This is an example that assumes you have a file in app's cache directory named "Completion_Report.pdf".
        // Replace with your actual file path.

        // first check if file exists; if not, show share of a link or simple text
        val cachedFile = File(requireContext().cacheDir, "Completion_Report.pdf")
        if (cachedFile.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                cachedFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share report"))
        } else {
            // fallback: share a simple text message
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Service report for ${binding.txtCompanyName.text}")
            }
            startActivity(Intent.createChooser(textIntent, "Share report"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
