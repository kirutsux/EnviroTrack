package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.HWMSAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.HWMSApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HWMSDashboardFragment : Fragment() {

    private lateinit var binding: FragmentHwmsDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val applications = mutableListOf<HWMSApplication>()
    private lateinit var adapter: HWMSAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHwmsDashboardBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupListeners()
        fetchApplications()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = HWMSAdapter(applications) { selectedApp ->
            Toast.makeText(requireContext(), "Selected: ${selectedApp.wasteType}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewHWMS.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHWMS.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddApplication.setOnClickListener {
            navigateToFragment(HwmsStep1Fragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun fetchApplications() {
        val pcoId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("HazardousWasteManifest")
            .whereEqualTo("pcoId", pcoId)
            .get()
            .addOnSuccessListener { result ->
                applications.clear()
                for (doc in result) {
                    val app = HWMSApplication(
                        id = doc.id,
                        wasteType = doc.getString("wasteType") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        storageLocation = doc.getString("storageLocation") ?: "",
                        dateGenerated = doc.getString("dateGenerated") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                    applications.add(app)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load applications.", Toast.LENGTH_SHORT).show()
            }
    }
}
