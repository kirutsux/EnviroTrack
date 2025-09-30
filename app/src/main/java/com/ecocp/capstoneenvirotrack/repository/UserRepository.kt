package com.ecocp.capstoneenvirotrack.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Send verification code via Nodemailer backend
    suspend fun sendVerificationCode(email: String): String? {
        val code = (100000..999999).random().toString()

        try {
            val url = "http://10.0.2.2:5000/send-email" // Emulator localhost
            val json = """
                {
                  "to": "$email",
                  "subject": "EnviroTrack Verification Code",
                  "text": "Your verification code is $code"
                }
            """.trimIndent()

            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(json.toByteArray()) }

            if (conn.responseCode == 200) {
                Log.d("UserRepository", "Verification code $code sent to $email")
                return code
            } else {
                Log.e("UserRepository", "Failed to send email: ${conn.responseMessage}")
                return null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error sending verification email", e)
            return null
        }
    }

    // Finalize manual registration after verification
    suspend fun registerUserWithEmail(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        userType: String
    ): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false

            saveUserToFirestore(
                uid = uid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                password = password,
                userType = userType
            )
            Log.d("UserRepository", "Manual registration successful for $email")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error registering user", e)
            false
        }
    }

    // Google sign-in (Firebase Auth + save to Firestore immediately)
    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                Log.d("UserRepository", "Google sign-in successful for ${user.email}")

                val displayName = user.displayName ?: ""
                val parts = displayName.split(" ")
                val firstName = parts.firstOrNull() ?: ""
                val lastName = parts.drop(1).joinToString(" ")

                // Save to Firestore right away
                saveUserToFirestore(
                    uid = user.uid,
                    email = user.email ?: "",
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = "",
                    password = "", // empty for Google accounts
                    userType = "PCO" // default PCO type
                )
                Log.d("UserRepository", "Google user saved to Firestore with uid=${user.uid}")
            } else {
                Log.w("UserRepository", "Google sign-in returned null user")
            }

            user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error in Google Sign-In", e)
            null
        }
    }

    // Optional: update user data later (e.g. phone/password)
    suspend fun completeGoogleRegistration(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        password: String,
        userType: String
    ): Boolean {
        return try {
            saveUserToFirestore(
                uid = uid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                password = password,
                userType = userType
            )
            Log.d("UserRepository", "Google registration completed for $email")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error completing Google registration", e)
            false
        }
    }

    // Login with email and password
    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error signing in with email/password", e)
            null
        }
    }

    // Shared method for saving any user to Firestore
    suspend fun saveUserToFirestore(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        password: String,
        userType: String
    ) {
        val user = hashMapOf(
            "uid" to uid,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "password" to password,
            "userType" to userType
        )

        Firebase.firestore.collection("users")
            .document(uid)
            .set(user)
            .await()

        Log.d("UserRepository", "User saved to Firestore: $email ($uid)")
    }
}
