package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentPttApplicationBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PttApplicationFragment : Fragment() {

    private lateinit var binding: FragmentPttApplicationBinding
    private val db = FirebaseFirestore.getInstance()

    // Firestore ID references
    private var selectedGeneratorId: String? = null
    private var selectedTransportBookingId: String? = null
    private var selectedTsdBookingId: String? = null

    // Uploaded file URIs
    private var generatorCertUri: Uri? = null
    private var transportPlanUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPttApplicationBinding.inflate(inflater, container, false)

        loadApprovedGenerators()
        loadConfirmedTransportBookings()
        loadConfirmedTsdBookings()

        binding.btnUploadGenCert.setOnClickListener { selectFile(1001) }
        binding.btnUploadTransportPlan.setOnClickListener { selectFile(1002) }
        binding.btnSubmitPTT.setOnClickListener { submitPttApplication() }

        return binding.root
    }

    // 1️⃣ Approved Generators
    private fun loadApprovedGenerators() {
        db.collection("HazardousWastegenerator")
            .whereEqualTo("status", "Approved")
            .get()
            .addOnSuccessListener { docs ->
                val names = mutableListOf<String>()
                val ids = mutableListOf<String>()

                for (doc in docs) {
                    names.add(doc.getString("generatorName") ?: "Unnamed Generator")
                    ids.add(doc.id)
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGeneratorApps.adapter = adapter

                binding.spinnerGeneratorApps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        selectedGeneratorId = ids.getOrNull(position)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    // 2️⃣ Confirmed Transport Bookings
    private fun loadConfirmedTransportBookings() {
        db.collection("transport_bookings")
            .whereEqualTo("status", "Confirmed")
            .get()
            .addOnSuccessListener { docs ->
                val names = mutableListOf<String>()
                val ids = mutableListOf<String>()

                for (doc in docs) {
                    val transporter = doc.getString("transporterName") ?: "Unknown Transporter"
                    val generator = doc.getString("generatorName") ?: "Unknown Generator"
                    names.add("$transporter → $generator")
                    ids.add(doc.id)
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTransportBookings.adapter = adapter

                binding.spinnerTransportBookings.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        selectedTransportBookingId = ids.getOrNull(position)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    // 3️⃣ Confirmed TSD Bookings
    private fun loadConfirmedTsdBookings() {
        db.collection("tsd_bookings")
            .whereEqualTo("status", "Confirmed")
            .get()
            .addOnSuccessListener { docs ->
                val names = mutableListOf<String>()
                val ids = mutableListOf<String>()

                for (doc in docs) {
                    names.add(doc.getString("facilityName") ?: "Unnamed Facility")
                    ids.add(doc.id)
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTsdBookings.adapter = adapter

                binding.spinnerTsdBookings.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        selectedTsdBookingId = ids.getOrNull(position)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
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
                    binding.tvGenCertStatus.text = uri?.lastPathSegment
                }
                1002 -> {
                    transportPlanUri = uri
                    binding.tvTransportPlanStatus.text = uri?.lastPathSegment
                }
            }
        }
    }

    private fun submitPttApplication() {
        if (selectedGeneratorId == null || selectedTransportBookingId == null || selectedTsdBookingId == null) {
            Toast.makeText(requireContext(), "Please complete all selections", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf<String, Any>(
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
        val storageRef = FirebaseStorage.getInstance().reference
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
                Toast.makeText(requireContext(), "PTT Application submitted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to submit: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
