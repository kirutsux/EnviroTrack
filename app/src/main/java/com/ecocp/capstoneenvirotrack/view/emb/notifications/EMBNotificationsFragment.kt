package com.ecocp.capstoneenvirotrack.view.emb.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.NotificationAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentEmbNotificationsBinding
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class EMBNotificationsFragment : Fragment() {

    private var _binding: FragmentEmbNotificationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmbNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        adapter = NotificationAdapter(emptyList())
        binding.recyclerembNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerembNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereEqualTo("receiverId", userId)
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
