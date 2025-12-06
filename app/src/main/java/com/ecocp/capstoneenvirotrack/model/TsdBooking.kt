package com.ecocp.capstoneenvirotrack.model

import androidx.datastore.preferences.protobuf.Timestamp

data class TsdBooking(
    val id: String = "",                  // Firestore document ID
    val bookingId: String? = null,        // Your custom booking reference
    val certificateUrl: String? = null,
    val contactNumber: String? = null,
    val dateCreated: String? = null,      // You store this as a String, so no Timestamp conversion
    val facilityId: String? = null,
    val facilityName: String? = null,
    val location: String? = null,
    val preferredDate: String? = null,    // Also stored as String in your schema
    val previousRecordUrl: String? = null,
    val quantity: Double? = null,
    val rate: Double? = null,
    val status: String? = null,           // pending / accepted / rejected / completed
    val totalPayment: Double? = null,
    val treatmentInfo: String? = null,
    val userId: String? = null, // Client user ID
    val generatorId: String? = null,

    val amount: Double? = null,                   // Firestore: amount (number)
    val bookingStatus: String? = null,            // Firestore: bookingStatus (string)
    val collectionProof: List<String>? = null,    // Firestore: array of image/file URLs

    val paymentStatus: String? = null,            // Firestore: "Paid" / "Unpaid"

    val confirmedAt: Timestamp? = null,           // Firestore timestamp
    val confirmedBy: String? = null,

    val statusUpdatedAt: Timestamp? = null,
    val statusUpdatedBy: String? = null,

    val dateBooked: com.google.firebase.Timestamp,// Firestore: timestamp (correct one)

    val tsdBookingId: String? = null,             // Firestore: tsdBookingId
    val tsdId: String? = null,                    // Firestore: tsdId (IMPORTANT)
    val tsdName: String? = null,                  // Firestore: tsdName

    val wasteType: String? = null                 // Firestore: wasteType
)

