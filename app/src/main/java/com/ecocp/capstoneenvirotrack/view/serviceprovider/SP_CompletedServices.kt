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
     */
    private fun subscribeToCompletedServicesRealtime(forceRefresh: Boolean = false) {
        binding.progressLoading?.visibility = View.VISIBLE
        binding.txtEmptyState?.visibility = View.GONE

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            // no user: show empty and return
            Log.w(TAG, "No authenticated user found; completed list will remain empty.")
            binding.progressLoading?.visibility = View.GONE
            binding.txtEmptyState.visibility = View.VISIBLE
            binding.swipeRefresh.isRefreshing = false
            return
        }

        // remove previous listener if forcing refresh or switching listeners
        if (forceRefresh) {
            listener?.remove()
            listener = null
        }

        if (listener != null && !forceRefresh) {
            Log.d(TAG, "Already listening for completed services.")
            binding.progressLoading?.visibility = View.GONE
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

                // Use uid as the facility identifier
                val facilityId = uid

                // NOTE: removed server-side status filter. We'll perform client-side filtering
                val qFacility: Query = db.collection("tsd_bookings")
                    .whereEqualTo("facilityId", facilityId)
                    .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

                // primary listener for facilityId
                listener = qFacility.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "TSD listener error (facilityId): ${error.message}", error)
                        Toast.makeText(requireContext(), "Failed to load completed services.", Toast.LENGTH_SHORT).show()
                        binding.progressLoading?.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w(TAG, "TSD listener snapshot is null (facilityId)")
                        binding.progressLoading?.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        binding.txtEmptyState.visibility = View.VISIBLE
                        return@addSnapshotListener
                    }

                    // also query tsdFacilityId and merge results (non-blocking)
                    val qTsdId: Query = db.collection("tsd_bookings")
                        .whereEqualTo("tsdFacilityId", uid)
                        .orderBy("statusUpdatedAt", Query.Direction.DESCENDING)
                        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

                    qTsdId.get()
                        .addOnSuccessListener { snap2 ->
                            // combine primary snapshot + snap2 while avoiding duplicates
                            val allDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                            snapshot.documents.forEach { allDocs.add(it) }
                            snap2.documents.forEach { d ->
                                if (!allDocs.any { a -> a.id == d.id }) allDocs.add(d)
                            }

                            // map to ServiceRequest with client-side filtering for Delivered/Completed
                            val list = allDocs.mapNotNull { doc ->
                                try {
                                    val m = doc.data ?: return@mapNotNull null

                                    // --- NORMALIZE status: prefer "status", fallback to "bookingStatus" ---
                                    val rawStatus = (m["status"] as? String) ?: (m["bookingStatus"] as? String) ?: ""

                                    val isCompletedStatus = rawStatus.equals("Delivered", ignoreCase = true)
                                            || rawStatus.equals("Completed", ignoreCase = true)

                                    if (!isCompletedStatus) {
                                        // skip non-completed docs
                                        return@mapNotNull null
                                    }

                                    // pick completed timestamp candidates used for display (confirmedAt/treatedAt/statusUpdatedAt/dateCreated)
                                    val completedTs = (m["completedAt"] as? Timestamp)
                                        ?: (m["updatedAt"] as? Timestamp)
                                        ?: (m["assignedAt"] as? Timestamp)
                                        ?: (m["dateBooked"] as? Timestamp)

                                    val displayDate = completedTs?.toDate()?.let {
                                        DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                                    } ?: ""

// attachments same as you have...
                                    val attachments = mutableListOf<String>()
                                    (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                                    (m["finalReportUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["transportPlanUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["storagePermitUrl"] as? String)?.takeIf { it.isNotBlank() }?.let { attachments.add(it) }
                                    (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }

                                    if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

// payment status appended to notes (transport only)
                                    val paymentStatus = (m["paymentStatus"] as? String)?.takeIf { it.isNotBlank() } ?: ""
                                    val notes = (m["specialInstructions"] as? String) ?: (m["notes"] as? String) ?: ""
                                    val combinedNotes = if (paymentStatus.isNotBlank()) "$notes\nPayment: $paymentStatus" else notes


                                    ServiceRequest(
                                        id = doc.id,
                                        bookingId = (m["bookingId"] as? String) ?: doc.id,
                                        clientName = (m["pcoId"] as? String) ?: "",
                                        companyName = (m["serviceProviderCompany"] as? String) ?: (m["companyName"] as? String) ?: "",
                                        providerName = (m["serviceProviderName"] as? String) ?: "",
                                        providerContact = (m["providerContact"] as? String) ?: "",
                                        serviceTitle = if (((m["wasteType"] as? String) ?: "").isNotBlank()) "Transport - ${m["wasteType"]}" else "Transport Booking",
                                        bookingStatus = (m["bookingStatus"] as? String) ?: "Delivered",
                                        origin = (m["origin"] as? String) ?: "",
                                        destination = (m["destination"] as? String) ?: "",
                                        dateRequested = displayDate,
                                        dateBooked = completedTs,
                                        wasteType = (m["wasteType"] as? String) ?: "",
                                        quantity = (m["quantity"] as? String) ?: "",
                                        packaging = (m["packaging"] as? String) ?: "",
                                        notes = combinedNotes,
                                        compliance = "Delivered",
                                        attachments = attachments,
                                        imageUrl = attachments.firstOrNull() ?: DEV_FALLBACK,
                                        finalReportUrl = (m["finalReportUrl"] as? String)
                                    )

                                } catch (ex: Exception) {
                                    Log.e(TAG, "Error mapping TSD doc ${doc.id}: ${ex.message}", ex)
                                    null
                                }
                            }

                            // update UI
                            completedList.clear()
                            completedList.addAll(list)
                            adapter.setData(list)

                            Log.d(TAG, "TSD UI updated — items: ${list.size}")
                            binding.progressLoading?.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed merging tsdFacilityId results: ${e.message}", e)
                            // fallback: still map primary snapshot only, with client-side filtering
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val m = doc.data ?: return@mapNotNull null

                                    val rawStatus = (m["status"] as? String) ?: (m["bookingStatus"] as? String) ?: ""
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
                                    ServiceRequest(
                                        id = doc.id,
                                        bookingId = (m["bookingId"] as? String) ?: doc.id,
                                        clientName = (m["userId"] as? String) ?: "",
                                        companyName = (m["facilityName"] as? String) ?: (m["facility"] as? String) ?: "",
                                        providerName = (m["confirmedBy"] as? String) ?: "",
                                        providerContact = (m["contactNumber"] as? String) ?: "",
                                        serviceTitle = if (((m["treatmentInfo"] as? String) ?: "").isNotBlank()) "TSD - ${m["treatmentInfo"]}" else "TSD Booking",
                                        bookingStatus = rawStatus,
                                        origin = (m["location"] as? String) ?: "",
                                        destination = (m["facilityName"] as? String) ?: "",
                                        dateRequested = displayDate,
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
                            binding.progressLoading?.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
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
                        binding.progressLoading?.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w(TAG, "Listener snapshot is null")
                        binding.progressLoading?.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        binding.txtEmptyState.visibility = View.VISIBLE
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
                    binding.progressLoading?.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.txtEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
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
