package com.ecocp.capstoneenvirotrack.view.emb.all

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.CompaniesAdapter
import com.ecocp.capstoneenvirotrack.model.CompanySummary
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.*
import kotlin.collections.ArrayList

class CompaniesFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CompaniesAdapter
    private val companies = mutableListOf<CompanySummary>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_companies, container, false)
        val rv = v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerCompanies)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = CompaniesAdapter(companies) { company ->
            val bundle = Bundle().apply {
                putString("companyId", company.docId)
                putString("userId", company.userId)
                putString("companyName", company.companyName)
            }
            findNavController().navigate(R.id.action_companiesFragment_to_companyDetailsFragment, bundle)
        }
        rv.adapter = adapter
        loadCompanies()
        return v
    }

    private fun loadCompanies() {
        companies.clear()
        db.collection("crs_applications")
            .whereEqualTo("status", "Approved") // ✅ Only approved companies
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                val summaryTasks = ArrayList<com.google.android.gms.tasks.Task<CompanySummary>>()

                for (doc in snapshot.documents) {
                    val userId = doc.getString("userId") ?: continue

                    val qCnc = db.collection("cnc_applications").whereEqualTo("uid", userId).get()
                    val qAcc = db.collection("accreditations").whereEqualTo("uid", userId).get()
                    val qPto = db.collection("opms_pto_applications").whereEqualTo("uid", userId).get()
                    val qDp = db.collection("opms_discharge_permits").whereEqualTo("uid", userId).get()

                    val combinedTask = Tasks.whenAllSuccess<QuerySnapshot>(qCnc, qAcc, qPto, qDp)
                        .continueWith { task ->
                            val results = task.result as List<QuerySnapshot>
                            buildCompanySummary(
                                doc.id,
                                doc.getString("companyName") ?: "Unknown",
                                userId,
                                results.getOrNull(0),
                                results.getOrNull(1),
                                results.getOrNull(2),
                                results.getOrNull(3)
                            )
                        }

                    summaryTasks.add(combinedTask)
                }

                Tasks.whenAllSuccess<CompanySummary>(summaryTasks)
                    .addOnSuccessListener { allSummaries ->
                        companies.addAll(allSummaries)
                        companies.sortBy { it.companyName.lowercase(Locale.getDefault()) }
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CompaniesFragment", "Error fetching related docs: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CompaniesFragment", "Error loading companies: ${e.message}", e)
            }
    }


    private fun buildCompanySummary(
        companyId: String,
        companyName: String,
        userId: String,
        cncSnap: QuerySnapshot?,
        accSnap: QuerySnapshot?,
        ptoSnap: QuerySnapshot?,
        dpSnap: QuerySnapshot?
    ): CompanySummary {
        val cncDoc = cncSnap?.documents?.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
        val cncStatus = cncDoc?.getString("status")

        val accDoc = accSnap?.documents?.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
        val pcoStatus = accDoc?.getString("status")
        val pcoExpiry = accDoc?.getTimestamp("expiryDate")?.toDate()

        val ptoDoc = ptoSnap?.documents?.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
        val ptoExpiry = ptoDoc?.getTimestamp("expiryDate")?.toDate()

        val dpDoc = dpSnap?.documents?.maxByOrNull { it.getTimestamp("submittedTimestamp")?.toDate()?.time ?: 0 }
        val dpExpiry = dpDoc?.getTimestamp("expiryDate")?.toDate()

        var score = 0
        if (pcoStatus.equals("Approved", true)) score += 30
        if (cncStatus.equals("Approved", true)) score += 20
        if (ptoExpiry != null && daysUntil(ptoExpiry) > 0) score += 25
        if (dpExpiry != null && daysUntil(dpExpiry) > 0) score += 25

        val overall = when {
            score >= 80 -> "Compliant"
            score >= 40 -> "Partially Compliant"
            else -> "Non-Compliant"
        }

        return CompanySummary(
            docId = companyId,
            companyName = companyName,
            userId = userId,
            latestCncStatus = cncStatus,
            latestPcoStatus = pcoStatus,
            latestPtoExpiry = ptoExpiry,
            latestDpExpiry = dpExpiry,
            pcoExpiry = pcoExpiry,
            complianceScore = score,
            overallStatus = overall
        )
    }

    private fun daysUntil(date: Date?): Long {
        if (date == null) return Long.MIN_VALUE
        val diff = date.time - System.currentTimeMillis()
        return diff / (1000L * 60 * 60 * 24)
    }
}