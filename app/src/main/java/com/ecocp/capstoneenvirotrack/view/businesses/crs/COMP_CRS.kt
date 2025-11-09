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
            onEditClick = { showEditDialog(it, true) },
            onDeleteClick = { deleteApplication(it, true) }
        )

        adapterPending = CrsAdapter(
            pendingList,
            onEditClick = { showEditDialog(it, false) },
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
                    val dateSubmittedFormatted = when (val dateValue = doc.get("dateSubmitted")) {
                        is Timestamp -> dateFormat.format(dateValue.toDate())
                        is String -> dateValue
                        else -> "Unknown"
                    }

                    val contactDetails = doc.get("contactDetails") as? Map<String, String> ?: emptyMap()
                    val representative = doc.get("representative") as? Map<String, String> ?: emptyMap()
                    val fileUrls = doc.get("fileUrls") as? List<String> ?: emptyList()

                    val app = Crs(
                        docId = doc.id,
                        companyName = doc.getString("companyName") ?: "Unknown",
                        companyType = doc.getString("companyType") ?: "Unknown",
                        tinNumber = doc.getString("tinNumber") ?: "",
                        ceoName = doc.getString("ceoName") ?: "",
                        ceoContact = doc.getString("ceoContact") ?: "",
                        natureOfBusiness = doc.getString("natureOfBusiness") ?: "",
                        psicNo = doc.getString("psicNo") ?: "",
                        industryDescriptor = doc.getString("industryDescriptor") ?: "",
                        address = doc.getString("address") ?: "No address",
                        phone = contactDetails["phone"],
                        email = contactDetails["email"],
                        website = contactDetails["website"],
                        repName = representative["name"],
                        repPosition = representative["position"],
                        repContact = representative["contact"],
                        repEmail = representative["email"],
                        fileUrls = fileUrls,
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

    private fun showEditDialog(app: Crs, isApproved: Boolean) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_edit_crs, null)
        builder.setView(view)

        // Initialize fields
        val etCompanyName = view.findViewById<android.widget.EditText>(R.id.etCompanyName)
        val etCompanyType = view.findViewById<android.widget.AutoCompleteTextView>(R.id.etCompanyType)
        val etTinNumber = view.findViewById<android.widget.EditText>(R.id.etTinNumber)
        val etCeoName = view.findViewById<android.widget.EditText>(R.id.etCeoName)
        val etCeoContact = view.findViewById<android.widget.EditText>(R.id.etCeoContact)
        val etNatureOfBusiness = view.findViewById<android.widget.AutoCompleteTextView>(R.id.etNatureOfBusiness)
        val etPsicNo = view.findViewById<android.widget.EditText>(R.id.etPsicNo)
        val etIndustryDescriptor = view.findViewById<android.widget.EditText>(R.id.etIndustryDescriptor)
        val etAddress = view.findViewById<android.widget.EditText>(R.id.etAddress)
        val etPhone = view.findViewById<android.widget.EditText>(R.id.etPhone)
        val etEmail = view.findViewById<android.widget.EditText>(R.id.etEmail)
        val etWebsite = view.findViewById<android.widget.EditText>(R.id.etWebsite)
        val etRepName = view.findViewById<android.widget.EditText>(R.id.etRepName)
        val etRepPosition = view.findViewById<android.widget.EditText>(R.id.etRepPosition)
        val etRepContact = view.findViewById<android.widget.EditText>(R.id.etRepContact)
        val etRepEmail = view.findViewById<android.widget.EditText>(R.id.etRepEmail)
        val btnSave = view.findViewById<android.widget.Button>(R.id.btnSave)
        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancel)

        // Set up dropdowns
        val companyTypes = listOf("Single Proprietorship", "Partnership", "Corporation", "Cooperative", "Other")
        val natureOfBusinessList = listOf("Manufacturing", "Service", "Trading", "Construction", "Other")
        etCompanyType.setAdapter(android.widget.ArrayAdapter(requireContext(), R.layout.dropdown_item, companyTypes))
        etNatureOfBusiness.setAdapter(android.widget.ArrayAdapter(requireContext(), R.layout.dropdown_item, natureOfBusinessList))

        // Populate fields
        etCompanyName.setText(app.companyName)
        etCompanyType.setText(app.companyType, false)
        etTinNumber.setText(app.tinNumber)
        etCeoName.setText(app.ceoName)
        etCeoContact.setText(app.ceoContact)
        etNatureOfBusiness.setText(app.natureOfBusiness, false)
        etPsicNo.setText(app.psicNo)
        etIndustryDescriptor.setText(app.industryDescriptor)
        etAddress.setText(app.address)
        etPhone.setText(app.phone)
        etEmail.setText(app.email)
        etWebsite.setText(app.website)
        etRepName.setText(app.repName)
        etRepPosition.setText(app.repPosition)
        etRepContact.setText(app.repContact)
        etRepEmail.setText(app.repEmail)

        val dialog = builder.create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val updatedCompanyName = etCompanyName.text.toString().trim()
            val updatedCompanyType = etCompanyType.text.toString().trim()
            val updatedTinNumber = etTinNumber.text.toString().trim()
            val updatedCeoName = etCeoName.text.toString().trim()
            val updatedCeoContact = etCeoContact.text.toString().trim()
            val updatedNatureOfBusiness = etNatureOfBusiness.text.toString().trim()
            val updatedPsicNo = etPsicNo.text.toString().trim()
            val updatedIndustryDescriptor = etIndustryDescriptor.text.toString().trim()
            val updatedAddress = etAddress.text.toString().trim()

            // Required fields validation
            if (listOf(updatedCompanyName, updatedCompanyType, updatedTinNumber, updatedCeoName,
                    updatedCeoContact, updatedNatureOfBusiness, updatedPsicNo,
                    updatedIndustryDescriptor, updatedAddress).any { it.isEmpty() }) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "companyName" to updatedCompanyName,
                "companyType" to updatedCompanyType,
                "tinNumber" to updatedTinNumber,
                "ceoName" to updatedCeoName,
                "ceoContact" to updatedCeoContact,
                "natureOfBusiness" to updatedNatureOfBusiness,
                "psicNo" to updatedPsicNo,
                "industryDescriptor" to updatedIndustryDescriptor,
                "address" to updatedAddress,
                "contactDetails" to mapOf(
                    "phone" to etPhone.text.toString().trim(),
                    "email" to etEmail.text.toString().trim(),
                    "website" to etWebsite.text.toString().trim()
                ),
                "representative" to mapOf(
                    "name" to etRepName.text.toString().trim(),
                    "position" to etRepPosition.text.toString().trim(),
                    "contact" to etRepContact.text.toString().trim(),
                    "email" to etRepEmail.text.toString().trim()
                )
            )

            db.collection("crs_applications").document(app.docId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show()

                    // Update list and refresh
                    val updatedApp = app.copy(
                        companyName = updatedCompanyName,
                        companyType = updatedCompanyType,
                        tinNumber = updatedTinNumber,
                        ceoName = updatedCeoName,
                        ceoContact = updatedCeoContact,
                        natureOfBusiness = updatedNatureOfBusiness,
                        psicNo = updatedPsicNo,
                        industryDescriptor = updatedIndustryDescriptor,
                        address = updatedAddress,
                        phone = etPhone.text.toString().trim(),
                        email = etEmail.text.toString().trim(),
                        website = etWebsite.text.toString().trim(),
                        repName = etRepName.text.toString().trim(),
                        repPosition = etRepPosition.text.toString().trim(),
                        repContact = etRepContact.text.toString().trim(),
                        repEmail = etRepEmail.text.toString().trim()
                    )

                    if (isApproved) {
                        val index = approvedList.indexOf(app)
                        if (index != -1) {
                            approvedList[index] = updatedApp
                            adapterApproved.notifyItemChanged(index)
                        }
                    } else {
                        val index = pendingList.indexOf(app)
                        if (index != -1) {
                            pendingList[index] = updatedApp
                            adapterPending.notifyItemChanged(index)
                        }
                    }

                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun deleteApplication(app: Crs, isApproved: Boolean) {
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