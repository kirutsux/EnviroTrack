package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.api.PaymentRequest
import com.ecocp.capstoneenvirotrack.api.PaymentResponse
import com.ecocp.capstoneenvirotrack.api.PcoSendNotificationRequest
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.databinding.FragmentPttApplicationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection
import java.net.URL

class PttApplicationFragment : Fragment() {

    private var _binding: FragmentPttApplicationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var progressDialog: ProgressDialog

    private var selectedGeneratorId: String? = null
    private var selectedTransportBookingId: String? = null
    private var selectedTsdBookingId: String? = null

    private var generatorCertUri: Uri? = null
    private var transportPlanUri: Uri? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null

    private val PTT_FEE = 50.0

    private lateinit var pendingPttData: Map<String, Any>

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val fileName = getFileName(it) ?: "document.pdf"
            if (generatorCertUri == null) {
                generatorCertUri = it
                binding.etGenCert.setText(fileName)
            } else {
                transportPlanUri = it
                binding.etTransportPlan.setText(fileName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPttApplicationBinding.inflate(inflater, container, false)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentResult)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUploadGenCert.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnUploadTransportPlan.setOnClickListener { pickPdf.launch("application/pdf") }

        binding.btnSelectGenerator.setOnClickListener { loadGenerators() }
        binding.btnSelectTransportBooking.setOnClickListener { loadTransportBookings() }
        binding.btnSelectTsdBooking.setOnClickListener { loadTsdBookings() }

        binding.btnSubmitPTT.setOnClickListener { initiatePttWithPayment() }

        updateSubmitButton()
    }

