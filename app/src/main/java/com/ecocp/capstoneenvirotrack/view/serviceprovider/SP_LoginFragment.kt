// package com.ecocp.capstoneenvirotrack.view.serviceprovider
//
// import android.os.Bundle
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import android.widget.Button
// import android.widget.EditText
// import android.widget.TextView
// import android.widget.Toast
// import androidx.fragment.app.Fragment
// import com.ecocp.capstoneenvirotrack.R
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
// import com.google.firebase.auth.FirebaseAuthInvalidUserException
//
// class SP_LoginFragment : Fragment() {
//
//     // Declare variables
//     private lateinit var auth: FirebaseAuth
//     private lateinit var etEmail: EditText
//     private lateinit var etPassword: EditText
//     private lateinit var btnLogin: Button
//     private lateinit var tvForgotPassword: TextView
//     private lateinit var tvRegister: TextView
//
//     override fun onCreateView(
//         inflater: LayoutInflater,
//         container: ViewGroup?,
//         savedInstanceState: Bundle?
//     ): View? {
//         // Inflate the layout for this fragment
//         return inflater.inflate(R.layout.sp_login_fragment, container, false)
//     }
//
//     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//         super.onViewCreated(view, savedInstanceState)
//
//         // Initialize Firebase Auth
//         auth = FirebaseAuth.getInstance()
//
//         // Initialize UI components
//         etEmail = view.findViewById(R.id.etEmail)
//         etPassword = view.findViewById(R.id.etPassword)
//         btnLogin = view.findViewById(R.id.btnLogin)
//         tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
//         tvRegister = view.findViewById(R.id.tvRegister)
//
//         // 🔐 Login Button Click
//         btnLogin.setOnClickListener {
//             val email = etEmail.text.toString().trim()
//             val password = etPassword.text.toString().trim()
//
//             if (email.isEmpty() || password.isEmpty()) {
//                 Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
//                 return@setOnClickListener
//             }
//
//             loginUser(email, password)
//         }
//
//         // 🔁 Forgot Password (optional future logic)
//         tvForgotPassword.setOnClickListener {
//             Toast.makeText(requireContext(), "Forgot Password feature coming soon", Toast.LENGTH_SHORT).show()
//             // TODO: Implement password reset logic if needed
//         }
//
//         // 🧾 Navigate to Registration Fragment
//         tvRegister.setOnClickListener {
//             requireActivity().supportFragmentManager.beginTransaction()
//                 .replace(R.id.fragment_container, SP_RegistrationFragment())
//                 .addToBackStack(null)
//                 .commit()
//         }
//     }
//
//     private fun loginUser(email: String, password: String) {
//         auth.signInWithEmailAndPassword(email, password)
//             .addOnCompleteListener(requireActivity()) { task ->
//                 if (task.isSuccessful) {
//                     Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
//
//                     // ✅ Navigate to Change Password Fragment
//                     requireActivity().supportFragmentManager.beginTransaction()
//                         .replace(R.id.fragment_container, SP_ChangepasswordFragment())
//                         .addToBackStack(null) // Allows user to go back to login
//                         .commit()
//                 } else {
//                     // ❌ Handle Login Failures
//                     try {
//                         throw task.exception ?: Exception("Unknown error")
//                     } catch (e: FirebaseAuthInvalidUserException) {
//                         Toast.makeText(requireContext(), "User does not exist", Toast.LENGTH_SHORT).show()
//                     } catch (e: FirebaseAuthInvalidCredentialsException) {
//                         Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
//                     } catch (e: Exception) {
//                         Toast.makeText(requireContext(), "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
//                     }
//                 }
//             }
//     }
// }
