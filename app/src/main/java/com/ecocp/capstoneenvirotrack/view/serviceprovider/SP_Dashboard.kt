package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class SP_Dashboard : Fragment() {

    private lateinit var notificationIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sp_dashboard, container, false)

        // find views
        val serviceRequestCard = view.findViewById<View>(R.id.cnc_card)
        val activeTasksCard = view.findViewById<View>(R.id.smr_card)
        val completedCard = view.findViewById<View>(R.id.hazewaste_card)
        val paymentsCard = view.findViewById<CardView?>(R.id.opms_card)

        // find notification icon
        notificationIcon = view.findViewById(R.id.sp_notification_icon)

        // click listeners
        activeTasksCard?.setOnClickListener {
            findNavController().navigate(R.id.SP_ActiveTasks)
        }

        serviceRequestCard?.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_Servicerequest)
        }

        completedCard?.setOnClickListener {
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_CompletedServices)
        }

        paymentsCard?.setOnClickListener {
            it.isEnabled = false
            findNavController().navigate(R.id.action_SP_Dashboard_to_SP_Payments)
            it.postDelayed({ it.isEnabled = true }, 400)
        }

        // ---------------- NOTIFICATIONS -----------------
        setupNotificationIcon()

        return view
    }

    // ---------------- NOTIFICATIONS -----------------
    private fun setupNotificationIcon() {
        notificationIcon.setOnClickListener {
            // Optional: create a Bundle if you want to pass data
            val bundle = Bundle()

            val options = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_left)
                .build()

            findNavController().navigate(
                R.id.action_SP_Dashboard_to_notificationsFragment,
                bundle,
                options
            )
        }
    }
}
