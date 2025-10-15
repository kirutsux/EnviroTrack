package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitPaymentBinding
import com.google.firebase.firestore.FirebaseFirestore

class DischargePermitPaymentFragment : Fragment() {

    private var _binding: FragmentDischargePermitPaymentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDischargePermitPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnProceedPayment.setOnClickListener {
            val selectedId = binding.paymentMethodGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(requireContext(), "Please select a payment method.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedMethod = view.findViewById<RadioButton>(selectedId).text.toString()

            val paymentData = hashMapOf(
                "processingFee" to 1000,
                "environmentalFee" to 500,
                "totalAmount" to 1500,
                "paymentMethod" to selectedMethod,
                "status" to "Pending"
            )

            db.collection("opms_discharge_payments")
                .add(paymentData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_payment_to_review)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save payment info.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
