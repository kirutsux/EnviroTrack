package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.NotificationAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentNotificationsBinding
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        adapter = NotificationAdapter(emptyList())
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                // Prevent crash when fragment is destroyed
                if (_binding == null) return@addSnapshotListener

                val notifications = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationModel::class.java)?.apply { documentId = doc.id }
                }

                val uniqueNotifications = notifications.distinctBy { it.documentId }
                val groupedNotifications = groupNotificationsByDate(uniqueNotifications)

                adapter.addNotifications(groupedNotifications)

                binding.emptyNotificationsText.visibility =
                    if (groupedNotifications.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun groupNotificationsByDate(notifications: List<NotificationModel>): List<NotificationModel> {
        val today = mutableListOf<NotificationModel>()
        val yesterday = mutableListOf<NotificationModel>()
        val earlier = mutableListOf<NotificationModel>()

        val cal = Calendar.getInstance()
        val todayStr = getDayString(cal.time)
        cal.add(Calendar.DATE, -1)
        val yesterdayStr = getDayString(cal.time)

        for (notif in notifications) {
            val notifDate = notif.timestamp?.toDate()?.let { getDayString(it) }
            when (notifDate) {
                todayStr -> today.add(notif)
                yesterdayStr -> yesterday.add(notif)
                else -> earlier.add(notif)
            }
        }

        val result = mutableListOf<NotificationModel>()
        if (today.isNotEmpty()) result.add(NotificationModel(title = "Today", isHeader = true)); result.addAll(today)
        if (yesterday.isNotEmpty()) result.add(NotificationModel(title = "Yesterday", isHeader = true)); result.addAll(yesterday)
        if (earlier.isNotEmpty()) result.add(NotificationModel(title = "Earlier", isHeader = true)); result.addAll(earlier)

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
