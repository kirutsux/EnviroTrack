package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
            findNavController().navigate(R.id.HwmsStep1Fragment)
        }

    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun fetchApplications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                applications.clear()
                val wasteList = result.documents

                if (wasteList.isEmpty()) {
                    Toast.makeText(requireContext(), "No applications found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Fetch transport bookings for this user
                db.collection("transport_bookings")
                    .whereEqualTo("pcoId", userId)
                    .get()
                    .addOnSuccessListener { bookingsSnap ->
                        val bookingMap = mutableMapOf<String, String>()

                        for (b in bookingsSnap) {
                            val wasteType = b.getString("wasteType") ?: ""
                            val providerType = b.getString("providerType") ?: ""
                            val status = b.getString("status") ?: "Unpaid"

                            // Combine both wasteType and providerType as key to avoid mismatch
                            if (wasteType.isNotEmpty() && providerType.isNotEmpty()) {
                                bookingMap["${wasteType}_${providerType}"] = status
                            }
                        }

                        // Build HWMSApplication list
                        for (doc in wasteList) {
                            val wasteDetailsList = doc.get("wasteDetails") as? List<Map<String, Any>> ?: emptyList()
                            val firstDetail = wasteDetailsList.firstOrNull()

                            val wasteType = firstDetail?.get("wasteName") as? String ?: ""
                            // Try to find a matching booking by waste type (case-insensitive)
                            val paymentStatus = bookingMap.entries.find {
                                it.key.startsWith("${wasteType}_", ignoreCase = true)
                            }?.value ?: "Unpaid"


                            val app = HWMSApplication(
                                id = doc.id,
                                wasteType = wasteType,
                                quantity = firstDetail?.get("quantity") as? String ?: "",
                                storageLocation = firstDetail?.get("currentPractice") as? String ?: "",
                                dateGenerated = doc.getTimestamp("timestamp")?.toDate().toString(),
                                status = paymentStatus // ✅ uses booking status
                            )

                            applications.add(app)
                        }

                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to load payment info.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load applications.", Toast.LENGTH_SHORT).show()
            }
    }
}
