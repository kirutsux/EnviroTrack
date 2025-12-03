package com.ecocp.capstoneenvirotrack.view.emb.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.emb.notifications.EMBNotificationsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EMB_Dashboard : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var greetingTextView: TextView
    private lateinit var notificationIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerMenu: ImageView

    private lateinit var cncCard: CardView
    private lateinit var opmsCard: CardView
    private lateinit var crsCard: CardView
    private lateinit var pcoCard: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.emb_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        greetingTextView = view.findViewById(R.id.greeting_message)
        notificationIcon = view.findViewById(R.id.emb_notification_icon)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        drawerMenu = view.findViewById(R.id.drawerMenu)

        cncCard = view.findViewById(R.id.cnc_card)
        opmsCard = view.findViewById(R.id.opms_card)
        crsCard = view.findViewById(R.id.crs_card)
        pcoCard = view.findViewById(R.id.pco_card)

        fetchGreetingMessage()
        setupDrawer()
        setupNotificationIcon()
        setupCardClicks()
    }

    private fun fetchGreetingMessage() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("firstName") ?: "Officer"
                greetingTextView.text = "Hi $name! ${getTimeBasedGreeting()}"
            }
            .addOnFailureListener {
                greetingTextView.text = "Hi Officer! ${getTimeBasedGreeting()}"
            }
    }

    private fun getTimeBasedGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun setupDrawer() {
        drawerMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNotificationIcon() {
        notificationIcon.setOnClickListener {
            val fragment = EMBNotificationsFragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.nav_host_fragment_emb, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupCardClicks() {
        val navController = findNavController()

        cncCard.setOnClickListener {
            navController.navigate(R.id.action_embDashboard_to_embCNCFragment)
        }
        opmsCard.setOnClickListener {
            navController.navigate(R.id.action_embDashboard_to_embOPMSFragment)
        }
        crsCard.setOnClickListener {
            navController.navigate(R.id.action_embDashboard_to_embCRSFragment)
        }
        pcoCard.setOnClickListener {
            navController.navigate(R.id.action_embDashboard_to_embPCOFragment)
        }
    }
}