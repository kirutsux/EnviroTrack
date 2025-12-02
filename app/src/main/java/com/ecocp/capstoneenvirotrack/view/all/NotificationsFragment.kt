package com.ecocp.capstoneenvirotrack.view.businesses.notifications

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
    private val notifList = mutableListOf<NotificationModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        setupRecyclerView()
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifList)
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val notifications = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationModel::class.java)?.apply {
                        // Only set documentId if you need the Firestore doc ID in your app
                        if (documentId.isNullOrEmpty()) {
                            documentId = doc.id
                        }
                    }
                }

                displayGroupedNotifications(notifications)
            }
    }


    private fun displayGroupedNotifications(notifications: List<NotificationModel>) {
        notifList.clear()

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

        notifList.addAll(buildSection("Today", today))
        notifList.addAll(buildSection("Yesterday", yesterday))
        notifList.addAll(buildSection("Earlier", earlier))

        adapter.notifyDataSetChanged()
        binding.emptyNotificationsText.visibility =
            if (notifList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildSection(title: String, items: List<NotificationModel>): List<NotificationModel> {
        if (items.isEmpty()) return emptyList()
        val header = NotificationModel(title = title, isHeader = true)
        return listOf(header) + items
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
