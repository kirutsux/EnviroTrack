package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.firestore.Query
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

        // Buttons navigation
        binding.btnApplyDischargePermit.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dischargePermitForm)
        }

        binding.btnApplyPto.setOnClickListener {
            // TODO: Add PTO form
        }

        binding.btnApplySwm.setOnClickListener {
            // TODO: Add SWM form
        }

        // Setup RecyclerView
        binding.recyclerSubmittedApplications.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubmittedApplicationsAdapter(applications) { selectedApp ->
            val bundle = Bundle().apply {
                putString("applicationId", selectedApp.id)
                putString("companyName", selectedApp.companyName)
                putString("companyAddress", selectedApp.companyAddress)
                putString("pcoName", selectedApp.pcoName)
                putString("pcoAccreditation", selectedApp.pcoAccreditation)
                putString("contactNumber", selectedApp.contactNumber)
                putString("email", selectedApp.email)
                putString("bodyOfWater", selectedApp.bodyOfWater)
                putString("sourceWastewater", selectedApp.sourceWastewater)
                putString("volume", selectedApp.volume)
                putString("treatmentMethod", selectedApp.treatmentMethod)
                putString("operationStartDate", selectedApp.operationStartDate)
                putString("status", selectedApp.status)
                putString("paymentInfo", selectedApp.paymentInfo)
                putString("timestamp", selectedApp.timestamp)
                putString("fileLinks", selectedApp.fileLinks.joinToString("\n"))
            }

            findNavController().navigate(
                R.id.action_dashboard_to_dischargePermitDetails,
                bundle
            )
        }

        binding.recyclerSubmittedApplications.adapter = adapter

        // Load Firestore data (real-time listener)
        listenToSubmittedApplications()
    }

    /**
     * Loads the list of submitted applications in real-time.
     */
    private fun listenToSubmittedApplications() {
        val uid = auth.currentUser?.uid ?: return

        // ✅ Use orderBy("timestamp") only (Firestore requires index if combining where+orderBy)
        db.collection("opms_discharge_permits")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "Submitted")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    applications.clear()
                    adapter.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                applications.clear()
                for (doc in snapshot.documents) {
                    val timestampValue = doc.get("timestamp")
                    val formattedTimestamp = when (timestampValue) {
                        is Timestamp -> {
                            val date = timestampValue.toDate()
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        }
                        is String -> timestampValue
                        else -> "N/A"
                    }

                    val app = SubmittedApplication(
                        id = doc.id,
                        companyName = doc.getString("companyName") ?: "",
                        companyAddress = doc.getString("companyAddress") ?: "",
                        pcoName = doc.getString("pcoName") ?: "",
                        pcoAccreditation = doc.getString("pcoAccreditationNumber") ?: "",
                        contactNumber = doc.getString("contactNumber") ?: "",
                        email = doc.getString("email") ?: "",
                        bodyOfWater = doc.getString("receivingBody") ?: "",
                        sourceWastewater = doc.getString("sourceWastewater") ?: "",
                        volume = doc.getString("dischargeVolume") ?: "",
                        treatmentMethod = doc.getString("dischargeMethod") ?: "",
                        operationStartDate = doc.getString("operationStartDate") ?: "",
                        fileLinks = doc.getString("uploadedFiles")?.split(",") ?: emptyList(),
                        status = doc.getString("status") ?: "Submitted",
                        paymentInfo = doc.getString("paymentInfo") ?: "",
                        timestamp = formattedTimestamp
                    )
                    applications.add(app)
                }

                // Sort manually by timestamp (descending)
                applications.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
