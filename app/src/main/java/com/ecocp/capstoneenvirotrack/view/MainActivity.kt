package com.ecocp.capstoneenvirotrack.view

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration // For more advanced Toolbar setup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )


        auth = FirebaseAuth.getInstance()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        Log.d("MainActivity", "NavController initialized from NavHostFragment in XML.")

        appBarConfiguration = AppBarConfiguration(navController.graph)

        handleDeepLink()
    }

    private fun handleDeepLink() {
        val intentData: Uri? = intent?.data
        if (intentData != null) {
            val emailLink = intentData.toString()
            Log.d("MainActivity", "Deep link received: $emailLink")
            // Check if the user is already signed in when the app is opened via link
            if (auth.isSignInWithEmailLink(emailLink)) {
                val email = getEmailFromPreferences() // Retrieve the stored email
                if (email != null) {
                    if (auth.currentUser != null) {
                        // User is already signed in (maybe with another provider), link this email
                        Log.d("MainActivity", "Linking email to existing user.")
                        linkEmailToExistingUser(email, emailLink)
                    } else {
                        // User is not signed in, sign them in with the email link
                        Log.d("MainActivity", "Signing in with email link.")
                        verifySignInLink(email, emailLink)
                    }
                } else {
                    Log.w("MainActivity", "Email for link sign-in not found in preferences.")
                    Toast.makeText(this, "Email for sign-in link not found. Please try sending the link again.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("MainActivity", "Intent data is not a sign-in email link.")
            }
        }
    }


    private fun verifySignInLink(email: String, emailLink: String) {
        auth.signInWithEmailLink(email, emailLink)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Successfully signed in with email link!")
                    Toast.makeText(this, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                    // Clear the stored email after successful sign-in
                    clearEmailFromPreferences()
                    try {
                        // Check current destination to avoid unnecessary navigation if already there
                        if (navController.currentDestination?.id != R.id.embDashboard) {
                            navController.navigate(R.id.embDashboard) // Or your main app screen
                            Log.d("Navigation", "Navigated to embDashboard after email link sign-in.")
                        } else {
                            Log.d("Navigation", "Already at embDashboard. No navigation needed.")
                        }
                    } catch (e: Exception) { // Catch broader exceptions for navigation
                        Log.e("Navigation", "Failed to navigate after email link sign-in", e)
                        Toast.makeText(this, "Navigation failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("FirebaseAuth", "Error signing in with email link: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun linkEmailToExistingUser(email: String, emailLink: String) {
        val credential = EmailAuthProvider.getCredentialWithLink(email, emailLink)
        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Successfully linked email authentication!")
                    Toast.makeText(this, "Email linked to account!", Toast.LENGTH_SHORT).show()
                    clearEmailFromPreferences()
                    try {
                        if (navController.currentDestination?.id != R.id.embDashboard) {
                            navController.navigate(R.id.embDashboard) // Or your main app screen
                            Log.d("Navigation", "Navigated to embDashboard after email link.")
                        } else {
                            Log.d("Navigation", "Already at embDashboard. No navigation needed.")
                        }
                    } catch (e: Exception) {
                        Log.e("Navigation", "Failed to navigate after email link", e)
                        Toast.makeText(this, "Navigation failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("FirebaseAuth", "Error linking email authentication: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, "Failed to link email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getEmailFromPreferences(): String? {
        val sharedPref = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_email_for_link", null) // Use a more specific key
    }

    // It's good practice to clear the email once it's used or no longer needed
    private fun clearEmailFromPreferences() {
        val sharedPref = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_email_for_link")
            apply()
        }
    }


    // Handle Up button press with NavController and AppBarConfiguration
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

