package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SubmittedApplicationsAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentOpmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.SubmittedApplication
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class OpmsDashboardFragment : Fragment() {

    private var _binding: FragmentOpmsDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val applications = mutableListOf<SubmittedApplication>()
    private lateinit var adapter: SubmittedApplicationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpmsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.COMP_Dashboard)
        }

        // Navigation buttons
        binding.btnApplyDischargePermit.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dischargePermitForm)
        }

        binding.btnApplyPto.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_ptoForm)
        }

        // ✅ Setup RecyclerView with click + long click
        binding.recyclerSubmittedApplications.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubmittedApplicationsAdapter(
            applications,
            onItemClick = { selectedApp ->
                val bundle = Bundle().apply { putString("applicationId", selectedApp.id) }

                when (selectedApp.applicationType) {
                    "Discharge Permit" -> findNavController().navigate(
                        R.id.action_dashboard_to_dischargePermitDetails,
                        bundle
                    )
                    "Permit to Operate" -> findNavController().navigate(
                        R.id.action_dashboard_to_ptoDetails,
                        bundle
                    )
                }
            },
            onItemLongClick = onItemLongClick@{ app ->
                // ✅ Check if status is pending first
                if (app.status != "Pending") {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Only pending applications can be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                // ✅ Ask confirmation before deleting
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this submitted ${app.applicationType} application?")
                    .setPositiveButton("Delete") { _, _ ->
                        val collectionName = when (app.applicationType) {
                            "Discharge Permit" -> "opms_discharge_permits"
                            "Permit to Operate" -> "opms_pto_applications"
                            else -> return@setPositiveButton
                        }

                        db.collection(collectionName).document(app.id)
                            .delete()
                            .addOnSuccessListener {
                                applications.remove(app)
                                adapter.notifyDataSetChanged()

                                Toast.makeText(
                                    requireContext(),
                                    "Application deleted successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete application: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerSubmittedApplications.adapter = adapter

        // Load all applications
        listenToApplications()
    }

    /**
     * Loads both Discharge Permit and PTO applications for the logged-in user.
     * Only applications with a submittedTimestamp are included.
     */
    private fun listenToApplications() {
        val uid = auth.currentUser?.uid ?: return
        applications.clear()

        // ✅ Discharge Permit applications
        db.collection("opms_discharge_permits")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val submittedTs = data["submittedTimestamp"] as? Timestamp
                    if (submittedTs != null) { // Only include submitted applications
                        val app = mapToApplication(doc.id, data, "Discharge Permit")
                        addOrUpdateApplication(app)
                    }
                }
                adapter.notifyDataSetChanged()
            }

        // ✅ Permit to Operate applications
        db.collection("opms_pto_applications")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val submittedTs = data["submittedTimestamp"] as? Timestamp
                    if (submittedTs != null) { // Only include submitted applications
                        val app = mapToApplication(doc.id, data, "Permit to Operate")
                        addOrUpdateApplication(app)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }


    /**
     * Maps Firestore data to SubmittedApplication model
     */
    private fun mapToApplication(id: String, data: MutableMap<String, Any>?, defaultType: String): SubmittedApplication {
        val timestampValue = data?.get("timestamp")
        val formattedTimestamp = when (timestampValue) {
            is Timestamp -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestampValue.toDate())
            is String -> timestampValue
            else -> "N/A"
        }

        // ✅ Prefer Firestore "applicationType" if available
        val appType = (data?.get("applicationType") ?: defaultType) as String

        return SubmittedApplication(
            id = id,
            companyName = (data?.get("establishmentName") ?: data?.get("companyName") ?: "") as String,
            companyAddress = (data?.get("mailingAddress") ?: data?.get("companyAddress") ?: "") as String,
            pcoName = (data?.get("pcoName") ?: "") as String,
            pcoAccreditation = (data?.get("pcoAccreditation") ?: data?.get("pcoAccreditationNumber") ?: "") as String,
            contactNumber = (data?.get("contactNumber") ?: "-") as String,
            email = (data?.get("email") ?: "-") as String,
            bodyOfWater = (data?.get("bodyOfWater") ?: data?.get("receivingBody") ?: "") as String,
            sourceWastewater = (data?.get("sourceWastewater") ?: "") as String,
            volume = (data?.get("volume") ?: data?.get("dischargeVolume") ?: "") as String,
            treatmentMethod = (data?.get("treatmentMethod") ?: data?.get("dischargeMethod") ?: "") as String,
            operationStartDate = (data?.get("operationStartDate") ?: "") as String,
            fileLinks = (data?.get("fileLinks") as? String)?.split("\n") ?: emptyList(),
            status = (data?.get("status") ?: "Pending") as String,
            paymentInfo = buildPaymentInfo(data),
            timestamp = formattedTimestamp,
            applicationType = appType
        )
    }

    /**
     * Builds formatted payment info
     */
    private fun buildPaymentInfo(data: MutableMap<String, Any>?): String {
        val amount = (data?.get("amount") as? Number)?.toDouble() ?: 0.0
        val currency = data?.get("currency") as? String ?: "PHP"
        val paymentTimestamp = (data?.get("paymentTimestamp") as? Timestamp)?.toDate()

        val formattedDate = paymentTimestamp?.let {
            SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
        } ?: "Not Paid"

        return "₱%.2f %s\nPaid on: %s".format(amount, currency, formattedDate)
    }

    /**
     * Adds or updates application in list (avoids duplicates)
     */
    private fun addOrUpdateApplication(app: SubmittedApplication) {
        val index = applications.indexOfFirst { it.id == app.id }
        if (index >= 0) {
            applications[index] = app
        } else {
            applications.add(app)
        }
        applications.sortByDescending { it.timestamp }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
