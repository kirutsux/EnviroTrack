package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
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
import java.net.HttpURLConnection
import java.net.URL

class PttApplicationFragment : Fragment() {

    private var _binding: FragmentPttApplicationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var selectedGeneratorId: String? = null
    private var selectedTransportBookingId: String? = null
    private var selectedTsdBookingId: String? = null

    private var generatorCertUri: Uri? = null
    private var transportPlanUri: Uri? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null

    private val PTT_FEE = 2500.0

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
        binding.progressBar.visibility = View.VISIBLE
        try {
            val docs = db.collection("HazardousWasteGenerator")
                .whereEqualTo("status", "Submitted")
                .get().await()

            if (docs.isEmpty) {
                Toast.makeText(requireContext(), "No approved generators found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = docs.documents.map { it.getString("pcoName") ?: "Unnamed Generator" }
            val ids = docs.documents.map { it.id }

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
            Toast.makeText(requireContext(), "Error loading generators", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    // SELECT TRANSPORT BOOKING
    private fun loadTransportBookings() = scope.launch {
        binding.progressBar.visibility = View.VISIBLE
        try {
            val snapshot = db.collection("transport_bookings")
                .whereEqualTo("bookingStatus", "Confirmed")
                .get().await()

            if (snapshot.isEmpty) {
                Toast.makeText(requireContext(), "No confirmed transport bookings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val displayNames = mutableListOf<String>()
            val ids = mutableListOf<String>()

            snapshot.documents.forEach { doc ->
                ids.add(doc.id)
                val transporter = doc.getString("serviceProviderName") ?: "Unknown Transporter"

                val genId = doc.getString("primaryWasteGeneratorId")
                    ?: (doc.get("wasteGeneratorIds") as? List<String>)?.firstOrNull()

                val companyName = if (genId != null) {
                    val genDoc = db.collection("HazardousWasteGenerator").document(genId).get().await()
                    genDoc.getString("companyName") ?: "Unknown Generator"
                } else "Unknown Generator"

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
            Toast.makeText(requireContext(), "Failed to load transport bookings", Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    // SELECT TSD BOOKING
    private fun loadTsdBookings() = scope.launch {
        binding.progressBar.visibility = View.VISIBLE
        try {
            val docs = db.collection("tsd_bookings")
                .whereEqualTo("status", "Confirmed")
                .get().await()

            if (docs.isEmpty) {
                Toast.makeText(requireContext(), "No confirmed TSD bookings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = docs.documents.map { it.getString("tsdName") ?: "Unnamed TSD Facility" }
            val ids = docs.documents.map { it.id }

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
            Toast.makeText(requireContext(), "Error loading TSD bookings", Toast.LENGTH_SHORT).show()
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
            "status" to "Pending Review",
            "paymentStatus" to "Pending",
            "amount" to PTT_FEE,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        createPaymentIntent(PTT_FEE)
    }

    private fun createPaymentIntent(amount: Double) = scope.launch(Dispatchers.IO) {
        try {
            val url = URL("http://10.0.2.2:8080/create-payment-intent")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply { put("amount", amount) }
            conn.outputStream.write(json.toString().toByteArray())

            val response = conn.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            clientSecret = jsonResponse.getString("clientSecret")

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                paymentSheet.presentWithPaymentIntent(
                    clientSecret!!,
                    PaymentSheet.Configuration("EnviroTrack - PTT Application")
                )
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitPTT.isEnabled = true
                Toast.makeText(requireContext(), "Payment failed to start: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
            val newDocRef = db.collection("ptt_applications").document()
            val pttId = newDocRef.id

            // Fetch human-readable names once at submission time
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

                // Save names — this makes dashboard instant & beautiful
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