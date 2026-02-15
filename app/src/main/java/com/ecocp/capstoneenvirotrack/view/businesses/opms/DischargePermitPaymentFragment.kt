package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.api.PaymentRequest
import com.ecocp.capstoneenvirotrack.api.PaymentResponse
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitPaymentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DischargePermitPaymentFragment : Fragment() {

    private var _binding: FragmentDischargePermitPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var stripe: Stripe
    private var paymentIntentClientSecret: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDischargePermitPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Initialize Stripe SDK
        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PF3r9J2KRREDP2eehrcDI42PVjLhtLQuEy55mabmKa63Etlh5DxHGupzcklVCnrEE0RF6SxYUQVEbJMNph0Zalf00Va9vwLxS"
        )
        stripe = Stripe(requireContext(), PaymentConfiguration.getInstance(requireContext()).publishableKey)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        binding.btnPayWithStripe.setOnClickListener {
            val phpAmount = 50
            createPaymentIntent(phpAmount)
        }
    }

    private fun createPaymentIntent(amountInPhp: Int) {
        RetrofitClient.instance.createPaymentIntent(PaymentRequest(amountInPhp))
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
            val configuration = PaymentSheet.Configuration(
                merchantDisplayName = "EnviroTrack",
                allowsDelayedPaymentMethods = true
            )
            paymentSheet.presentWithPaymentIntent(it, configuration)
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show()
                savePaymentToFirestore()
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment failed: ${paymentSheetResult.error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePaymentToFirestore() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Payment data to add to the *existing document*
        val paymentData = mapOf(
            "amount" to 50,
            "currency" to "PHP",
            "paymentStatus" to "Paid",
            "paymentMethod" to "Stripe",
            "paymentTimestamp" to Timestamp.now() // renamed key to avoid overwriting main timestamp
        )

        // Find the latest document (based on timestamp) under this uid
        db.collection("opms_discharge_permits")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val latestDoc = documents.documents[0].reference

                    // Update the existing document with payment data
                    latestDoc.update(paymentData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_payment_to_review)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update document: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "No existing application found for this user.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching document: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
