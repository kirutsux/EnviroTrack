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
import com.google.firebase.firestore.EventListener
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
     * Subscribe in realtime to bookings for current provider where wasteStatus == "Delivered".
     * Logs results and updates adapter immediately.
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

        Log.d(TAG, "Subscribing to completed services for providerId=$uid")

        val q: Query = db.collection("transport_bookings")
            .whereEqualTo("providerId", uid)
            .whereEqualTo("wasteStatus", "Delivered")
            .orderBy("assignedAt", Query.Direction.DESCENDING)

        listener = q.addSnapshotListener(EventListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listener error: ${error.message}", error)
                Toast.makeText(requireContext(), "Failed to load completed services.", Toast.LENGTH_SHORT).show()
                binding.progressLoading?.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                return@EventListener
            }

            if (snapshot == null) {
                Log.w(TAG, "Listener snapshot is null")
                binding.progressLoading?.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.txtEmptyState.visibility = View.VISIBLE
                return@EventListener
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
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
