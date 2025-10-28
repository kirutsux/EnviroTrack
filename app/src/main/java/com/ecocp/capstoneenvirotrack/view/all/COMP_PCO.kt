package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.PCOAdapter
import com.ecocp.capstoneenvirotrack.view.businesses.dialogs.PCODetailsDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class COMP_PCO : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var etSearch: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var adapter: PCOAdapter
    private lateinit var backButton: ImageView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comp_pco, container, false)

        recyclerView = view.findViewById(R.id.pcoRecyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)
        etSearch = view.findViewById(R.id.etSearch)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        backButton = view.findViewById(R.id.backButton)
        val newApplicationButton: FloatingActionButton = view.findViewById(R.id.NewApplication)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        adapter = PCOAdapter(mutableListOf()) { selectedItem ->
            showDetails(selectedItem)
        }
        recyclerView.adapter = adapter

        setupSearch()
        setupSpinner()

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_PCO_to_pcoDashboard)
        }

        newApplicationButton.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_PCO_to_COMP_PCOAccreditation)
        }

        fetchAccreditations()
        return view
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSpinner() {
        val statuses = listOf("All", "Approved", "Rejected", "Pending", "Submitted")
        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = spinnerAdapter

        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                adapter.filterByStatus(statuses[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchAccreditations() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("accreditations")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val fetchedList = mutableListOf<PCO>()
                for (doc in documents) {
                    val accreditationId = doc.getString("accreditationId") ?: "N/A"
                    val shortId = if (accreditationId.length >= 4) "ID: ${accreditationId.take(4)}" else "ID: $accreditationId"
                    val fullName = doc.getString("fullName") ?: "N/A"
                    val company = doc.getString("companyAffiliation") ?: "N/A"
                    val status = doc.getString("status") ?: "Submitted"
                    val issueDate = doc.getTimestamp("issueDate")
                    val expiryDate = doc.getTimestamp("expiryDate")
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val formattedDate = if (timestamp > 0)
                        dateFormat.format(Date(timestamp))
                    else "N/A"

                    if (status.equals("Approved", true) && expiryDate != null) {
                        checkExpiryAndNotify(accreditationId, expiryDate, currentUser.uid, company)
                    }

                    fetchedList.add(
                        PCO(
                            appId = shortId,
                            appName = company,
                            applicant = fullName,
                            forwardedTo = "EMB",
                            updatedDate = formattedDate,
                            status = status,
                            issueDate = issueDate,
                            expiryDate = expiryDate
                        )
                    )
                }

                if (fetchedList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvNoData.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvNoData.visibility = View.GONE
                    adapter.updateList(fetchedList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_PCO", "Error fetching accreditations", e)
                recyclerView.visibility = View.GONE
                tvNoData.visibility = View.VISIBLE
            }
    }

    // ✅ Check expiry and send reminders (30, 15, 7 days, or on expiry)
    private fun checkExpiryAndNotify(accreditationId: String, expiryDate: Timestamp, uid: String, company: String) {
        val daysLeft = daysUntilExpiry(expiryDate)
        Log.d("PCO_Reminder", "Checking PCO Accreditation ($accreditationId): $daysLeft days left")

        val interval = when {
            daysLeft in 29..31 -> "30_days"
            daysLeft in 14..16 -> "15_days"
            daysLeft in 6..8 -> "7_days"
            abs(daysLeft) < 1 -> "on_expiry"
            else -> null
        } ?: return

        val notifKey = "pco_${accreditationId}_$interval"
        val notifSentRef = firestore.collection("notificationsSent").document(notifKey)

        notifSentRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val message = when (interval) {
                    "30_days" -> "Your PCO Accreditation for $company will expire in 30 days."
                    "15_days" -> "Your PCO Accreditation for $company will expire in 15 days."
                    "7_days" -> "Your PCO Accreditation for $company will expire in 7 days."
                    "on_expiry" -> "Your PCO Accreditation for $company has expired today."
                    else -> return@addOnSuccessListener
                }
                sendReminder(uid, company, interval, message)
                notifSentRef.set(mapOf("sent" to true))
            } else {
                Log.d("PCO_Reminder", "Reminder already sent for $accreditationId [$interval]")
            }
        }
    }

    // ✅ Store notification in Firestore
    private fun sendReminder(uid: String, company: String, interval: String, message: String) {
        val notification = hashMapOf(
            "receiverId" to uid,
            "receiverType" to "pco",
            "senderId" to "system",
            "title" to "PCO Accreditation Reminder",
            "message" to message,
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "applicationId" to company
        )

        firestore.collection("notifications").add(notification)
            .addOnSuccessListener {
                Log.d("PCO_Reminder", "✅ Reminder sent for $company [$interval]")
            }
            .addOnFailureListener { e ->
                Log.e("PCO_Reminder", "❌ Failed to send reminder: ${e.message}")
            }
    }

    // ✅ Compute remaining days until expiry
    private fun daysUntilExpiry(expiryDate: Timestamp?): Long {
        if (expiryDate == null) return Long.MAX_VALUE
        val now = System.currentTimeMillis()
        val expiryMillis = expiryDate.toDate().time
        return (expiryMillis - now) / (1000 * 60 * 60 * 24)
    }

    // Updated to pass feedback & EMB certificate URL to dialog
    private fun showDetails(selectedItem: PCO) {
        firestore.collection("accreditations")
            .whereEqualTo("fullName", selectedItem.applicant)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents.first()
                    val dialog = PCODetailsDialog.newInstance(
                        fullName = doc.getString("fullName") ?: "N/A",
                        position = doc.getString("positionDesignation") ?: "N/A",
                        accreditationNumber = doc.getString("accreditationNumber") ?: "N/A",
                        company = doc.getString("companyAffiliation") ?: "N/A",
                        educationBackground = doc.getString("educationalBackground") ?: "N/A",
                        experienceEnvManagement = doc.getString("experienceInEnvManagement") ?: "N/A",
                        governmentIdUrl = doc.getString("governmentIdUrl"),
                        certificateUrl = doc.getString("certificateUrl"),
                        trainingCertificateUrl = doc.getString("trainingCertificateUrl"),
                        feedback = doc.getString("feedback"),                     // NEW
                        embCertificateUrl = doc.getString("embCertificateUrl")    // NEW
                    )
                    dialog.show(parentFragmentManager, "PCODetailsDialog")
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_PCO", "Error fetching details: ${e.message}", e)
            }
    }
}