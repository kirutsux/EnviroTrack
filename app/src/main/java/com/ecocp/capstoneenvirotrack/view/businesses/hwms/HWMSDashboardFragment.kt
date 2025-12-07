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
import com.ecocp.capstoneenvirotrack.adapter.Tab1WasteGenAdapter
import com.ecocp.capstoneenvirotrack.adapter.Tab2TransportAdapter
import com.ecocp.capstoneenvirotrack.adapter.Tab3TsdAdapter
import com.ecocp.capstoneenvirotrack.adapter.Tab4PttAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem
import com.ecocp.capstoneenvirotrack.model.WasteGenDisplay
import com.ecocp.capstoneenvirotrack.model.WasteItem
import com.ecocp.capstoneenvirotrack.view.businesses.dialogs.TransportDetailsBottomSheet
import com.ecocp.capstoneenvirotrack.view.businesses.dialogs.TsdBookingBottomSheet
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HWMSDashboardFragment : Fragment() {

    private var _binding: FragmentHwmsDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tab1WasteGenAdapter: Tab1WasteGenAdapter
    private lateinit var tab2TransportAdapter: Tab2TransportAdapter
    private lateinit var tab3TsdAdapter: Tab3TsdAdapter
    private lateinit var tab4PttAdapter: Tab4PttAdapter

    // This prevents crashes when listener fires after view is destroyed
    private var pttListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHwmsDashboardBinding.inflate(inflater, container, false)
        setupAdapters()
        setupTabs()
        setupListeners()
        loadWasteGeneration()
        return binding.root
    }

    private fun setupAdapters() {
        tab1WasteGenAdapter = Tab1WasteGenAdapter(mutableListOf()) { item ->
            Toast.makeText(requireContext(), "Selected: ${item.companyName}", Toast.LENGTH_SHORT).show()
        }

        tab2TransportAdapter = Tab2TransportAdapter(mutableListOf()) { item ->
            TransportDetailsBottomSheet(item.id).show(parentFragmentManager, "TransportDetailsBottomSheet")
        }

        tab3TsdAdapter = Tab3TsdAdapter(mutableListOf()) { item ->
            TsdBookingBottomSheet(item.rawMap ?: emptyMap()).show(parentFragmentManager, "TsdBookingBottomSheet")
        }

        tab4PttAdapter = Tab4PttAdapter(mutableListOf()) { /* TODO: Open PTT details */ }

        binding.recyclerViewHWMS.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupTabs() {
        with(binding.tabLayoutCategories) {
            addTab(newTab().setText("Waste Gen"))
            addTab(newTab().setText("Transporter"))
            addTab(newTab().setText("TSD"))
            addTab(newTab().setText("PTT"))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> loadWasteGeneration()
                        1 -> loadTransportBookings()
                        2 -> loadTsdBookings()
                        3 -> loadPttBookings()
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun setupListeners() {
        binding.btnAddApplication.setOnClickListener {
            findNavController().navigate(R.id.HwmsStep1Fragment)
        }
    }

    // -------------------- WASTE GENERATION --------------------
    private fun loadWasteGeneration() {
        val userId = auth.currentUser?.uid ?: return
        binding.recyclerViewHWMS.adapter = tab1WasteGenAdapter

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (snap.isEmpty) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    tab1WasteGenAdapter.update(emptyList())
                    return@addOnSuccessListener
                }

                val list = snap.documents.map { doc ->
                    val wastes = (doc["wasteDetails"] as? List<Map<String, Any>>)?.map { w ->
                        WasteItem(
                            wasteName = w["wasteName"]?.toString() ?: "",
                            quantity = w["quantity"]?.toString() ?: "",
                            wasteCode = w["wasteCode"]?.toString() ?: ""
                        )
                    } ?: emptyList()

                    WasteGenDisplay(
                        id = doc.id,
                        companyName = doc.getString("companyName") ?: "",
                        embRegNo = doc.getString("embRegNo") ?: "",
                        status = doc.getString("status") ?: "Pending",
                        timestamp = doc.getTimestamp("timestamp"),
                        wasteList = wastes,
                        transportBookingId = doc.getString("bookingId"),
                        transportStatus = null
                    )
                }

                tab1WasteGenAdapter.update(list)
                binding.tvEmptyState.visibility = View.GONE

                // Fetch transport statuses (same logic as before, with safety checks)
                loadTransportStatusesForGenerators(userId, list)
            }
            .addOnFailureListener {
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_SHORT).show()
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
    }

    private fun loadTransportStatusesForGenerators(userId: String, list: List<WasteGenDisplay>) {
        db.collection("transport_bookings")
            .whereEqualTo("pcoId", userId)
            .get()
            .addOnSuccessListener { bookingSnap ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val bookingStatusMap = bookingSnap.documents.associate { it.id to (it.getString("status") ?: it.getString("bookingStatus") ?: "waiting") }
                val wasteGenToBookingMap = mutableMapOf<String, String>()

                bookingSnap.documents.forEach { doc ->
                    (doc.get("wasteGeneratorIds") as? List<String>)?.forEach { wasteGenToBookingMap[it] = doc.id }
                    doc.getString("primaryWasteGeneratorId")?.let { wasteGenToBookingMap[it] = doc.id }
                }

                list.forEach { item ->
                    var bookingId = item.transportBookingId
                    if (bookingId.isNullOrEmpty()) {
                        bookingId = wasteGenToBookingMap[item.id]
                        if (bookingId != null) item.transportBookingId = bookingId
                    }

                    if (!bookingId.isNullOrEmpty() && bookingStatusMap.containsKey(bookingId)) {
                        tab1WasteGenAdapter.updateTransportStatusForBooking(bookingId, bookingStatusMap[bookingId]!!.lowercase())
                    }
                }
            }
    }

    // -------------------- TRANSPORT BOOKINGS --------------------
    private fun loadTransportBookings() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return
        binding.recyclerViewHWMS.adapter = tab2TransportAdapter
        binding.tvEmptyState.visibility = View.GONE

        db.collection("transport_bookings")
            .whereEqualTo("pcoId", userId)
            .get()
            .addOnSuccessListener { snap ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val list = snap.documents.map { doc ->
                    DisplayItem(
                        id = doc.id,
                        title = doc.getString("wasteType") ?: "Transport Booking",
                        subtitle = "Quantity: ${doc.getString("quantity")}",
                        transporter = doc.getString("serviceProviderName") ?: "",
                        tsdFacility = doc.getString("tsdFacilityName") ?: "",
                        permitNo = doc.getString("bookingId") ?: doc.id,
                        paymentStatus = doc.getString("paymentStatus") ?: "Unpaid",
                        status = doc.getString("status") ?: "Pending",
                        rawMap = doc.data ?: emptyMap()
                    )
                }

                tab2TransportAdapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    // -------------------- TSD BOOKINGS --------------------
    private fun loadTsdBookings() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return
        binding.recyclerViewHWMS.adapter = tab3TsdAdapter

        db.collection("tsd_bookings")
            .whereEqualTo("generatorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (snap.isEmpty) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    tab3TsdAdapter.update(emptyList())
                    return@addOnSuccessListener
                }

                val list = snap.documents.map { doc ->
                    DisplayItem(
                        id = doc.id,
                        title = doc.getString("tsdName") ?: "TSD Booking",
                        subtitle = "Waste: ${doc.getString("wasteType") ?: "N/A"}",
                        transporter = doc.getString("transporterName") ?: "",
                        tsdFacility = doc.getString("tsdName") ?: "",
                        permitNo = doc.getString("tsdBookingId") ?: doc.id,
                        paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                        status = doc.getString("bookingStatus") ?: "Waiting",
                        rawMap = doc.data ?: emptyMap()
                    )
                }

                tab3TsdAdapter.update(list)
                binding.tvEmptyState.visibility = View.GONE
            }
    }

    // -------------------- PTT BOOKINGS (NOW 100% CRASH-PROOF) --------------------
    private fun loadPttBookings() {
        if (!isAdded || _binding == null) return

        binding.recyclerViewHWMS.adapter = tab4PttAdapter
        val userId = auth.currentUser?.uid ?: return

        pttListener?.remove()

        pttListener = db.collection("ptt_applications")
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    tab4PttAdapter.update(emptyList())
                    return@addSnapshotListener
                }

                val pttDocs = snapshot.documents

                // Extract IDs we need to look up
                val transportBookingIds = pttDocs.mapNotNull { it.getString("transportBookingId") }
                val tsdBookingIds = pttDocs.mapNotNull { it.getString("tsdBookingId") }
                val generatorIds = pttDocs.mapNotNull { it.getString("generatorId") }

                // === 1. Fetch Transport Bookings (by document ID, not custom field!) ===
                if (transportBookingIds.isEmpty()) {
                    showPttListWithFallback(pttDocs, emptyMap(), emptyMap(), emptyMap())
                    return@addSnapshotListener
                }

                db.collection("transport_bookings")
                    .whereIn(FieldPath.documentId(), transportBookingIds)
                    .get()
                    .addOnSuccessListener { transportSnap ->
                        val transportMap = transportSnap.documents.associate { doc ->
                            doc.id to (doc.getString("serviceProviderName") ?: "Unknown Transporter")
                        }

                        // === 2. Fetch TSD Bookings (by document ID) ===
                        db.collection("tsd_bookings")
                            .whereIn(FieldPath.documentId(), tsdBookingIds)
                            .get()
                            .addOnSuccessListener { tsdSnap ->
                                val tsdMap = tsdSnap.documents.associate { doc ->
                                    doc.id to (doc.getString("tsdName") ?: "Unknown TSD")
                                }

                                // === 3. Fetch Generators (by document ID) ===
                                db.collection("HazardousWasteGenerator")
                                    .whereIn(FieldPath.documentId(), generatorIds)
                                    .get()
                                    .addOnSuccessListener { genSnap ->
                                        if (!isAdded || _binding == null) return@addOnSuccessListener

                                        val generatorMap = genSnap.documents.associate { doc ->
                                            doc.id to (doc.getString("companyName") ?: "Unknown Company")
                                        }

                                        showPttListWithFallback(pttDocs, transportMap, tsdMap, generatorMap)
                                    }
                            }
                    }
                    .addOnFailureListener {
                        if (isAdded && _binding != null) {
                            Toast.makeText(requireContext(), "Failed to load details", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    // Helper function to avoid nested hell
    private fun showPttListWithFallback(
        pttDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        transportMap: Map<String, String>,
        tsdMap: Map<String, String>,
        generatorMap: Map<String, String>
    ) {
        val displayList = pttDocs.map { doc ->
            val transportId = doc.getString("transportBookingId")
            val tsdId = doc.getString("tsdBookingId")
            val genId = doc.getString("generatorId")
            val pttId = doc.getString("pttId") ?: doc.id.takeLast(8)

            DisplayItem(
                id = doc.id,
                title = "PTT #$pttId",
                subtitle = generatorMap[genId] ?: "Generator",
                transporter = transportMap[transportId] ?: "Transporter",
                tsdFacility = tsdMap[tsdId] ?: "TSD Facility",
                permitNo = pttId,
                paymentStatus = doc.getString("paymentStatus") ?: "Paid",
                status = when (doc.getString("status")) {
                    "Pending Review" -> "Pending"
                    "Approved" -> "Approved"
                    "Rejected" -> "Rejected"
                    else -> doc.getString("status") ?: "Pending"
                },
                rawMap = doc.data ?: emptyMap()
            )
        }

        tab4PttAdapter.update(displayList)
        binding.tvEmptyState.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pttListener?.remove()  // This is the magic line that stops all crashes
        pttListener = null
        _binding = null
    }
}