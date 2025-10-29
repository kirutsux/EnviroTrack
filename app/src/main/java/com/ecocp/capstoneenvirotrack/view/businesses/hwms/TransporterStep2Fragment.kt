package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ServiceProviderAdapter
import com.ecocp.capstoneenvirotrack.model.ServiceProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

        // Initialize Stripe with publishable key
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

    private fun showBookingDialog(provider: ServiceProvider) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transporter_booking, null)
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

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val wasteType = etWasteType.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val packaging = etPackaging.text.toString().trim()
            val origin = etOrigin.text.toString().trim()
            val destination = etDestination.text.toString().trim()
            val special = etSpecial.text.toString().trim()
            val amountText = etAmount.text.toString().trim()

            if (wasteType.isEmpty() || quantity.isEmpty() || packaging.isEmpty() ||
                origin.isEmpty() || selectedDateMillis == null || amountText.isEmpty()
            ) {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            paymentAmount = amountText.toDoubleOrNull() ?: 0.0
            if (paymentAmount <= 0) {
                Toast.makeText(requireContext(), "Invalid amount.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            selectedProvider = provider

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
                "status" to "Pending"
            )

            dialog.dismiss()
            createPaymentIntent(paymentAmount)
        }

        dialog.show()
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

    private fun saveBookingToFirestore(status: String) {
        bookingData["status"] = status
        db.collection("transport_bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking saved!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
