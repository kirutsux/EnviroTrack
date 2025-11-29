package com.ecocp.capstoneenvirotrack.model

data class ChecklistItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "Pending", // Pending, Submitted, Approved, Rejected
    val dueDate: String = "", // format: YYYY-MM-DD
    val submissionDate: String? = null,
    val userId: String = "" // Add this line
)
