package com.ecocp.capstoneenvirotrack.view.emb.all

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.businesses.opms.OpmsActivity
import com.ecocp.capstoneenvirotrack.view.emb.cnc.embcncactivity
import com.ecocp.capstoneenvirotrack.view.emb.crs.EmbcrsActivity
import com.ecocp.capstoneenvirotrack.view.emb.hwms.EmbhwmsActivity
import com.ecocp.capstoneenvirotrack.view.emb.notifications.EMBNotificationsFragment
import com.ecocp.capstoneenvirotrack.view.emb.opms.EmbopmsActivity
import com.ecocp.capstoneenvirotrack.view.emb.pcoacc.EmbpcoActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EMB_Dashboard : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var cncCard: CardView
    private lateinit var smrCard: CardView
    private lateinit var opmsCard: CardView
    private lateinit var hazewasteCard: CardView
    private lateinit var crsCard: CardView
    private lateinit var pcoCard: CardView
    private lateinit var greetingTextView: TextView
    private lateinit var notificationIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var drawerMenu: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.emb_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cncCard = view.findViewById(R.id.cnc_card)
        smrCard = view.findViewById(R.id.smr_card)
        opmsCard = view.findViewById(R.id.opms_card)
        hazewasteCard = view.findViewById(R.id.hazewaste_card)
        crsCard = view.findViewById(R.id.crs_card)
        pcoCard = view.findViewById(R.id.pco_card)
        greetingTextView = view.findViewById(R.id.greeting_message)
        notificationIcon = view.findViewById(R.id.emb_notification_icon)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        drawerMenu = view.findViewById(R.id.drawerMenu)

        notificationIcon = view.findViewById(R.id.emb_notification_icon)

        fetchGreetingMessage()
        setupNotificationIcon()

        val navController = findNavController()

        cncCard.setOnClickListener {
            startActivity(Intent(requireContext(), embcncactivity::class.java))
        }

        pcoCard.setOnClickListener {
            startActivity(Intent(requireContext(), EmbpcoActivity::class.java))
        }

//        smrCard.setOnClickListener {
//            navController.navigate(R.id.action_embDashboard_to_embSMRFragment)
//        }

        opmsCard.setOnClickListener {
            startActivity(Intent(requireContext(), EmbopmsActivity::class.java))
        }

        hazewasteCard.setOnClickListener {
            startActivity(Intent(requireContext(), EmbhwmsActivity::class.java))
        }

        crsCard.setOnClickListener {
            startActivity(Intent(requireContext(), EmbcrsActivity::class.java))
        }
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
                Log.e("EMB_Dashboard", "Error fetching user name: ", e)
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

    // ---------------- NOTIFICATIONS -----------------
    private fun setupNotificationIcon() {
        notificationIcon.setOnClickListener {
            // Use NavController to navigate to NotificationsFragment
            val navController = findNavController() // if inside a fragment
            // OR: requireActivity().findNavController(R.id.nav_host_fragment) if inside activity

            // Optional: create a Bundle if you want to pass data
            val bundle = Bundle() // add args if needed

            // Navigate using global action or direct fragment ID
            val options = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_left)
                .build()
            navController.navigate(R.id.action_embDashboard_to_embnotificationsFragment, bundle, options)

        }
    }
}