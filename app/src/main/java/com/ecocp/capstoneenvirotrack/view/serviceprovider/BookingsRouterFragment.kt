package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookingsRouterFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // No UI needed — inflate a very small built-in layout to avoid creating xml
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false)
    }

    override fun onResume() {
        super.onResume()
        routeToProperBookings()
    }

    private fun routeToProperBookings() {
        val user = auth.currentUser
        if (user == null) {
            Log.d("BookingsRouter", "No user signed in — default to SP_Bookings")
            navigateToSP()
            return
        }

        val uid = user.uid
        db.collection("service_providers").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                Log.d("BookingsRouter", "service_providers/$uid role='$role'")

                if (role.lowercase().contains("tsd")) {
                    navigateToTSD()
                } else {
                    navigateToSP()
                }
            }
            .addOnFailureListener { e ->
                Log.w("BookingsRouter", "Failed to read service_providers/$uid — defaulting to SP", e)
                navigateToSP()
            }
    }

    private fun navigateToTSD() {
        try { findNavController().navigate(com.ecocp.capstoneenvirotrack.R.id.TSD_Bookings) }
        catch (e: Exception) { Log.e("BookingsRouter", "nav to TSD failed", e) }
    }

    private fun navigateToSP() {
        try { findNavController().navigate(com.ecocp.capstoneenvirotrack.R.id.SP_Bookings) }
        catch (e: Exception) { Log.e("BookingsRouter", "nav to SP failed", e) }
    }
}
