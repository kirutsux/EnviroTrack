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
     * - If current user is TSD: query tsd_bookings for ownership (tsdId / facilityId / tsdFacilityId)
     *   and filter client-side for confirmed statuses.
     * - Otherwise: query transport_bookings with bookingStatus == "Confirmed"
     *
     * Transporter side left unchanged.
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

        // statuses we treat as "active" — compare case-insensitively
        val activeStatusSet = setOf("confirmed", "in transit", "delivered", "completed", "in_transit")

        checkIfCurrentUserIsTsd { isTsd ->
            if (isTsd) {
                // USER IS TSD -> fetch candidate docs by ownership fields, then filter client-side
                // Accept Pairs here because you construct lists with "id to data"
                fun processSnapshot(snapDocs: List<Pair<String, Map<String, Any>>>) {
                    for ((id, map) in snapDocs) {
                        if (seen.contains(id)) continue
                        val rawStatus = (map["bookingStatus"] as? String)
                            ?: (map["status"] as? String)
                            ?: ""
                        if (rawStatus.lowercase() !in activeStatusSet) continue

                        val b = TransporterBooking(
                            bookingId = id,
                            serviceProviderName = (map["confirmedBy"] as? String) ?: "",
                            serviceProviderCompany = (map["tsdName"] as? String)
                                ?: (map["facilityName"] as? String)
                                ?: (map["facility"] as? String)
                                ?: "",
                            bookingStatus = rawStatus,
                            bookingDate = (map["timestamp"] as? Timestamp)
                                ?: (map["dateCreated"] as? Timestamp)
                                ?: (map["bookingDate"] as? Timestamp)
                        )
                        bookings.add(b); seen.add(id)
                    }
                }

                // 1) Query by tsdId
                db.collection("tsd_bookings")
                    .whereEqualTo("tsdId", uid)
                    .get()
                    .addOnSuccessListener { snap1 ->
                        val list1 = snap1.documents.mapNotNull { d ->
                            val dataMap = (d.data ?: emptyMap<String, Any>()) as Map<String, Any>
                            d.id to dataMap
                        }
                        processSnapshot(list1)
                        // 2) Also query by facilityId (older key) if needed
                        db.collection("tsd_bookings")
                            .whereEqualTo("facilityId", uid)
                            .get()
                            .addOnSuccessListener { snap2 ->
                                val list2 = snap2.documents.mapNotNull { d ->
                                    val dataMap = (d.data ?: emptyMap<String, Any>()) as Map<String, Any>
                                    d.id to dataMap
                                }
                                processSnapshot(list2)
                                // 3) Also query by tsdFacilityId (another possible key)
                                db.collection("tsd_bookings")
                                    .whereEqualTo("tsdFacilityId", uid)
                                    .get()
                                    .addOnSuccessListener { snap3 ->
                                        val list3 = snap3.documents.mapNotNull { d ->
                                            val dataMap = (d.data ?: emptyMap<String, Any>()) as Map<String, Any>
                                            d.id to dataMap
                                        }
                                        processSnapshot(list3)
                                        // 4) Final fallback: try matching by tsdName/facilityName (companyName)
                                        db.collection("service_providers").document(uid)
                                            .get()
                                            .addOnSuccessListener { spDoc ->
                                                val tsdName = spDoc.getString("companyName")?.trim().orEmpty()
                                                if (tsdName.isNotEmpty()) {
                                                    db.collection("tsd_bookings")
                                                        .whereEqualTo("tsdName", tsdName)
                                                        .get()
                                                        .addOnSuccessListener { snap4 ->
                                                            val list4 = snap4.documents.mapNotNull { d ->
                                                                val dataMap = (d.data ?: emptyMap<String, Any>()) as Map<String, Any>
                                                                d.id to dataMap
                                                            }
                                                            processSnapshot(list4)
                                                            adapter.notifyDataSetChanged(); done()
                                                        }
                                                        .addOnFailureListener {
                                                            adapter.notifyDataSetChanged(); done()
                                                        }
                                                } else {
                                                    adapter.notifyDataSetChanged(); done()
                                                }
                                            }
                                            .addOnFailureListener {
                                                adapter.notifyDataSetChanged(); done()
                                            }
                                    }
                                    .addOnFailureListener {
                                        adapter.notifyDataSetChanged(); done()
                                    }
                            }
                            .addOnFailureListener {
                                adapter.notifyDataSetChanged(); done()
                            }
                    }
                    .addOnFailureListener {
                        // If primary tsdId query fails, try fallback facilityId, then proceed similarly
                        db.collection("tsd_bookings")
                            .whereEqualTo("facilityId", uid)
                            .get()
                            .addOnSuccessListener { snap2 ->
                                val list2 = snap2.documents.mapNotNull { d ->
                                    val dataMap = (d.data ?: emptyMap<String, Any>()) as Map<String, Any>
                                    d.id to dataMap
                                }
                                processSnapshot(list2)
                                adapter.notifyDataSetChanged(); done()
                            }
                            .addOnFailureListener {
                                adapter.notifyDataSetChanged(); done()
                            }
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
