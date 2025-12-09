package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.ItemServiceRequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.google.firebase.firestore.FirebaseFirestore

class ServiceRequestAdapter(
    private val requests: MutableList<ServiceRequest>,
    private val isActiveTasks: Boolean,
    private val onActionClick: (ServiceRequest) -> Unit,
    private var role: String = "transporter" // NEW: role support, default transporter
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Firestore instance for CRS lookup
    private val db = FirebaseFirestore.getInstance()

    // Simple in-memory cache to avoid repeated CRS queries: key = pcoId -> companyName
    // value = null -> not attempted, "" -> attempted but not found, non-empty -> resolved company name
    private val companyCache = mutableMapOf<String, String?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requests.size

    // ⭐ Called by Sorting / Filtering
    fun updateList(newList: List<ServiceRequest>) {
        Log.d(
            "TSD_DEBUG",
            "Adapter.updateList called -> newList.size=${newList.size}. Sample statuses: ${
                newList.take(5).map { it.bookingStatus }.joinToString()
            }"
        )
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }

    // NEW: allow fragment to change role at runtime (call after role detection)
    fun setRole(role: String) {
        this.role = role.lowercase()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.binding.apply {
            // ---------- Build displays ----------
            val pcoId = request.clientName.takeIf { it.isNotBlank() } // pcoId (booked-by)
            val bookedByDisplay = request.clientName.takeIf { it.isNotBlank() }
                ?: request.providerName.takeIf { it.isNotBlank() } ?: "Unknown"

            // middle: waste type
            val wasteDisplay = request.wasteType.takeIf { it.isNotBlank() } ?: "-"

            // provider company (fallback only)
            val providerCompanyFallback = request.companyName.takeIf { it.isNotBlank() } ?: "Unknown Company"

            // show bookedBy as the title
            txtServiceTitle.text = bookedByDisplay

            // show waste immediately and a placeholder for company
            txtCompanyName.text = buildString {
                append(wasteDisplay)
                append("\n")
                append("Loading company...")
            }

            Log.d(
                "ADP_DBG",
                "bind pos=$position id=${request.id} bookedBy='$bookedByDisplay' wasteType='$wasteDisplay' providerCompany='${providerCompanyFallback}' pcoId='${pcoId ?: "null"}'"
            )

            // If pcoId present -> prefer CRS company. Otherwise show provider fallback immediately.
            if (pcoId != null) {
                // check cache first
                val cached = companyCache[pcoId]

                when {
                    // resolved previously -> show CRS company
                    !cached.isNullOrEmpty() -> {
                        val companyShown = cached
                        if (holder.adapterPosition != RecyclerView.NO_POSITION && holder.adapterPosition == position) {
                            txtCompanyName.text = buildString {
                                append(wasteDisplay)
                                append("\n")
                                append(companyShown)
                            }
                        }
                    }

                    // attempted earlier but not found -> show provider fallback
                    cached != null && cached.isEmpty() -> {
                        if (holder.adapterPosition != RecyclerView.NO_POSITION && holder.adapterPosition == position) {
                            txtCompanyName.text = buildString {
                                append(wasteDisplay)
                                append("\n")
                                append(providerCompanyFallback)
                            }
                        }
                    }

                    // cache miss (null): perform CRS lookup and update cache
                    else -> {
                        // mark as attempted with empty string to avoid duplicate simultaneous queries
                        companyCache[pcoId] = ""

                        // 1) Try direct doc read (fast) crs_applications/{pcoId}
                        db.collection("crs_applications").document(pcoId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val crsCompany = doc.getString("companyName")?.takeIf { it.isNotBlank() } ?: ""
                                    companyCache[pcoId] = crsCompany
                                    val companyNow = if (crsCompany.isNotBlank()) crsCompany else providerCompanyFallback
                                    val currentPos = holder.adapterPosition
                                    if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                        txtCompanyName.text = buildString {
                                            append(wasteDisplay)
                                            append("\n")
                                            append(companyNow)
                                        }
                                    }
                                } else {
                                    // 2) Fallback: try query by userId
                                    db.collection("crs_applications")
                                        .whereEqualTo("userId", pcoId)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            val found = snap.documents.firstOrNull()?.getString("companyName")?.takeIf { it.isNotBlank() } ?: ""
                                            if (found.isNotBlank()) {
                                                companyCache[pcoId] = found
                                                val currentPos = holder.adapterPosition
                                                if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                                    txtCompanyName.text = buildString {
                                                        append(wasteDisplay)
                                                        append("\n")
                                                        append(found)
                                                    }
                                                }
                                            } else {
                                                // 3) Next fallback: query by applicationId
                                                db.collection("crs_applications")
                                                    .whereEqualTo("applicationId", pcoId)
                                                    .limit(1)
                                                    .get()
                                                    .addOnSuccessListener { snap2 ->
                                                        val found2 = snap2.documents.firstOrNull()?.getString("companyName")?.takeIf { it.isNotBlank() } ?: ""
                                                        companyCache[pcoId] = found2
                                                        val companyNow = if (found2.isNotBlank()) found2 else providerCompanyFallback
                                                        val currentPos = holder.adapterPosition
                                                        if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                                            txtCompanyName.text = buildString {
                                                                append(wasteDisplay)
                                                                append("\n")
                                                                append(companyNow)
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.w("ADP_DBG", "CRS applicationId lookup failed for pcoId='$pcoId': ${e.message}")
                                                        companyCache[pcoId] = "" // mark attempted & not found
                                                        val currentPos = holder.adapterPosition
                                                        if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                                            txtCompanyName.text = buildString {
                                                                append(wasteDisplay)
                                                                append("\n")
                                                                append(providerCompanyFallback)
                                                            }
                                                        }
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("ADP_DBG", "CRS userId lookup failed for pcoId='$pcoId': ${e.message}")
                                            companyCache[pcoId] = ""
                                            val currentPos = holder.adapterPosition
                                            if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                                txtCompanyName.text = buildString {
                                                    append(wasteDisplay)
                                                    append("\n")
                                                    append(providerCompanyFallback)
                                                }
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("ADP_DBG", "CRS doc read failed for pcoId='$pcoId': ${e.message}")
                                companyCache[pcoId] = ""
                                val currentPos = holder.adapterPosition
                                if (currentPos != RecyclerView.NO_POSITION && currentPos == position) {
                                    txtCompanyName.text = buildString {
                                        append(wasteDisplay)
                                        append("\n")
                                        append(providerCompanyFallback)
                                    }
                                }
                            }
                    }
                }
            } else {
                // no pcoId -> show provider company (booking company)
                txtCompanyName.text = buildString {
                    append(wasteDisplay)
                    append("\n")
                    append(providerCompanyFallback)
                }
            }

            // ---------- status / badge logic (unchanged) ----------
            val statusRaw = request.bookingStatus ?: "Pending"
            val status = statusRaw.lowercase()

            bookingStatus.text = status.replaceFirstChar { it.uppercase() }
            bookingStatus.setBackgroundResource(R.drawable.bg_status_badge)

            val colorRes = when (status) {
                "confirmed", "completed" -> R.color.status_approved
                "rejected" -> R.color.status_rejected
                "pending", "paid" -> R.color.status_pending
                else -> R.color.status_pending
            }

            val badgeColor = ContextCompat.getColor(root.context, colorRes)
            bookingStatus.backgroundTintList = ColorStateList.valueOf(badgeColor)

            // ⭐ Button behavior adapts by role + activeTasks flag
            if (role == "tsd" || role == "tsdfacility" || request.serviceTitle.startsWith("TSD", true)) {
                // TSD view: prefer "Manage" for active tasks, otherwise "View"
                btnView.text = if (isActiveTasks) "Manage" else "View"
            } else {
                // Transporter / default behavior
                btnView.text = if (isActiveTasks) "Update Status" else "View"
            }

            // ⭐ Image (fallback avatar)
            Glide.with(imgClient.context)
                .load(
                    request.imageUrl.ifEmpty {
                        "https://i.pravatar.cc/150?img=3"
                    }
                )
                .circleCrop()
                .into(imgClient)

            // ⭐ Click callback
            btnView.setOnClickListener { onActionClick(request) }
        }
    }
}
