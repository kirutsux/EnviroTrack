package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.TSDFacilityAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentTsdFacilitySelectionBinding
import com.ecocp.capstoneenvirotrack.model.TSDFacility
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class TsdFacilitySelectionFragment : Fragment() {

    private lateinit var binding: FragmentTsdFacilitySelectionBinding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tsdList = mutableListOf<TSDFacility>()
    private lateinit var adapter: TSDFacilityAdapter
    private var selectedFacility: TSDFacility? = null
    private var certificateUri: Uri? = null
    private var prevRecordUri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    // Stripe
    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var paymentAmount: Double = 0.0
    private lateinit var bookingData: HashMap<String, Any?>

    // Selected waste generator IDs for linking
    private var selectedWasteGenIds = mutableListOf<String>()

    // Optional transporter data from transport booking
    private var transportBookingIdArg: String? = null
    private var transporterNameArg: String? = null
    private var transporterCompanyArg: String? = null

    // Store the user's full name for bookedBy field
    private var currentUserFullName: String = "PCO User"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTsdFacilitySelectionBinding.inflate(inflater, container, false)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Processing...")
            setCancelable(false)
        }

        // Read optional transport booking data
        arguments?.let { bundle ->
            transportBookingIdArg = bundle.getString("transportBookingId")
            transporterNameArg = bundle.getString("transporterName")
            transporterCompanyArg = bundle.getString("transporterCompany")
        }

        // Display transporter info if available
        updateTransporterDisplay()

        // Initialize Stripe
        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentResult)

        // Fetch user's name early
        fetchCurrentUserFullName()

        setupRecyclerView()
        setupListeners()
        fetchTSDFacilities()

        return binding.root
    }

    // Fetch current user's full name from users collection
    private fun fetchCurrentUserFullName() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName")?.trim() ?: ""
                val lastName = document.getString("lastName")?.trim() ?: ""
                currentUserFullName = when {
                    firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
                    firstName.isNotEmpty() -> firstName
                    lastName.isNotEmpty() -> lastName
                    else -> "PCO User"
                }
            }
            .addOnFailureListener {
                currentUserFullName = "PCO User"
            }
    }

    /**
     * Update transporter display with info from transport booking
     */
    private fun updateTransporterDisplay() {
        if (transportBookingIdArg != null) {
            val transporterText = if (!transporterNameArg.isNullOrEmpty() && !transporterCompanyArg.isNullOrEmpty()) {
                "Transporter: $transporterNameArg ($transporterCompanyArg)"
            } else {
                "Transporter: Linked (ID: $transportBookingIdArg)"
            }
            binding.tvSelectedTransporter.text = transporterText
        } else {
            binding.tvSelectedTransporter.text = "Transporter: Not linked"
        }
    }

    private fun setupRecyclerView() {
        adapter = TSDFacilityAdapter(tsdList) { selected ->
            selectedFacility = selected
            binding.tvSelectedFacility.text =
                "Selected: ${selected.companyName}\nLocation: ${selected.location}"
        }
        binding.recyclerViewTSD.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTSD.adapter = adapter
    }

    private fun setupListeners() {
        binding.etPreferredDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    binding.etPreferredDate.setText("$year-${month + 1}-$day")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        binding.btnUploadCertificate.setOnClickListener { pickFile(REQUEST_CERTIFICATE) }
        binding.btnUploadPreviousRecord.setOnClickListener { pickFile(REQUEST_PREV_RECORD) }
        binding.btnSubmitBooking.setOnClickListener {
            // Show waste generator selection first
            showWasteGenSelectionDialog()
        }
    }

    /**
     * Show waste generator selection dialog (same as transport booking)
     */
    private fun showWasteGenSelectionDialog() {
        val currentUser = auth.currentUser ?: return

        progressDialog.setMessage("Loading your waste applications...")
        progressDialog.show()

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("status", "Submitted")
            .get()
            .addOnSuccessListener { querySnapshot ->
                progressDialog.dismiss()

                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(),
                        "No submitted waste applications found. Please submit a waste generator application first.",
                        Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Sort in memory
                val wasteGens = querySnapshot.documents
                    .sortedByDescending { it.getTimestamp("timestamp")?.toDate()?.time ?: 0L }
                    .map { doc ->
                        Triple(
                            doc.id,
                            doc.getString("companyName") ?: "Unknown",
                            doc.getString("embRegNo") ?: "N/A"
                        )
                    }

                // If only one, auto-select
                if (wasteGens.size == 1) {
                    selectedWasteGenIds.clear()
                    selectedWasteGenIds.add(wasteGens[0].first)
                    validateAndProceed()
                    return@addOnSuccessListener
                }

                // Show selection dialog
                val items = wasteGens.map { "${it.second} (EMB: ${it.third})" }.toTypedArray()
                val checkedItems = BooleanArray(items.size) { false }

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Waste Application(s)")
                    .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton("Continue") { _, _ ->
                        selectedWasteGenIds.clear()
                        checkedItems.forEachIndexed { index, isChecked ->
                            if (isChecked) {
                                selectedWasteGenIds.add(wasteGens[index].first)
                            }
                        }

                        if (selectedWasteGenIds.isEmpty()) {
                            Toast.makeText(requireContext(),
                                "Please select at least one waste application.",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            validateAndProceed()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(),
                    "Error loading applications: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun pickFile(requestCode: Int) {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        val mime = arrayOf("application/pdf", "image/jpeg", "image/png")
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, mime)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                when (requestCode) {
                    REQUEST_CERTIFICATE -> {
                        certificateUri = it
                        binding.tvCertificateStatus.text = "Certificate Uploaded"
                    }
                    REQUEST_PREV_RECORD -> {
                        prevRecordUri = it
                        binding.tvPrevRecordStatus.text = "Previous Record Uploaded"
                    }
                }
            }
        }
    }

    private fun fetchTSDFacilities() {
        db.collection("service_providers")
            .whereEqualTo("role", "TSD Facility")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { result ->
                tsdList.clear()
                for (doc in result) {
                    val facility = TSDFacility(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        companyName = doc.getString("companyName") ?: "Unknown Company",
                        contactNumber = doc.getString("contactNumber") ?: "N/A",
                        location = doc.getString("location") ?: "N/A",
                        rate = doc.getDouble("rate") ?: 500.0,
                        wasteType = doc.getString("wasteType") ?: "General Waste",
                        capacity = (doc.getLong("capacity")?.toInt() ?: 0),
                        profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    )
                    tsdList.add(facility)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading facilities: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateAndProceed() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val facility = selectedFacility ?: run {
            Toast.makeText(requireContext(), "Select a TSD Facility", Toast.LENGTH_SHORT).show()
            return
        }

        val treatmentInfo = binding.etTreatmentInfo.text.toString().trim()
        val quantityStr = binding.etQuantity.text.toString().trim()
        val date = binding.etPreferredDate.text.toString().trim()
        val wasteType = binding.etWasteType.text.toString().trim()
        val rate = facility.rate

        if (treatmentInfo.isEmpty() || quantityStr.isEmpty() || date.isEmpty() || wasteType.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = quantityStr.toDoubleOrNull() ?: run {
            Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        paymentAmount = qty * rate
        if (paymentAmount <= 0.0) {
            Toast.makeText(requireContext(), "Invalid payment amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare bookingData with consistent fields (FIXED TYPE)
        bookingData = hashMapOf(
            "generatorId" to userId,
            "generatorEmail" to (auth.currentUser?.email ?: ""),
            "bookedBy" to currentUserFullName,
            "bookedByUid" to auth.currentUser?.uid,
            "tsdId" to facility.id,
            "tsdName" to facility.companyName,
            "contactNumber" to facility.contactNumber,
            "location" to facility.location,
            "wasteType" to wasteType,
            "treatmentInfo" to treatmentInfo,
            "quantity" to qty,
            "preferredDate" to date,
            "rate" to rate,
            "amount" to paymentAmount,
            "bookingStatus" to "pending",
            "paymentStatus" to "Pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Attach transporter information if present
        transportBookingIdArg?.let { bookingData["transportBookingId"] = it }
        transporterNameArg?.let { bookingData["transporterName"] = it }
        transporterCompanyArg?.let { bookingData["transporterCompany"] = it }

        createPaymentIntent(paymentAmount)
    }

    private fun createPaymentIntent(amount: Double) {
        progressDialog.setMessage("Initializing payment...")
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:8080/create-payment-intent")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonBody = JSONObject().apply { put("amount", amount) }
                conn.outputStream.use { it.write(jsonBody.toString().toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server responded with code $responseCode")
                }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                clientSecret = json.optString("clientSecret", null)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    clientSecret?.let {
                        paymentSheet.presentWithPaymentIntent(
                            it,
                            PaymentSheet.Configuration("EnviroTrack")
                        )
                    } ?: run {
                        Toast.makeText(requireContext(), "Payment initialization failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Payment initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show()
                finalizeBookingAfterPayment()
            }
            is PaymentSheetResult.Failed ->
                Toast.makeText(requireContext(), "Payment failed: ${result.error.message}", Toast.LENGTH_SHORT).show()
            PaymentSheetResult.Canceled ->
                Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finalizeBookingAfterPayment() {
        progressDialog.setMessage("Uploading documents...")
        progressDialog.show()

        val newDocRef = db.collection("tsd_bookings").document()
        val bookingId = newDocRef.id
        bookingData["tsdBookingId"] = bookingId
        bookingData["paymentStatus"] = "Paid"

        uploadTsdFiles(bookingId, certificateUri, prevRecordUri) { success, fileUrls ->
            progressDialog.dismiss()
            if (!success) {
                Toast.makeText(requireContext(), "File upload failed. Booking not saved.", Toast.LENGTH_LONG).show()
                return@uploadTsdFiles
            }

            bookingData.putAll(fileUrls)

            newDocRef.set(bookingData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "TSD Booking successful!", Toast.LENGTH_SHORT).show()
                    // Link to waste generators, then go back to dashboard
                    linkBookingToWasteGenerators(bookingId)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    /**
     * Link TSD booking to selected waste generator(s) - same pattern as transport
     */
    private fun linkBookingToWasteGenerators(tsdBookingId: String) {
        if (selectedWasteGenIds.isEmpty()) {
            // If no selection (shouldn't happen), just navigate back
            navigateToDashboard()
            return
        }

        android.util.Log.d("TsdFacility",
            "Linking TSD booking $tsdBookingId to ${selectedWasteGenIds.size} waste gen(s)")

        // Update TSD booking with waste generator IDs
        db.collection("tsd_bookings")
            .document(tsdBookingId)
            .update(
                mapOf(
                    "wasteGeneratorIds" to selectedWasteGenIds,
                    "primaryWasteGeneratorId" to selectedWasteGenIds.firstOrNull()
                )
            )
            .addOnSuccessListener {
                android.util.Log.d("TsdFacility", "✅ Added wasteGeneratorIds to TSD booking")
            }

        // Update waste generators with TSD booking ID
        val updates = selectedWasteGenIds.map { wasteGenId ->
            db.collection("HazardousWasteGenerator")
                .document(wasteGenId)
                .update("tsdBookingId", tsdBookingId)
        }

        Tasks.whenAllComplete(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Linked ${selectedWasteGenIds.size} waste application(s) to TSD booking!",
                    Toast.LENGTH_SHORT).show()
                selectedWasteGenIds.clear()
                navigateToDashboard()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to link waste generators", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
    }

    /**
     * Navigate back to HWMS Dashboard
     */
    private fun navigateToDashboard() {
        try {
            findNavController().popBackStack(R.id.HWMSDashboardFragment, false)
        } catch (e: Exception) {
            try {
                findNavController().navigate(R.id.HWMSDashboardFragment)
            } catch (_: Exception) {}
        }
    }

    private fun uploadTsdFiles(
        bookingId: String,
        certificate: Uri?,
        previousRecord: Uri?,
        callback: (Boolean, Map<String, Any?>) -> Unit
    ) {
        val storageRef = storage.reference
        val uploadedUrls = mutableMapOf<String, Any?>()
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        fun extFromUri(uri: Uri): String {
            val mime = requireContext().contentResolver.getType(uri) ?: ""
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: ""
            return if (ext.isNotBlank()) ".$ext" else ""
        }

        certificate?.let { uri ->
            try {
                val ext = extFromUri(uri).ifEmpty { ".pdf" }
                val ref = storageRef.child("tsd_bookings/$bookingId/documents/certificate$ext")
                val uploadTask = ref.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }.addOnSuccessListener { url ->
                        uploadedUrls["certificateUrl"] = url.toString()
                    }
                uploadTasks.add(uploadTask)
            } catch (e: Exception) { }
        }

        previousRecord?.let { uri ->
            try {
                val ext = extFromUri(uri).ifEmpty { ".pdf" }
                val ref = storageRef.child("tsd_bookings/$bookingId/documents/previous_record$ext")
                val uploadTask = ref.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }.addOnSuccessListener { url ->
                        uploadedUrls["previousRecordUrl"] = url.toString()
                    }
                uploadTasks.add(uploadTask)
            } catch (e: Exception) { }
        }

        if (uploadTasks.isEmpty()) {
            callback(true, emptyMap())
            return
        }

        Tasks.whenAllComplete(uploadTasks)
            .addOnCompleteListener { task ->
                val allSuccess = task.result?.all { it.isSuccessful } ?: false
                callback(allSuccess, if (allSuccess) uploadedUrls else emptyMap())
            }
    }

    companion object {
        private const val REQUEST_CERTIFICATE = 1
        private const val REQUEST_PREV_RECORD = 2
    }
}