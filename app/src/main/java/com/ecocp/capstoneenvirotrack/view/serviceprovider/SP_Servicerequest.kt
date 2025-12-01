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
        determineAndLoadBookings() // decide transporter vs tsd and load accordingly

        return binding.root
    }

    // ---------------------------
    // Setup RecyclerView
    // ---------------------------
    private fun setupRecyclerView() {
        adapter = ServiceRequestAdapter(
            requests = mutableListOf(),
            isActiveTasks = false,
            onActionClick = { selected ->
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
        )

        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter
    }

    // ---------------------------
    // Determine role, then load transport or tsd bookings accordingly
    // ---------------------------
    private fun determineAndLoadBookings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            Log.d("TSD_DEBUG", "determineAndLoadBookings -> no logged-in user")
            return
        }

        val db = FirebaseFirestore.getInstance()
        Log.d("TSD_DEBUG", "determineAndLoadBookings -> currentUser.uid='$uid'")

        // 1) try users collection
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                Log.d("TSD_DEBUG", "users doc exists=${userDoc.exists()}, data=${userDoc.data}")
                val role = userDoc.getString("role")?.trim()?.lowercase()
                Log.d("TSD_DEBUG", "users.role = '$role'")
                if (role != null && (role.contains("tsd") || role.contains("tsd_facility") || role.contains("tsdfacility"))) {
                    try { adapter.setRole("tsd") } catch (_: Exception) {}
                    loadTsdBookingsOnly(uid)
                } else {
                    // 2) fallback: check service_providers doc
                    db.collection("service_providers").document(uid).get()
                        .addOnSuccessListener { spDoc ->
                            Log.d("TSD_DEBUG", "service_providers exists=${spDoc.exists()}, data=${spDoc.data}")
                            val providerType = spDoc.getString("providerType")?.trim()?.lowercase().orEmpty()
                            val spRole = spDoc.getString("role")?.trim()?.lowercase().orEmpty()
                            val companyName = spDoc.getString("companyName")?.trim().orEmpty()

                            Log.d("TSD_DEBUG", "service_providers.providerType='$providerType', service_providers.role='$spRole', companyName='$companyName'")

                            val isTsdByProviderType = providerType == "tsd" || providerType == "tsdfacility" || providerType == "tsd_facility"
                            val isTsdByRoleField = spRole.contains("tsd") || spRole.contains("tsd_facility") || spRole.contains("tsdfacility")

                            if (isTsdByProviderType || isTsdByRoleField) {
                                try { adapter.setRole("tsd") } catch (_: Exception) {}
                                loadTsdBookingsOnly(uid)
                            } else {
                                // 3) Last-resort heuristic: if companyName exists and there are tsd_bookings for it,
                                // treat as TSD (this helps when metadata is incomplete)
                                if (companyName.isNotEmpty()) {
                                    Log.d("TSD_DEBUG", "Attempting heuristic: query tsd_bookings where facilityName == '$companyName'")
                                    db.collection("tsd_bookings")
                                        .whereEqualTo("facilityName", companyName)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { qs ->
                                            Log.d("TSD_DEBUG", "heuristic facilityName query -> size=${qs.size()}")
                                            if (qs.size() > 0) {
                                                try { adapter.setRole("tsd") } catch (_: Exception) {}
                                                loadTsdBookingsOnly(uid)
                                            } else {
                                                try { adapter.setRole("transporter") } catch (_: Exception) {}
                                                loadTransporterBookingsOnly()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("TSD_DEBUG", "heuristic facilityName query failed: ${e.message}", e)
                                            try { adapter.setRole("transporter") } catch (_: Exception) {}
                                            loadTransporterBookingsOnly()
                                        }
                                } else {
                                    try { adapter.setRole("transporter") } catch (_: Exception) {}
                                    loadTransporterBookingsOnly()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("TSD_DEBUG", "failed fetching service_providers doc: ${e.message}", e)
                            try { adapter.setRole("transporter") } catch (_: Exception) {}
                            loadTransporterBookingsOnly()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TSD_DEBUG", "failed fetching users doc: ${e.message}", e)
                // If users doc missing/fails, try service_providers directly (same logic)
                db.collection("service_providers").document(uid).get()
                    .addOnSuccessListener { spDoc ->
                        Log.d("TSD_DEBUG", "service_providers exists=${spDoc.exists()}, data=${spDoc.data}")
                        val providerType = spDoc.getString("providerType")?.trim()?.lowercase().orEmpty()
                        val spRole = spDoc.getString("role")?.trim()?.lowercase().orEmpty()
                        val companyName = spDoc.getString("companyName")?.trim().orEmpty()

                        val isTsdByProviderType = providerType == "tsd" || providerType == "tsdfacility" || providerType == "tsd_facility"
                        val isTsdByRoleField = spRole.contains("tsd") || spRole.contains("tsd_facility") || spRole.contains("tsdfacility")

                        if (isTsdByProviderType || isTsdByRoleField) {
                            try { adapter.setRole("tsd") } catch (_: Exception) {}
                            loadTsdBookingsOnly(uid)
                        } else if (companyName.isNotEmpty()) {
                            Log.d("TSD_DEBUG", "Attempting heuristic after users failure: query tsd_bookings where facilityName == '$companyName'")
                            db.collection("tsd_bookings")
                                .whereEqualTo("facilityName", companyName)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { qs ->
                                    Log.d("TSD_DEBUG", "heuristic facilityName query -> size=${qs.size()}")
                                    if (qs.size() > 0) {
                                        try { adapter.setRole("tsd") } catch (_: Exception) {}
                                        loadTsdBookingsOnly(uid)
                                    } else {
                                        try { adapter.setRole("transporter") } catch (_: Exception) {}
                                        loadTransporterBookingsOnly()
                                    }
                                }
                                .addOnFailureListener { e2 ->
                                    Log.e("TSD_DEBUG", "heuristic facilityName query failed after users failure: ${e2.message}", e2)
                                    try { adapter.setRole("transporter") } catch (_: Exception) {}
                                    loadTransporterBookingsOnly()
                                }
                        } else {
                            try { adapter.setRole("transporter") } catch (_: Exception) {}
                            loadTransporterBookingsOnly()
                        }
                    }
                    .addOnFailureListener {
                        try { adapter.setRole("transporter") } catch (_: Exception) {}
                        loadTransporterBookingsOnly()
                    }
            }
    }


    // ---------------------------
    // Load bookings for transporter (unchanged)
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
    // Fallback single-field query for transporter
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
    // Convert Firestore → Model (Transporter mapping)
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
    // Load TSD bookings and map to ServiceRequest for UI
    //  - Try facilityId == tsdUid first (fast), then fallback to facilityName == companyName
    // ---------------------------
    private fun loadTsdBookingsOnly(tsdUid: String) {
        val db = FirebaseFirestore.getInstance()

        binding.progressLoading.visibility = View.VISIBLE
        binding.txtEmptyState.visibility = View.GONE

        // 1) Try facilityId == tsdUid
        db.collection("tsd_bookings")
            .whereEqualTo("facilityId", tsdUid)
            .get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.mapNotNull { doc -> doc.data?.let { mapTsdToServiceRequest(doc.id, it) } }
                if (items.isNotEmpty()) {
                    handleBookingsResult(items)
                } else {
                    // fallback: fetch service_providers companyName (if any) and query facilityName
                    db.collection("service_providers").document(tsdUid)
                        .get()
                        .addOnSuccessListener { spDoc ->
                            val facilityName = spDoc.getString("companyName")?.trim().orEmpty()
                            if (facilityName.isNotEmpty()) {
                                db.collection("tsd_bookings")
                                    .whereEqualTo("facilityName", facilityName)
                                    .get()
                                    .addOnSuccessListener { qs2 ->
                                        val items2 = qs2.documents.mapNotNull { d -> d.data?.let { mapTsdToServiceRequest(d.id, it) } }
                                        handleBookingsResult(items2)
                                    }
                                    .addOnFailureListener { e ->
                                        binding.progressLoading.visibility = View.GONE
                                        binding.txtEmptyState.visibility = View.VISIBLE
                                        Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                handleBookingsResult(emptyList())
                            }
                        }
                        .addOnFailureListener { e ->
                            binding.progressLoading.visibility = View.GONE
                            binding.txtEmptyState.visibility = View.VISIBLE
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.progressLoading.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------------------------
    // Mapping TSD doc -> ServiceRequest (so UI can reuse same adapter)
    //  - Also normalizes status like "Pending Payment" -> "Pending"
    // ---------------------------
    private fun mapTsdToServiceRequest(docId: String, m: Map<String, Any>): ServiceRequest {

        fun s(key: String, alt: String = ""): String {
            return (m[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt
        }

        val bookingId = s("bookingId", docId)
        val facilityName = s("facilityName", s("facility", "TSD Facility"))
        val location = s("location")
        val preferredDate = s("preferredDate")
        val contactNumber = s("contactNumber")
        val treatmentInfo = s("treatmentInfo")
        val previousRecord = s("previousRecordUrl", "")
        val rawStatus = s("status", "Pending")

        // Normalize status (explicitly convert "Pending Payment" -> "Pending")
        val normalizedStatus = when {
            rawStatus.equals("Pending Payment", true) -> "Pending"
            rawStatus.contains("pending", true) -> "Pending"
            rawStatus.contains("paid", true) -> "Paid"
            rawStatus.contains("confirm", true) -> "Confirmed"
            rawStatus.contains("reject", true) -> "Rejected"
            else -> rawStatus
        }

        val qty = (m["quantity"] as? Number)?.toDouble() ?: 0.0
        val totalPayment = (m["totalPayment"] as? Number)?.toDouble() ?: 0.0

        return ServiceRequest(
            id = docId,
            bookingId = bookingId,
            clientName = s("userId", "-"),
            companyName = facilityName,
            providerName = facilityName,
            providerContact = contactNumber,
            serviceTitle = "TSD - ${treatmentInfo.ifEmpty { "Treatment" }}",
            bookingStatus = normalizedStatus,
            origin = location,
            destination = facilityName,
            dateRequested = preferredDate.ifEmpty { m["dateCreated"]?.toString() ?: "N/A" },
            wasteType = treatmentInfo,
            quantity = if (qty > 0) qty.toString() else "-",
            packaging = "-",
            notes = s("notes", s("specialInstructions", "No notes")),
            compliance = "Payment: ${if (totalPayment > 0) "₱ ${"%,.2f".format(totalPayment)}" else "-"}",
            attachments = if (previousRecord.isNotBlank()) listOf(previousRecord) else listOf(DEV_ATTACHMENT),
            imageUrl = if (previousRecord.isNotBlank()) previousRecord else DEV_ATTACHMENT
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
            "Sort by Pending" -> masterList.filter { it.bookingStatus?.contains("pending", true) == true }
            "Sort by Confirmed" -> masterList.filter { it.bookingStatus?.contains("confirm", true) == true }
            "Sort by Rejected" -> masterList.filter { it.bookingStatus?.contains("reject", true) == true }
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
                        "Sort by Pending" -> masterList.filter { it.bookingStatus?.contains("pending", true) == true }
                        "Sort by Confirmed" -> masterList.filter { it.bookingStatus?.contains("confirm", true) == true }
                        "Sort by Rejected" -> masterList.filter { it.bookingStatus?.contains("reject", true) == true }
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
