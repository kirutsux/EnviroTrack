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
import android.media.RingtoneManager
import android.media.Ringtone
import android.net.Uri
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import androidx.core.content.ContextCompat
import com.ecocp.capstoneenvirotrack.view.businesses.smr.SmrActivity
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat

class COMP_Dashboard : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var notificationListener: ListenerRegistration? = null
    private var lastNotificationCount = 0
    private var isFirstLoad = true

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
    private lateinit var notificationIcon: ImageView

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
        notificationIcon = view.findViewById(R.id.pco_notification_icon)

        drawerMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        fetchGreetingMessage()
        setupNavigationView()
        checkUserAccreditation()
        setupNotificationIcon()
        checkPermitExpiryVisualAlert()
    }

    // ---------------- GREETING -----------------
    private fun fetchGreetingMessage() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: "User"
                greetingTextView.text = "Hi $firstName! ${getTimeBasedGreeting()}"
            }
            .addOnFailureListener { e ->
                Log.e("COMP_Dashboard", "Error fetching user name: ", e)
                greetingTextView.text = "Hi there! ${getTimeBasedGreeting()}"
            }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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
                    findNavController().navigate(R.id.action_pcoDashboard_to_modulesFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_inbox -> {
                    findNavController().navigate(R.id.action_pcoDashboard_to_inboxFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_feedback -> {
                    findNavController().navigate(R.id.action_pcoDashboard_to_feedbackFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_service_providers -> {
                    findNavController().navigate(R.id.action_pcoDashboard_to_serviceProvidersFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_about_us -> {
                    findNavController().navigate(R.id.action_pcoDashboard_to_aboutUsFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
    }

    private fun playNotificationSound() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r: Ringtone = RingtoneManager.getRingtone(requireContext(), notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenForNewNotifications() {
        val userId = auth.currentUser?.uid ?: return

        notificationListener = db.collection("notifications")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val notifications = snapshot.documents
                val newCount = notifications.size

                if (!isFirstLoad && newCount > lastNotificationCount) {
                    playNotificationSound()
                }

                lastNotificationCount = newCount
                isFirstLoad = false
            }
    }

    // ---------------- NOTIFICATIONS -----------------
    private fun setupNotificationIcon() {
        notificationIcon.setOnClickListener {
            val fragment = com.ecocp.capstoneenvirotrack.view.businesses.notifications.NotificationsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // ---------------- CARD LISTENERS -----------------
    private fun setupCardListeners() {
        val navController = findNavController()
        pcoCard.setOnClickListener { navController.navigate(R.id.action_pcoDashboard_to_COMP_PCO) }

        if (isAccredited) {
            smrCard.setOnClickListener { startActivity(Intent(requireContext(), SmrActivity::class.java)) }
            cncCard.setOnClickListener { startActivity(Intent(requireContext(), CncActivity::class.java)) }
            opmsCard.setOnClickListener { startActivity(Intent(requireContext(), OpmsActivity::class.java)) }
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
        val user = auth.currentUser ?: return
        db.collection("accreditations")
            .whereEqualTo("uid", user.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                isAccredited = !querySnapshot.isEmpty
                if (!isAccredited) {
                    PCOVerificationDialogFragment().show(childFragmentManager, "PCOVerificationDialog")
                }
                setupCardListeners()
            }
            .addOnFailureListener { e ->
                Log.e("COMP_Dashboard", "Error checking accreditation: ", e)
                Toast.makeText(requireContext(), "Failed to verify accreditation.", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- EXPIRY ALERTS -----------------
    private fun checkPermitExpiryVisualAlert() {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val alertSentRef = db.collection("dailyAlerts").document(userId)

        alertSentRef.get().addOnSuccessListener { snapshot ->
            val lastAlertDate = snapshot.getString("lastAlertDate")
            if (lastAlertDate == today) return@addOnSuccessListener

            // Check permits
            val permitCollections = listOf("opms_discharge_permits", "opms_pto_applications")
            for (collection in permitCollections) {
                db.collection(collection)
                    .whereEqualTo("uid", userId)
                    .whereEqualTo("status", "Approved")
                    .get()
                    .addOnSuccessListener { docs ->
                        for (doc in docs) {
                            val expiry = doc.getTimestamp("expiryDate") ?: continue
                            val daysLeft = daysUntilExpiry(expiry)
                            if (daysLeft in 1..7) {
                                flashDashboardRed()
                                playNotificationSound()
                                sendExpiryNotification("Permit", daysLeft)
                                break
                            }
                        }
                    }
            }

            // Check PCO accreditations
            db.collection("accreditations")
                .whereEqualTo("uid", userId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener { docs ->
                    for (doc in docs) {
                        val expiry = doc.getTimestamp("expiryDate") ?: continue
                        val daysLeft = daysUntilExpiry(expiry)
                        if (daysLeft in 0..7) {
                            flashDashboardRed()
                            playNotificationSound()
                            sendExpiryNotification("PCO Accreditation", daysLeft)
                            break
                        }
                    }
                    alertSentRef.set(mapOf("lastAlertDate" to today))
                }
        }
    }

    private fun daysUntilExpiry(expiryDate: Timestamp): Long {
        val now = Calendar.getInstance().timeInMillis
        val expiry = expiryDate.toDate().time
        val diffMillis = expiry - now
        return (diffMillis / (1000.0 * 60 * 60 * 24)).toLong() + 1 // +1 to include today
    }


    private fun sendExpiryNotification(type: String, daysLeft: Long) {
        val userId = auth.currentUser?.uid ?: return
        val message = when(type) {
            "Permit" -> "Your permit to operate will expire in $daysLeft day(s)."
            "PCO Accreditation" -> "Your PCO accreditation will expire in $daysLeft day(s)."
            else -> "$type will expire in $daysLeft day(s)."
        }
        val notification = hashMapOf(
            "receiverId" to userId,
            "receiverType" to "pco",
            "senderId" to "system",
            "title" to "$type Expiry Alert",
            "message" to message,
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )
        db.collection("notifications").add(notification)
            .addOnSuccessListener { Log.d("COMP_Dashboard", "✅ Expiry notification sent: $type") }
            .addOnFailureListener { e -> Log.e("COMP_Dashboard", "❌ Failed sending expiry notification: ${e.message}") }
    }


    // Smooth pulsating red effect
    private fun flashDashboardRed() {
        val rootView = view ?: return
        val normalColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val alertColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), normalColor, alertColor)
        animator.duration = 1000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.addUpdateListener { valueAnimator ->
            rootView.setBackgroundColor(valueAnimator.animatedValue as Int)
        }
        animator.start()
    }

    override fun onStart() {
        super.onStart()
        listenForNewNotifications()
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
    }
}
