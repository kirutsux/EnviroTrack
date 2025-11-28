package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServicerequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ServiceRequestAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SP_Servicerequest : Fragment() {

    private var _binding: FragmentSpServicerequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceRequestAdapter

    // 🔥 main list (master list)
    private val masterList = mutableListOf<ServiceRequest>()

    // developer fallback attachment
    private val DEV_ATTACHMENT = "/mnt/data/6d1108ac-01c7-4f2c-b8e7-fc2390192bc4.png"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServicerequestBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSortingSpinner()
        loadTransporterBookingsOnly()

        return binding.root
    }

    // ---------------------------
    // Setup RecyclerView
    // ---------------------------
    private fun setupRecyclerView() {
        adapter = ServiceRequestAdapter(
            mutableListOf(),   // <-- FIXED: MutableList instead of emptyList()
            isActiveTasks = false
        ) { selected ->

            val bundle = Bundle().apply {
                putString("bookingId", selected.bookingId)
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("origin", selected.origin)
                putString("destination", selected.destination)
                putString("dateRequested", selected.dateRequested)
                putString("providerName", selected.providerName)
                putString("providerContact", selected.providerContact)
                putString("bookingStatus", selected.bookingStatus)
                putString("notes", selected.notes)
                putString("attachment", selected.attachments?.firstOrNull() ?: DEV_ATTACHMENT)
            }

            findNavController().navigate(
                R.id.action_SP_Servicerequest_to_SP_ServiceRequestDetails,
                bundle
            )
        }

        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter
    }


    // ---------------------------
    // Load bookings for transporter
    // ---------------------------
    private fun loadTransporterBookingsOnly() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        binding.progressLoading.visibility = View.VISIBLE
        binding.txtEmptyState.visibility = View.GONE

        db.collection("service_providers")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val companyName = doc.getString("companyName")?.trim().orEmpty()
                val providerName = doc.getString("serviceProviderName")?.trim().orEmpty()

                when {
                    companyName.isNotEmpty() -> {
                        db.collection("transport_bookings")
                            .whereEqualTo("providerType", "Transporter")
                            .whereEqualTo("serviceProviderCompany", companyName)
                            .get()
                            .addOnSuccessListener { qs ->
                                handleBookingsResult(qs.documents.mapNotNull {
                                    it.data?.let { data -> mapToServiceRequest(it.id, data) }
                                })
                            }
                            .addOnFailureListener {
                                fetchAndFilterByNameFallback(db, companyName, providerName)
                            }
                    }

                    providerName.isNotEmpty() -> {
                        db.collection("transport_bookings")
                            .whereEqualTo("providerType", "Transporter")
                            .whereEqualTo("serviceProviderName", providerName)
                            .get()
                            .addOnSuccessListener { qs ->
                                handleBookingsResult(qs.documents.mapNotNull {
                                    it.data?.let { data -> mapToServiceRequest(it.id, data) }
                                })
                            }
                            .addOnFailureListener {
                                fetchAndFilterByNameFallback(db, companyName, providerName)
                            }
                    }

                    else -> {
                        fetchAndFilterByNameFallback(db, companyName, providerName)
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                binding.progressLoading.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------------------------
    // Fallback single-field query
    // ---------------------------
    private fun fetchAndFilterByNameFallback(
        db: FirebaseFirestore,
        companyName: String,
        providerName: String
    ) {
        db.collection("transport_bookings")
            .whereEqualTo("providerType", "Transporter")
            .get()
            .addOnSuccessListener { qs ->

                val filtered = qs.documents.filter {
                    val c = it.getString("serviceProviderCompany")?.trim().orEmpty()
                    val n = it.getString("serviceProviderName")?.trim().orEmpty()
                    (companyName.isNotEmpty() && c == companyName) ||
                            (providerName.isNotEmpty() && n == providerName)
                }

                handleBookingsResult(
                    filtered.mapNotNull {
                        it.data?.let { data -> mapToServiceRequest(it.id, data) }
                    }
                )
            }
            .addOnFailureListener { e ->
                binding.progressLoading.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------------------------
    // Convert Firestore → Model
    // ---------------------------
    private fun mapToServiceRequest(docId: String, m: Map<String, Any>): ServiceRequest {

        fun s(key: String, alt: String = ""): String {
            return (m[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt
        }

        val bookingStatus = s("bookingStatus", "Pending")
        val wasteType = s("wasteType")
        val serviceTitle =
            if (wasteType.isNotBlank()) "Transport - $wasteType" else "Transport Booking"

        val dateBooked = (m["bookingDate"] as? com.google.firebase.Timestamp)
            ?: (m["dateBooked"] as? com.google.firebase.Timestamp)

        val dateRequested = dateBooked?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
        } ?: "N/A"

        val attachments = mutableListOf<String>()
        (m["transportPlanUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
        (m["storagePermitUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
        if (attachments.isEmpty()) attachments.add(DEV_ATTACHMENT)

        return ServiceRequest(
            id = docId,
            bookingId = s("bookingId", docId),
            clientName = s("pcoId", s("clientId")),
            companyName = s("serviceProviderCompany", s("companyName")),
            providerName = s("serviceProviderName", s("name")),
            providerContact = s("providerContact", s("contactNumber")),
            serviceTitle = serviceTitle,
            bookingStatus = bookingStatus,
            origin = s("origin", s("pickupLocation")),
            destination = s("destination", s("dropoffLocation")),
            dateRequested = dateRequested,
            wasteType = wasteType,
            quantity = s("quantity"),
            packaging = s("packaging"),
            notes = s("specialInstructions", s("notes", "No notes")),
            compliance = "Qty: ${s("quantity").ifEmpty { "-" }}",
            attachments = attachments,
            imageUrl = attachments.first()
        )
    }

    // ---------------------------
    // Apply sorting & update UI
    // ---------------------------
    private fun handleBookingsResult(list: List<ServiceRequest>) {

        masterList.clear()
        masterList.addAll(list)

        val selected = binding.spinnerSort.selectedItem?.toString() ?: "All"

        val filtered = when (selected) {
            "Sort by Pending" -> masterList.filter { it.bookingStatus.equals("Pending", true) }
            "Sort by Confirmed" -> masterList.filter { it.bookingStatus.equals("Confirmed", true) }
            "Sort by Rejected" -> masterList.filter { it.bookingStatus.equals("Rejected", true) }
            else -> masterList
        }

        adapter.updateList(filtered)

        binding.progressLoading.visibility = View.GONE
        binding.txtEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------------------------
    // Spinner sorting logic
    // ---------------------------
    private fun setupSortingSpinner() {
        binding.spinnerSort.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selected = parent.getItemAtPosition(position).toString()

                    val filtered = when (selected) {
                        "Sort by Pending" -> masterList.filter { it.bookingStatus.equals("Pending", true) }
                        "Sort by Confirmed" -> masterList.filter { it.bookingStatus.equals("Confirmed", true) }
                        "Sort by Rejected" -> masterList.filter { it.bookingStatus.equals("Rejected", true) }
                        else -> masterList
                    }

                    adapter.updateList(filtered)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
