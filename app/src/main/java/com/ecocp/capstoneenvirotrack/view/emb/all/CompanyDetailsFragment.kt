package com.ecocp.capstoneenvirotrack.view.emb.all

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CompanyDetailsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private lateinit var tvCompanyName: TextView
    private lateinit var tvCncStatus: TextView
    private lateinit var tvPcoStatus: TextView
    private lateinit var tvPtoExpiry: TextView
    private lateinit var tvDpExpiry: TextView
    private lateinit var tvComplianceStatus: TextView

    private var companyId: String? = null
    private var userId: String? = null
    private var companyName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId")
            userId = it.getString("userId")
            companyName = it.getString("companyName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_company_details, container, false)

        tvCompanyName = v.findViewById(R.id.tvCompanyName)
        tvCncStatus = v.findViewById(R.id.tvCncStatus)
        tvPcoStatus = v.findViewById(R.id.tvPcoStatus)
        tvPtoExpiry = v.findViewById(R.id.tvPtoExpiry)
        tvDpExpiry = v.findViewById(R.id.tvDpExpiry)
        tvComplianceStatus = v.findViewById(R.id.tvComplianceStatus)

        tvCompanyName.text = companyName ?: "Unknown Company"

        loadCompanyDetails()

        return v
    }

    private fun loadCompanyDetails() {
        if (userId == null) return

        // Load CNC
        db.collection("cnc_applications").whereEqualTo("uid", userId).get()
            .addOnSuccessListener { snap ->
                val cncDoc = snap.documents.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
                val cncStatus = cncDoc?.getString("status") ?: "No Record"
                tvCncStatus.text = "CNC Status: $cncStatus"
            }

        // Load PCO Accreditation
        db.collection("accreditations").whereEqualTo("uid", userId).get()
            .addOnSuccessListener { snap ->
                val accDoc = snap.documents.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
                val accStatus = accDoc?.getString("status") ?: "No Record"
                val expiry = accDoc?.getTimestamp("expiryDate")?.toDate()
                tvPcoStatus.text = "PCO Accreditation: $accStatus" + if (expiry != null) " (expires ${dateFormat.format(expiry)})" else ""
            }

        // Load PTO
        db.collection("opms_pto_applications").whereEqualTo("uid", userId).get()
            .addOnSuccessListener { snap ->
                val ptoDoc = snap.documents.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
                val expiry = ptoDoc?.getTimestamp("expiryDate")?.toDate()
                tvPtoExpiry.text = if (expiry != null) "PTO Expiry: ${dateFormat.format(expiry)}" else "PTO Expiry: No Record"
            }

        // Load Discharge Permit
        db.collection("opms_discharge_permits").whereEqualTo("uid", userId).get()
            .addOnSuccessListener { snap ->
                val dpDoc = snap.documents.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
                val expiry = dpDoc?.getTimestamp("expiryDate")?.toDate()
                tvDpExpiry.text = if (expiry != null) "Discharge Permit Expiry: ${dateFormat.format(expiry)}" else "Discharge Permit Expiry: No Record"
            }

        // Compliance logic (basic)
        db.collection("accreditations").whereEqualTo("uid", userId).get()
            .addOnSuccessListener { accSnap ->
                val accDoc = accSnap.documents.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
                val accStatus = accDoc?.getString("status") ?: "Pending"
                val expiry = accDoc?.getTimestamp("expiryDate")?.toDate()

                val compliant = accStatus.equals("Approved", true) && expiry != null && expiry.after(Date())
                tvComplianceStatus.text = if (compliant) {
                    "Overall Compliance: ✅ Compliant"
                } else {
                    "Overall Compliance: ❌ Non-Compliant"
                }
            }
            .addOnFailureListener { e ->
                Log.e("CompanyDetails", "Error loading compliance info: ${e.message}")
            }
    }
}