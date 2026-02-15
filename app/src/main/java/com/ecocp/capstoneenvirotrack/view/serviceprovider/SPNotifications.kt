package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.NotificationAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpNotificationsBinding
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*


class SPNotifications : Fragment() {
    private var _binding: FragmentSpNotificationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        adapter = NotificationAdapter(emptyList())
        binding.recyclerspNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerspNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Determine the role of the current user
        db.collection("service_providers").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role") ?: ""

                // Fetch notifications only for this user and their role
                db.collection("notifications")
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("module", when(role) {
                        "Transporter" -> "Transport"
                        "TSD Facility" -> "TSD"
                        else -> "" // fallback if role is unknown
                    })
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null || _binding == null) return@addSnapshotListener

                        val notifications = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(NotificationModel::class.java)?.apply { documentId = doc.id }
                        }.distinctBy { it.documentId }

                        adapter.addNotifications(groupNotificationsByDate(notifications))

                        binding.emptyNotificationsText.visibility =
                            if (notifications.isEmpty()) View.VISIBLE else View.GONE
                    }

            }
            .addOnFailureListener {
                // fallback if we cannot fetch the role
                Log.e("SPNotifications", "Failed to fetch user role: ${it.message}")
            }
    }


    private fun groupNotificationsByDate(notifications: List<NotificationModel>): List<NotificationModel> {
        if (notifications.isEmpty()) return emptyList()

        val todayStr = getDayString(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayStr = getDayString(cal.time)

        val todayList = mutableListOf<NotificationModel>()
        val yesterdayList = mutableListOf<NotificationModel>()
        val earlierList = mutableListOf<NotificationModel>()

        notifications.forEach { notif ->
            val notifDate = notif.timestamp?.toDate()?.let { getDayString(it) } ?: ""
            when (notifDate) {
                todayStr -> todayList.add(notif)
                yesterdayStr -> yesterdayList.add(notif)
                else -> earlierList.add(notif)
            }
        }

        val result = mutableListOf<NotificationModel>()
        if (todayList.isNotEmpty()) {
            result.add(NotificationModel(title = "Today", isHeader = true))
            result.addAll(todayList)
        }
        if (yesterdayList.isNotEmpty()) {
            result.add(NotificationModel(title = "Yesterday", isHeader = true))
            result.addAll(yesterdayList)
        }
        if (earlierList.isNotEmpty()) {
            result.add(NotificationModel(title = "Earlier", isHeader = true))
            result.addAll(earlierList)
        }

        return result
    }

    private fun getDayString(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}