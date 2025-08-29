package com.ecocp.capstoneenvirotrack.view

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Safely initialize NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: run {
                Log.w("MainActivity", "NavHostFragment not found in layout with ID nav_host_fragment, creating new instance")
                try {
                    NavHostFragment.create(R.navigation.nav_graph).also { fragment ->
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, fragment)
                            .setPrimaryNavigationFragment(fragment)
                            .commitNow()
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity", "Failed to create NavHostFragment: ${e.message}", e)
                    Toast.makeText(this, "Navigation setup failed, please check configuration", Toast.LENGTH_LONG).show()
                    return
                }
            }
        navController = navHostFragment.navController
        Log.d("MainActivity", "NavHostFragment found: ${navHostFragment != null}, NavController: $navController")

        // Set up ActionBar with NavController and Toolbar
        setupActionBarWithNavController(navController, findViewById(R.id.toolbar))
        Log.d("MainActivity", "ActionBar set up with NavController and Toolbar")

        // Handle deep link for email authentication
        val intentData: Uri? = intent?.data
        if (intentData != null) {
            val emailLink = intentData.toString()
            val email = getEmailFromPreferences()

            if (auth.isSignInWithEmailLink(emailLink) && email != null) {
                if (auth.currentUser != null) {
                    linkEmailToExistingUser(email, emailLink)
                } else {
                    verifySignInLink(email, emailLink)
                }
            }
        }
    }

    // Verify Email Link and Sign In
    private fun verifySignInLink(email: String, emailLink: String) {
        auth.signInWithEmailLink(email, emailLink)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Successfully signed in with email link!")
                    Toast.makeText(this, "Sign-in successful!", Toast.LENGTH_SHORT).show()

                    try {
                        navController.navigate(R.id.embDashboard)
                        Log.d("Navigation", "Navigated to embDashboard")
                    } catch (e: IllegalStateException) {
                        Log.e("Navigation", "Failed to navigate to embDashboard", e)
                        Toast.makeText(this, "Navigation failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("FirebaseAuth", "Error signing in with email link: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Retrieve Stored Email from SharedPreferences
    private fun getEmailFromPreferences(): String? {
        val sharedPref = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_email", null)
    }

    // Link Email Authentication to an Existing User
    private fun linkEmailToExistingUser(email: String, emailLink: String) {
        val credential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Successfully linked email authentication!")
                    Toast.makeText(this, "Email linked to account!", Toast.LENGTH_SHORT).show()

                    try {
                        navController.navigate(R.id.embDashboard)
                        Log.d("Navigation", "Navigated to embDashboard")
                    } catch (e: IllegalStateException) {
                        Log.e("Navigation", "Failed to navigate to embDashboard", e)
                        Toast.makeText(this, "Navigation failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("FirebaseAuth", "Error linking email authentication: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, "Failed to link email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Handle back press with NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}