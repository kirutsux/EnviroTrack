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
                    putString("bookedBy", selected.clientName)   // or selected.bookedBy if you added that field
                    putString("wasteType", selected.wasteType)
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
                // SAFELY update UI only if binding still exists
                val b = _binding ?: run {
                    Log.w("SP_Servicerequest", "UI gone while handling transporter failure, skipping UI update")
                    return@addOnFailureListener
                }
                b.progressLoading.visibility = View.GONE
                b.txtEmptyState.visibility = View.VISIBLE
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
                // SAFELY update UI only if binding still exists
                val b = _binding ?: run {
                    Log.w("SP_Servicerequest", "UI gone while fetching fallback transport bookings, skipping UI update")
                    return@addOnSuccessListener
                }

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
                val b = _binding ?: run {
                    Log.w("SP_Servicerequest", "UI gone while handling fallback transport failure, skipping UI update")
                    return@addOnFailureListener
                }
                b.progressLoading.visibility = View.GONE
                b.txtEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------------------------
    // Convert Firestore → Model (Transporter mapping)
    // ---------------------------
    private fun mapToServiceRequest(
        docId: String,
        m: Map<String, Any>,
        crs: Map<String, Any>? = null // pass the crs_application doc map when you already have it
    ): ServiceRequest {

        fun s(map: Map<String, Any>, key: String, alt: String = ""): String {
            return (map[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt
        }
        fun s(key: String, alt: String = ""): String {
            return s(m, key, alt)
        }

        // human-readable bookedBy (same as you had)
        val bookedBy = s(
            "bookedBy",
            s(
                "bookedById",
                s(
                    "generatorId",
                    s(
                        "requestedBy",
                        s("clientName", s("name", "Unknown"))
                    )
                )
            )
        )

        // pcoId for lookup (from transport booking fields)
        val pcoId = s(
            "pcoId",
            s(
                "clientId",
                s("bookedById", s("generatorId", s("requestedBy", "")))
            )
        ).ifBlank { "" }

        // prefer companyName from CRS doc if provided, otherwise fall back to booking's serviceProviderCompany or booking-level companyName
        val crsCompanyName = crs?.let { (it["companyName"] as? String)?.trim() }?.takeUnless { it.isEmpty() }
        val companyName = crsCompanyName
            ?: s("serviceProviderCompany", s("companyName"))

        val wasteType = s("wasteType")
        val finalWasteType = if (wasteType.isNotBlank()) wasteType else s("waste", s("waste_type"))

        // ... date formatting and attachments (same as yours)
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

            clientName = bookedBy,
            pcoId = pcoId,              // you can keep it for internal use but don't display it in UI
            companyName = companyName,  // <-- prefer CRS companyName
            providerName = s("serviceProviderName", s("name")),
            providerContact = s("providerContact", s("contactNumber")),

            serviceTitle = if (finalWasteType.isNotBlank()) "Transport - $finalWasteType" else "Transport Booking",
            bookingStatus = s("bookingStatus", "Pending"),

            origin = s("origin", s("pickupLocation")),
            destination = s("destination", s("dropoffLocation")),
            dateRequested = dateRequested,
            dateBooked = dateBooked,

            wasteType = finalWasteType,
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
    private fun loadTsdBookingsOnly(tsdUid: String, statusFilter: String? = null) {
        val db = FirebaseFirestore.getInstance()

        // safe UI guard
        val bStart = _binding ?: run {
            Log.w("SP_Servicerequest", "UI gone before starting loadTsdBookingsOnly(), aborting")
            return
        }
        bStart.progressLoading.visibility = View.VISIBLE
        bStart.txtEmptyState.visibility = View.GONE

        // helper: map loose filter text -> exact bookingStatus stored in DB
        fun mapFilterToBookingStatus(filter: String?): String? {
            if (filter.isNullOrBlank()) return null
            val f = filter.trim().lowercase()
            return when {
                f.contains("pending") -> "Pending"
                f.contains("confirm") || f.contains("confirmed") -> "Confirmed"
                f.contains("rejected") || f.contains("reject") -> "Rejected"
                f.contains("complete") || f.contains("completed") -> "Completed"
                f.contains("delivered") -> "Delivered"
                f.contains("paid") -> "Paid"
                // add more DB status mappings here if you have them
                else -> null
            }
        }

        val dbStatusValue = mapFilterToBookingStatus(statusFilter)
        if (statusFilter != null) {
            Log.d("TSD_DEBUG", "Requested statusFilter='$statusFilter' -> dbStatusValue='$dbStatusValue'")
        }

        // Primary query: tsdId == tsdUid
        var primaryQuery = db.collection("tsd_bookings")
            .whereEqualTo("tsdId", tsdUid)

        if (dbStatusValue != null) {
            primaryQuery = primaryQuery.whereEqualTo("bookingStatus", dbStatusValue)
        }

        primaryQuery.get()
            .addOnSuccessListener { qs ->
                Log.d("TSD_DEBUG", "Primary query (tsdId) returned ${qs.size()} documents")
                val items = qs.documents.mapNotNull { doc -> doc.data?.let { mapTsdToServiceRequest(doc.id, it) } }
                if (items.isNotEmpty()) {
                    handleBookingsResult(items)
                    return@addOnSuccessListener
                }

                // Fallback: try tsdName using service_providers.companyName
                db.collection("service_providers").document(tsdUid)
                    .get()
                    .addOnSuccessListener { spDoc ->
                        val tsdName = spDoc.getString("companyName")?.trim().orEmpty()
                        if (tsdName.isNotEmpty()) {
                            var fallbackQuery = db.collection("tsd_bookings")
                                // some docs store tsdName lowercase — try matching both patterns
                                .whereEqualTo("tsdName", tsdName)

                            if (dbStatusValue != null) fallbackQuery = fallbackQuery.whereEqualTo("bookingStatus", dbStatusValue)

                            fallbackQuery.get()
                                .addOnSuccessListener { qs2 ->
                                    Log.d("TSD_DEBUG", "Fallback query (tsdName) returned ${qs2.size()} documents")
                                    val items2 = qs2.documents.mapNotNull { d -> d.data?.let { mapTsdToServiceRequest(d.id, it) } }
                                    handleBookingsResult(items2)
                                }
                                .addOnFailureListener { e ->
                                    val b = _binding ?: run {
                                        Log.w("SP_Servicerequest", "UI gone while handling tsdName fallback failure, skipping update")
                                        return@addOnFailureListener
                                    }
                                    b.progressLoading.visibility = View.GONE
                                    b.txtEmptyState.visibility = View.VISIBLE
                                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            // no tsdName → empty
                            handleBookingsResult(emptyList())
                        }
                    }
                    .addOnFailureListener { e ->
                        val b = _binding ?: run {
                            Log.w("SP_Servicerequest", "UI gone while fetching service_providers for tsd fallback, skipping update")
                            return@addOnFailureListener
                        }
                        b.progressLoading.visibility = View.GONE
                        b.txtEmptyState.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                val b = _binding ?: run {
                    Log.w("SP_Servicerequest", "UI gone while loading tsd bookings primary query, skipping update")
                    return@addOnFailureListener
                }
                b.progressLoading.visibility = View.GONE
                b.txtEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---------------------------
// Mapping TSD doc -> ServiceRequest (UPDATED for new tsd_bookings schema)
// - Uses bookingStatus, tsdBookingId, tsdId, tsdName, amount, paymentStatus, etc.
// ---------------------------
    private fun mapTsdToServiceRequest(docId: String, m: Map<String, Any>): ServiceRequest {

        fun s(key: String, alt: String = ""): String {
            return (m[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt
        }

        // prefer tsdBookingId if present
        val bookingId = s("tsdBookingId", s("bookingId", docId))
        val facilityName = s("tsdName", s("facilityName", s("facility", "TSD Facility")))
        val location = s("location")
        val preferredDate = s("preferredDate")
        val contactNumber = s("contactNumber", s("providerContact", ""))
        val treatmentInfo = s("treatmentInfo")
        val wasteType = s("wasteType", treatmentInfo)
        val previousRecord = s("previousRecordUrl", "")
        val certificateUrl = s("certificateUrl", "")
        val rawStatus = s("bookingStatus", s("status", "Pending"))
        val paymentStatus = s("paymentStatus", s("status", ""))

        // Normalize status for UI (map common variants to display labels)
        val normalizedStatus = when {
            rawStatus.equals("Pending Payment", true) -> "Pending"
            rawStatus.equals("Pending", true) -> "Pending"
            rawStatus.equals("Confirmed", true) -> "Confirmed"
            rawStatus.equals("Delivered", true) -> "Delivered"
            rawStatus.equals("Rejected", true) -> "Rejected"
            rawStatus.equals("Completed", true) -> "Completed"
            rawStatus.contains("pending", true) -> "Pending"
            rawStatus.contains("confirm", true) -> "Confirmed"
            rawStatus.contains("deliver", true) -> "Delivered"
            rawStatus.contains("reject", true) -> "Rejected"
            rawStatus.contains("complete", true) -> "Completed"
            else -> rawStatus.replaceFirstChar { it.uppercase() }
        }

        val qty = (m["quantity"] as? Number)?.toDouble() ?: 0.0
        val amount = (m["amount"] as? Number)?.toDouble()
            ?: (m["totalPayment"] as? Number)?.toDouble()
            ?: 0.0

        // prefer certificate, else previous record, else dev fallback
        val documentUrl = certificateUrl.ifBlank { previousRecord.ifBlank { DEV_ATTACHMENT } }

        val timestamp = (m["timestamp"] as? com.google.firebase.Timestamp)
            ?: (m["dateCreated"] as? com.google.firebase.Timestamp)
        val dateRequested = preferredDate.ifEmpty {
            timestamp?.toDate()?.let {
                android.text.format.DateFormat.format("MMM dd, yyyy", it).toString()
            } ?: "N/A"
        }

        val providerContact = contactNumber.ifBlank { s("contactNumber", "") }

        return ServiceRequest(
            id = docId,
            bookingId = bookingId,
            clientName = s("generatorId", s("userId", "-")),
            companyName = facilityName,
            providerName = facilityName,
            providerContact = providerContact,
            serviceTitle = "TSD - ${if (treatmentInfo.isNotBlank()) treatmentInfo else (wasteType.ifBlank { "Treatment" })}",
            bookingStatus = normalizedStatus,
            origin = location,
            destination = facilityName,
            dateRequested = dateRequested,
            wasteType = if (wasteType.isNotBlank()) wasteType else treatmentInfo,
            quantity = if (qty > 0) qty.toString() else "-",
            packaging = "-",
            notes = s("notes", s("specialInstructions", "No notes")),
            compliance = "Amount: ${if (amount > 0) "₱ ${"%,.2f".format(amount)}" else "-"} | Payment: ${if (paymentStatus.isNotBlank()) paymentStatus else "-"}",
            attachments = if (documentUrl.isNotBlank()) listOf(documentUrl) else listOf(DEV_ATTACHMENT),
            imageUrl = if (documentUrl.isNotBlank()) documentUrl else DEV_ATTACHMENT
        )
    }

    // ---------------------------
    // Apply sorting & update UI
    // ---------------------------
    private fun handleBookingsResult(list: List<ServiceRequest>) {
        // SAFELY bail if view gone
        val b = _binding ?: run {
            Log.w("SP_Servicerequest", "UI gone while handling bookings result, skipping update")
            return
        }

        masterList.clear()
        masterList.addAll(list)

        val selected = b.spinnerSort.selectedItem?.toString() ?: "All"

        val filtered = when (selected) {
            "Sort by Pending" -> masterList.filter { it.bookingStatus?.contains("pending", true) == true }
            "Sort by Confirmed" -> masterList.filter { it.bookingStatus?.contains("confirm", true) == true }
            "Sort by Rejected" -> masterList.filter { it.bookingStatus?.contains("reject", true) == true }
            else -> masterList
        }

        adapter.updateList(filtered)

        b.progressLoading.visibility = View.GONE
        b.txtEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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
