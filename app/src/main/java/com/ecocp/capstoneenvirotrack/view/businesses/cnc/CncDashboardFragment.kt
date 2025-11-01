package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.CncAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncDashboardBinding
import com.ecocp.capstoneenvirotrack.model.CncApplication
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

import java.text.SimpleDateFormat
import java.util.*

class CncDashboardFragment : Fragment() {

    private var _binding: FragmentCncDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val cncList = mutableListOf<CncApplication>()
    private lateinit var cncAdapter: CncAdapter

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCncDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cncAdapter = CncAdapter(
            cncList,
            requireContext(),
            onItemClick = { cnc ->
                val bundle = Bundle().apply { putString("applicationId", cnc.applicationId) }
                findNavController().navigate(R.id.action_cncDashboardFragment_to_cncDetailsFragment, bundle)
            },
            onItemLongClick = onItemLongClick@{ cnc ->
                // ✅ Check if status is pending first
                if (cnc.status != "Pending") {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Only pending applications can be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                // ✅ Proceed to delete if pending
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this submitted CNC application?")
                    .setPositiveButton("Delete") { _, _ ->
                        val appId = cnc.applicationId ?: return@setPositiveButton // Prevent null id

                        db.collection("cnc_applications").document(appId)
                            .delete()
                            .addOnSuccessListener {
                                // Update the local list and refresh adapter
                                val updatedList = cncList.toMutableList()
                                updatedList.remove(cnc)
                                cncList.clear()
                                cncList.addAll(updatedList)
                                cncAdapter.notifyDataSetChanged()

                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Application deleted successfully.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Failed to delete application: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }


        )

        binding.recyclerCncList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cncAdapter
        }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_cncDashboardFragment_to_pcoDashboardFragment)
        }

        binding.btnAddCnc.setOnClickListener {
            findNavController().navigate(R.id.action_cncDashboardFragment_to_cncFormFragment)
        }

        loadCncApplications()
    }

    private fun loadCncApplications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        listenerRegistration = db.collection("cnc_applications")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("CNC", "Error loading CNC applications: ${e.message}")
                    return@addSnapshotListener
                }

                // Use null-safe binding
                _binding?.let { binding ->

                    cncList.clear()

                    snapshots?.documents?.forEach { doc ->
                        try {
                            val data = doc.data ?: return@forEach

                            val submittedTimestamp = when (val value = data["submittedTimestamp"]) {
                                is Timestamp -> value
                                is String -> {
                                    try {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        Timestamp(sdf.parse(value) ?: Date())
                                    } catch (_: Exception) {
                                        Timestamp.now()
                                    }
                                }
                                else -> Timestamp.now()
                            }

                            val timestamp = when (val value = data["timestamp"]) {
                                is Timestamp -> value
                                else -> Timestamp.now()
                            }

                            val cnc = CncApplication(
                                applicationId = doc.id,
                                uid = data["uid"] as? String,
                                companyName = data["companyName"] as? String,
                                businessName = data["businessName"] as? String,
                                projectTitle = data["projectTitle"] as? String,
                                natureOfBusiness = data["natureOfBusiness"] as? String,
                                projectLocation = data["projectLocation"] as? String,
                                email = data["email"] as? String,
                                managingHead = data["managingHead"] as? String,
                                pcoName = data["pcoName"] as? String,
                                pcoAccreditation = data["pcoAccreditation"] as? String,
                                dateEstablished = data["dateEstablished"] as? String,
                                numEmployees = data["numEmployees"] as? String,
                                psicCode = data["psicCode"] as? String,
                                projectType = data["projectType"] as? String,
                                projectScale = data["projectScale"] as? String,
                                projectCost = data["projectCost"] as? String,
                                landArea = data["landArea"] as? String,
                                rawMaterials = data["rawMaterials"] as? String,
                                productionCapacity = data["productionCapacity"] as? String,
                                utilitiesUsed = data["utilitiesUsed"] as? String,
                                wasteGenerated = data["wasteGenerated"] as? String,
                                coordinates = data["coordinates"] as? String,
                                nearbyWaters = data["nearbyWaters"] as? String,
                                residentialProximity = data["residentialProximity"] as? String,
                                envFeatures = data["envFeatures"] as? String,
                                zoning = data["zoning"] as? String,
                                amount = (data["amount"] as? Number)?.toDouble(),
                                currency = data["currency"] as? String,
                                paymentMethod = data["paymentMethod"] as? String,
                                paymentStatus = data["paymentStatus"] as? String,
                                paymentTimestamp = data["paymentTimestamp"] as? Timestamp,
                                submittedTimestamp = submittedTimestamp,
                                status = data["status"] as? String,
                                fileLinks = data["fileLinks"] as? List<String>,
                                timestamp = timestamp
                            )

                            cncList.add(cnc)

                        } catch (ex: Exception) {
                            Log.e("CNC", "Failed to parse document ${doc.id}: ${ex.message}")
                        }
                    }

                    cncAdapter.notifyDataSetChanged()

                    if (cncList.isEmpty()) {
                        binding.txtNoApplications.visibility = View.VISIBLE
                        binding.recyclerCncList.visibility = View.GONE
                    } else {
                        binding.txtNoApplications.visibility = View.GONE
                        binding.recyclerCncList.visibility = View.VISIBLE
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove() // Remove Firestore listener
        _binding = null
    }
}
