package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = FragmentPttApplicationBinding.inflate(inflater, container, false)

        binding.btnSelectGenerator.setOnClickListener { showGeneratorDialog() }
        binding.btnSelectTransportBooking.setOnClickListener { showTransportDialog() }
        binding.btnSelectTsdBooking.setOnClickListener { showTsdDialog() }

        binding.btnUploadGenCert.setOnClickListener { selectFile(1001) }
        binding.btnUploadTransportPlan.setOnClickListener { selectFile(1002) }

        binding.btnSubmitPTT.setOnClickListener { submitPttApplication() }

        return binding.root
    }

    private fun showGeneratorDialog() {
        db.collection("HazardousWasteGenerator").whereEqualTo("status", "Approved")
            .get().addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(), "No approved generators found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val names = docs.map { it.getString("generatorName") ?: "Unnamed Generator" }
                val ids = docs.map { it.id }

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Generator")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedGeneratorId = ids[index]
                        binding.tvSelectedGenerator.text = names[index]
                    }.show()
            }
    }

    private fun showTransportDialog() {
        db.collection("transport_bookings").whereEqualTo("status", "Confirmed")
            .get().addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(), "No confirmed transport bookings found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val names = docs.map { "${it.getString("transporterName") ?: "Unknown"} → ${it.getString("generatorName") ?: "Unknown"}" }
                val ids = docs.map { it.id }

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Transport Booking")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedTransportBookingId = ids[index]
                        binding.tvSelectedTransportBooking.text = names[index]
                    }.show()
            }
    }

    private fun showTsdDialog() {
        db.collection("tsd_bookings").whereEqualTo("status", "Confirmed")
            .get().addOnSuccessListener { docs ->
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
                    }.show()
            }
    }

    private fun selectFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data
            when (requestCode) {
                1001 -> {
                    generatorCertUri = uri
                    binding.etGenCert.setText(uri?.lastPathSegment)
                }
                1002 -> {
                    transportPlanUri = uri
                    binding.etTransportPlan.setText(uri?.lastPathSegment)
                }
            }
        }
    }

    private fun submitPttApplication() {
        if (selectedGeneratorId == null || selectedTransportBookingId == null || selectedTsdBookingId == null) {
            Toast.makeText(requireContext(), "Please complete all selections", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "generatorId" to selectedGeneratorId!!,
            "transportBookingId" to selectedTransportBookingId!!,
            "tsdBookingId" to selectedTsdBookingId!!,
            "remarks" to (binding.etRemarks.text.toString().ifEmpty { "None" }),
            "status" to "Pending Review",
            "timestamp" to System.currentTimeMillis()
        )

        val uploads = mutableListOf<Pair<String, Uri>>()
        generatorCertUri?.let { uploads.add("generatorCertificate" to it) }
        transportPlanUri?.let { uploads.add("transportPlan" to it) }

        if (uploads.isEmpty()) savePttData(data)
        else uploadFilesAndSave(data, uploads)
    }

    private fun uploadFilesAndSave(data: HashMap<String, Any>, uploads: List<Pair<String, Uri>>) {
        var uploadedCount = 0
        uploads.forEach { (key, uri) ->
            val ref = storageRef.child("ptt_requirements/${System.currentTimeMillis()}_$key")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        data[key] = downloadUrl.toString()
                        uploadedCount++
                        if (uploadedCount == uploads.size) savePttData(data)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "File upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun savePttData(data: HashMap<String, Any>) {
        db.collection("ptt_applications")
            .add(data)
            .addOnSuccessListener {
                binding.tvStatus.text = "PTT Application submitted successfully!"
            }
            .addOnFailureListener {
                binding.tvStatus.text = "Failed to submit: ${it.message}"
            }
    }
}
