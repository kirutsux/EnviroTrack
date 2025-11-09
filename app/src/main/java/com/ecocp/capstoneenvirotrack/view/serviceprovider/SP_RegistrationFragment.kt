package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.*

class SP_RegistrationFragment : Fragment() {

    private lateinit var etEmail: EditText
    private lateinit var btnRequestAccess: Button
    private lateinit var btnBack: ImageView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sp_registration, container, false)
        etEmail = view.findViewById(R.id.etEmail)
        btnRequestAccess = view.findViewById(R.id.btnRequestAccess)
        btnBack = view.findViewById(R.id.btnBack)
        // Initialize tvLogin
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)


        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_SP_RegistrationFragment_to_loginFragment)
        }

        android.util.Log.d("SP_Registration", "Firestore initialized: ${firestore.app.name}")
        if (!checkPlayServices()) {
            android.util.Log.e("SP_Registration", "Play Services check failed")
            return view
        }

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        btnRequestAccess.setOnClickListener { submitAccessRequest() }
        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        return view
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(requireActivity(), resultCode, 9000)?.show()
                android.util.Log.e("SP_Registration", "Play Services resolvable error: $resultCode")
            } else {
                Toast.makeText(requireContext(), "Google Play Services unavailable", Toast.LENGTH_SHORT).show()
                android.util.Log.e("SP_Registration", "Play Services unavailable: $resultCode")
            }
            return false
        }
        android.util.Log.d("SP_Registration", "Play Services available")
        return true
    }

    private fun submitAccessRequest() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a unique UID (this will be used later in ChangePassword)
        val uid = firestore.collection("uids").document().id

        val requestData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "status" to "pending",
            "createdAt" to com.google.firebase.Timestamp(Date())
        )

        android.util.Log.d("SP_Registration", "Submitting request for: $email with UID: $uid")

        firestore.collection("service_requests").document(uid)
            .set(requestData)
            .addOnSuccessListener {
                android.util.Log.d("SP_Registration", "Request submitted successfully for: $email")
                Toast.makeText(requireContext(), "Request submitted successfully!", Toast.LENGTH_LONG).show()
                etEmail.text.clear()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SP_Registration", "Request failed: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}