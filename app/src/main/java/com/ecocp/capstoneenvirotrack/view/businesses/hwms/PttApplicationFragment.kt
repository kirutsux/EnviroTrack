package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentPttApplicationBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PttApplicationFragment : Fragment() {

    private lateinit var binding: FragmentPttApplicationBinding
    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    private var selectedGeneratorId: String? = null
    private var selectedTransportBookingId: String? = null
    private var selectedTsdBookingId: String? = null

    private var generatorCertUri: Uri? = null
    private var transportPlanUri: Uri? = null

    // Modern Activity Result Launcher — replaces deprecated onActivityResult
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri = data?.data ?: return@registerForActivityResult

                when (currentRequestCode) {
                    1001 -> {
                        generatorCertUri = uri
                        binding.etGenCert.setText(uri.lastPathSegment ?: "Selected File")
                    }
                    1002 -> {
                        transportPlanUri = uri
                        binding.etTransportPlan.setText(uri.lastPathSegment ?: "Selected File")
                    }
                }
            }
        }

    private var currentRequestCode = -1

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = FragmentPttApplicationBinding.inflate(inflater, container, false)

        // Button Listeners
        binding.btnSelectGenerator.setOnClickListener { showGeneratorDialog() }
        binding.btnSelectTransportBooking.setOnClickListener { showTransportDialog() }
        binding.btnSelectTsdBooking.setOnClickListener { showTsdDialog() }

        binding.btnUploadGenCert.setOnClickListener { selectFile(1001) }
        binding.btnUploadTransportPlan.setOnClickListener { selectFile(1002) }

        binding.btnSubmitPTT.setOnClickListener { submitPttApplication() }

        return binding.root
    }

    // ======================
    // GENERATOR SELECT
    // ======================
    private fun showGeneratorDialog() {
        db.collection("HazardousWasteGenerator")
            .whereEqualTo("status", "Approved")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(), "No approved generators found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val names = docs.map { it.getString("pcoName") ?: "Unnamed Generator" }
                val ids = docs.map { it.id }

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Generator")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedGeneratorId = ids[index]
                        binding.tvSelectedGenerator.text = names[index]
                    }
                    .show()
            }
    }

    // ======================
    // TRANSPORT BOOKING SELECT
    // ======================
    private fun showTransportDialog() {
        db.collection("transport_bookings")
            .whereEqualTo("bookingStatus", "Confirmed")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(), "No confirmed transport bookings found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val names = docs.map {
                    val tName = it.getString("transporterName") ?: "Unknown Transporter"
                    val gName = it.getString("generatorName") ?: "Unknown Generator"
                    "$tName → $gName"
                }

                val ids = docs.map { it.id }

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Transport Booking")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedTransportBookingId = ids[index]
                        binding.tvSelectedTransportBooking.text = names[index]
                    }
                    .show()
            }
    }

    // ======================
    // TSD BOOKING SELECT
    // ======================
    private fun showTsdDialog() {
        db.collection("tsd_bookings")
            .whereEqualTo("status", "Confirmed")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(), "No confirmed TSD bookings found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val names = docs.map { it.getString("facilityName") ?: "Unnamed Facility" }
                val ids = docs.map { it.id }

                AlertDialog.Builder(requireContext())
                    .setTitle("Select TSD Booking")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedTsdBookingId = ids[index]
                        binding.tvSelectedTsdBooking.text = names[index]
                    }
                    .show()
            }
    }

    // ======================
    // FILE PICKER
    // ======================
    private fun selectFile(requestCode: Int) {
        currentRequestCode = requestCode
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        filePickerLauncher.launch(intent)
    }

    // ======================
    // SUBMIT PTT
    // ======================
    private fun submitPttApplication() {
        if (selectedGeneratorId == null ||
            selectedTransportBookingId == null ||
            selectedTsdBookingId == null
        ) {
            Toast.makeText(requireContext(), "Please complete all selections", Toast.LENGTH_SHORT).show()
            return
        }

        // Fix 1: Use mutableMapOf<String, Any>() explicitly
        val data = mutableMapOf<String, Any>(
            "generatorId" to selectedGeneratorId!!,
            "transportBookingId" to selectedTransportBookingId!!,
            "tsdBookingId" to selectedTsdBookingId!!,
            "remarks" to binding.etRemarks.text.toString().ifEmpty { "None" },
            "paymentMethod" to "A",
            "status" to "Pending Review",
            "timestamp" to System.currentTimeMillis(),
            "submittedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Best practice!
        )

        val uploads = mutableListOf<Pair<String, Uri>>()
        generatorCertUri?.let { uploads.add("generatorCertificate" to it) }
        transportPlanUri?.let { uploads.add("transportPlan" to it) }

        if (uploads.isEmpty()) {
            savePttData(data)
        } else {
            uploadFilesAndSave(data, uploads)
        }
    }

    // ======================
    // FILE UPLOAD → FIRESTORE SAVE
    // ======================
    private fun uploadFilesAndSave(
        data: MutableMap<String, Any>,  // Changed from HashMap<String, Any>
        uploads: List<Pair<String, Uri>>
    ) {
        var uploadedCount = 0

        uploads.forEach { (fieldName, uri) ->
            val fileName = "${System.currentTimeMillis()}_${fieldName}_${uri.lastPathSegment}"
            val ref = storageRef.child("ptt_requirements/$fileName")

            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        data[fieldName] = downloadUrl.toString()  // This now works safely
                        uploadedCount++

                        if (uploadedCount == uploads.size) {
                            savePttData(data)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // ======================
    // SAVE APPLICATION
    // ======================
    private fun savePttData(data: Map<String, Any>) {  // Accept Map<String, Any>
        db.collection("ptt_applications")
            .add(data)
            .addOnSuccessListener {
                binding.tvStatus.apply {
                    text = "PTT Application submitted successfully!"
                    setTextColor(android.graphics.Color.GREEN)
                }
                Toast.makeText(requireContext(), "Submitted successfully!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                binding.tvStatus.apply {
                    text = "Submission failed: ${e.message}"
                    setTextColor(android.graphics.Color.RED)
                }
            }
    }
}
