package com.ecocp.capstoneenvirotrack.view.all

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.RegulationAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentRegulationsBinding
import com.ecocp.capstoneenvirotrack.model.Regulation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegulationsFragment : Fragment() {

    private var _binding: FragmentRegulationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var regulationAdapter: RegulationAdapter
    private val regulations = mutableListOf<Regulation>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegulationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadRegulations()
    }

    private fun setupRecyclerView() {
        regulationAdapter = RegulationAdapter(
            onItemClick = { regulation ->
                showRegulationDetails(regulation)
            },
            onDownloadClick = { regulation ->
                downloadFile(regulation)
            }
        )

        binding.rvRegulations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = regulationAdapter
        }
    }

    private fun loadRegulations() {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snapshot = db.collection("regulations").get().await()

                regulations.clear()
                for (document in snapshot.documents) {
                    val regulation = document.toObject(Regulation::class.java)?.copy(
                        id = document.id
                    )
                    regulation?.let { regulations.add(it) }
                }

                if (regulations.isEmpty()) {
                    binding.tvNoData.visibility = View.VISIBLE
                    binding.rvRegulations.visibility = View.GONE
                } else {
                    binding.tvNoData.visibility = View.GONE
                    binding.rvRegulations.visibility = View.VISIBLE
                    regulationAdapter.submitList(regulations.sortedByDescending { it.createdAt })
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error loading regulations: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // Fixed: Use NavController + Safe Args instead of manual Fragment transaction
    private fun showRegulationDetails(regulation: Regulation) {
        val action = RegulationsFragmentDirections
            .actionEmbRegulationsFragmentToRegulationDetailsFragment(
                regulationId = regulation.id.orEmpty(),
                regulationName = regulation.regulationName.orEmpty(),
                republicAct = regulation.republicAct.orEmpty(),
                description = regulation.description.orEmpty(),
                complianceRequirements = regulation.complianceRequirements.orEmpty(),
                fileURL = regulation.fileURL.orEmpty()
            )

        try {
            findNavController().navigate(action)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Cannot open regulation details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFile(regulation: Regulation) {
        val url = regulation.fileURL
        if (url.isBlank()) {
            Toast.makeText(requireContext(), "No file available for download", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Unable to open file: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}