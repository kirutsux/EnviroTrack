package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class Booking(
    val bookingId: String? = null,
    val bookingDate: Timestamp? = null,
    val dateBooked: Timestamp? = null,
    val destination: String? = null,
    val generatorId: String? = null,
    val origin: String? = null,
    val packaging: String? = null,
    val pcoId: String? = null,
    val providerContact: String? = null,
    val providerType: String? = null,
    val quantity: String? = null,
    val serviceProviderCompany: String? = null,
    val serviceProviderName: String? = null,
    val specialInstructions: String? = null,
    val status: String? = null,
    val wasteType: String? = null
)
