package com.ecocp.capstoneenvirotrack.view.all

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ecocp.capstoneenvirotrack.databinding.FragmentRegulationDetailsBinding

class RegulationDetailsFragment : Fragment() {

    private var _binding: FragmentRegulationDetailsBinding? = null
    private val binding get() = _binding!!

    // Use Safe Args to get the arguments
    private val args: RegulationDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegulationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRegulationDetails()
        setupClickListeners()
    }

    private fun loadRegulationDetails() {
        binding.apply {
            // Get values from Safe Args (type-safe)
            tvDetailRegulationName.text = args.regulationName
            tvDetailRepublicAct.text = args.republicAct
            tvDetailDescription.text = args.description
            tvDetailComplianceRequirements.text = args.complianceRequirements

            btnBack.setOnClickListener {
                // Use NavController to navigate back
                findNavController().popBackStack()
            }
        }
    }

    private fun setupClickListeners() {
        val fileURL = args.fileURL

        binding.btnViewFile.apply {
            isEnabled = fileURL.isNotEmpty()
            setOnClickListener {
                if (fileURL.isNotEmpty()) {
                    openFile(fileURL)
                }
            }
        }
    }

    private fun openFile(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}