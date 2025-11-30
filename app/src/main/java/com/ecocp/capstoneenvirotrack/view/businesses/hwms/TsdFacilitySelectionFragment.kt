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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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
    private lateinit var bookingData: HashMap<String, Any>

    // Optional transporter data received from previous step (if any)
    private var transportBookingIdArg: String? = null
    private var transporterIdArg: String? = null
    private var transporterNameArg: String? = null
    private var transporterInfoMap: HashMap<String, Any>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTsdFacilitySelectionBinding.inflate(inflater, container, false)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Processing...")
            setCancelable(false)
        }

        // read optional args passed from Step 2 (best-effort: check common keys)
        arguments?.let { bundle ->
            transportBookingIdArg = bundle.getString("transportBookingId")
            transporterIdArg = bundle.getString("transporterId")
            transporterNameArg = bundle.getString("transporterName")
            // if you passed a map-like JSON string, try to parse it (optional)
            bundle.getString("transporterJson")?.let { jsonStr ->
                try {
                    val obj = JSONObject(jsonStr)
                    val map = HashMap<String, Any>()
                    obj.keys().forEach { k -> map[k] = obj.get(k) }
                    transporterInfoMap = map
                } catch (_: Exception) { /* ignore */ }
            }
        }

        // Initialize Stripe
        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentResult)

        setupRecyclerView()
        setupListeners()
        fetchTSDFacilities()

        return binding.root
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
        binding.btnSubmitBooking.setOnClickListener { validateAndProceed() }
    }

    private fun pickFile(requestCode: Int) {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        // restrict to commonly accepted types - PDF or images recommended
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
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
        val rate = facility.rate

        if (treatmentInfo.isEmpty() || quantityStr.isEmpty() || date.isEmpty()) {
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

        // prepare bookingData skeleton
        bookingData = hashMapOf(
            "userId" to userId,
            "facilityId" to facility.id,
            "facilityName" to facility.companyName,
            "contactNumber" to facility.contactNumber,
            "location" to facility.location,
            "treatmentInfo" to treatmentInfo,
            "quantity" to qty,
            "preferredDate" to date,
            "rate" to rate,
            "totalPayment" to paymentAmount,
            "status" to "Pending Payment",
            "dateCreated" to FieldValue.serverTimestamp()
        )

        // attach transporter data if present
        transportBookingIdArg?.let { bookingData["transportBookingId"] = it }
        transporterIdArg?.let { bookingData["transporterId"] = it }
        transporterNameArg?.let { bookingData["transporterName"] = it }
        transporterInfoMap?.let { bookingData["transporterInfo"] = it }

        // proceed to create payment intent (payment flow A)
        createPaymentIntent(paymentAmount)
    }

    private fun createPaymentIntent(amount: Double) {
        progressDialog.setMessage("Initializing payment...")
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:3000/create-payment-intent")
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
                // continue: upload files (if any) then save booking
                finalizeBookingAfterPayment()
            }
            is PaymentSheetResult.Failed ->
                Toast.makeText(requireContext(), "Payment failed: ${result.error.message}", Toast.LENGTH_SHORT).show()
            PaymentSheetResult.Canceled ->
                Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * After payment completes, upload the optional documents, merge URLs into bookingData,
     * then save booking document to Firestore.
     */
    private fun finalizeBookingAfterPayment() {
        progressDialog.setMessage("Uploading documents...")
        progressDialog.show()

        // create a new booking doc first so we have bookingId for storage path
        val newDocRef = db.collection("tsd_bookings").document()
        val bookingId = newDocRef.id
        bookingData["bookingId"] = bookingId

        // upload files (if any)
        uploadTsdFiles(bookingId, certificateUri, prevRecordUri) { success, fileUrls ->
            progressDialog.dismiss()
            if (!success) {
                Toast.makeText(requireContext(), "File upload failed. Booking not saved.", Toast.LENGTH_LONG).show()
                return@uploadTsdFiles
            }

            // merge file urls
            bookingData.putAll(fileUrls)

            // set booked-by user status/time already in bookingData -> save to Firestore
            newDocRef.set(bookingData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "TSD Booking successful!", Toast.LENGTH_SHORT).show()
                    // navigate to PTT application with bookingId
                    val bundle = Bundle().apply { putString("bookingId", bookingId) }
                    findNavController().navigate(R.id.action_tsdFacilitySelectionFragment_to_pttApplicationFragment, bundle)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    /**
     * Upload certificate and previous record (if present) to:
     *  tsd_bookings/{bookingId}/documents/
     * Returns map with keys like "certificateUrl" and "previousRecordUrl"
     */
    private fun uploadTsdFiles(
        bookingId: String,
        certificate: Uri?,
        previousRecord: Uri?,
        callback: (Boolean, Map<String, Any>) -> Unit
    ) {
        val storageRef = storage.reference
        val uploadedUrls = mutableMapOf<String, Any>()
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
                    }.addOnFailureListener {
                        // log handled below
                    }
                uploadTasks.add(uploadTask)
            } catch (e: Exception) { /* will be handled by Tasks.whenAllComplete */ }
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
                    }.addOnFailureListener {
                        // log handled below
                    }
                uploadTasks.add(uploadTask)
            } catch (e: Exception) { /* will be handled by Tasks.whenAllComplete */ }
        }

        // If no file to upload, callback immediately success
        if (uploadTasks.isEmpty()) {
            callback(true, emptyMap())
            return
        }

        // Wait for all upload tasks to complete
        Tasks.whenAllComplete(uploadTasks)
            .addOnCompleteListener { task ->
                val results = task.result
                val allSuccess = results?.all { t ->
                    t.isSuccessful
                } ?: false

                callback(allSuccess, if (allSuccess) uploadedUrls else emptyMap())
            }
            .addOnFailureListener {
                callback(false, emptyMap())
            }

    }

    companion object {
        private const val REQUEST_CERTIFICATE = 1
        private const val REQUEST_PREV_RECORD = 2
    }
}
