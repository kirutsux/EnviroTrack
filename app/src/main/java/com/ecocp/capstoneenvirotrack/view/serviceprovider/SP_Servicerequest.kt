package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServicerequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ServiceRequestAdapter
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.text.format.DateFormat
import android.widget.Toast


class SP_Servicerequest : Fragment() {

    private var _binding: FragmentSpServicerequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceRequestAdapter
    private val requests = mutableListOf<ServiceRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServicerequestBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadRequestsFromBookings()

        return binding.root
    }

    private fun setupRecyclerView() {
        // Note: ServiceRequestAdapter now expects (requests, isActiveTasks, onActionClick)
        adapter = ServiceRequestAdapter(requests, isActiveTasks = false) { selected ->

            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("origin", selected.origin)
                putString("dateRequested", selected.dateRequested)  // already a String
                putString("providerName", selected.providerName)
                putString("providerContact", selected.providerContact)
                putString("status", selected.status)
                putString("notes", selected.notes.ifEmpty { selected.compliance })

                // attachments — send first or fallback to imageUrl or dev path
                val fallback = selected.imageUrl.ifEmpty { "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png" }
                putString("attachment", selected.attachments?.firstOrNull() ?: fallback)
            }

            // navigate to details and pass the bundle
            findNavController().navigate(
                R.id.action_SP_Servicerequest_to_SP_ServiceRequestDetails,
                bundle
            )
        }

        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter
    }

    private fun loadRequestsFromBookings() {
        val TAG = "SRDBG"
        val auth = FirebaseAuth.getInstance()
        val currentProviderId = auth.currentUser?.uid
        if (currentProviderId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Not logged in as provider (null uid)", Toast.LENGTH_LONG).show()
            android.util.Log.d(TAG, "currentProviderId is null")
            return
        }
        android.util.Log.d(TAG, "currentProviderId=$currentProviderId")

        val db = FirebaseFirestore.getInstance()
        val devAttachment = "/mnt/data/6d1108ac-01c7-4f2c-b8e7-fc2390192bc4.png"

        // Simple helper that prints doc fields
        fun printSnapshot(kind: String, snap: com.google.firebase.firestore.QuerySnapshot?) {
            val size = snap?.size() ?: 0
            android.util.Log.d(TAG, "$kind snapshot size = $size")
            snap?.documents?.forEach { d ->
                android.util.Log.d(TAG, "$kind docId=${d.id} fields=${d.data}")
            }
        }

        // show loading
        binding.progressLoading?.visibility = View.VISIBLE
        binding.txtEmptyState?.visibility = View.GONE

        // Query both collections but **no filtering** to test first (temporarily)
        val q1 = db.collection("transport_bookings")/* .whereEqualTo("transporterId", currentProviderId) */
            .get()
        val q2 = db.collection("tsd_bookings")/* .whereEqualTo("facilityId", currentProviderId) */
            .get()

        // Wait for both
        com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(q1, q2)
            .addOnSuccessListener { results ->
                // results[0] => transport_bookings, results[1] => tsd_bookings
                val collected = mutableListOf<Pair<ServiceRequest, com.google.firebase.Timestamp?>>()

                results.forEachIndexed { idx, r ->
                    val snap = r as? com.google.firebase.firestore.QuerySnapshot
                    val name = if (idx == 0) "transport_bookings" else "tsd_bookings"
                    printSnapshot(name, snap)

                    snap?.documents?.forEach { doc ->
                        val m = doc.data ?: return@forEach
                        // safe read fields (use keys exactly from your Firestore)
                        val bookingId = m["bookingId"] as? String ?: doc.id
                        val facilityName = m["facilityName"] as? String ?: ""
                        val location = m["location"] as? String ?: ""
                        val preferredDate = m["preferredDate"] as? String ?: ""
                        val quantity = m["quantity"] as? String ?: ""
                        val totalPayment = m["totalPayment"]?.toString() ?: ""
                        val status = m["status"] as? String ?: "Pending"
                        val treatmentInfo = m["treatmentInfo"] as? String ?: ""
                        val contactNumber = m["contactNumber"] as? String ?: ""
                        val userId = m["userId"] as? String ?: ""
                        val ts = m["dateCreated"] as? com.google.firebase.Timestamp

                        val dateRequested = ts?.let { android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it.toDate()).toString() }
                            ?: preferredDate.ifEmpty { "N/A" }

                        val compliance = "Qty: ${quantity.ifEmpty { "-" }} • Total: ₱${totalPayment.ifEmpty { "0" }}"

                        val sr = ServiceRequest(
                            id = bookingId,
                            bookingId = bookingId,
                            clientName = userId,
                            companyName = facilityName,
                            providerName = "",
                            providerContact = contactNumber,
                            serviceTitle = treatmentInfo.ifEmpty { "Booking (${if (idx==0) "Transport" else "TSD"})" },
                            status = status,
                            origin = location,
                            destination = facilityName,
                            dateRequested = dateRequested,
                            wasteType = treatmentInfo,
                            quantity = quantity,
                            packaging = "",
                            notes = "",
                            compliance = compliance,
                            attachments = listOf(devAttachment),
                            imageUrl = devAttachment
                        )

                        collected.add(Pair(sr, ts))
                    }
                }

                // update UI
                requireActivity().runOnUiThread {
                    requests.clear()
                    requests.addAll(collected.map { it.first })
                    adapter.notifyDataSetChanged()
                    binding.progressLoading?.visibility = View.GONE
                    binding.txtEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE

                    if (requests.isEmpty()) {
                        Toast.makeText(requireContext(), "No bookings found (check filters/fields)", Toast.LENGTH_LONG).show()
                    } else {
                        android.util.Log.d(TAG, "Loaded ${requests.size} service requests")
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                android.util.Log.e(TAG, "Query failed: ${e.message}")
                requireActivity().runOnUiThread {
                    binding.progressLoading?.visibility = View.GONE
                    binding.txtEmptyState.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Failed to load bookings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