    // SELECT GENERATOR
    private fun loadGenerators() = scope.launch {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
        binding.progressBar.visibility = View.VISIBLE

        try {
            // 1️⃣ Fetch confirmed TSD bookings for this user
            val confirmedBookings = db.collection("tsd_bookings")
                .whereEqualTo("bookingStatus", "Confirmed")
                .whereEqualTo("generatorId", currentUser.uid)
                .get()
                .await()

            if (confirmedBookings.isEmpty) {
                Toast.makeText(requireContext(), "No confirmed TSD bookings found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val confirmedBookingIds = confirmedBookings.documents.map { it.id }

            // Firestore allows a maximum of 10 elements in 'whereIn', so split if needed
            val batches = confirmedBookingIds.chunked(10)
            val generatorDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

            for (batch in batches) {
                val docs = db.collection("HazardousWasteGenerator")
                    .whereEqualTo("status", "Submitted")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereIn("tsdBookingId", batch)
                    .get()
                    .await()
                generatorDocs.addAll(docs.documents)
            }

            if (generatorDocs.isEmpty()) {
                Toast.makeText(requireContext(), "No submitted generators linked to confirmed TSD bookings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = generatorDocs.map { it.getString("pcoName") ?: "Unnamed Generator" }
            val ids = generatorDocs.map { it.id }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Generator")
                .setItems(names.toTypedArray()) { _, i ->
                    selectedGeneratorId = ids[i]
                    binding.tvSelectedGenerator.text = names[i]
                    updateCardSelected(binding.cardGenerator, true)
                    updateSubmitButton()
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading generators: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    // SELECT TRANSPORT BOOKING
    private fun loadTransportBookings() = scope.launch {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
        binding.progressBar.visibility = View.VISIBLE

        try {
            // 1️⃣ Fetch confirmed transport bookings for this user
            val confirmedBookings = db.collection("transport_bookings")
                .whereEqualTo("bookingStatus", "Confirmed")
                .get()
                .await()

            if (confirmedBookings.isEmpty) {
                Toast.makeText(requireContext(), "No confirmed transport bookings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Filter bookings where current user is linked via primaryWasteGeneratorId or wasteGeneratorIds
            val userBookingDocs = confirmedBookings.documents.filter { doc ->
                val primaryGenId = doc.getString("primaryWasteGeneratorId")
                val otherGenIds = doc.get("wasteGeneratorIds") as? List<String> ?: emptyList()
                val allGenIds = listOfNotNull(primaryGenId) + otherGenIds

                // Check if any of the generator IDs belong to current user
                allGenIds.any { genId ->
                    val genDoc = runCatching {
                        db.collection("HazardousWasteGenerator").document(genId).get().await()
                    }.getOrNull()
                    genDoc?.getString("userId") == currentUser.uid
                }
            }

            if (userBookingDocs.isEmpty()) {
                Toast.makeText(requireContext(), "No transport bookings linked to your applications", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val displayNames = mutableListOf<String>()
            val ids = mutableListOf<String>()

            for (doc in userBookingDocs) {
                val transporter = doc.getString("serviceProviderName") ?: "Unknown Transporter"

                val primaryGenId = doc.getString("primaryWasteGeneratorId")
                    ?: (doc.get("wasteGeneratorIds") as? List<String>)?.firstOrNull()

                val companyName = if (primaryGenId != null) {
                    val genDoc = db.collection("HazardousWasteGenerator").document(primaryGenId).get().await()
                    genDoc.getString("companyName") ?: "Unknown Generator"
                } else "Unknown Generator"

                ids.add(doc.id)
                displayNames.add("$transporter → $companyName")
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Transport Booking")
                .setItems(displayNames.toTypedArray()) { _, i ->
                    selectedTransportBookingId = ids[i]
                    binding.tvSelectedTransportBooking.text = displayNames[i]
                    updateCardSelected(binding.cardTransportBooking, true)
                    updateSubmitButton()
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load transport bookings: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    // SELECT TSD BOOKING
    private fun loadTsdBookings() = scope.launch {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
        binding.progressBar.visibility = View.VISIBLE

        try {
            // 1️⃣ Fetch all confirmed TSD bookings for the current user
            val confirmedBookings = db.collection("tsd_bookings")
                .whereEqualTo("bookingStatus", "Confirmed")
                .get()
                .await()

            if (confirmedBookings.isEmpty) {
                Toast.makeText(requireContext(), "No confirmed TSD bookings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2️⃣ Filter bookings where current user is linked via primaryWasteGeneratorId or wasteGeneratorIds
            val userBookingDocs = confirmedBookings.documents.filter { doc ->
                val transportBookingId = doc.getString("transportBookingId")
                val primaryGenId = doc.getString("primaryWasteGeneratorId")
                val otherGenIds = doc.get("wasteGeneratorIds") as? List<String> ?: emptyList()
                val allGenIds = listOfNotNull(primaryGenId) + otherGenIds

                // Check if any linked generator belongs to current user
                allGenIds.any { genId ->
                    val genDoc = runCatching {
                        db.collection("HazardousWasteGenerator").document(genId).get().await()
                    }.getOrNull()
                    genDoc?.getString("userId") == currentUser.uid
                }
            }

            if (userBookingDocs.isEmpty()) {
                Toast.makeText(requireContext(), "No TSD bookings linked to your applications", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 3️⃣ Prepare display names (bookedBy + TSD name)
            val names = userBookingDocs.map { doc ->
                val tsdName = doc.getString("tsdName") ?: "Unnamed TSD Facility"
                val bookedBy = doc.getString("bookedBy") ?: "Unknown"
                "$tsdName (Booked by: $bookedBy)"
            }
            val ids = userBookingDocs.map { it.id }

            // 4️⃣ Show dialog
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select TSD Booking")
                .setItems(names.toTypedArray()) { _, i ->
                    selectedTsdBookingId = ids[i]
                    binding.tvSelectedTsdBooking.text = names[i]
                    updateCardSelected(binding.cardTsdBooking, true)
                    updateSubmitButton()
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading TSD bookings: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }


    private fun initiatePttWithPayment() = scope.launch {
        if (!isFormValid()) return@launch

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitPTT.isEnabled = false

        pendingPttData = mapOf(
            "generatorId" to selectedGeneratorId!!,
            "transportBookingId" to selectedTransportBookingId!!,
            "tsdBookingId" to selectedTsdBookingId!!,
            "userId" to auth.currentUser!!.uid,
            "remarks" to binding.etRemarks.text.toString().ifEmpty { "None" },
            "status" to "Pending",
            "paymentStatus" to "Pending",
            "amount" to PTT_FEE,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        createPaymentIntent(PTT_FEE)
    }

    private fun createPaymentIntent(paymentAmount: Double) { // amount in PHP, flexible input
        progressDialog.setMessage("Initializing payment...")
        progressDialog.show()

        // Convert to cents for Stripe
        val amountInCents = (paymentAmount).toInt()

        RetrofitClient.instance.createPaymentIntent(PaymentRequest(amountInCents))
            .enqueue(object : Callback<PaymentResponse> {
                override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                    progressDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        clientSecret = response.body()!!.clientSecret
                        paymentSheet.presentWithPaymentIntent(
                            clientSecret!!,
                            PaymentSheet.Configuration("EnviroTrack")
                        )
                    } else {
                        Toast.makeText(requireContext(), "Failed to create payment intent", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment successful! Submitting PTT...", Toast.LENGTH_SHORT).show()
                finalizePttSubmission()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment failed: ${result.error.message}", Toast.LENGTH_LONG).show()
                binding.btnSubmitPTT.isEnabled = true
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show()
                binding.btnSubmitPTT.isEnabled = true
            }
        }
    }

    // FINAL & BEST VERSION — SAVES NAMES DIRECTLY
    private fun finalizePttSubmission() = scope.launch {
        binding.progressBar.visibility = View.VISIBLE
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val newDocRef = db.collection("ptt_applications").document()
            val pttId = newDocRef.id

            // Fetch human-readable names
            val generatorDoc = db.collection("HazardousWasteGenerator")
                .document(selectedGeneratorId!!).get().await()
            val transportDoc = db.collection("transport_bookings")
                .document(selectedTransportBookingId!!).get().await()
            val tsdDoc = db.collection("tsd_bookings")
                .document(selectedTsdBookingId!!).get().await()

            val generatorName = generatorDoc.getString("companyName") ?: "Unknown Generator"
            val transporterName = transportDoc.getString("serviceProviderName") ?: "Unknown Transporter"
            val tsdName = tsdDoc.getString("tsdName") ?: "Unknown TSD Facility"

            val finalData = pendingPttData.toMutableMap().apply {
                this["pttId"] = pttId
                this["paymentStatus"] = "Paid"
                this["generatorName"] = generatorName
                this["transporterName"] = transporterName
                this["tsdFacilityName"] = tsdName
            }

            // Upload files
            generatorCertUri?.let {
                val url = uploadFile(it, "ptt_requirements/$pttId/generator_certificate.pdf")
                finalData["generatorCertificateUrl"] = url
            }
            transportPlanUri?.let {
                val url = uploadFile(it, "ptt_requirements/$pttId/transport_plan.pdf")
                finalData["transportPlanUrl"] = url
            }

            // Save to Firestore
            newDocRef.set(finalData).await()

            // --------------------------------------------------------
            // 🔔 CALL BACKEND API — NOTIFY PCO + ALL EMB USERS
            // --------------------------------------------------------
            val request = PcoSendNotificationRequest(
                receiverId = uid,      // PCO UID
                module = "PTT",        // Module name for PTT / Hazardous Waste
                documentId = pttId
            )

            RetrofitClient.instance.sendPcoSubmissionNotification(request)
                .enqueue(object : retrofit2.Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("NOTIF", "PTT submission notifications sent.")
                        } else {
                            Log.e("NOTIF", "Notification error: ${response.code()}")
                            Toast.makeText(requireContext(),
                                "Notification failed: ${response.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("NOTIF", "Notification error: ${t.message}")
                        Toast.makeText(requireContext(),
                            "Failed to send notifications",
                            Toast.LENGTH_SHORT).show()
                    }
                })

            Toast.makeText(requireContext(), "PTT Application submitted successfully!", Toast.LENGTH_LONG).show()
            resetForm()
            findNavController().popBackStack(R.id.HWMSDashboardFragment, false)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Submission failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }


    private suspend fun uploadFile(uri: Uri, path: String): String {
        val ref = storage.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private fun isFormValid(): Boolean {
        if (selectedGeneratorId == null || selectedTransportBookingId == null || selectedTsdBookingId == null) {
            Toast.makeText(requireContext(), "Please complete all selections", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun getFileName(uri: Uri): String? = try {
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    } catch (e: Exception) { null }

    private fun updateSubmitButton() {
        val ready = selectedGeneratorId != null && selectedTransportBookingId != null && selectedTsdBookingId != null
        binding.btnSubmitPTT.isEnabled = ready
        binding.btnSubmitPTT.alpha = if (ready) 1.0f else 0.5f
    }

    private fun updateCardSelected(card: com.google.android.material.card.MaterialCardView, selected: Boolean) {
        card.strokeWidth = if (selected) 4 else 2
        card.strokeColor = if (selected) {
            // Beautiful blue accent from your theme — already exists!
            ContextCompat.getColor(requireContext(), R.color.accent)
        } else {
            ContextCompat.getColor(requireContext(), R.color.darker_gray)
        }
    }

    private fun resetForm() {
        selectedGeneratorId = null
        selectedTransportBookingId = null
        selectedTsdBookingId = null
        generatorCertUri = null
        transportPlanUri = null

        binding.tvSelectedGenerator.text = "Tap to select generator"
        binding.tvSelectedTransportBooking.text = "Tap to select transport booking"
        binding.tvSelectedTsdBooking.text = "Tap to select TSD facility"
        binding.etGenCert.setText("No file selected")
        binding.etTransportPlan.setText("No file selected")
        binding.etRemarks.text?.clear()

        updateCardSelected(binding.cardGenerator, false)
        updateCardSelected(binding.cardTransportBooking, false)
        updateCardSelected(binding.cardTsdBooking, false)
        updateSubmitButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}