package com.ecocp.capstoneenvirotrack.repository

import com.ecocp.capstoneenvirotrack.model.BookingUiModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class BookingRepository(private val firestore: FirebaseFirestore) {

    // convenience constructor
    constructor() : this(FirebaseFirestore.getInstance())

    // Role enum
    enum class Role { TSD, TRANSPORTER }

    // Exposed single-document fetch that returns BookingUiModel
    fun fetchBooking(bookingDocId: String, role: Role, callback: (BookingUiModel?) -> Unit) {
        firestore.collection("tsd_bookings")
            .document(bookingDocId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    callback(null)
                    return@addOnSuccessListener
                }
                val model = when (role) {
                    Role.TSD -> mapFromTsd(doc)
                    Role.TRANSPORTER -> mapFromTransporter(doc)
                }
                callback(model)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Mapper for TSD documents
    private fun mapFromTsd(doc: DocumentSnapshot): BookingUiModel {
        val bookingId = doc.getString("bookingId") ?: doc.id
        val facilityId = doc.getString("facilityId") ?: ""
        val status = doc.getString("status") ?: ""
        val updated = doc.getTimestamp("statusUpdatedAt")?.toDate()
        val cert = doc.getString("certificateUrl")
        return BookingUiModel(
            bookingId = bookingId,
            actorId = facilityId,
            status = status,
            statusUpdatedAt = updated,
            certificateUrl = cert,
            isTsdView = true
        )
    }

    // Mapper for Transporter documents (if your transporter docs are in same collection use this mapping)
    private fun mapFromTransporter(doc: DocumentSnapshot): BookingUiModel {
        val bookingId = doc.getString("bookingId") ?: doc.id
        val transporterId = doc.getString("transporterId") ?: ""
        val status = doc.getString("bookingStatus") ?: doc.getString("status") ?: ""
        val updated = doc.getTimestamp("statusUpdatedAt")?.toDate()
        val cert = doc.getString("certificateUrl")
        return BookingUiModel(
            bookingId = bookingId,
            actorId = transporterId,
            status = status,
            statusUpdatedAt = updated,
            certificateUrl = cert,
            isTsdView = false
        )
    }
}
