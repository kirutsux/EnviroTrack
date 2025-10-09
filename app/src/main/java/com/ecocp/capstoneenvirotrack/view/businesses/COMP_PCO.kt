package com.ecocp.capstoneenvirotrack.view.businesses

import android.view.View
import android.widget.ImageView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.PCOAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class COMP_PCO : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var adapter: PCOAdapter
    private val list = mutableListOf<PCO>()
    private lateinit var backButton: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comp_pco, container, false)

        recyclerView = view.findViewById(R.id.pcoRecyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)
        backButton = view.findViewById(R.id.backButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PCOAdapter(list)
        recyclerView.adapter = adapter

        // 🔙 Handle back navigation
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        fetchAccreditations()

        return view
    }

    private fun fetchAccreditations() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("accreditations")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                list.clear()

                for (doc in documents) {
                    val accreditationId = doc.getString("accreditationId") ?: "N/A"
                    val shortId = if (accreditationId.length >= 4) {
                        "ID: ${accreditationId.take(4)}"
                    } else {
                        "ID: $accreditationId"
                    }

                    val fullName = doc.getString("fullName") ?: "N/A"
                    val company = doc.getString("companyAffiliation") ?: "N/A"
                    val position = doc.getString("positionDesignation") ?: "N/A"
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    val formattedDate = if (timestamp > 0) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
                    } else {
                        "N/A"
                    }

                    val item = PCO(
                        appId = shortId,
                        appName = company,
                        applicant = fullName,
                        forwardedTo = position,
                        updatedDate = formattedDate,
                        type = "Accreditation",
                        status = "Submitted"
                    )
                    list.add(item)
                }

                adapter.notifyDataSetChanged()
                tvNoData.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                tvNoData.visibility = View.VISIBLE
            }
    }
}
