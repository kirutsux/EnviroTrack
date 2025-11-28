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
import com.ecocp.capstoneenvirotrack.adapter.HWMSAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HWMSDashboardFragment : Fragment() {

    private var _binding: FragmentHwmsDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: HWMSAdapter

    // current mode: "transport", "tsd", "ptt"
    private var currentMode = "transport"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHwmsDashboardBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupTabs()
        setupListeners()

        // default load transport bookings
        loadTransportBookings()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = HWMSAdapter(mutableListOf<DisplayItem>()) { selected ->
            Toast.makeText(requireContext(), "Selected: ${selected.title}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewHWMS.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHWMS.adapter = adapter
    }


    private fun setupTabs() {
        val tl = binding.tabLayoutCategories
        tl.addTab(tl.newTab().setText("Transporter"))
        tl.addTab(tl.newTab().setText("TSD"))
        tl.addTab(tl.newTab().setText("PTT"))

        // Tab selected listener — switch mode and query
        tl.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        currentMode = "transport"
                        loadTransportBookings()
                    }
                    1 -> {
                        currentMode = "tsd"
                        loadTsdBookings()
                    }
                    2 -> {
                        currentMode = "ptt"
                        loadPttBookings()
                    }
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

    // ------------- Queries & mapping --------------

    private fun loadTransportBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.tvEmptyState.visibility = View.GONE

        db.collection("transport_bookings")
            .whereEqualTo("pcoId", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { doc ->
                    val wasteType = doc.getString("wasteType") ?: ""
                    val quantity = doc.getString("quantity") ?: ""
                    val transporter = doc.getString("serviceProviderName") ?: doc.getString("providerName") ?: ""
                    val tsd = doc.getString("tsdFacilityName") ?: "" // if you stored it
                    val permitNo = doc.getString("bookingId") ?: doc.id
                    val paymentStatus = doc.getString("paymentStatus") ?: doc.getString("status") ?: "Unpaid"
                    val status = doc.getString("status") ?: doc.getString("bookingStatus") ?: "Pending"

                    DisplayItem(
                        id = doc.id,
                        title = wasteType.ifEmpty { "Transport Booking" },
                        subtitle = "Quantity: $quantity",
                        transporter = transporter,
                        tsdFacility = tsd,
                        permitNo = permitNo,
                        paymentStatus = paymentStatus,
                        status = status,
                        rawMap = doc.data
                    )
                }
                adapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTsdBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.tvEmptyState.visibility = View.GONE

        db.collection("tsd_bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { doc ->
                    val facilityName = doc.getString("facilityName") ?: "TSD Booking"

                    // Safely get quantity as string
                    val rawQuantity = doc.get("quantity")
                    val quantity = when (rawQuantity) {
                        is Number -> rawQuantity.toString()
                        is String -> rawQuantity
                        else -> ""
                    }

                    val transporter = doc.getString("transporterName") ?: ""
                    val tsd = doc.getString("facilityName") ?: ""
                    val permitNo = doc.getString("bookingId") ?: doc.id
                    val paymentStatus = when {
                        doc.getString("status")?.contains("Paid", true) == true -> "Paid"
                        doc.getString("status")?.contains("Pending", true) == true -> "Pending"
                        else -> doc.getString("status") ?: "Pending Payment"
                    }
                    val status = doc.getString("status") ?: "Pending"

                    DisplayItem(
                        id = doc.id,
                        title = facilityName,
                        subtitle = "Quantity: $quantity",
                        transporter = transporter,
                        tsdFacility = tsd,
                        permitNo = permitNo,
                        paymentStatus = paymentStatus,
                        status = status,
                        rawMap = doc.data
                    )
                }

                adapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadPttBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.tvEmptyState.visibility = View.GONE

        db.collection("ptt_applications")
            .whereEqualTo("generatorId", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { doc ->
                    val gid = doc.getString("generatorId") ?: ""
                    val tid = doc.getString("transportBookingId") ?: ""
                    val tsdid = doc.getString("tsdBookingId") ?: ""
                    val remarks = doc.getString("remarks") ?: ""
                    val paymentStatus = doc.getString("paymentStatus") ?: doc.getString("status") ?: "Unpaid"
                    val status = doc.getString("status") ?: "Pending Review"

                    val subtitle = "Transport booking: ${tid.ifEmpty { "-"} }\nTSD booking: ${tsdid.ifEmpty { "-" }}"

                    DisplayItem(
                        id = doc.id,
                        title = "PTT Application",
                        subtitle = subtitle,
                        transporter = tid,
                        tsdFacility = tsdid,
                        permitNo = doc.id,
                        paymentStatus = paymentStatus,
                        status = status,
                        rawMap = doc.data
                    )
                }

                adapter.update(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
