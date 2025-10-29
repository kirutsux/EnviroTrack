package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SubmittedApplicationsAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentOpmsDashboardBinding
import com.ecocp.capstoneenvirotrack.model.SubmittedApplication
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class OpmsDashboardFragment : Fragment() {

    private var _binding: FragmentOpmsDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val applications = mutableListOf<SubmittedApplication>()
    private lateinit var adapter: SubmittedApplicationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpmsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnApplyDischargePermit.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dischargePermitForm)
        }

        binding.btnApplyPto.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_ptoForm)
        }

        // ✅ Setup RecyclerView
        binding.recyclerSubmittedApplications.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubmittedApplicationsAdapter(
            applications,
            onItemClick = { selectedApp ->
                val bundle = Bundle().apply { putString("applicationId", selectedApp.id) }
                when (selectedApp.applicationType) {
                    "Discharge Permit" -> findNavController().navigate(
                        R.id.action_dashboard_to_dischargePermitDetails, bundle
                    )
                    "Permit to Operate" -> findNavController().navigate(
                        R.id.action_dashboard_to_ptoDetails, bundle
                    )
                }
            },
            onItemLongClick = onItemLongClick@{ app ->
                if (app.status != "Pending") {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Only pending applications can be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this submitted ${app.applicationType} application?")
                    .setPositiveButton("Delete") { _, _ ->
                        val collectionName = when (app.applicationType) {
                            "Discharge Permit" -> "opms_discharge_permits"
                            "Permit to Operate" -> "opms_pto_applications"
                            else -> return@setPositiveButton
                        }

                        db.collection(collectionName).document(app.id)
                            .delete()
                            .addOnSuccessListener {
                                applications.remove(app)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(requireContext(), "Application deleted successfully.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerSubmittedApplications.adapter = adapter

        listenToApplications()
    }

    /**
     * Load Discharge Permit and PTO Applications
     */
    private fun listenToApplications() {
        val uid = auth.currentUser?.uid ?: return
        applications.clear()

        db.collection("opms_discharge_permits").whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.forEach { doc ->
                    val app = mapToApplication(doc.id, doc.data, "Discharge Permit")
                    addOrUpdateApplication(app)
                    checkPermitExpiryAndNotify(app)
                }
                adapter.notifyDataSetChanged()
            }

        db.collection("opms_pto_applications").whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.forEach { doc ->
                    val app = mapToApplication(doc.id, doc.data, "Permit to Operate")
                    addOrUpdateApplication(app)
                    checkPermitExpiryAndNotify(app)
                }
                adapter.notifyDataSetChanged()
            }
    }

    /**
     * Checks if permit expiry is near and sends reminder notifications
     */
    private fun checkPermitExpiryAndNotify(app: SubmittedApplication) {
        if (!app.status.equals("Approved", ignoreCase = true)) return
        val expiryDate = app.expiryDate ?: return
        val daysLeft = daysUntilExpiry(expiryDate)
        val uid = auth.currentUser?.uid ?: return

        // 🧠 Debug log for checking
        Log.d("ReminderCheck", "Checking ${app.applicationType} (${app.id}) -> $daysLeft days left")

        // ✅ For demo/testing — if expiry is within 1 hour, trigger a reminder
        val hoursLeft = (expiryDate.toDate().time - System.currentTimeMillis()) / (1000 * 60 * 60)
        if (hoursLeft in 0..1) {
            sendReminder(app, uid, "test_mode", "Your ${app.applicationType} permit is about to expire (test mode).")
            return
        }

        // ✅ Normal day-based reminders with ±1-day margin
        val interval = when {
            daysLeft in 29..31 -> "30_days"
            daysLeft in 14..16 -> "15_days"
            daysLeft in 6..8 -> "7_days"
            abs(daysLeft) < 1 -> "on_expiry"
            else -> null
        } ?: return

        val notifKey = "${app.id}_$interval"
        val notifSentRef = db.collection("notificationsSent").document(notifKey)

        notifSentRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val message = when (interval) {
                    "30_days" -> "Your ${app.applicationType} permit will expire in 30 days."
                    "15_days" -> "Your ${app.applicationType} permit will expire in 15 days."
                    "7_days" -> "Your ${app.applicationType} permit will expire in 7 days."
                    "on_expiry" -> "Your ${app.applicationType} permit has expired today."
                    else -> return@addOnSuccessListener
                }
                sendReminder(app, uid, interval, message)
                notifSentRef.set(mapOf("sent" to true))
            } else {
                Log.d("ReminderCheck", "Notification for ${app.id} [$interval] already sent.")
            }
        }
    }

    private fun sendReminder(app: SubmittedApplication, uid: String, interval: String, message: String) {
        val notification = hashMapOf(
            "receiverId" to uid,
            "receiverType" to "pco",
            "senderId" to "system",
            "title" to "Permit Expiry Reminder",
            "message" to message,
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "applicationId" to app.id
        )

        db.collection("notifications").add(notification)
            .addOnSuccessListener {
                Log.d("ReminderCheck", "✅ Reminder sent for ${app.id} [$interval]")
            }
            .addOnFailureListener { e ->
                Log.e("ReminderCheck", "❌ Failed to send reminder: ${e.message}")
            }
    }

    /**
     * Calculates days left until expiry
     */
    private fun daysUntilExpiry(expiryDate: Timestamp?): Long {
        if (expiryDate == null) return Long.MAX_VALUE
        val now = System.currentTimeMillis()
        val expiryMillis = expiryDate.toDate().time
        val diff = (expiryMillis - now) / (1000 * 60 * 60 * 24)
        return diff
    }

    private fun mapToApplication(id: String, data: MutableMap<String, Any>?, defaultType: String): SubmittedApplication {
        val timestampValue = data?.get("timestamp")
        val formattedTimestamp = when (timestampValue) {
            is Timestamp -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestampValue.toDate())
            is String -> timestampValue
            else -> "N/A"
        }

        val appType = (data?.get("applicationType") ?: defaultType) as String
        val issueDate = data?.get("issueDate") as? Timestamp
        val expiryDate = data?.get("expiryDate") as? Timestamp

        return SubmittedApplication(
            id = id,
            companyName = (data?.get("establishmentName") ?: data?.get("companyName") ?: "") as String,
            companyAddress = (data?.get("mailingAddress") ?: data?.get("companyAddress") ?: "") as String,
            pcoName = (data?.get("pcoName") ?: "") as String,
            pcoAccreditation = (data?.get("pcoAccreditation") ?: data?.get("pcoAccreditationNumber") ?: "") as String,
            contactNumber = (data?.get("contactNumber") ?: "-") as String,
            email = (data?.get("email") ?: "-") as String,
            bodyOfWater = (data?.get("bodyOfWater") ?: data?.get("receivingBody") ?: "") as String,
            sourceWastewater = (data?.get("sourceWastewater") ?: "") as String,
            volume = (data?.get("volume") ?: data?.get("dischargeVolume") ?: "") as String,
            treatmentMethod = (data?.get("treatmentMethod") ?: data?.get("dischargeMethod") ?: "") as String,
            operationStartDate = (data?.get("operationStartDate") ?: "") as String,
            fileLinks = (data?.get("fileLinks") as? String)?.split("\n") ?: emptyList(),
            status = (data?.get("status") ?: "Pending") as String,
            paymentInfo = buildPaymentInfo(data),
            timestamp = formattedTimestamp,
            applicationType = appType,
            issueDate = issueDate,
            expiryDate = expiryDate
        )
    }

    private fun buildPaymentInfo(data: MutableMap<String, Any>?): String {
        val amount = (data?.get("amount") as? Number)?.toDouble() ?: 0.0
        val currency = data?.get("currency") as? String ?: "PHP"
        val paymentTimestamp = (data?.get("paymentTimestamp") as? Timestamp)?.toDate()

        val formattedDate = paymentTimestamp?.let {
            SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
        } ?: "Not Paid"

        return "₱%.2f %s\nPaid on: %s".format(amount, currency, formattedDate)
    }

    private fun addOrUpdateApplication(app: SubmittedApplication) {
        val index = applications.indexOfFirst { it.id == app.id }
        if (index >= 0) applications[index] = app else applications.add(app)
        applications.sortByDescending { it.timestamp }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
