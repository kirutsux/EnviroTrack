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
import com.ecocp.capstoneenvirotrack.adapter.*
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem
import com.ecocp.capstoneenvirotrack.model.WasteGenDisplay
import com.ecocp.capstoneenvirotrack.model.WasteItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HWMSDashboardFragment : Fragment() {

    private var _binding: FragmentHwmsDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    // adapters
    private lateinit var wasteGenAdapter: WasteGenAdapter
    private lateinit var transportAdapter: TransportAdapter
    private lateinit var tsdAdapter: TsdAdapter
    private lateinit var pttAdapter: PttAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHwmsDashboardBinding.inflate(inflater, container, false)

        setupAdapters()
        setupTabs()
        setupListeners()

        // initial tab load
        loadWasteGeneration()

        return binding.root
    }

    private fun setupAdapters() {
        wasteGenAdapter = WasteGenAdapter(mutableListOf()) { item ->
            Toast.makeText(requireContext(), "Selected: ${item.companyName}", Toast.LENGTH_SHORT).show()
        }
        transportAdapter = TransportAdapter(mutableListOf()) { item ->
            Toast.makeText(requireContext(), "Selected: ${item.title}", Toast.LENGTH_SHORT).show()
        }
        tsdAdapter = TsdAdapter(mutableListOf()) { item ->
            Toast.makeText(requireContext(), "Selected: ${item.title}", Toast.LENGTH_SHORT).show()
        }
        pttAdapter = PttAdapter(mutableListOf()) { item ->
            Toast.makeText(requireContext(), "Selected: ${item.title}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewHWMS.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupTabs() {
        val tl = binding.tabLayoutCategories
        tl.addTab(tl.newTab().setText("Waste Gen"))
        tl.addTab(tl.newTab().setText("Transporter"))
        tl.addTab(tl.newTab().setText("TSD"))
        tl.addTab(tl.newTab().setText("PTT"))

        tl.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

    private fun setupListeners() {
        binding.btnAddApplication.setOnClickListener {
            findNavController().navigate(R.id.HwmsStep1Fragment)
        }
    }

    // ---------------- WASTE GENERATION ------------------

    private fun loadWasteGeneration() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.recyclerViewHWMS.adapter = wasteGenAdapter

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    wasteGenAdapter.update(emptyList())
                    binding.tvEmptyState.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val list = snap.documents.map { doc ->

                    val wasteList = (doc["wasteDetails"] as? List<Map<String, Any>>)?.map { w ->
                        WasteItem(
                            wasteName = w["wasteName"]?.toString() ?: "",
                            quantity = w["quantity"]?.toString() ?: "",
                            wasteCode = w["wasteCode"]?.toString() ?: ""
                        )
                    } ?: emptyList()

                    WasteGenDisplay(
                        id = doc.id,
                        companyName = doc.getString("companyName") ?: "Waste Generator",
                        embRegNo = doc.getString("embRegNo") ?: "",
                        status = doc.getString("status") ?: "Pending",
                        timestamp = doc.getString("timestamp") ?: "",
                        wasteList = wasteList
                    )
                }

                wasteGenAdapter.update(list)
                binding.tvEmptyState.visibility = View.GONE
            }
    }

    // ---------------- TRANSPORT BOOKINGS ------------------

    private fun loadTransportBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.recyclerViewHWMS.adapter = transportAdapter
        binding.tvEmptyState.visibility = View.GONE

        db.collection("transport_bookings")
            .whereEqualTo("pcoId", userId)
            .get()
            .addOnSuccessListener { snap ->

                val list = snap.map { doc ->
                    DisplayItem(
                        id = doc.id,
                        title = doc.getString("wasteType") ?: "Transport Booking",
                        subtitle = "Quantity: ${doc.getString("quantity")}",
                        transporter = doc.getString("serviceProviderName") ?: "",
                        tsdFacility = doc.getString("tsdFacilityName") ?: "",
                        permitNo = doc.getString("bookingId") ?: doc.id,
                        paymentStatus = doc.getString("paymentStatus") ?: "Unpaid",
                        status = doc.getString("status") ?: "Pending",
                        rawMap = doc.data
                    )
                }

                transportAdapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    // ---------------- TSD BOOKINGS ------------------

    private fun loadTsdBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.recyclerViewHWMS.adapter = tsdAdapter

        db.collection("tsd_bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snap ->

                val list = snap.map { doc ->
                    DisplayItem(
                        id = doc.id,
                        title = doc.getString("facilityName") ?: "TSD Booking",
                        subtitle = "Quantity: ${doc.get("quantity")}",
                        transporter = doc.getString("transporterName") ?: "",
                        tsdFacility = doc.getString("facilityName") ?: "",
                        permitNo = doc.getString("bookingId") ?: doc.id,
                        paymentStatus = doc.getString("status") ?: "Pending Payment",
                        status = doc.getString("status") ?: "Pending",
                        rawMap = doc.data
                    )
                }

                tsdAdapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    // ---------------- PTT APPLICATIONS ------------------

    private fun loadPttBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.recyclerViewHWMS.adapter = pttAdapter

        db.collection("ptt_applications")
            .whereEqualTo("generatorId", userId)
            .get()
            .addOnSuccessListener { snap ->

                val list = snap.map { doc ->
                    DisplayItem(
                        id = doc.id,
                        title = "PTT Application",
                        subtitle = "Transport: ${doc.getString("transportBookingId")}\n" +
                                "TSD: ${doc.getString("tsdBookingId")}",
                        transporter = doc.getString("transportBookingId") ?: "",
                        tsdFacility = doc.getString("tsdBookingId") ?: "",
                        permitNo = doc.id,
                        paymentStatus = doc.getString("paymentStatus") ?: "Unpaid",
                        status = doc.getString("status") ?: "Pending Review",
                        rawMap = doc.data
                    )
                }

                pttAdapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
