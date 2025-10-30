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

    private fun fetchApplications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.tvEmptyState.visibility = View.GONE

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                applications.clear()
                val wasteList = result.documents

                if (wasteList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // 🔹 Step 1: Fetch all transport bookings for this generator
                db.collection("transport_bookings")
                    .whereEqualTo("pcoId", userId)
                    .get()
                    .addOnSuccessListener { bookingsSnap ->

                        // 🔹 Step 2: Build bookingId → status lookup map
                        val bookingStatusMap = mutableMapOf<String, String>()
                        for (booking in bookingsSnap) {
                            val bookingId = booking.getString("bookingId") ?: booking.id
                            val status = booking.getString("status") ?: "Unpaid"
                            bookingStatusMap[bookingId] = status
                        }

                        // 🔹 Step 3: Build HWMS applications list
                        for (doc in wasteList) {
                            val wasteDetailsList =
                                doc.get("wasteDetails") as? List<Map<String, Any>> ?: emptyList()
                            val firstDetail = wasteDetailsList.firstOrNull()

                            val wasteType = firstDetail?.get("wasteName") as? String ?: ""
                            val quantity = firstDetail?.get("quantity") as? String ?: ""
                            val storageLocation = firstDetail?.get("currentPractice") as? String ?: ""
                            val dateGenerated = doc.getTimestamp("timestamp")?.toDate().toString()

                            // 🔹 Try to get bookingId from HWMS document
                            val bookingId = doc.getString("bookingId")

                            // 🔹 Step 4: Determine payment status
                            val paymentStatus = when {
                                // Match by bookingId (exact link)
                                !bookingId.isNullOrEmpty() && bookingStatusMap.containsKey(bookingId) ->
                                    bookingStatusMap[bookingId] ?: "Unpaid"

                                // If not found, match by generatorId (fallback)
                                bookingsSnap.any { it.getString("generatorId") == userId } -> {
                                    bookingsSnap.firstOrNull {
                                        it.getString("generatorId") == userId
                                    }?.getString("status") ?: "Unpaid"
                                }

                                else -> "Unpaid"
                            }

                            // 🔹 EMB permit or processing status (placeholder)
                            val embStatus = "Pending"

                            val app = HWMSApplication(
                                id = doc.id,
                                wasteType = wasteType,
                                quantity = quantity,
                                storageLocation = storageLocation,
                                dateGenerated = dateGenerated,
                                status = paymentStatus,
                                embStatus = embStatus
                            )

                            applications.add(app)
                        }

                        adapter.notifyDataSetChanged()
                        binding.tvEmptyState.visibility =
                            if (applications.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load transport booking info.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to load HWMS applications.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}
