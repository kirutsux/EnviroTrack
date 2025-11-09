package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPttApplicationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class PttApplicationFragment : Fragment() {

    private var _binding: FragmentPttApplicationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // lists
    private val generatorDocs = mutableListOf<Pair<String, String>>() // Pair(docId, displayText)
    private val transportBookings = mutableListOf<Pair<String, String>>() // Pair(docId, displayText)
    private val tsdBookings = mutableListOf<Pair<String, String>>() // Pair(docId, displayText)

    // file URIs
    private var genCertUri: Uri? = null
    private var transportPlanUri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    companion object {
        private const val REQ_GEN_CERT = 101
        private const val REQ_TRANSPORT_PLAN = 102
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPttApplicationBinding.inflate(inflater, container, false)

        progressDialog = ProgressDialog(requireContext()).apply {
            setCancelable(false)
            setMessage("Please wait...")
        }

        setupUi()
        fetchAllData()

        return binding.root
    }

    private fun setupUi() {
        binding.btnUploadGenCert.setOnClickListener {
            pickFile(REQ_GEN_CERT)
        }
        binding.btnUploadTransportPlan.setOnClickListener {
            pickFile(REQ_TRANSPORT_PLAN)
        }
        binding.btnSubmitPTT.setOnClickListener {
            submitPttApplication()
        }

        // initialize spinners with placeholder adapters
        binding.spinnerGeneratorApps.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Loading..."))
        binding.spinnerTransportBookings.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Loading..."))
        binding.spinnerTsdBookings.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Loading..."))
    }

    private fun fetchAllData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        fetchGeneratorApps(userId)
        fetchTransportBookings(userId)
        fetchTsdBookings(userId)
    }

    private fun fetchGeneratorApps(userId: String) {
        // fetch HazardousWasteGenerator docs for this PCO user that are APPROVED (or all if you prefer)
        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                generatorDocs.clear()
                for (doc in snap.documents) {
                    // build friendly label (you can customize)
                    val firstDetail = (doc.get("wasteDetails") as? List<Map<String, Any>>)?.firstOrNull()
                    val wt = firstDetail?.get("wasteName") as? String ?: "No waste type"
                    val label = "${wt} — ${doc.id.take(6)}"
                    generatorDocs.add(doc.id to label)
                }
                updateSpinner(binding.spinnerGeneratorApps, generatorDocs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load generator apps: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchTransportBookings(userId: String) {
        // transport_bookings where pcoId == userId and status is Paid/Confirmed (allow others if needed)
        db.collection("transport_bookings")
            .whereEqualTo("pcoId", userId)
            .whereIn("status", listOf("Paid", "Confirmed", "Completed"))
            .orderBy("dateBooked", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                transportBookings.clear()
                for (doc in snap.documents) {
                    val sp = doc.getString("serviceProviderCompany") ?: doc.getString("serviceProviderName") ?: "Transporter"
                    val wt = doc.getString("wasteType") ?: ""
                    val label = "$sp — ${wt.take(16)} — ${doc.id.take(6)}"
                    transportBookings.add(doc.id to label)
                }
                updateSpinner(binding.spinnerTransportBookings, transportBookings)
            }
            .addOnFailureListener { e ->
                // If whereIn fails due to indexes or Firestore restrictions, fallback to a broader query
                // fallback: load pcoId only and filter client-side
                db.collection("transport_bookings")
                    .whereEqualTo("pcoId", userId)
                    .orderBy("dateBooked", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        transportBookings.clear()
                        for (doc in snap2.documents) {
                            val status = doc.getString("status") ?: ""
                            if (status in listOf("Paid", "Confirmed", "Completed")) {
                                val sp = doc.getString("serviceProviderCompany") ?: doc.getString("serviceProviderName") ?: "Transporter"
                                val wt = doc.getString("wasteType") ?: ""
                                val label = "$sp — ${wt.take(16)} — ${doc.id.take(6)}"
                                transportBookings.add(doc.id to label)
                            }
                        }
                        updateSpinner(binding.spinnerTransportBookings, transportBookings)
                    }
                    .addOnFailureListener { e2 ->
                        Toast.makeText(requireContext(), "Failed to load transport bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun fetchTsdBookings(userId: String) {
        // tsd_bookings where userId == pco userId and status is Paid/Confirmed
        db.collection("tsd_bookings")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("Paid", "Confirmed", "Completed"))
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                tsdBookings.clear()
                for (doc in snap.documents) {
                    val facilityName = doc.getString("facilityName") ?: "TSD"
                    val label = "$facilityName — ${doc.id.take(6)}"
                    tsdBookings.add(doc.id to label)
                }
                updateSpinner(binding.spinnerTsdBookings, tsdBookings)
            }
            .addOnFailureListener { e ->
                // fallback similar to transport_bookings
                db.collection("tsd_bookings")
                    .whereEqualTo("userId", userId)
                    .orderBy("dateCreated", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        tsdBookings.clear()
                        for (doc in snap2.documents) {
                            val status = doc.getString("status") ?: ""
                            if (status in listOf("Paid", "Confirmed", "Completed")) {
                                val facilityName = doc.getString("facilityName") ?: "TSD"
                                val label = "$facilityName — ${doc.id.take(6)}"
                                tsdBookings.add(doc.id to label)
                            }
                        }
                        updateSpinner(binding.spinnerTsdBookings, tsdBookings)
                    }
                    .addOnFailureListener { e2 ->
                        Toast.makeText(requireContext(), "Failed to load TSD bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun updateSpinner(spinner: Spinner, list: List<Pair<String, String>>) {
        val labels = list.map { it.second.ifEmpty { it.first } }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select file"), requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_GEN_CERT -> {
                genCertUri = uri
                binding.tvGenCertStatus.text = "Generator certificate selected"
            }
            REQ_TRANSPORT_PLAN -> {
                transportPlanUri = uri
                binding.tvTransportPlanStatus.text = "Transport plan selected"
            }
        }
    }

    private fun submitPttApplication() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (generatorDocs.isEmpty()) {
            Toast.makeText(requireContext(), "No generator application selected", Toast.LENGTH_SHORT).show()
            return
        }

        val genIndex = binding.spinnerGeneratorApps.selectedItemPosition
        val transportIndex = binding.spinnerTransportBookings.selectedItemPosition
        val tsdIndex = binding.spinnerTsdBookings.selectedItemPosition

        if (genIndex < 0 || genIndex >= generatorDocs.size) {
            Toast.makeText(requireContext(), "Select a generator application", Toast.LENGTH_SHORT).show()
            return
        }

        val generatorDocId = generatorDocs[genIndex].first
        val transportBookingId = transportBookings.getOrNull(transportIndex)?.first
        val tsdBookingId = tsdBookings.getOrNull(tsdIndex)?.first

        if (transportBookingId == null || tsdBookingId == null) {
            Toast.makeText(requireContext(), "Select transport booking and TSD booking (confirmed/paid)", Toast.LENGTH_LONG).show()
            return
        }

        progressDialog.setMessage("Uploading files and submitting...")
        progressDialog.show()

        // Upload files (if present), then save doc
        val uploads = mutableMapOf<String, String>()

        // helper to chain uploads
        fun uploadIfPresent(uri: Uri?, path: String, onDone: (String?) -> Unit) {
            if (uri == null) {
                onDone(null)
                return
            }
            val ref = storage.reference.child(path)
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { url ->
                    onDone(url.toString())
                }
                .addOnFailureListener { e ->
                    onDone(null)
                }
        }

        // generate IDs for storage paths
        val timestamp = System.currentTimeMillis()
        val genCertPath = "ptt_docs/${userId}/gen_cert_${timestamp}"
        val transportPlanPath = "ptt_docs/${userId}/transport_plan_${timestamp}"

        // nested upload sequence
        uploadIfPresent(genCertUri, genCertPath) { genCertUrl ->
            genCertUrl?.let { uploads["genCertUrl"] = it }
            uploadIfPresent(transportPlanUri, transportPlanPath) { planUrl ->
                planUrl?.let { uploads["transportPlanUrl"] = it }

                // Build Firestore doc
                val pttDoc = hashMapOf<String, Any>(
                    "pcoId" to userId,
                    "generatorAppId" to generatorDocId,
                    "transportBookingId" to transportBookingId,
                    "tsdBookingId" to tsdBookingId,
                    "remarks" to (binding.etRemarks.text.toString().takeIf { it.isNotBlank() } ?: ""),
                    "status" to "Submitted",
                    "dateSubmitted" to FieldValue.serverTimestamp()
                )

                // attach uploaded URLs
                pttDoc.putAll(uploads)

                // save
                db.collection("ptt_applications")
                    .add(pttDoc)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "PTT application submitted.", Toast.LENGTH_LONG).show()
                        binding.tvStatus.text = "Application submitted (status: Submitted). Update in Firestore for EMB review."
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
