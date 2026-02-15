package com.ecocp.capstoneenvirotrack.model

data class Provider(
    override val id: String = "",
    override val name: String = "",
    override val description: String = "",
    override val imageUrl: String = "",
    override val status: String = "",
    override val contact: String = "",
    override val email: String = "",
    override val address: String = ""
): InboxItem

data class PCOMessages(
    override val id: String = "",
    override val name: String = "",
    override val description: String = "",
    override val imageUrl: String = "",
    override val status: String = "",
    override val contact: String = "",
    override val email: String = "",
    override val address: String = ""
): InboxItem