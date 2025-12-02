package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.TransporterBooking
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ActiveTasksAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class TransporterBooking(
    val bookingId: String = "",
    val serviceProviderName: String = "",
    val serviceProviderCompany: String = "",
    val bookingStatus: String = "",
    val bookingDate: Timestamp? = null
)

class SP_ActiveTasks : Fragment() {

    private lateinit var recyclerActiveTasks: RecyclerView
    private lateinit var txtEmptyState: TextView
    private lateinit var progressLoading: ProgressBar

    private val bookings = mutableListOf<TransporterBooking>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_active_tasks, container, false)

        recyclerActiveTasks = view.findViewById(R.id.recyclerActiveTasks)
        txtEmptyState = view.findViewById(R.id.txtEmptyState)
        progressLoading = view.findViewById(R.id.progressLoading)

        recyclerActiveTasks.layoutManager = LinearLayoutManager(requireContext())

        val adapter = ActiveTasksAdapter(bookings) { booking ->
            val bundle = Bundle().apply {
                putString("bookingId", booking.bookingId)
            }
            // navigate to your existing details screen - keep the nav id you already use
            findNavController().navigate(
                R.id.action_spActiveTasksFragment_to_spTaskUpdateDetailsFragment,
                bundle
            )
        }

        recyclerActiveTasks.adapter = adapter

        // UI start state
        txtEmptyState.visibility = View.GONE
        progressLoading.visibility = View.VISIBLE

        // initial load
        loadAllConfirmedBookings(adapter) {
            progressLoading.visibility = View.GONE
            txtEmptyState.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }

        return view
    }

    /**
     * Load bookings:
     * - If current user is TSD: query tsd_bookings for facilityId OR tsdFacilityId with status == "Confirmed"
     * - Otherwise: query transport_bookings with bookingStatus == "Confirmed"
     *
     * This function mirrors transport_bookings data mapping for the list rows (does not modify transport_bookings).
     */
    private fun loadAllConfirmedBookings(adapter: ActiveTasksAdapter, done: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            done(); return
        }

        bookings.clear()
        val seen = mutableSetOf<String>()

        // Helper: determine if current user is TSD (check service_providers role or users.role)
        fun checkIfCurrentUserIsTsd(callback: (Boolean) -> Unit) {
            db.collection("service_providers").document(uid)
                .get()
                .addOnSuccessListener { sp ->
                    // read 'role' OR fallback keys used in your project
                    val providerType = (sp.getString("role")
                        ?: sp.getString("providerType")
                        ?: sp.getString("type")
                        ?: "").lowercase()
                    if (providerType.contains("tsd")) {
                        callback(true)
                    } else {
                        // fallback to users/{uid}
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

        checkIfCurrentUserIsTsd { isTsd ->
            if (isTsd) {
                // USER IS TSD -> mirror transport row mapping using tsd_bookings data
                db.collection("tsd_bookings")
                    .whereEqualTo("facilityId", uid)
                    .whereIn("status", listOf("Confirmed", "In Transit", "Delivered"))
                    .get()
                    .addOnSuccessListener { snap1 ->
                        for (doc in snap1.documents) {
                            val id = doc.id
                            if (seen.contains(id)) continue
                            val map = doc.data ?: continue
                            val b = TransporterBooking(
                                bookingId = id,
                                serviceProviderName = (map["confirmedBy"] as? String) ?: "",
                                serviceProviderCompany = (map["facilityName"] as? String)
                                    ?: (map["facility"] as? String) ?: "",
                                bookingStatus = (map["status"] as? String)
                                    ?: (map["bookingStatus"] as? String) ?: "",
                                bookingDate = map["dateCreated"] as? Timestamp
                            )
                            bookings.add(b); seen.add(id)
                        }

                        // also check tsdFacilityId ownership
                        db.collection("tsd_bookings")
                            .whereEqualTo("tsdFacilityId", uid)
                            .whereIn("status", listOf("Confirmed", "In Transit", "Delivered"))
                            .get()
                            .addOnSuccessListener { snap2 ->
                                for (doc in snap2.documents) {
                                    val id = doc.id
                                    if (seen.contains(id)) continue
                                    val map = doc.data ?: continue
                                    val b = TransporterBooking(
                                        bookingId = id,
                                        serviceProviderName = (map["confirmedBy"] as? String) ?: "",
                                        serviceProviderCompany = (map["facilityName"] as? String)
                                            ?: (map["facility"] as? String) ?: "",
                                        bookingStatus = (map["status"] as? String)
                                            ?: (map["bookingStatus"] as? String) ?: "",
                                        bookingDate = map["dateCreated"] as? Timestamp
                                    )
                                    bookings.add(b); seen.add(id)
                                }
                                adapter.notifyDataSetChanged()
                                done()
                            }
                            .addOnFailureListener {
                                adapter.notifyDataSetChanged()
                                done()
                            }
                    }
                    .addOnFailureListener {
                        adapter.notifyDataSetChanged()
                        done()
                    }
            } else {
                // NOT TSD -> keep transport_bookings query EXACTLY as before (mirror behavior)
                db.collection("transport_bookings")
                    .whereEqualTo("bookingStatus", "Confirmed")
                    .get()
                    .addOnSuccessListener { snapTransport ->
                        for (doc in snapTransport.documents) {
                            val map = doc.data ?: continue
                            val b = TransporterBooking(
                                bookingId = doc.id,
                                serviceProviderName = map["serviceProviderName"] as? String ?: "",
                                serviceProviderCompany = map["serviceProviderCompany"] as? String ?: "",
                                bookingStatus = map["bookingStatus"] as? String ?: "",
                                bookingDate = map["bookingDate"] as? Timestamp
                            )
                            bookings.add(b); seen.add(doc.id)
                        }
                        adapter.notifyDataSetChanged()
                        done()
                    }
                    .addOnFailureListener {
                        adapter.notifyDataSetChanged()
                        done()
                    }
            }
        }
    }
}

