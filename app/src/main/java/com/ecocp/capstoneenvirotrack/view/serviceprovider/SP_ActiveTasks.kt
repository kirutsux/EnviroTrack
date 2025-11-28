package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ActiveTasksAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

data class TransporterBooking(
    val bookingId: String = "",
    val serviceProviderName: String = "",
    val serviceProviderCompany: String = "",
    val bookingStatus: String = "",
    val bookingDate: Timestamp? = null // Firestore Timestamp
)

class SP_ActiveTasks : Fragment() {

    private lateinit var recyclerActiveTasks: RecyclerView
    private val bookings = mutableListOf<TransporterBooking>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_active_tasks, container, false)

        recyclerActiveTasks = view.findViewById(R.id.recyclerActiveTasks)
        recyclerActiveTasks.layoutManager = LinearLayoutManager(requireContext())

        val adapter = ActiveTasksAdapter(bookings) { booking ->
            val bundle = Bundle().apply {
                putString("bookingId", booking.bookingId)
            }
            findNavController().navigate(
                R.id.action_spActiveTasksFragment_to_spTaskUpdateDetailsFragment,
                bundle
            )
        }
        recyclerActiveTasks.adapter = adapter

        recyclerActiveTasks.adapter = adapter

        // Fetch only bookings with bookingStatus = "Confirmed"
        db.collection("transport_bookings")
            .whereEqualTo("bookingStatus", "Confirmed")
            .get()
            .addOnSuccessListener { snapshot ->
                bookings.clear()
                for (doc in snapshot.documents) {
                    try {
                        val booking = doc.toObject(TransporterBooking::class.java)
                        if (booking != null) {
                            bookings.add(booking)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Safe deserialization
                    }
                }
                adapter.notifyDataSetChanged()

                if (bookings.isEmpty()) {
                    Toast.makeText(requireContext(), "No confirmed bookings found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch bookings.", Toast.LENGTH_SHORT).show()
            }

        return view
    }
}
