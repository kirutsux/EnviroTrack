package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap

data class PaymentRecord(
    val amount: Double = 0.0,
    val payerId: String = "",
    val method: String = "",
    val reference: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "completed"
)

class SP_Payments : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI
    private lateinit var tvCollectedBalance: TextView

    // booking-specific views can be absent in history layout -> make nullable to avoid NPE
    private var tvBookingTitle: TextView? = null
    private var tvBookingTotal: TextView? = null
    private var tvPaidAmount: TextView? = null
    private var tvBalance: TextView? = null

    private lateinit var paymentsContainer: LinearLayout

    // Optional TSD section view (hidden for TRANSPORTER)
    private var tsdSection: View? = null

    // Role: "TRANSPORTER" or "TSD" — change this as needed elsewhere in your app
    // made var because we set it after detection
    private var userRole = "TRANSPORTER"

    // Collection name will depend on role
    private val collectionName: String
        get() = if (userRole == "TRANSPORTER") "transport_bookings" else "tsd_bookings"

    private var bookingId: String? = null
    private var transportBalanceListener: ListenerRegistration? = null
    private var tsdBalanceListener: ListenerRegistration? = null

    // small cache for user names to reduce reads
    private val userNameCache = HashMap<String, String>()
    private val HISTORY_TAG = "SP_PAY_HISTORY"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_sp__payments, container, false)

        // Bind Views (IDs unchanged)
        tvCollectedBalance = view.findViewById(R.id.tvCollectedBalance)

        // Bind booking-specific views if present in the layout (safe)

        paymentsContainer = view.findViewById(R.id.payments_list_container)

        // optional tsd section (may be absent in layout)

        // hide TSD-only UI when transporter is using this screen (will adjust after detection)
        if (userRole == "TRANSPORTER") {
            tsdSection?.visibility = View.GONE
        } else {
            tsdSection?.visibility = View.VISIBLE
        }

        // Back button (if present in layout)
        view.findViewById<ImageButton?>(R.id.btnBack)?.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // BookingId optional from args
        bookingId = arguments?.getString("bookingId")

        // If viewing a specific booking, load booking-level details/payments (these guard on null)
        loadBookingDetails()
        loadBookingPayments()

        // Detect user role and initialize appropriate listeners & history loaders
        detectUserRoleAndInit()

        return view
    }

    // ----------------------------
    // Hide bottom navigation while Payments is visible
    // ----------------------------
    override fun onResume() {
        super.onResume()
        try {
            val bottomNav = requireActivity().findViewById<View>(R.id.spBottomNavigation)
            bottomNav?.visibility = View.GONE
        } catch (e: Exception) {
            Log.w(HISTORY_TAG, "Failed to hide bottom navigation: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val bottomNav = requireActivity().findViewById<View>(R.id.spBottomNavigation)
            bottomNav?.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.w(HISTORY_TAG, "Failed to show bottom navigation: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        transportBalanceListener?.remove()
        tsdBalanceListener?.remove()
    }

    // ----------------------------
    // Booking details (per booking)
    // ----------------------------
    private fun loadBookingDetails() {
        val bid = bookingId ?: return
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        db.collection(collectionName)
            .document(bid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val total = doc.getDouble("amount")
                        ?: doc.getLong("amount")?.toDouble()
                        ?: 0.0

                    val paid = doc.getDouble("paidAmount")
                        ?: doc.getLong("paidAmount")?.toDouble()
                        ?: 0.0

                    tvBookingTitle?.text = "Booking: $bid"
                    tvBookingTotal?.text = "Total: ${nf.format(total)}"
                    tvPaidAmount?.text = "Paid: ${nf.format(paid)}"
                    tvBalance?.text = "Balance: ${nf.format(total - paid)}"
                } else {
                    tvBookingTitle?.text = "Booking: $bid (not found)"
                }
            }
            .addOnFailureListener { e ->
                tvBookingTitle?.text = "Booking: $bid"
                Log.e(HISTORY_TAG, "loadBookingDetails failed: ${e.message}", e)
            }
    }

    /**
     * Detect role using service_providers collection first (by uid). Falls back to users/{uid}.
     * If role contains "TSD" (case-insensitive) it becomes TSD; otherwise TRANSPORTER.
     */
    private fun detectUserRoleAndInit() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // no signed-in user — keep TRANSPORTER as fallback
            userRole = "TRANSPORTER"
            startCollectedBalanceListener()
            loadTransportBookingsHistory()
            return
        }

        // Try service_providers lookup first
        db.collection("service_providers")
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { spSnap ->
                if (spSnap != null && !spSnap.isEmpty) {
                    val spDoc = spSnap.documents[0]
                    val spRole = spDoc.getString("role") ?: spDoc.getString("type") ?: ""
                    if (spRole.contains("TSD", ignoreCase = true)) {
                        userRole = "TSD"
                        startTsdCollectedBalanceListener()
                        loadTsdBookingsHistory()
                    } else {
                        userRole = "TRANSPORTER"
                        startCollectedBalanceListener()
                        loadTransportBookingsHistory()
                    }
                } else {
                    // fallback to users collection if no service_providers record present
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val roleFromDoc = userDoc?.getString("role")
                                ?: userDoc?.getString("userType")
                                ?: userDoc?.getString("type")
                            userRole = if (!roleFromDoc.isNullOrBlank() &&
                                (roleFromDoc.equals("TSD", ignoreCase = true) || roleFromDoc.equals("tsd", ignoreCase = true))
                            ) {
                                "TSD"
                            } else {
                                "TRANSPORTER"
                            }

                            if (userRole == "TSD") {
                                startTsdCollectedBalanceListener()
                                loadTsdBookingsHistory()
                            } else {
                                startCollectedBalanceListener()
                                loadTransportBookingsHistory()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w(HISTORY_TAG, "Role fallback detection failed: ${e.message}. Defaulting to TRANSPORTER")
                            userRole = "TRANSPORTER"
                            startCollectedBalanceListener()
                            loadTransportBookingsHistory()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(HISTORY_TAG, "service_providers lookup failed: ${e.message}. Falling back to users/{uid}")
                // fallback same as above
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val roleFromDoc = userDoc?.getString("role")
                            ?: userDoc?.getString("userType")
                            ?: userDoc?.getString("type")
                        userRole = if (!roleFromDoc.isNullOrBlank() &&
                            (roleFromDoc.equals("TSD", ignoreCase = true) || roleFromDoc.equals("tsd", ignoreCase = true))
                        ) {
                            "TSD"
                        } else {
                            "TRANSPORTER"
                        }

                        if (userRole == "TSD") {
                            startTsdCollectedBalanceListener()
                            loadTsdBookingsHistory()
                        } else {
                            startCollectedBalanceListener()
                            loadTransportBookingsHistory()
                        }
                    }
                    .addOnFailureListener { e2 ->
                        Log.w(HISTORY_TAG, "Fallback users lookup failed: ${e2.message}. Defaulting to TRANSPORTER")
                        userRole = "TRANSPORTER"
                        startCollectedBalanceListener()
                        loadTransportBookingsHistory()
                    }
            }
    }

    // ----------------------------
    // Payment history (per booking)
    // ----------------------------
    private fun loadBookingPayments() {
        val bid = bookingId ?: return
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        db.collection(collectionName)
            .document(bid)
            .collection("payments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                paymentsContainer.removeAllViews()
                if (snap != null && !snap.isEmpty) {
                    for (doc in snap.documents) {
                        val amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0
                        val method = doc.getString("method") ?: "Unknown"
                        val reference = doc.getString("reference") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")

                        val row = TextView(requireContext()).apply {
                            text = "${nf.format(amount)} • $method${if (reference.isNotBlank()) " • $reference" else ""}\n${
                                timestamp?.toDate()?.let {
                                    DateFormat.format("yyyy-MM-dd hh:mm a", it)
                                } ?: ""
                            }"
                            textSize = 14f
                            setPadding(12, 16, 12, 16)
                        }
                        paymentsContainer.addView(row)
                    }
                } else {
                    // show empty state text
                    val tv = TextView(requireContext()).apply {
                        text = "No payments recorded for this booking."
                        setPadding(12, 12, 12, 12)
                        textSize = 14f
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    paymentsContainer.addView(tv)
                }
            }
            .addOnFailureListener { e ->
                Log.e(HISTORY_TAG, "loadBookingPayments failed: ${e.message}", e)
            }
    }

    // ------------------------------------------------------
    // SUM collected balance across all transport bookings (TRANSPORTER)
    // ------------------------------------------------------
    private fun startCollectedBalanceListener() {
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        tvCollectedBalance.text = "Loading..."

        transportBalanceListener = db.collection("transport_bookings")
            .whereEqualTo("paymentStatus", "Paid")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    tvCollectedBalance.text = nf.format(0.0)
                    Log.e(HISTORY_TAG, "startCollectedBalanceListener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                var total = 0.0
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val amt = doc.get("amount")
                        val value = when (amt) {
                            is Double -> amt
                            is Long -> amt.toDouble()
                            is Int -> amt.toDouble()
                            is String -> amt.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        total += value
                    }
                }
                tvCollectedBalance.text = nf.format(total)
            }
    }

    // ----------------------------------------
    // Render a single history row into container
    // ----------------------------------------
    private fun renderHistoryRow(
        container: LinearLayout,
        bookingId: String,
        reference: String,
        payerName: String,
        amountText: String,
        dateText: String
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params

        }

        val top = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvName = TextView(requireContext()).apply {
            text = payerName
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvAmount = TextView(requireContext()).apply {
            text = amountText
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.holo_green_dark))
        }

        top.addView(tvName)
        top.addView(tvAmount)

        val tvRef = TextView(requireContext()).apply {
            text = if (reference.isNotBlank()) "Ref: $reference • ID: $bookingId" else "ID: $bookingId"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray))
        }

        val tvDate = TextView(requireContext()).apply {
            text = dateText
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        }

        row.addView(top)
        row.addView(tvRef)
        row.addView(tvDate)

        container.addView(row)
    }

    // ----------------------------------------
    // fetch user name helper
    // ----------------------------------------
    private fun fetchUserNameAndRenderRow(
        userId: String,
        bookingId: String,
        reference: String,
        amount: Double,
        nf: NumberFormat,
        container: LinearLayout,
        dateText: String
    ) {
        // check name cache first
        val cached = userNameCache[userId]
        if (cached != null) {
            renderHistoryRow(container, bookingId, reference, cached, nf.format(amount), dateText)
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc?.getString("displayName")
                    ?: userDoc?.getString("name")
                    ?: userDoc?.getString("fullName")
                    ?: userId

                userNameCache[userId] = name
                renderHistoryRow(container, bookingId, reference, name, nf.format(amount), dateText)
            }
            .addOnFailureListener {
                renderHistoryRow(container, bookingId, reference, userId, nf.format(amount), dateText)
            }
    }

    /**
     * Lists transport_bookings with payment info for TRANSPORTER view.
     * Shows bookingId, payment reference (if any), payer name, and amount.
     */
    private fun loadTransportBookingsHistory() {
        val container = paymentsContainer
        container.removeAllViews()
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        // Query paid transport bookings (no orderBy on server to avoid composite index)
        db.collection("transport_bookings")
            .whereEqualTo("paymentStatus", "Paid")
            .get()
            .addOnSuccessListener { snap ->
                if (snap == null || snap.isEmpty) {
                    val tv = TextView(requireContext()).apply {
                        text = "No paid transport bookings yet."
                        setPadding(12, 12, 12, 12)
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    container.addView(tv)
                    return@addOnSuccessListener
                }

                // Sort locally by dateBooked (desc)
                val docs = snap.documents.toMutableList()
                docs.sortWith(compareByDescending { doc ->
                    val ts = doc.getTimestamp("dateBooked")
                    ts?.toDate()?.time ?: 0L
                })

                // For each booking, skip zero-amount records and then get latest payment (to obtain reference/payer) then render row
                var anyRendered = false
                for (bookingDoc in docs) {
                    val bookingDocId = bookingDoc.id
                    val bookingId = bookingDoc.getString("bookingId") ?: bookingDocId

                    // extract amount (booking-level)
                    val amountVal = bookingDoc.get("amount")
                    val amount = when (amountVal) {
                        is Double -> amountVal
                        is Long -> amountVal.toDouble()
                        is Int -> amountVal.toDouble()
                        is String -> amountVal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    // SKIP bookings with zero (or negative) amount
                    if (amount <= 0.0) {
                        continue
                    }

                    anyRendered = true

                    // dateBooked formatted
                    val dateBookedTs = bookingDoc.getTimestamp("dateBooked")
                    val dateBookedText = dateBookedTs?.toDate()?.let {
                        DateFormat.format("yyyy-MM-dd • hh:mm a", it).toString()
                    } ?: "No date"

                    // fallback name if no payer found
                    val fallbackName = bookingDoc.getString("primaryWasteGeneratorId")
                        ?: bookingDoc.getString("providerId") ?: "Unknown"

                    // fetch latest payment in payments subcollection (limit 1)
                    val paymentsCol = db.collection("transport_bookings")
                        .document(bookingDocId)
                        .collection("payments")

                    paymentsCol.orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                        .get()
                        .addOnSuccessListener { pSnap ->
                            var referenceDisplay = ""
                            var payerIdFromPayment: String? = null

                            if (pSnap != null && !pSnap.isEmpty) {
                                val p = pSnap.documents[0]
                                referenceDisplay = p.getString("reference") ?: ""
                                payerIdFromPayment = p.getString("payerId") ?: p.getString("payer") ?: p.getString("userId")
                            }

                            if (!payerIdFromPayment.isNullOrBlank()) {
                                // fetch user name then render with date
                                fetchUserNameAndRenderRow(
                                    payerIdFromPayment,
                                    bookingId,
                                    referenceDisplay,
                                    amount,
                                    nf,
                                    container,
                                    dateBookedText
                                )
                            } else {
                                // try primary waste generator id
                                val primaryId = bookingDoc.getString("primaryWasteGeneratorId")
                                if (!primaryId.isNullOrBlank()) {
                                    fetchUserNameAndRenderRow(primaryId, bookingId, referenceDisplay, amount, nf, container, dateBookedText)
                                } else {
                                    // render with fallback name
                                    renderHistoryRow(container, bookingId, referenceDisplay, fallbackName, nf.format(amount), dateBookedText)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            // if we can't read payments, render the booking with fallback name
                            renderHistoryRow(container, bookingId, "", fallbackName, nf.format(amount), dateBookedText)
                        }
                }

                if (!anyRendered) {
                    val tv = TextView(requireContext()).apply {
                        text = "No paid transport bookings with amount > 0."
                        setPadding(12, 12, 12, 12)
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    container.addView(tv)
                }
            }
            .addOnFailureListener { e ->
                Log.e(HISTORY_TAG, "loadTransportBookingsHistory failed: ${e.message}", e)
                val tv = TextView(requireContext()).apply {
                    text = "Failed to load history."
                    setPadding(12, 12, 12, 12)
                    setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
                container.addView(tv)
            }
    }

    // TSD: Render a TSD-specific history row
    // ----------------------------
    // TSD: Render a TSD-specific history row (status hidden when "pending")
    private fun renderTsdHistoryRow(
        container: LinearLayout,
        tsdBookingId: String,
        generatorName: String,
        tsdName: String,
        amountText: String,
        dateText: String,
        statusText: String
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }

        val top = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val left = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvGenerator = TextView(requireContext()).apply {
            text = generatorName
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.black))
        }

        val tvMeta = TextView(requireContext()).apply {
            text = "$tsdName • ID: $tsdBookingId"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray))
        }

        left.addView(tvGenerator)
        left.addView(tvMeta)

        val tvAmount = TextView(requireContext()).apply {
            text = amountText
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.holo_green_dark))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        top.addView(left)
        top.addView(tvAmount)

        val tvDate = TextView(requireContext()).apply {
            text = dateText
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        }

        // Only show status when it's not blank and not "pending"
        val statusTrim = statusText?.trim() ?: ""
        if (statusTrim.isNotEmpty() && !statusTrim.equals("pending", ignoreCase = true)) {
            val tvStatus = TextView(requireContext()).apply {
                text = statusTrim
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            }
            row.addView(top)
            row.addView(tvStatus)
        } else {
            // if status is pending or empty, just add top without status
            row.addView(top)
        }

        row.addView(tvDate)

        container.addView(row)
    }

    // ----------------------------
    // TSD: fetch generator name then render
    // ----------------------------
    private fun fetchGeneratorNameAndRenderRowTsd(
        generatorId: String,
        tsdBookingId: String,
        tsdName: String,
        amount: Double,
        nf: NumberFormat,
        container: LinearLayout,
        dateText: String,
        statusText: String
    ) {
        // quick cache lookup
        val cached = userNameCache[generatorId]
        if (cached != null) {
            renderTsdHistoryRow(container, tsdBookingId, cached, tsdName, nf.format(amount), dateText, statusText)
            return
        }

        db.collection("users").document(generatorId)
            .get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc?.getString("displayName")
                    ?: userDoc?.getString("name")
                    ?: generatorId
                userNameCache[generatorId] = name
                renderTsdHistoryRow(container, tsdBookingId, name, tsdName, nf.format(amount), dateText, statusText)
            }
            .addOnFailureListener {
                renderTsdHistoryRow(container, tsdBookingId, generatorId, tsdName, nf.format(amount), dateText, statusText)
            }
    }

    // ----------------------------
    // TSD: load TSD bookings history (paid ones)
    // - shows dateBooked (or timestamp), tsdName, generator name, amount, and status
    // ----------------------------
    private fun loadTsdBookingsHistory() {
        // only run when userRole is TSD
        if (userRole != "TSD") return

        val container = paymentsContainer
        container.removeAllViews()
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        db.collection("tsd_bookings")
            .whereEqualTo("paymentStatus", "Paid")
            .get()
            .addOnSuccessListener { snap ->
                if (snap == null || snap.isEmpty) {
                    val tv = TextView(requireContext()).apply {
                        text = "No paid TSD bookings yet."
                        setPadding(12, 12, 12, 12)
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    container.addView(tv)
                    return@addOnSuccessListener
                }

                // sort by timestamp (descending) if available
                val docs = snap.documents.toMutableList()
                docs.sortWith(compareByDescending { d ->
                    val ts = d.getTimestamp("timestamp") ?: d.getTimestamp("dateBooked")
                    ts?.toDate()?.time ?: 0L
                })

                for (doc in docs) {
                    val tsdBookingId = doc.getString("tsdBookingId") ?: doc.id
                    val tsdName = doc.getString("tsdName") ?: "TSD"
                    val generatorId = doc.getString("generatorId") ?: ""

                    // prefer explicit 'amount' field; fallback to rate * quantity
                    val amount = when (val a = doc.get("amount")) {
                        is Double -> a
                        is Long -> a.toDouble()
                        is Int -> a.toDouble()
                        is String -> a.toDoubleOrNull() ?: 0.0
                        else -> {
                            val rate = (doc.get("rate") as? Number)?.toDouble() ?: 0.0
                            val qty = (doc.get("quantity") as? Number)?.toDouble() ?: 0.0
                            if (rate > 0.0 && qty > 0.0) rate * qty else 0.0
                        }
                    }

                    // skip zero amounts (if you want)
                    if (amount <= 0.0) continue

                    val statusText = doc.getString("status") ?: doc.getString("paymentStatus") ?: "Unknown"

                    // timestamp/date display
                    val ts = doc.getTimestamp("timestamp") ?: doc.getTimestamp("dateBooked")
                    val dateText = ts?.toDate()?.let {
                        DateFormat.format("yyyy-MM-dd • hh:mm a", it).toString()
                    } ?: (doc.getString("preferredDate") ?: "No date")

                    if (generatorId.isNotBlank()) {
                        fetchGeneratorNameAndRenderRowTsd(generatorId, tsdBookingId, tsdName, amount, nf, container, dateText, statusText)
                    } else {
                        // no generator id — fall back to text in booking (if exists)
                        val genNameFallback = doc.getString("contactNumber") ?: "Generator"
                        renderTsdHistoryRow(container, tsdBookingId, genNameFallback, tsdName, nf.format(amount), dateText, statusText)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(HISTORY_TAG, "loadTsdBookingsHistory failed: ${e.message}", e)
                val tv = TextView(requireContext()).apply {
                    text = "Failed to load TSD history."
                    setPadding(12, 12, 12, 12)
                    setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
                container.addView(tv)
            }
    }

    // ----------------------------
    // TSD: real-time collected balance listener
    // ----------------------------
    private fun startTsdCollectedBalanceListener() {
        if (userRole != "TSD") return

        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        tvCollectedBalance.text = "Loading..."

        tsdBalanceListener = db.collection("tsd_bookings")
            .whereEqualTo("paymentStatus", "Paid")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    tvCollectedBalance.text = nf.format(0.0)
                    Log.e(HISTORY_TAG, "startTsdCollectedBalanceListener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                var total = 0.0
                if (snap != null) {
                    for (doc in snap.documents) {
                        val amt = doc.get("amount")
                        val value = when (amt) {
                            is Double -> amt
                            is Long -> amt.toDouble()
                            is Int -> amt.toDouble()
                            is String -> amt.toDoubleOrNull() ?: 0.0
                            else -> {
                                val rate = (doc.get("rate") as? Number)?.toDouble() ?: 0.0
                                val qty = (doc.get("quantity") as? Number)?.toDouble() ?: 0.0
                                if (rate > 0.0 && qty > 0.0) rate * qty else 0.0
                            }
                        }
                        total += value
                    }
                }
                tvCollectedBalance.text = nf.format(total)
            }
    }

    // ----------------------------
    // Utility: single-shot load (if needed)
    // ----------------------------
    private fun loadCollectedBalanceOnce() {
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        tvCollectedBalance.text = "Loading..."

        db.collection("transport_bookings")
            .whereEqualTo("paymentStatus", "Paid")
            .get()
            .addOnSuccessListener { snap ->
                var total = 0.0
                for (doc in snap.documents) {
                    val amt = doc.get("amount")
                    val value = when (amt) {
                        is Double -> amt
                        is Long -> amt.toDouble()
                        is Int -> amt.toDouble()
                        is String -> amt.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    total += value
                }
                tvCollectedBalance.text = nf.format(total)
            }
            .addOnFailureListener {
                tvCollectedBalance.text = nf.format(0.0)
            }
    }
}
