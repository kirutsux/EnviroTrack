package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Tasks

class SP_TaskUpdateDetails : Fragment() {

    private var bookingId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val uploadedFiles = mutableListOf<Uri>()

    private lateinit var txtStatusPill: TextView
    private lateinit var txtNoAttachments: TextView
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnSaveStatus: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: Button

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                uploadedFiles.addAll(uris)
                displayUploadedFiles()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookingId = arguments?.getString("bookingId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_sp_task_update_details, container, false)

        val txtCompanyName = view.findViewById<TextView>(R.id.txtCompanyName)
        val txtCompanyAddress = view.findViewById<TextView>(R.id.txtCompanyAddress)
        val txtTaskRef = view.findViewById<TextView>(R.id.txtTaskRef)
        txtStatusPill = view.findViewById(R.id.txtStatusPill)
        val txtOriginDestination = view.findViewById<TextView>(R.id.txtOriginDestination)
        val txtWasteType = view.findViewById<TextView>(R.id.txtWasteType)
        val txtQuantity = view.findViewById<TextView>(R.id.txtQuantity)
        val txtPackaging = view.findViewById<TextView>(R.id.txtPackaging)
        val txtSpecialInstructions = view.findViewById<TextView>(R.id.txtSpecialInstructions)

        attachmentContainer = view.findViewById(R.id.attachmentContainer)
        txtNoAttachments = view.findViewById(R.id.txtNoAttachments)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        btnSaveStatus = view.findViewById(R.id.btnSaveStatus)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnUpload = view.findViewById(R.id.btnUploadFile)

        setupSpinner()

        loadBookingDetails(
            txtCompanyName, txtCompanyAddress, txtTaskRef, txtStatusPill,
            txtOriginDestination, txtWasteType, txtQuantity, txtPackaging, txtSpecialInstructions
        )

        btnUpload.setOnClickListener { filePickerLauncher.launch("*/*") }
        btnSaveStatus.setOnClickListener { saveStatus() }
        btnCancel.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        return view
    }

    private fun setupSpinner() {
        val statusOptions = resources.getStringArray(R.array.transporter_status_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun displayUploadedFiles() {
        attachmentContainer.removeAllViews()

        if (uploadedFiles.isEmpty()) {
            txtNoAttachments.visibility = View.VISIBLE
            return
        }

        txtNoAttachments.visibility = View.GONE

        uploadedFiles.forEach { uri ->
            val txt = TextView(requireContext())
            txt.text = uri.lastPathSegment ?: uri.toString()
            attachmentContainer.addView(txt)
        }
    }

    private fun loadBookingDetails(
        txtCompanyName: TextView,
        txtCompanyAddress: TextView,
        txtTaskRef: TextView,
        txtStatusPill: TextView,
        txtOriginDestination: TextView,
        txtWasteType: TextView,
        txtQuantity: TextView,
        txtPackaging: TextView,
        txtSpecialInstructions: TextView
    ) {
        bookingId?.let { id ->
            db.collection("transport_bookings")
                .document(id)
                .get()
                .addOnSuccessListener { doc ->

                    if (doc != null && doc.exists()) {

                        txtCompanyName.text = doc.getString("serviceProviderCompany") ?: ""
                        txtCompanyAddress.text = ""
                        txtTaskRef.text = "Ref: ${doc.getString("bookingId") ?: ""}"

                        val savedStatus = doc.getString("wasteStatus") ?: "Pending"
                        updateStatusPill(savedStatus)
                        applyDeliveredLock(savedStatus)

                        txtOriginDestination.text =
                            "${doc.getString("origin") ?: ""} → ${doc.getString("destination") ?: ""}"

                        txtWasteType.text = doc.getString("wasteType") ?: ""
                        txtQuantity.text = doc.getString("quantity") ?: ""
                        txtPackaging.text = doc.getString("packaging") ?: ""
                        txtSpecialInstructions.text = doc.getString("specialInstructions") ?: ""

                        uploadedFiles.clear()
                        val existingProof = doc.get("collectionProof") as? List<String> ?: emptyList()
                        uploadedFiles.addAll(existingProof.map { Uri.parse(it) })
                        displayUploadedFiles()

                        val statusOptions = resources.getStringArray(R.array.transporter_status_options)
                        val index = statusOptions.indexOf(savedStatus)
                        spinnerStatus.setSelection(if (index >= 0) index else 0)
                    }
                }
        }
    }

    private fun saveStatus() {
        val newStatus = spinnerStatus.selectedItem.toString()

        bookingId?.let { id ->
            val updateMap = mutableMapOf<String, Any>("wasteStatus" to newStatus)

            val newFiles = uploadedFiles.filter { it.scheme == "content" || it.scheme == "file" }
            val oldUrls = uploadedFiles.filter { it.scheme == "https" }.map { it.toString() }

            if (newFiles.isNotEmpty()) {
                val uploadTasks = newFiles.map { uri ->
                    val ref = storage.reference.child("transport_bookings/$id/booking_proofs/${uri.lastPathSegment}")
                    ref.putFile(uri).continueWithTask { task ->
                        if (!task.isSuccessful) task.exception?.let { throw it }
                        ref.downloadUrl
                    }
                }

                Tasks.whenAllSuccess<Uri>(uploadTasks)
                    .addOnSuccessListener { uris ->
                        val merged = oldUrls + uris.map { it.toString() }
                        updateMap["collectionProof"] = merged

                        updateBookingInFirestore(id, updateMap)
                        updateStatusPill(newStatus)
                        applyDeliveredLock(newStatus)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "File upload failed!", Toast.LENGTH_SHORT).show()
                    }

            } else {
                updateMap["collectionProof"] = oldUrls
                updateBookingInFirestore(id, updateMap)
                updateStatusPill(newStatus)
                applyDeliveredLock(newStatus)
            }
        }
    }

    /** 🔥 Shows correct pill text and color */
    private fun updateStatusPill(status: String) {
        txtStatusPill.text = when (status) {
            "Delivered" -> "Completed"
            "In Transit" -> "In Transit"
            else -> "Pending"
        }
    }

    /** 🔥 If Delivered → lock UI (hide buttons + disable spinner) */
    private fun applyDeliveredLock(status: String) {
        if (status == "Delivered") {
            btnSaveStatus.visibility = View.GONE
            btnCancel.visibility = View.GONE
            btnUpload.visibility = View.GONE
            spinnerStatus.isEnabled = false
        } else {
            btnSaveStatus.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            btnUpload.visibility = View.VISIBLE
            spinnerStatus.isEnabled = true
        }
    }

    private fun updateBookingInFirestore(id: String, updateMap: Map<String, Any>) {
        db.collection("transport_bookings")
            .document(id)
            .update(updateMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Update saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save update!", Toast.LENGTH_SHORT).show()
            }
    }
}
