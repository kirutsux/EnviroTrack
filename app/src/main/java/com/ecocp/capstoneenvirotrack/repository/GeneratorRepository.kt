package com.ecocp.capstoneenvirotrack.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class GeneratorRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Submits a generator application:
     * - uploads all required document URIs to Firebase Storage
     * - saves application data to Firestore
     */
    fun submitGeneratorApplication(
        uid: String,
        payload: Map<String, Any>,
        docUris: Map<String, Uri>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val appId = UUID.randomUUID().toString()
        val storageRoot = storage.reference.child("hazwaste_manifests_generator/$uid/$appId")

        // Step 1: Upload all required documents in parallel
        val uploadedUrls = mutableMapOf<String, String>()
        val total = docUris.size
        var successCount = 0
        var failed = false

        for ((key, uri) in docUris) {
            val fileRef = storageRoot.child("$key-${System.currentTimeMillis()}.pdf")

            fileRef.putFile(uri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { url ->
                        uploadedUrls[key] = url.toString()
                        successCount++
                        if (successCount == total && !failed) {
                            // Step 2: All files uploaded — now save Firestore doc
                            saveApplication(uid, appId, payload, uploadedUrls, onSuccess, onFailure)
                        }
                    }.addOnFailureListener { e ->
                        if (!failed) {
                            failed = true
                            onFailure(e)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (!failed) {
                        failed = true
                        onFailure(e)
                    }
                }
        }
    }

    /**
     * Writes the generator application data to Firestore
     */
    private fun saveApplication(
        uid: String,
        appId: String,
        payload: Map<String, Any>,
        fileUrls: Map<String, String>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val appData = mutableMapOf<String, Any>(
            "applicationId" to appId,
            "userId" to uid,
            "status" to "Submitted",
            "submittedAt" to FieldValue.serverTimestamp(),
            "documents" to fileUrls
        )
        appData.putAll(payload)

        db.collection("HazardousWasteGenerator").document(appId)
            .set(appData)
            .addOnSuccessListener { onSuccess(appId) }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
