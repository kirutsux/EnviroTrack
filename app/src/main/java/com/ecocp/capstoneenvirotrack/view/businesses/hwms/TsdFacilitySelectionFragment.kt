package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class TsdFacilitySelectionFragment : Fragment() {

    private lateinit var binding: FragmentTsdFacilitySelectionBinding
    private val db = FirebaseFirestore.getInstance()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTsdFacilitySelectionBinding.inflate(inflater, container, false)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Processing...")
            setCancelable(false)
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
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
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
                        rate = 500.0, // default placeholder rate
                        wasteType = "General Waste" // default placeholder
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val facility = selectedFacility ?: run {
            Toast.makeText(requireContext(), "Select a TSD Facility", Toast.LENGTH_SHORT).show()
            return
        }

        val treatmentInfo = binding.etTreatmentInfo.text.toString()
        val quantity = binding.etQuantity.text.toString()
        val date = binding.etPreferredDate.text.toString()
        val rate = facility.rate

        if (treatmentInfo.isEmpty() || quantity.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = quantity.toDoubleOrNull() ?: 0.0
        paymentAmount = qty * rate
        if (paymentAmount <= 0) {
            Toast.makeText(requireContext(), "Invalid payment amount", Toast.LENGTH_SHORT).show()
            return
        }

        bookingData = hashMapOf(
            "userId" to userId,
            "facilityId" to facility.id,
            "facilityName" to facility.companyName,
            "contactNumber" to facility.contactNumber,
            "location" to facility.location,
            "treatmentInfo" to treatmentInfo,
            "quantity" to quantity,
            "preferredDate" to date,
            "rate" to rate,
            "totalPayment" to paymentAmount,
            "status" to "Pending Payment",
            "dateCreated" to FieldValue.serverTimestamp()
        )

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
                saveBookingToFirestore("Paid")
            }
            is PaymentSheetResult.Failed ->
                Toast.makeText(requireContext(), "Payment failed: ${result.error.message}", Toast.LENGTH_SHORT).show()
            PaymentSheetResult.Canceled ->
                Toast.makeText(requireContext(), "Payment canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBookingToFirestore(status: String) {
        bookingData["status"] = status

        val newDocRef = db.collection("transport_bookings").document()
        val bookingId = newDocRef.id
        bookingData["bookingId"] = bookingId

        newDocRef.set(bookingData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "TSD Booking successful!", Toast.LENGTH_SHORT).show()

                // ✅ Navigate to PTT Application
                val bundle = Bundle().apply {
                    putString("bookingId", bookingId)
                }
                findNavController().navigate(
                    R.id.action_tsdFacilitySelectionFragment_to_pttApplicationFragment,
                    bundle
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    companion object {
        private const val REQUEST_CERTIFICATE = 1
        private const val REQUEST_PREV_RECORD = 2
    }
}
