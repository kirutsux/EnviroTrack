package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class ServiceRequest(
    val id: String = "",               // SR document id
    val bookingId: String? = null,     // original booking id (optional)

    // Client / Generator
    val clientName: String = "",       // shown on UI as client/generator name

    // Provider
    val companyName: String = "",      // txtCompanyName -> used for filtering
    val providerName: String = "",     // provider contact name / rep
    val providerContact: String = "",  // phone/email

    // Service / status
    val serviceTitle: String = "",     // txtServiceType
    val bookingStatus: String = "Pending",    // txtStatusPill
    val compliance: String = "",       // compliance / short notes

    // Booking details mapped from Booking
    val origin: String = "",           // txtLocation
    val destination: String = "",
    val dateRequested: String = "",
    val dateBooked: Timestamp? = null,

    // Waste specifics
    val wasteType: String = "",
    val quantity: String = "",
    val packaging: String = "",

    // Notes / instructions
    val notes: String = "",            // txtNotes (specialInstructions)

    // Attachments / image
    val attachments: List<String>? = null, // list of file urls or local paths
    val imageUrl: String = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png" // logo / preview (dev path)
)
