package com.ecocp.capstoneenvirotrack.view.all

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.businesses.cnc.CncActivity
import com.ecocp.capstoneenvirotrack.view.businesses.opms.OpmsActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class COMP_Dashboard : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var cncCard: CardView
    private lateinit var smrCard: CardView
    private lateinit var opmsCard: CardView
    private lateinit var hazewasteCard: CardView
    private lateinit var pcoCard: CardView
    private lateinit var crsCard: CardView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var drawerMenu: ImageView
    private lateinit var greetingTextView: TextView

    private var isAccredited = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pco_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        cncCard = view.findViewById(R.id.cnc_card)
        smrCard = view.findViewById(R.id.smr_card)
        opmsCard = view.findViewById(R.id.opms_card)
        hazewasteCard = view.findViewById(R.id.hazewaste_card)
        pcoCard = view.findViewById(R.id.pco_card)
        crsCard = view.findViewById(R.id.crs_card)
        greetingTextView = view.findViewById(R.id.greeting_message)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        drawerMenu = view.findViewById(R.id.drawerMenu)

        // Setup drawer menu toggle
        drawerMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        fetchGreetingMessage()
        // Setup navigation view
        setupNavigationView()

        // Check user accreditation
        checkUserAccreditation()
    }

    private fun fetchGreetingMessage() {
        val user = auth.currentUser
        if (user == null) {
            Log.e("COMP_Dashboard", "No logged-in user.")
            return
        }

        val userId = user.uid
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: "User"
                    val greeting = getTimeBasedGreeting()
                    greetingTextView.text = "Hi $firstName! $greeting"
                } else {
                    greetingTextView.text = "Hi there! ${getTimeBasedGreeting()}"
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_Dashboard", "Error fetching user name: ", e)
                greetingTextView.text = "Hi there! ${getTimeBasedGreeting()}"
            }
    }

    private fun getTimeBasedGreeting(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_modules -> {
                    // Navigate to Modules fragment
                    findNavController().navigate(R.id.action_pcoDashboard_to_modulesFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_inbox -> {
                    // Navigate to Inbox fragment
                    findNavController().navigate(R.id.action_pcoDashboard_to_inboxFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_feedback -> {
                    // Navigate to Feedback fragment
                    findNavController().navigate(R.id.action_pcoDashboard_to_feedbackFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_service_providers -> {
                    // Navigate to Service Providers fragment
                    findNavController().navigate(R.id.action_pcoDashboard_to_serviceProvidersFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_about_us -> {
                    // Navigate to About Us fragment
                    findNavController().navigate(R.id.action_pcoDashboard_to_aboutUsFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupCardListeners() {
        val navController = findNavController()

        // PCO card is always accessible
        pcoCard.setOnClickListener {
            navController.navigate(R.id.action_pcoDashboard_to_COMP_PCO)
        }

        if (isAccredited) {
            cncCard.setOnClickListener {
                val intent = Intent(requireContext(), CncActivity::class.java)
                startActivity(intent)
            }
            smrCard.setOnClickListener { navController.navigate(R.id.action_pcoDashboard_to_COMP_SMR) }
            opmsCard.setOnClickListener {
                val intent = Intent(requireContext(), OpmsActivity::class.java)
                startActivity(intent)
            }
            hazewasteCard.setOnClickListener { navController.navigate(R.id.action_pcoDashboard_to_HWMSDashboardFragment) }
            crsCard.setOnClickListener { navController.navigate(R.id.action_pcoDashboard_to_COMP_CRS) }
        } else {
            val lockMessage = "Please complete your accreditation first via the PCO section."
            val showToast = { Toast.makeText(requireContext(), lockMessage, Toast.LENGTH_SHORT).show() }

            cncCard.setOnClickListener { showToast() }
            smrCard.setOnClickListener { showToast() }
            opmsCard.setOnClickListener { showToast() }
            hazewasteCard.setOnClickListener { showToast() }
            crsCard.setOnClickListener { showToast() }
        }
    }

    private fun checkUserAccreditation() {
        val user = auth.currentUser
        if (user == null) {
            Log.e("COMP_Dashboard", "No logged-in user.")
            return
        }

        val userId = user.uid
        db.collection("accreditations")
            .whereEqualTo("uid", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    isAccredited = true
                    Log.d("COMP_Dashboard", "✅ Accreditation found for user: $userId")
                } else {
                    isAccredited = false
                    Log.d("COMP_Dashboard", "⚠️ No accreditation found — showing dialog.")
                    val dialog = PCOVerificationDialogFragment()
                    dialog.show(childFragmentManager, "PCOVerificationDialog")
                }
                setupCardListeners()
            }
            .addOnFailureListener { e ->
                Log.e("COMP_Dashboard", "Error checking accreditation: ", e)
                Toast.makeText(requireContext(), "Failed to verify accreditation.", Toast.LENGTH_SHORT).show()
            }
    }
}