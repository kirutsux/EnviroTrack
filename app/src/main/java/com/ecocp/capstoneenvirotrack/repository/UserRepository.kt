package com.ecocp.capstoneenvirotrack.repository

import android.util.Log
import com.ecocp.capstoneenvirotrack.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
                fullName = "$firstName $lastName",
                phoneNumber = phoneNumber,
                password = password,
                userType = userType
            )
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error registering user", e)
            false
        }
    }

    // Google sign-in (Firebase Auth only, before extra fields)
    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error in Google Sign-In", e)
            null
        }
    }

    // Finalize Google registration (extra fields)
    suspend fun completeGoogleRegistration(
        uid: String,
        email: String,
        fullName: String,
        phoneNumber: String,
        password: String,
        userType: String
    ): Boolean {
        return try {
            saveUserToFirestore(
                uid = uid,
                email = email,
                fullName = fullName,
                phoneNumber = phoneNumber,
                password = password,
                userType = userType
            )
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
        fullName: String,
        phoneNumber: String,
        password: String,
        userType: String
    ) {
        val user = hashMapOf(
            "uid" to uid,
            "email" to email,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "password" to password,
            "userType" to userType
        )

        Firebase.firestore.collection("users")
            .document(uid)
            .set(user)
            .await()
    }
}