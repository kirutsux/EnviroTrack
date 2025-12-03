package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class TSD_Bookings : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // reuse exact same layout
        val view = inflater.inflate(R.layout.fragment_sp_bookings, container, false)

        recyclerView = view.findViewById(R.id.rvBookings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookingAdapter = BookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        // Read service_providers/{uid} first to ensure this account is a TSD facility
        fetchServiceProviderAndQuery()

        return view
    }

    /**
     * Read service_providers/{uid} to detect role. If role indicates TSD facility,
     * query tsd_bookings where facilityId == uid. Otherwise show a clear message.
     */
    private fun fetchServiceProviderAndQuery() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Not signed in.", Toast.LENGTH_SHORT).show()
            Log.d("TSD_Bookings", "No signed-in user.")
            return
        }

        val uid = currentUser.uid
        val spRef = db.collection("service_providers").document(uid)

        spRef.get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val role = doc.getString("role") ?: ""
                    val companyName = doc.getString("companyName") ?: doc.getString("name")
                    Log.d("TSD_Bookings", "service_providers doc found: uid=$uid role='$role' company='$companyName'")

                    // simple role detection
                    if (role.lowercase().contains("tsd")) {
                        // user is TSD facility — fetch bookings where facilityId == uid
                        fetchTsdBookingsForCurrentUser(uid)
                    } else {
                        // not a TSD account — helpful log + toast (avoids silent confusion)
                        Log.d("TSD_Bookings", "Signed-in account is NOT a TSD facility. Role='$role'")
                        Toast.makeText(requireContext(), "This account is not a TSD facility.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No service_providers doc found — still attempt facilityId query (in case data exists only in tsd_bookings)
                    Log.d("TSD_Bookings", "service_providers doc not found for uid=$uid; attempting facilityId query anyway")
                    fetchTsdBookingsForCurrentUser(uid)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TSD_Bookings", "Failed to read service_providers/$uid", e)
                // attempt the query anyway as a fallback
                fetchTsdBookingsForCurrentUser(uid)
            }
    }

    /**
     * Query tsd_bookings where facilityId equals the given uid.
     */
    private fun fetchTsdBookingsForCurrentUser(uid: String) {
        Log.d("TSD_Bookings", "Querying tsd_bookings where facilityId = $uid")

        db.collection("tsd_bookings")
            .whereEqualTo("facilityId", uid)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                bookingList.clear()
                Log.d("TSD_Bookings", "Fetched count: ${result.size()}")

                for (doc in result.documents) {
                    val booking = doc.toObject<Booking>()
                    if (booking != null) bookingList.add(booking)
                    Log.d("TSD_Bookings", "DOC ${doc.id}: ${doc.data}")
                }

                bookingAdapter.notifyDataSetChanged()

                if (bookingList.isEmpty()) {
                    Toast.makeText(requireContext(), "No bookings found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TSD_Bookings", "Failed to fetch tsd_bookings", e)
                Toast.makeText(requireContext(), "Failed to fetch bookings.", Toast.LENGTH_SHORT).show()
            }
    }
}
