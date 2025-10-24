package com.ecocp.capstoneenvirotrack.model

data class Booking(
    val bookingDate: com.google.firebase.Timestamp? = null,
    val dateBooked: com.google.firebase.Timestamp? = null,
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
