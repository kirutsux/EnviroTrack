package com.ecocp.capstoneenvirotrack.view.emb.hwms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject

class PttReviewDetailsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var selectedFileUri: Uri? = null
    private var pickFileLauncher: ActivityResultLauncher<String>? = null

    // HWMS Views
    private lateinit var txtHwmsCompanyName: TextView
    private lateinit var txtHwmsCompanyAddress: TextView
    private lateinit var txtHwmsStatus: TextView
    private lateinit var hwmsFilesContainer: LinearLayout

    // Transport Views
    private lateinit var txtTransportBookingId: TextView
    private lateinit var txtTransportStatus: TextView
    private lateinit var txtTransportOrigin: TextView
    private lateinit var txtTransportQuantity: TextView
    private lateinit var txtTransportProvider: TextView
    private lateinit var transportFilesContainer: LinearLayout

    // TSD Views
    private lateinit var txtTsdBookingId: TextView
    private lateinit var txtTsdStatus: TextView
    private lateinit var txtTsdAmount: TextView
    private lateinit var tsdFilesContainer: LinearLayout

    // Feedback & certificate
    private lateinit var btnUploadCertificate: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var inputFeedback: EditText
    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button

    private lateinit var pttId: String
    private var uploadedCertificateUrl: String? = null

    companion object {
        private const val PICK_FILE_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ptt_review_details, container, false)

        // Bind Feedback & certificate views
        btnUploadCertificate = view.findViewById(R.id.btnUploadCertificate)
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        inputFeedback = view.findViewById(R.id.inputFeedback)
        btnApprove = view.findViewById(R.id.btnApprove)
        btnReject = view.findViewById(R.id.btnReject)

        pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedFileUri = it
                tvSelectedFile.text = it.lastPathSegment ?: "File Selected"
            }
        }

        btnUploadCertificate.setOnClickListener { pickFileLauncher?.launch("*/*") }
        btnApprove.setOnClickListener { updateStatus("Approved") }
        btnReject.setOnClickListener { updateStatus("Rejected") }

        // Bind HWMS views
        txtHwmsCompanyName = view.findViewById(R.id.txtHwmsCompanyName)
        txtHwmsCompanyAddress = view.findViewById(R.id.txtHwmsCompanyAddress)
        txtHwmsStatus = view.findViewById(R.id.txtHwmsStatus)
        hwmsFilesContainer = view.findViewById(R.id.hwmsFilesContainer)

        // Bind Transport views
        txtTransportBookingId = view.findViewById(R.id.txtTransportBookingId)
        txtTransportStatus = view.findViewById(R.id.txtTransportStatus)
        txtTransportOrigin = view.findViewById(R.id.txtTransportOrigin)
        txtTransportQuantity = view.findViewById(R.id.txtTransportQuantity)
        txtTransportProvider = view.findViewById(R.id.txtTransportProvider)
        transportFilesContainer = view.findViewById(R.id.transportFilesContainer)

        // Bind TSD views
        txtTsdBookingId = view.findViewById(R.id.txtTsdBookingId)
        txtTsdStatus = view.findViewById(R.id.txtTsdStatus)
        txtTsdAmount = view.findViewById(R.id.txtTsdAmount)
        tsdFilesContainer = view.findViewById(R.id.tsdFilesContainer)

        pttId = arguments?.getString("pttId") ?: ""
        if (pttId.isNotEmpty()) loadPttApplication(pttId)

        return view
    }

    private fun updateStatus(status: String) {
        val feedback = inputFeedback.text.toString().trim()
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (selectedFileUri != null) {
            val fileRef = storage.reference.child("ptt_certificates/$pttId/${selectedFileUri!!.lastPathSegment}")
            fileRef.putFile(selectedFileUri!!)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        saveToFirestore(status, feedback, uri.toString(), embUid)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload certificate", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveToFirestore(status, feedback, uploadedCertificateUrl, embUid)
        }
    }

    private fun saveToFirestore(status: String, feedback: String, certificateUrl: String?, embUid: String) {
        val data = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )
        certificateUrl?.let { data["pttCertificate"] = it }

        db.collection("ptt_applications").document(pttId)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()
                Log.d("PTTReview", "Firestore updated successfully for $pttId with status $status")

                // Navigate back to HwmsEmbDashboardFragment and clear PttReviewDetailsFragment
                if (isAdded) {
                    val navController = findNavController()
                    Log.d("PTTReview", "Navigating to HwmsEmbDashboardFragment and clearing PttReviewDetailsFragment")
                    navController.navigate(
                        R.id.hwmsEmbDashboardFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.PttReviewDetailsFragment) {
                                inclusive = true
                            }
                        }
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PTTReview", "Failed to update Firestore: ${exception.message}")
                Toast.makeText(requireContext(), "Failed to update status: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun loadPttApplication(pttId: String) {
        db.collection("ptt_applications")
            .document(pttId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 🔹 Feedback Handling
                    val feedback = doc.getString("feedback") ?: ""
                    val status = doc.getString("status") ?: "Pending Review"

                    if (feedback.isNotBlank()) {
                        inputFeedback.visibility = View.VISIBLE
                        inputFeedback.setText(feedback)
                        inputFeedback.isEnabled = false
                        inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                    } else {
                        inputFeedback.visibility = if (status == "Pending Review") View.VISIBLE else View.GONE
                        inputFeedback.isEnabled = true
                        inputFeedback.setText("")
                    }

                    // 🔹 Certificate Handling
                    val certificateUrl = doc.getString("pttCertificate")
                    btnUploadCertificate.visibility = if (status == "Pending Review") View.VISIBLE else View.GONE
                    if (status.lowercase() == "approved" && !certificateUrl.isNullOrBlank()) {
                        val fileName = certificateUrl.substringAfterLast('/').substringBefore('?')
                        tvSelectedFile.text = fileName
                        uploadedCertificateUrl = certificateUrl
                    }

                    // 🔹 Approve/Reject Buttons
                    btnApprove.visibility = if (status == "Pending Review") View.VISIBLE else View.GONE
                    btnReject.visibility = if (status == "Pending Review") View.VISIBLE else View.GONE

                    val generatorId = doc.getString("generatorId") ?: ""
                    val transportBookingId = doc.getString("transportBookingId") ?: ""
                    val tsdBookingId = doc.getString("tsdBookingId") ?: ""

                    if (generatorId.isNotEmpty()) loadHwmsData(generatorId)
                    if (transportBookingId.isNotEmpty()) loadTransportData(transportBookingId)
                    if (tsdBookingId.isNotEmpty()) loadTsdData(tsdBookingId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading PTT details.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHwmsData(generatorId: String) {
        db.collection("HazardousWasteGenerator").document(generatorId).get()
            .addOnSuccessListener { hwms ->
                if (hwms.exists()) {
                    txtHwmsCompanyName.text = hwms.getString("companyName") ?: "-"
                    txtHwmsCompanyAddress.text = hwms.getString("companyAddress") ?: "-"
                    txtHwmsStatus.text = hwms.getString("status") ?: "-"

                    hwmsFilesContainer.removeAllViews()
                    hwms.getString("labAnalysis")?.let { addFileButton(hwmsFilesContainer, "Lab Analysis", it) }

                    addTextView(hwmsFilesContainer, "Booking ID: ${hwms.getString("bookingId") ?: "-"}")
                    addTextView(hwmsFilesContainer, "EMB Reg No: ${hwms.getString("embRegNo") ?: "-"}")
                    addTextView(hwmsFilesContainer, "PCO Name: ${hwms.getString("pcoName") ?: "-"}")
                    addTextView(hwmsFilesContainer, "PCO Contact: ${hwms.getString("pcoContact") ?: "-"}")
                    addTextView(hwmsFilesContainer, "TSD Booking ID: ${hwms.getString("tsdBookingId") ?: "-"}")
                    addTextView(hwmsFilesContainer, "Timestamp: ${hwms.getTimestamp("timestamp")?.toDate() ?: "-"}")

                    val wasteDetails = hwms.get("wasteDetails") as? List<Map<String, Any>> ?: emptyList()
                    wasteDetails.forEachIndexed { index, waste ->
                        val wasteName = waste["wasteName"] as? String ?: "Waste $index"
                        val wasteType = waste["wasteType"] as? String ?: "-"
                        val dateGenerated = waste["dateGenerated"] as? String ?: "-"
                        val quantity = waste["quantity"] as? String ?: "-"
                        val storageLocation = waste["storageLocation"] as? String ?: "-"
                        val wasteCode = waste["wasteCode"] as? String ?: "-"

                        addTextView(hwmsFilesContainer, "$wasteName - $wasteType")
                        addTextView(hwmsFilesContainer, "Code: $wasteCode, Quantity: $quantity, Generated: $dateGenerated, Storage: $storageLocation")

                        (waste["wasteInventory"] as? String)?.let { addFileButton(hwmsFilesContainer, "$wasteName Inventory", it) }
                        (waste["wasteStoragePhoto"] as? String)?.let { addFileButton(hwmsFilesContainer, "$wasteName Storage Photo", it) }
                    }
                }
            }
    }

    private fun loadTransportData(transportBookingId: String) {
        db.collection("transport_bookings").document(transportBookingId).get()
            .addOnSuccessListener { booking ->
                if (booking.exists()) {
                    txtTransportBookingId.text = booking.getString("bookingId") ?: "-"
                    txtTransportStatus.text = booking.getString("bookingStatus") ?: "-"
                    txtTransportOrigin.text = booking.getString("origin") ?: "-"
                    txtTransportQuantity.text = booking.getString("quantity") ?: "-"
                    txtTransportProvider.text = booking.getString("serviceProviderName") ?: "-"

                    transportFilesContainer.removeAllViews()
                    (booking.get("collectionProof") as? List<String>)?.forEachIndexed { index, url ->
                        addFileButton(transportFilesContainer, "Collection Proof ${index + 1}", url)
                    }
                    booking.getString("storagePermitUrl")?.let { addFileButton(transportFilesContainer, "Storage Permit", it) }
                    booking.getString("transportPlanUrl")?.let { addFileButton(transportFilesContainer, "Transport Plan", it) }

                    addTextView(transportFilesContainer, "Amount: ₱${booking.getDouble("amount")?.toInt() ?: 0}")
                    addTextView(transportFilesContainer, "Assigned At: ${booking.getTimestamp("assignedAt")?.toDate() ?: "-"}")
                    addTextView(transportFilesContainer, "Booking Date: ${booking.getTimestamp("bookingDate")?.toDate() ?: "-"}")
                    addTextView(transportFilesContainer, "Confirmed At: ${booking.getTimestamp("confirmedAt")?.toDate() ?: "-"}")
                    addTextView(transportFilesContainer, "Packaging: ${booking.getString("packaging") ?: "-"}")
                    addTextView(transportFilesContainer, "Payment Status: ${booking.getString("paymentStatus") ?: "-"}")
                    addTextView(transportFilesContainer, "Provider Contact: ${booking.getString("providerContact") ?: "-"}")
                    addTextView(transportFilesContainer, "Provider Type: ${booking.getString("providerType") ?: "-"}")
                    addTextView(transportFilesContainer, "Service Provider Company: ${booking.getString("serviceProviderCompany") ?: "-"}")
                    addTextView(transportFilesContainer, "Special Instructions: ${booking.getString("specialInstructions") ?: "-"}")
                    addTextView(transportFilesContainer, "Status Updated At: ${booking.getTimestamp("statusUpdatedAt")?.toDate() ?: "-"}")
                    addTextView(transportFilesContainer, "Status Updated By: ${booking.getString("statusUpdatedBy") ?: "-"}")
                    addTextView(transportFilesContainer, "Waste Status: ${booking.getString("wasteStatus") ?: "-"}")
                    addTextView(transportFilesContainer, "Waste Type: ${booking.getString("wasteType") ?: "-"}")
                }
            }
    }

    private fun loadTsdData(tsdBookingId: String) {
        db.collection("tsd_bookings").document(tsdBookingId).get()
            .addOnSuccessListener { tsd ->
                if (tsd.exists()) {
                    txtTsdBookingId.text = tsd.getString("tsdBookingId") ?: "-"
                    txtTsdStatus.text = tsd.getString("bookingStatus") ?: "-"
                    txtTsdAmount.text = "₱${tsd.getDouble("amount")?.toInt() ?: 0}"

                    tsdFilesContainer.removeAllViews()
                    tsd.getString("certificateUrl")?.let { addFileButton(tsdFilesContainer, "Certificate", it) }
                    tsd.getString("previousRecordUrl")?.let { addFileButton(tsdFilesContainer, "Previous Record", it) }
                    (tsd.get("collectionProof") as? List<String>)?.forEachIndexed { index, url ->
                        addFileButton(tsdFilesContainer, "Collection Proof ${index + 1}", url)
                    }

                    addTextView(tsdFilesContainer, "Confirmed At: ${tsd.getTimestamp("confirmedAt")?.toDate() ?: "-"}")
                    addTextView(tsdFilesContainer, "Confirmed By: ${tsd.getString("confirmedBy") ?: "-"}")
                    addTextView(tsdFilesContainer, "Contact Number: ${tsd.getString("contactNumber") ?: "-"}")
                    addTextView(tsdFilesContainer, "Generator ID: ${tsd.getString("generatorId") ?: "-"}")
                    addTextView(tsdFilesContainer, "Location: ${tsd.getString("location") ?: "-"}")
                    addTextView(tsdFilesContainer, "Payment Status: ${tsd.getString("paymentStatus") ?: "-"}")
                    addTextView(tsdFilesContainer, "Preferred Date: ${tsd.getString("preferredDate") ?: "-"}")
                    addTextView(tsdFilesContainer, "Quantity: ${tsd.getLong("quantity")?.toInt() ?: 0}")
                    addTextView(tsdFilesContainer, "Rate: ₱${tsd.getDouble("rate")?.toInt() ?: 0}")
                    addTextView(tsdFilesContainer, "Status Updated At: ${tsd.getTimestamp("statusUpdatedAt")?.toDate() ?: "-"}")
                    addTextView(tsdFilesContainer, "Status Updated By: ${tsd.getString("statusUpdatedBy") ?: "-"}")
                    addTextView(tsdFilesContainer, "Timestamp: ${tsd.getTimestamp("timestamp")?.toDate() ?: "-"}")
                    addTextView(tsdFilesContainer, "Treatment Info: ${tsd.getString("treatmentInfo") ?: "-"}")
                    addTextView(tsdFilesContainer, "TSD ID: ${tsd.getString("tsdId") ?: "-"}")
                    addTextView(tsdFilesContainer, "TSD Name: ${tsd.getString("tsdName") ?: "-"}")
                    addTextView(tsdFilesContainer, "Waste Type: ${tsd.getString("wasteType") ?: "-"}")
                }
            }
    }

    private fun addTextView(container: LinearLayout, text: String) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }
        container.addView(tv)
    }

    private fun addFileButton(container: LinearLayout, label: String, url: String) {
        val button = Button(requireContext()).apply {
            text = "⬇ $label"
            setOnClickListener { downloadFile(url) }
        }
        container.addView(button)
    }

    private fun downloadFile(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
