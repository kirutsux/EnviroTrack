package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.BookingAdapter
import com.ecocp.capstoneenvirotrack.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import android.widget.Toast

class SP_Bookings : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_bookings, container, false)

        recyclerView = view.findViewById(R.id.rvBookings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookingAdapter = BookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        fetchCurrentServiceProvider()

        return view
    }

    private fun fetchCurrentServiceProvider() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val docRef = db.collection("service_providers").document(userId)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val company = document.getString("companyName")
                val name = document.getString("fullName")

                if (!company.isNullOrEmpty() && !name.isNullOrEmpty()) {
                    fetchBookings(company, name)
                } else {
                    Toast.makeText(requireContext(), "Service Provider info missing", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Try lookup by email if doc not found
                db.collection("service_providers")
                    .whereEqualTo("email", currentUser.email)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val doc = result.documents[0]
                            val company = doc.getString("companyName")
                            val name = doc.getString("serviceProviderName")

                            if (!company.isNullOrEmpty() && !name.isNullOrEmpty()) {
                                Log.d("SP_Bookings", "✅ Fetched company=$company name=$name")
                                fetchBookings(company, name)
                            } else {
                                Log.d("SP_Bookings", "⚠️ Missing company=$company name=$name")
                                Toast.makeText(requireContext(), "Service Provider info missing", Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            Toast.makeText(requireContext(), "Service Provider record not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to get Service Provider info", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetchBookings(company: String, name: String) {
        db.collection("transport_bookings")
            .whereEqualTo("serviceProviderCompany", company)
            .whereEqualTo("serviceProviderName", name)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                bookingList.clear()
                for (doc in result) {
                    val booking = doc.toObject<Booking>()
                    bookingList.add(booking)
                }
                bookingAdapter.notifyDataSetChanged()

                if (bookingList.isEmpty()) {
                    Toast.makeText(requireContext(), "No bookings found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch bookings.", Toast.LENGTH_SHORT).show()
            }
    }
}
