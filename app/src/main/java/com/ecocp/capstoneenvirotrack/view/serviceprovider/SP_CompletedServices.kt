package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpCompletedServicesBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.CompletedServiceAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlin.Exception

private const val TAG = "SP_CompletedServices"

class SP_CompletedServices : Fragment() {

    private var _binding: FragmentSpCompletedServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CompletedServiceAdapter
    private val completedList = mutableListOf<ServiceRequest>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Configurable fallback image
    private val DEV_FALLBACK = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    // Firestore listener handle so we can remove it
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpCompletedServicesBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSwipeToRefresh()
        subscribeToCompletedServicesRealtime()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = CompletedServiceAdapter(completedList) { selected ->
            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("status", selected.bookingStatus)
                putString("compliance", selected.compliance)
                putString("clientName", selected.clientName)
                putString("requestId", selected.id)
                putString("finalReportUrl", selected.finalReportUrl ?: "")
            }
            findNavController().navigate(R.id.SP_ServiceReport, bundle)
        }

        binding.recyclerCompleted.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCompleted.adapter = adapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // force a single refresh by re-subscribing
            subscribeToCompletedServicesRealtime(forceRefresh = true)
        }
    }

    /**
     * Subscribe in realtime to bookings for current provider where delivered/completed.
     * Mirrors transport behaviour for TSD (maps tsd_bookings into ServiceRequest).
     *
     * Minimal changes:
     * - Prefer tsdName instead of facilityName for TSD documents (company/provider/destination)
     * - Show paymentStatus (if present) in the UI slot previously used for booking date (dateRequested)
     * - Transport branch remains unchanged
     */
    private fun subscribeToCompletedServicesRealtime(forceRefresh: Boolean = false) {
        // safe UI updates even when binding may be null in some lifecycle races
        try {
            binding.progressLoading.visibility = View.VISIBLE
            binding.txtEmptyState.visibility = View.GONE
        } catch (ignored: Exception) { /* ignore if view gone */ }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "No authenticated user found; completed list will remain empty.")
            try {
                binding.progressLoading.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false
            } catch (ignored: Exception) {}
            return
        }

        // remove previous listener if forcing refresh
        if (forceRefresh) {
            listener?.remove()
            listener = null
        }

        if (listener != null && !forceRefresh) {
            Log.d(TAG, "Already listening for completed services.")
            try { binding.progressLoading.visibility = View.GONE } catch (_: Exception) {}
            return
        }

        Log.d(TAG, "Checking user role to decide query (providerId=$uid)")

        // Helper: detect TSD role (check service_providers.role or fallback to users.role)
        fun isCurrentUserTsd(callback: (Boolean) -> Unit) {
            db.collection("service_providers").document(uid)
                .get()
                .addOnSuccessListener { sp ->
                    val spRole = (sp.getString("role") ?: sp.getString("providerType") ?: sp.getString("type") ?: "").lowercase()
                    if (spRole.contains("tsd")) {
                        callback(true)
                    } else {
                        // fallback to users doc
                        db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { u ->
                                val uRole = (u.getString("role") ?: u.getString("accountType") ?: "").lowercase()
                                callback(uRole.contains("tsd"))
                            }
                            .addOnFailureListener {
                                callback(false)
                            }
                    }
                }
                .addOnFailureListener {
                    // fallback to users doc
                    db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { u ->
                            val uRole = (u.getString("role") ?: u.getString("accountType") ?: "").lowercase()
                            callback(uRole.contains("tsd"))
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
        }

        isCurrentUserTsd { isTsd ->
            if (isTsd) {
                Log.d(TAG, "User is TSD → subscribing to tsd_bookings (mirror transport 'Completed')")

                // Primary query: attempt to listen by tsdId; we'll also merge tsdFacilityId and fallback to facilityId/tsdName
                val qFacility: Query = db.collection("tsd_bookings")
                    .whereEqualTo("tsdId", uid)
                    .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

                // primary listener for tsdId
                listener = qFacility.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "TSD listener error (tsdId): ${error.message}", error)
                        Toast.makeText(requireContext(), "Failed to load completed services.", Toast.LENGTH_SHORT).show()
                        try {
                            binding.progressLoading.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                        } catch (_: Exception) {}
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w(TAG, "TSD listener snapshot is null (tsdId)")
                        try {
                            binding.progressLoading.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            binding.txtEmptyState.visibility = View.VISIBLE
                        } catch (_: Exception) {}
                        return@addSnapshotListener
                    }

                    // also fetch tsdFacilityId docs and merge results
                    val qTsdFacilityId: Query = db.collection("tsd_bookings")
                        .whereEqualTo("tsdFacilityId", uid)
                        .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

                    qTsdFacilityId.get()
                        .addOnSuccessListener { snap2 ->
                            // combine primary snapshot + snap2 while avoiding duplicates
                            val allDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                            snapshot.documents.forEach { allDocs.add(it) }
                            snap2.documents.forEach { d ->
                                if (!allDocs.any { a -> a.id == d.id }) allDocs.add(d)
                            }

                            // final fallback: if nothing found, try facilityId and tsdName later
                            // Also do client-side filtering for Completed/Delivered status
                            val list = allDocs.mapNotNull { doc ->
                                try {
                                    val m = doc.data ?: return@mapNotNull null

                                    // normalize status: prefer bookingStatus then status
                                    val rawStatus = (m["bookingStatus"] as? String) ?: (m["status"] as? String) ?: ""
                                    val isCompletedStatus = rawStatus.equals("Delivered", ignoreCase = true)
                                            || rawStatus.equals("Completed", ignoreCase = true)

                                    if (!isCompletedStatus) return@mapNotNull null

                                    // choose the best completed timestamp available (kept for dateBooked field)
                                    val completedTs = (m["completedAt"] as? Timestamp)
                                        ?: (m["statusUpdatedAt"] as? Timestamp)
                                        ?: (m["confirmedAt"] as? Timestamp)
                                        ?: (m["dateCreated"] as? Timestamp)

                                    val displayDate = completedTs?.toDate()?.let {
                                        DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                                    } ?: ""

                                    // attachments
                                    val attachments = mutableListOf<String>()
                                    (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                                    (m["finalReportUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["certificateUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["previousRecordUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }

                                    if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

                                    // payment status value (we will show this in the dateRequested slot if present)
                                    val paymentStatus = (m["paymentStatus"] as? String)?.takeIf { it.isNotBlank() } ?: ""

                                    // notes combine
                                    val notes = (m["specialInstructions"] as? String) ?: (m["notes"] as? String) ?: ""
                                    val combinedNotes = if (paymentStatus.isNotBlank()) "$notes\nPayment: $paymentStatus" else notes

                                    // PREFER tsdName over facilityName for TSD docs (company/provider/destination)
                                    val tsdName = (m["tsdName"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: (m["facilityName"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: (m["facility"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: "TSD Facility"

                                    // dateRequested: SHOW paymentStatus if available, otherwise fallback to displayDate
                                    val dateRequestedToShow = if (paymentStatus.isNotBlank()) paymentStatus else displayDate

                                    // Build ServiceRequest mapping for TSD doc (keep fields consistent with UI)
                                    ServiceRequest(
                                        id = doc.id,
                                        bookingId = (m["tsdBookingId"] as? String) ?: (m["bookingId"] as? String) ?: doc.id,
                                        clientName = (m["generatorId"] as? String) ?: (m["userId"] as? String) ?: "",
                                        companyName = tsdName,
                                        providerName = tsdName,
                                        providerContact = (m["contactNumber"] as? String) ?: (m["providerContact"] as? String) ?: "",
                                        serviceTitle = if (((m["treatmentInfo"] as? String) ?: "").isNotBlank()) "TSD - ${m["treatmentInfo"]}" else "TSD Booking",
                                        bookingStatus = rawStatus.ifBlank { "Delivered" },
                                        origin = (m["location"] as? String) ?: "",
                                        destination = tsdName,
                                        dateRequested = dateRequestedToShow,
                                        dateBooked = completedTs,
                                        wasteType = (m["treatmentInfo"] as? String) ?: (m["wasteType"] as? String) ?: "",
                                        quantity = when (val q = m["quantity"]) { is Number -> q.toString(); is String -> q; else -> "" },
                                        packaging = (m["packaging"] as? String) ?: "",
                                        notes = combinedNotes,
                                        compliance = "Completed",
                                        attachments = attachments,
                                        imageUrl = attachments.firstOrNull() ?: DEV_FALLBACK,
                                        finalReportUrl = (m["finalReportUrl"] as? String)
                                    )
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Error mapping TSD doc ${doc.id}: ${ex.message}", ex)
                                    null
                                }
                            }

                            // If nothing returned and we suspect other ownership fields may exist, attempt fallback fetches:
                            if (list.isEmpty()) {
                                // attempt facilityId fallback then tsdName (companyName) as final attempt (non-blocking)
                                val fallbackByFacility = db.collection("tsd_bookings")
                                    .whereEqualTo("facilityId", uid)
                                    .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

                                fallbackByFacility.get()
                                    .addOnSuccessListener { snapFacility ->
                                        val docs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                                        docs.addAll(snapFacility.documents)

                                        // if still empty, try tsdName (companyName)
                                        db.collection("service_providers").document(uid)
                                            .get()
                                            .addOnSuccessListener { spDoc ->
                                                val tsdNameFromProfile = spDoc.getString("companyName")?.trim().orEmpty()
                                                if (tsdNameFromProfile.isNotEmpty()) {
                                                    db.collection("tsd_bookings")
                                                        .whereEqualTo("tsdName", tsdNameFromProfile)
                                                        .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                                                        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                                                        .get()
                                                        .addOnSuccessListener { snapName ->
                                                            docs.addAll(snapName.documents.filter { nd -> docs.none { d -> d.id == nd.id } })
                                                            // map docs -> ServiceRequest similarly to above
                                                            val fallbackList = docs.mapNotNull { doc ->
                                                                try {
                                                                    val m = doc.data ?: return@mapNotNull null
                                                                    val rawStatus = (m["bookingStatus"] as? String) ?: (m["status"] as? String) ?: ""
                                                                    val isCompletedStatus = rawStatus.equals("Delivered", ignoreCase = true) || rawStatus.equals("Completed", ignoreCase = true)
                                                                    if (!isCompletedStatus) return@mapNotNull null
                                                                    val completedTs = (m["completedAt"] as? Timestamp)
                                                                        ?: (m["statusUpdatedAt"] as? Timestamp)
                                                                        ?: (m["confirmedAt"] as? Timestamp)
                                                                        ?: (m["dateCreated"] as? Timestamp)
                                                                    val displayDate = completedTs?.toDate()?.let {
                                                                        DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                                                                    } ?: ""
                                                                    val paymentStatus = (m["paymentStatus"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                                                                    val dateRequestedToShow = if (paymentStatus.isNotBlank()) paymentStatus else displayDate
                                                                    val attachments = mutableListOf<String>()
                                                                    (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                                                                    (m["finalReportUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                                                    if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)
                                                                    val tsdName = (m["tsdName"] as? String)?.takeIf { it.isNotBlank() }
                                                                        ?: (m["facilityName"] as? String)?.takeIf { it.isNotBlank() }
                                                                        ?: (m["facility"] as? String)?.takeIf { it.isNotBlank() }
                                                                        ?: "TSD Facility"
                                                                    ServiceRequest(
                                                                        id = doc.id,
                                                                        bookingId = (m["tsdBookingId"] as? String) ?: (m["bookingId"] as? String) ?: doc.id,
                                                                        clientName = (m["generatorId"] as? String) ?: (m["userId"] as? String) ?: "",
                                                                        companyName = tsdName,
                                                                        providerName = tsdName,
                                                                        providerContact = (m["contactNumber"] as? String) ?: "",
                                                                        serviceTitle = if (((m["treatmentInfo"] as? String) ?: "").isNotBlank()) "TSD - ${m["treatmentInfo"]}" else "TSD Booking",
                                                                        bookingStatus = rawStatus.ifBlank { "Delivered" },
                                                                        origin = (m["location"] as? String) ?: "",
                                                                        destination = tsdName,
                                                                        dateRequested = dateRequestedToShow,
                                                                        dateBooked = completedTs,
                                                                        wasteType = (m["treatmentInfo"] as? String) ?: (m["wasteType"] as? String) ?: "",
                                                                        quantity = when (val q = m["quantity"]) { is Number -> q.toString(); is String -> q; else -> "" },
                                                                        packaging = (m["packaging"] as? String) ?: "",
                                                                        notes = (m["treatmentInfo"] as? String) ?: (m["notes"] as? String) ?: "",
                                                                        compliance = "Completed",
                                                                        attachments = attachments,
                                                                        imageUrl = attachments.firstOrNull() ?: DEV_FALLBACK,
                                                                        finalReportUrl = (m["finalReportUrl"] as? String)
                                                                    )
                                                                } catch (ex: Exception) {
                                                                    Log.e(TAG, "Error fallback-mapping TSD doc ${doc.id}: ${ex.message}", ex)
                                                                    null
                                                                }
                                                            }

                                                            // update UI with fallbackList
                                                            completedList.clear()
                                                            completedList.addAll(fallbackList)
                                                            adapter.setData(fallbackList)
                                                            try {
                                                                binding.progressLoading.visibility = View.GONE
                                                                binding.swipeRefresh.isRefreshing = false
                                                                binding.txtEmptyState.visibility = if (fallbackList.isEmpty()) View.VISIBLE else View.GONE
                                                            } catch (_: Exception) {}
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e(TAG, "Failed tsdName fallback: ${e.message}", e)
                                                            completedList.clear()
                                                            adapter.setData(emptyList())
                                                            try {
                                                                binding.progressLoading.visibility = View.GONE
                                                                binding.swipeRefresh.isRefreshing = false
                                                                binding.txtEmptyState.visibility = View.VISIBLE
                                                            } catch (_: Exception) {}
                                                        }
                                                } else {
                                                    // no tsdName -> show empty
                                                    completedList.clear()
                                                    adapter.setData(emptyList())
                                                    try {
                                                        binding.progressLoading.visibility = View.GONE
                                                        binding.swipeRefresh.isRefreshing = false
                                                        binding.txtEmptyState.visibility = View.VISIBLE
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "Failed fetching service_providers for tsdName fallback: ${e.message}", e)
                                                completedList.clear()
                                                adapter.setData(emptyList())
                                                try {
                                                    binding.progressLoading.visibility = View.GONE
                                                    binding.swipeRefresh.isRefreshing = false
                                                    binding.txtEmptyState.visibility = View.VISIBLE
                                                } catch (_: Exception) {}
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Facility fallback failed: ${e.message}", e)
                                        completedList.clear()
                                        adapter.setData(emptyList())
                                        try {
                                            binding.progressLoading.visibility = View.GONE
                                            binding.swipeRefresh.isRefreshing = false
                                            binding.txtEmptyState.visibility = View.VISIBLE
                                        } catch (_: Exception) {}
                                    }
                            } else {
                                // update UI with list
                                completedList.clear()
                                completedList.addAll(list)
                                adapter.setData(list)
                                Log.d(TAG, "TSD UI updated — items: ${list.size}")
                                try {
                                    binding.progressLoading.visibility = View.GONE
                                    binding.swipeRefresh.isRefreshing = false
                                    binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                                } catch (_: Exception) {}
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed merging tsdFacilityId results: ${e.message}", e)
                            // fallback: map primary snapshot only, with client-side filtering
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val m = doc.data ?: return@mapNotNull null
                                    val rawStatus = (m["bookingStatus"] as? String) ?: (m["status"] as? String) ?: ""
                                    val isCompletedStatus = rawStatus.equals("Delivered", ignoreCase = true)
                                            || rawStatus.equals("Completed", ignoreCase = true)
                                    if (!isCompletedStatus) return@mapNotNull null
                                    val completedTs = (m["completedAt"] as? Timestamp)
                                        ?: (m["statusUpdatedAt"] as? Timestamp)
                                        ?: (m["confirmedAt"] as? Timestamp)
                                        ?: (m["dateCreated"] as? Timestamp)
                                    val displayDate = completedTs?.toDate()?.let {
                                        DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                                    } ?: ""
                                    val attachments = mutableListOf<String>()
                                    (m["previousRecordUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["certificateUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

                                    val paymentStatus = (m["paymentStatus"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                                    val dateRequestedToShow = if (paymentStatus.isNotBlank()) paymentStatus else displayDate

                                    val tsdName = (m["tsdName"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: (m["facilityName"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: (m["facility"] as? String)?.takeIf { it.isNotBlank() }
                                        ?: ""

                                    ServiceRequest(
                                        id = doc.id,
                                        bookingId = (m["tsdBookingId"] as? String) ?: (m["bookingId"] as? String) ?: doc.id,
                                        clientName = (m["userId"] as? String) ?: (m["generatorId"] as? String) ?: "",
                                        companyName = tsdName,
                                        providerName = (m["confirmedBy"] as? String) ?: "",
                                        providerContact = (m["contactNumber"] as? String) ?: "",
                                        serviceTitle = if (((m["treatmentInfo"] as? String) ?: "").isNotBlank()) "TSD - ${m["treatmentInfo"]}" else "TSD Booking",
                                        bookingStatus = rawStatus,
                                        origin = (m["location"] as? String) ?: "",
                                        destination = tsdName,
                                        dateRequested = dateRequestedToShow,
                                        dateBooked = completedTs,
                                        wasteType = (m["treatmentInfo"] as? String) ?: "",
                                        quantity = when (val q = m["quantity"]) { is Number -> q.toString(); is String -> q; else -> "" },
                                        packaging = (m["packaging"] as? String) ?: "",
                                        notes = (m["treatmentInfo"] as? String) ?: (m["notes"] as? String) ?: "",
                                        compliance = "Completed",
                                        attachments = attachments,
                                        imageUrl = attachments.firstOrNull() ?: DEV_FALLBACK,
                                        finalReportUrl = (m["finalReportUrl"] as? String)
                                    )
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Error mapping TSD doc ${doc.id}: ${ex.message}", ex)
                                    null
                                }
                            }
                            completedList.clear()
                            completedList.addAll(list)
                            adapter.setData(list)
                            try {
                                binding.progressLoading.visibility = View.GONE
                                binding.swipeRefresh.isRefreshing = false
                                binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                            } catch (_: Exception) {}
                        }
                }
            } else {
                Log.d(TAG, "User is NOT TSD → subscribing to transport_bookings (UNCHANGED)")

                // Use the EXACT transport query you already had (no changes)
                val q: Query = db.collection("transport_bookings")
                    .whereEqualTo("providerId", uid)
                    .whereEqualTo("wasteStatus", "Delivered")
                    .orderBy("assignedAt", Query.Direction.DESCENDING)

                listener = q.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listener error: ${error.message}", error)
                        Toast.makeText(requireContext(), "Failed to load completed services.", Toast.LENGTH_SHORT).show()
                        try {
                            binding.progressLoading.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                        } catch (_: Exception) {}
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w(TAG, "Listener snapshot is null")
                        try {
                            binding.progressLoading.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            binding.txtEmptyState.visibility = View.VISIBLE
                        } catch (_: Exception) {}
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Snapshot received: ${snapshot.size()} documents")
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val m = doc.data ?: return@mapNotNull null

                            val completedTs = (m["completedAt"] as? Timestamp)
                                ?: (m["updatedAt"] as? Timestamp)
                                ?: (m["assignedAt"] as? Timestamp)
                                ?: (m["dateBooked"] as? Timestamp)

                            val displayDate = completedTs?.toDate()?.let {
                                DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                            } ?: ""

                            val attachments = mutableListOf<String>()
                            (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                            (m["finalReportUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                            (m["transportPlanUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                            (m["storagePermitUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                            (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }

                            if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

                            // 🚚 TRANSPORT-SAFE MAPPING (no TSD fields shown)
                            ServiceRequest(
                                id = doc.id,
                                bookingId = (m["bookingId"] as? String) ?: doc.id,
                                clientName = (m["pcoId"] as? String) ?: "",
                                companyName = (m["serviceProviderCompany"] as? String) ?: (m["companyName"] as? String) ?: "",
                                providerName = (m["serviceProviderName"] as? String) ?: "",
                                providerContact = (m["providerContact"] as? String) ?: "",
                                serviceTitle = if (((m["wasteType"] as? String) ?: "").isNotBlank()) "Transport - ${m["wasteType"]}" else "Transport Booking",
                                bookingStatus = (m["bookingStatus"] as? String) ?: "Delivered",

                                // 🚫 Do NOT display TSD treatment/location fields
                                origin = (m["origin"] as? String) ?: "",
                                destination = (m["destination"] as? String) ?: "",

                                dateRequested = displayDate,
                                dateBooked = completedTs,

                                wasteType = (m["wasteType"] as? String) ?: "",
                                quantity = (m["quantity"] as? String) ?: "",
                                packaging = (m["packaging"] as? String) ?: "",

                                // Transport-specific notes only
                                notes = (m["specialInstructions"] as? String) ?: (m["notes"] as? String) ?: "",

                                compliance = "Delivered",
                                attachments = attachments,
                                imageUrl = attachments.firstOrNull() ?: DEV_FALLBACK,
                                finalReportUrl = (m["finalReportUrl"] as? String)
                            )
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error mapping doc ${doc.id}: ${ex.message}", ex)
                            null
                        }
                    }

                    // update UI
                    completedList.clear()
                    completedList.addAll(list)
                    adapter.setData(list)

                    Log.d(TAG, "UI updated — items: ${list.size}")
                    try {
                        binding.progressLoading.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
