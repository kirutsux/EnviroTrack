package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ServiceProviderAdapter
import com.ecocp.capstoneenvirotrack.model.ServiceProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
import java.text.SimpleDateFormat
import java.util.*

class TransporterStep2Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val providers = mutableListOf<ServiceProvider>()
    private lateinit var adapter: ServiceProviderAdapter
    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressDialog: ProgressDialog

    // Stripe variables
    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var paymentAmount: Double = 0.0
    private lateinit var selectedProvider: ServiceProvider
    private lateinit var bookingData: HashMap<String, Any>

    // URIs for uploads (set in dialog via pick)
    private var transportPlanUri: Uri? = null
    private var storagePermitUri: Uri? = null

    // Request codes
    private val REQ_PICK_PLAN = 2001
    private val REQ_PICK_PERMIT = 2002

    // Temp: the provisional doc id used while uploading files before payment
    private var provisionalBookingId: String? = null

    // Dialog references (fixed)
    private var currentDialogView: View? = null
    private var bookingDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_transporter_step2, container, false)
        recycler = v.findViewById(R.id.recyclerViewTransporters)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = ServiceProviderAdapter(providers) { provider ->
            showBookingDialog(provider)
        }
        recycler.adapter = adapter

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        // Stripe setup (keep your publishable key)
        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentResult)

        fetchTransporters()
        return v
    }

    private fun fetchTransporters() {
        progressDialog.show()
        db.collection("service_providers")
            .whereEqualTo("role", "Transporter")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snap ->
                progressDialog.dismiss()
                providers.clear()
                providers.addAll(snap.toObjects(ServiceProvider::class.java))
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to load transporters: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Show booking dialog. This dialog now includes:
     * - Upload Transport Plan (PDF/JPG/PNG)
     * - Upload Storage Permit (PDF/JPG/PNG)
     * Files must be uploaded before the payment flow begins.
     */
    private fun showBookingDialog(provider: ServiceProvider) {
        // reset URIs
        transportPlanUri = null
        storagePermitUri = null
        provisionalBookingId = null

        // inflate and keep reference
        val dialogView = layoutInflater.inflate(R.layout.dialog_transporter_booking, null)
        currentDialogView = dialogView

        val tvProviderTitle = dialogView.findViewById<TextView>(R.id.tvProviderTitle)
        val etWasteType = dialogView.findViewById<EditText>(R.id.etWasteType)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPackaging = dialogView.findViewById<EditText>(R.id.etPackaging)
        val etOrigin = dialogView.findViewById<EditText>(R.id.etOrigin)
        val etDestination = dialogView.findViewById<EditText>(R.id.etDestination)
        val etSpecial = dialogView.findViewById<EditText>(R.id.etSpecialInstructions)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnPickDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val tvDateSelected = dialogView.findViewById<TextView>(R.id.tvDateSelected)

        val btnUploadPlan = dialogView.findViewById<Button>(R.id.btnUploadPlan)
        val tvPlanStatus = dialogView.findViewById<TextView>(R.id.tvPlanStatus)
        val btnUploadPermit = dialogView.findViewById<Button>(R.id.btnUploadPermit)
        val tvPermitStatus = dialogView.findViewById<TextView>(R.id.tvPermitStatus)

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmBooking)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelBooking)

        tvProviderTitle.text = "Book with: ${provider.name} — ${provider.companyName}"

        var selectedDateMillis: Long? = null
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(requireContext(), { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 0, 0, 0)
                selectedDateMillis = c.timeInMillis
                val formatted = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(c.time)
                tvDateSelected.text = formatted
            },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dp.datePicker.minDate = System.currentTimeMillis() - 1000
            dp.show()
        }

        // disable confirm until uploads are done (we require both plan & permit uploaded)
        btnConfirm.isEnabled = false

        btnUploadPlan.setOnClickListener {
            pickFile(REQ_PICK_PLAN)
        }
        btnUploadPermit.setOnClickListener {
            pickFile(REQ_PICK_PERMIT)
        }

        // Helper to update status UI and confirm button enable check (local initial update)
        fun updateStatusUI_Local() {
            tvPlanStatus.text = if (transportPlanUri != null) "✅ Uploaded" else "Not uploaded"
            tvPermitStatus.text = if (storagePermitUri != null) "✅ Uploaded" else "Not uploaded"
            btnConfirm.isEnabled = (transportPlanUri != null && storagePermitUri != null)
        }

        // initial statuses
        tvPlanStatus.text = "Not uploaded"
        tvPermitStatus.text = "Not uploaded"

        // Build dialog and show
        bookingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // ensure currentDialogView cleared on dismiss
        bookingDialog?.setOnDismissListener {
            currentDialogView = null
        }

        btnCancel.setOnClickListener {
            // clear temp
            provisionalBookingId = null
            transportPlanUri = null
            storagePermitUri = null
            // dismiss dialog
            bookingDialog?.dismiss()
        }

        btnConfirm.setOnClickListener {
            val wasteType = etWasteType.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val packaging = etPackaging.text.toString().trim()
            val origin = etOrigin.text.toString().trim()
            val destination = etDestination.text.toString().trim()
            val special = etSpecial.text.toString().trim()
            val amountText = etAmount.text.toString().trim()

            if (wasteType.isEmpty() || quantity.isEmpty() || packaging.isEmpty() ||
                origin.isEmpty() || selectedDateMillis == null || amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (transportPlanUri == null || storagePermitUri == null) {
                Toast.makeText(requireContext(), "Please upload Transport Plan and Storage Permit before proceeding.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            paymentAmount = amountText.toDoubleOrNull() ?: 0.0
            if (paymentAmount <= 0) {
                Toast.makeText(requireContext(), "Invalid amount.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
                Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedProvider = provider

            // create bookingData skeleton
            bookingData = hashMapOf(
                "pcoId" to currentUser.uid,
                "serviceProviderName" to provider.name,
                "serviceProviderCompany" to provider.companyName,
                "providerType" to provider.role,
                "providerContact" to provider.contactNumber,
                "wasteType" to wasteType,
                "quantity" to quantity,
                "packaging" to packaging,
                "origin" to origin,
                "destination" to destination,
                "specialInstructions" to special,
                "bookingDate" to Date(selectedDateMillis!!),
                "dateBooked" to FieldValue.serverTimestamp(),
                "bookingStatus" to "pending",
                "status" to "Pending" // will change to "Paid" after payment
            )

            // 1) create provisional bookingId BEFORE uploading files so path is known
            val newDocRef = db.collection("transport_bookings").document()
            provisionalBookingId = newDocRef.id
            bookingData["bookingId"] = provisionalBookingId!!

            // 2) Upload required files to storage under transport_bookings/{bookingId}/
            progressDialog.setMessage("Uploading required documents...")
            progressDialog.show()
            uploadBookingFilesAndProceed(newDocRef, transportPlanUri!!, storagePermitUri!!) { success, fileUrls ->
                progressDialog.dismiss()
                if (!success) {
                    Toast.makeText(requireContext(), "Failed to upload required documents. Try again.", Toast.LENGTH_LONG).show()
                    return@uploadBookingFilesAndProceed
                }

                // merge file urls into bookingData
                bookingData.putAll(fileUrls)

                // 3) Start payment flow (Stripe) — only after uploads finished
                createPaymentIntent(paymentAmount)
                bookingDialog?.dismiss()
            }
        }

        // show dialog
        bookingDialog?.show()

        // initial local update in case URIs were already set (unlikely but safe)
        updateStatusUI_Local()
    }

    /**
     * Upload the two required files and return a map of file field names -> downloadURL
     * callback(success, mapOf("transportPlanUrl"->..., "storagePermitUrl"->...))
     */
    private fun uploadBookingFilesAndProceed(
        newDocRef: com.google.firebase.firestore.DocumentReference,
        planUri: Uri,
        permitUri: Uri,
        callback: (Boolean, Map<String, Any>) -> Unit
    ) {
        val bookingId = newDocRef.id
        val urls = mutableMapOf<String, Any>()
        var uploadedCount = 0
        val total = 2

        fun checkDone() {
            if (uploadedCount == total) {
                callback(true, urls)
            }
        }

        // helper to get extension from mime
        fun extFromMime(uri: Uri): String {
            val type = requireContext().contentResolver.getType(uri)
            return when (type) {
                "application/pdf" -> ".pdf"
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                else -> {
                    val mime = type ?: ""
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: ""
                    if (extension.isNotBlank()) ".$extension" else ""
                }
            }
        }

        // upload plan
        try {
            val planExt = extFromMime(planUri)
            val planRef = storage.reference.child("transport_bookings/$bookingId/transport_plan$planExt")
            planRef.putFile(planUri)
                .addOnSuccessListener {
                    planRef.downloadUrl.addOnSuccessListener { uri ->
                        urls["transportPlanUrl"] = uri.toString()
                        uploadedCount++
                        checkDone()
                    }.addOnFailureListener {
                        callback(false, emptyMap())
                    }
                }
                .addOnFailureListener {
                    callback(false, emptyMap())
                }
        } catch (e: Exception) {
            callback(false, emptyMap())
            return
        }

        // upload permit
        try {
            val permitExt = extFromMime(permitUri)
            val permitRef = storage.reference.child("transport_bookings/$bookingId/storage_permit$permitExt")
            permitRef.putFile(permitUri)
                .addOnSuccessListener {
                    permitRef.downloadUrl.addOnSuccessListener { uri ->
                        urls["storagePermitUrl"] = uri.toString()
                        uploadedCount++
                        checkDone()
                    }.addOnFailureListener {
                        callback(false, emptyMap())
                    }
                }
                .addOnFailureListener {
                    callback(false, emptyMap())
                }
        } catch (e: Exception) {
            callback(false, emptyMap())
            return
        }
    }

    /**
     * Launch file chooser. We'll accept PDF, JPEG, PNG.
     */
    private fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        // allow only pdf and images via chooser MIME filters:
        val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(Intent.createChooser(intent, "Select file"), requestCode)
    }

    /**
     * Receive selected file URIs for plan and permit.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return

        // Validate mime type
        val mime = requireContext().contentResolver.getType(uri) ?: ""
        val allowed = listOf("application/pdf", "image/jpeg", "image/png")
        if (!allowed.contains(mime)) {
            Toast.makeText(requireContext(), "Only PDF / JPG / PNG files are allowed.", Toast.LENGTH_LONG).show()
            return
        }

        when (requestCode) {
            REQ_PICK_PLAN -> {
                transportPlanUri = uri
                Toast.makeText(requireContext(), "Transport Plan selected", Toast.LENGTH_SHORT).show()
            }
            REQ_PICK_PERMIT -> {
                storagePermitUri = uri
                Toast.makeText(requireContext(), "Storage Permit selected", Toast.LENGTH_SHORT).show()
            }
        }

        // update status textviews and confirm button inside the open dialog (if any)
        updateDialogStatuses()
    }

    /**
     * Update the TextViews and Confirm button inside the currently open booking dialog.
     * Uses the stored currentDialogView reference (no searching the activity view tree).
     */
    private fun updateDialogStatuses() {
        val root = currentDialogView ?: return
        val tvPlanStatus = root.findViewById<TextView?>(R.id.tvPlanStatus)
        val tvPermitStatus = root.findViewById<TextView?>(R.id.tvPermitStatus)
        val btnConfirm = root.findViewById<Button?>(R.id.btnConfirmBooking)

        tvPlanStatus?.text = if (transportPlanUri != null) "✅ Selected" else "Not uploaded"
        tvPermitStatus?.text = if (storagePermitUri != null) "✅ Selected" else "Not uploaded"
        btnConfirm?.isEnabled = (transportPlanUri != null && storagePermitUri != null)
    }

    /**
     * Initiates payment intent using your local endpoint and Stripe.
     * After payment completes (onPaymentResult Completed) we save booking to Firestore.
     */
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

                val jsonBody = JSONObject()
                jsonBody.put("amount", amount)
                val out = conn.outputStream
                out.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                out.flush()
                out.close()

                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server responded with code $responseCode")
                }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                clientSecret = json.getString("clientSecret")

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    clientSecret?.let {
                        paymentSheet.presentWithPaymentIntent(
                            it,
                            PaymentSheet.Configuration("EnviroTrack")
                        )
                    } ?: run {
                        Toast.makeText(requireContext(), "Payment initialization error.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Payment initialization failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun onPaymentResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show()
                // Save booking with status Paid (bookingData already contains bookingId and file urls)
                saveBookingToFirestore("Paid")
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment failed: ${paymentResult.error.message}", Toast.LENGTH_SHORT).show()
            }
            PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment canceled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Save booking to Firestore using the provisional bookingId (created before file upload).
     * Booking doc will include uploaded file URLs and bookingId already set.
     */
    private fun saveBookingToFirestore(status: String) {
        bookingData["paymentStatus"] = status

        val bookingId = bookingData["bookingId"] as? String
        if (bookingId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing bookingId", Toast.LENGTH_SHORT).show()
            return
        }

        // Save at document(bookingId) so path matches uploaded files
        db.collection("transport_bookings")
            .document(bookingId)
            .set(bookingData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking saved successfully!", Toast.LENGTH_LONG).show()
                linkBookingToHazardousWasteGenerator(bookingId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Link bookingId to HazardousWasteGenerator (unchanged)
    private fun linkBookingToHazardousWasteGenerator(bookingId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No HazardousWasteGenerator record found for this user.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in querySnapshot.documents) {
                    db.collection("HazardousWasteGenerator")
                        .document(doc.id)
                        .update("bookingId", bookingId)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Linked booking to HWMS application!", Toast.LENGTH_SHORT).show()

                            // Navigate to Step 3 (TSD Facility Selection)
                            try {
                                findNavController().navigate(R.id.action_transporterStep2Fragment_to_tsdFacilitySelectionFragment)
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to link booking: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching HWMS application: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
