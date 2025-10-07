package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class GeneratorApplication(
    var appId: String? = null,
    var uid: String? = null,
    var companyName: String? = null,
    var managingHead: String? = null,
    var establishmentName: String? = null,
    var mobileNumber: String? = null,
    var natureOfBusiness: String? = null,
    var psicNumber: String? = null,
    var dateOfEstablishment: String? = null,
    var noOfEmployees: Int? = null,

    // PCO info
    var pcoName: String? = null,
    var pcoMobileNumber: String? = null,
    var pcoTelephoneNumber: String? = null,
    var pcoAccreditationNumber: String? = null,
    var pcoDateOfAccreditation: String? = null,

    var permits: List<Map<String, String>>? = listOf(),
    var products: List<Map<String, String>>? = listOf(),
    var wastes: List<Map<String, String>>? = listOf(),

    var documents: Map<String, String>? = mapOf(), // key->download URL
    var status: String? = "submitted",
    var submittedAt: Timestamp? = null
)
