package com.ecocp.capstoneenvirotrack.view.businesses.crs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.CrsAdapter
import com.ecocp.capstoneenvirotrack.model.Crs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class COMP_CRS : Fragment() {

    private lateinit var recyclerApproved: RecyclerView
    private lateinit var recyclerPending: RecyclerView
    private lateinit var adapterApproved: CrsAdapter
    private lateinit var adapterPending: CrsAdapter
    private val approvedList = mutableListOf<Crs>()
    private val pendingList = mutableListOf<Crs>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp_crs, container, false)

        // 🔙 Back button
        view.findViewById<ImageView>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }

        // ➕ Add new CRS application
        view.findViewById<FloatingActionButton>(R.id.btnAddCompany).setOnClickListener {
            findNavController().navigate(R.id.action_COMP_CRS_to_COMP_CRSApplication)
        }

        // ♻️ Recycler setup
        recyclerApproved = view.findViewById(R.id.recyclerApproved)
        recyclerPending = view.findViewById(R.id.recyclerPending)

        recyclerApproved.layoutManager = LinearLayoutManager(requireContext())
        recyclerPending.layoutManager = LinearLayoutManager(requireContext())

        adapterApproved = CrsAdapter(
            approvedList,
            onEditClick = {
                Toast.makeText(requireContext(), "Edit ${it.companyName}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { deleteApplication(it, true) }
        )

        adapterPending = CrsAdapter(
            pendingList,
            onEditClick = {
                Toast.makeText(requireContext(), "Edit ${it.companyName}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { deleteApplication(it, false) }
        )

        recyclerApproved.adapter = adapterApproved
        recyclerPending.adapter = adapterPending

        // 🔥 Fetch data from Firestore
        fetchCrsApplications()

        return view
    }

    private fun fetchCrsApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("crs_applications")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                approvedList.clear()
                pendingList.clear()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (doc in result) {
                    // dateSubmitted may be a Timestamp (recommended) or already a String.
                    val dateSubmittedFormatted = when (val dateValue = doc.get("dateSubmitted")) {
                        is Timestamp -> dateFormat.format(dateValue.toDate())
                        is String -> dateValue
                        else -> "Unknown"
                    }

                    val app = Crs(
                        docId = doc.id,
                        companyName = doc.getString("companyName") ?: "Unknown",
                        address = doc.getString("address") ?: "No address",
                        status = doc.getString("status") ?: "Pending",
                        dateSubmitted = dateSubmittedFormatted
                    )

                    if (app.status.equals("Approved", ignoreCase = true)) {
                        approvedList.add(app)
                    } else {
                        pendingList.add(app)
                    }
                }

                adapterApproved.notifyDataSetChanged()
                adapterPending.notifyDataSetChanged()

                if (approvedList.isEmpty() && pendingList.isEmpty()) {
                    Toast.makeText(requireContext(), "No CRS applications found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteApplication(app: Crs, isApproved: Boolean) {
        // delete by document id (docId)
        db.collection("crs_applications").document(app.docId)
            .delete()
            .addOnSuccessListener {
                if (isApproved) {
                    val index = approvedList.indexOf(app)
                    if (index != -1) {
                        approvedList.removeAt(index)
                        adapterApproved.notifyItemRemoved(index)
                    }
                } else {
                    val index = pendingList.indexOf(app)
                    if (index != -1) {
                        pendingList.removeAt(index)
                        adapterPending.notifyItemRemoved(index)
                    }
                }
                Toast.makeText(requireContext(), "Deleted ${app.companyName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { err ->
                Toast.makeText(requireContext(), "Delete failed: ${err.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
