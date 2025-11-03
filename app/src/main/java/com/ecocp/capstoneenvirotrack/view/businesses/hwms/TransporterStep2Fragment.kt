package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ServiceProviderAdapter
import com.ecocp.capstoneenvirotrack.api.PaymentRequest
import com.ecocp.capstoneenvirotrack.api.PaymentResponse
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.databinding.FragmentTransporterStep2Binding
import com.ecocp.capstoneenvirotrack.model.ServiceProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class TransporterStep2Fragment : Fragment() {

    private var _binding: FragmentTransporterStep2Binding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val providers = mutableListOf<ServiceProvider>()
    private lateinit var adapter: ServiceProviderAdapter
    private lateinit var progressDialog: ProgressDialog

    // Stripe variables
    private lateinit var paymentSheet: PaymentSheet
    private var paymentIntentClientSecret: String? = null
    private var paymentAmount: Double = 0.0
    private lateinit var selectedProvider: ServiceProvider
    private lateinit var bookingData: HashMap<String, Any>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransporterStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Stripe SDK (same publishable key as CNC)
        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        setupRecyclerView()
        fetchTransporters()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTransporters.layoutManager = LinearLayoutManager(requireContext())
        adapter = ServiceProviderAdapter(providers) { provider ->
            showBookingDialog(provider)
        }
        binding.recyclerViewTransporters.adapter = adapter
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

        tvProviderTitle.text = "Book with: ${provider.name} — ${provider.companyName}"

        var selectedDateMillis: Long? = null
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(requireContext(), { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day)
                selectedDateMillis = c.timeInMillis
                tvDateSelected.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(c.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dp.datePicker.minDate = System.currentTimeMillis()
            dp.show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancelBooking).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnConfirmBooking).setOnClickListener {
            val wasteType = etWasteType.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val packaging = etPackaging.text.toString().trim()
            val origin = etOrigin.text.toString().trim()
            val destination = etDestination.text.toString().trim()
            val special = etSpecial.text.toString().trim()
            val amountText = etAmount.text.toString().trim()

            if (wasteType.isEmpty() || quantity.isEmpty() || packaging.isEmpty() ||
                origin.isEmpty() || destination.isEmpty() || amountText.isEmpty() || selectedDateMillis == null
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
        RetrofitClient.instance.createPaymentIntent(PaymentRequest(amount.toInt()))
            .enqueue(object : Callback<PaymentResponse> {
                override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        paymentIntentClientSecret = response.body()!!.clientSecret
                        showPaymentSheet()
                    } else {
                        Toast.makeText(requireContext(), "Failed to create payment intent", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showPaymentSheet() {
        paymentIntentClientSecret?.let {
            val config = PaymentSheet.Configuration(
                merchantDisplayName = "EnviroTrack",
                allowsDelayedPaymentMethods = true
            )
            paymentSheet.presentWithPaymentIntent(it, config)
        }
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show()
                saveBookingToFirestore("Paid")
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment canceled.", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment failed: ${result.error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveBookingToFirestore(status: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        bookingData["status"] = status
        bookingData["paymentStatus"] = "Paid"
        bookingData["paymentMethod"] = "Stripe"
        bookingData["paymentTimestamp"] = Timestamp.now()
        bookingData["amount"] = paymentAmount
        bookingData["currency"] = "PHP"

        val newDocRef = db.collection("transport_bookings").document()
        val bookingId = newDocRef.id
        bookingData["bookingId"] = bookingId

        newDocRef.set(bookingData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking saved successfully!", Toast.LENGTH_LONG).show()
                linkBookingToHazardousWasteGenerator(bookingId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun linkBookingToHazardousWasteGenerator(bookingId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No HWMS record found for this user.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in querySnapshot.documents) {
                    db.collection("HazardousWasteGenerator")
                        .document(doc.id)
                        .update("bookingId", bookingId)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Linked booking to HWMS application!", Toast.LENGTH_SHORT).show()

                            // ✅ Safe navigation: only navigate if we're still in TransporterStep2Fragment
                            val navController = findNavController()
                            val currentDestId = navController.currentDestination?.id
                            if (currentDestId == R.id.TransporterStep2Fragment) {
                                navController.navigate(R.id.action_transporterStep2Fragment_to_tsdFacilitySelectionFragment)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
