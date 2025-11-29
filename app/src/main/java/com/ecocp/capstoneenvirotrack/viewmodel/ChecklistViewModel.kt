package com.ecocp.capstoneenvirotrack.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ecocp.capstoneenvirotrack.model.ChecklistItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class ChecklistViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val collectionRef = db.collection("checklist_items")

    private val _checklistItems = MutableLiveData<List<ChecklistItem>>(emptyList())
    val checklistItems: LiveData<List<ChecklistItem>> = _checklistItems

    private var listener: ListenerRegistration? = null

    init {
        listenToFirestore()
    }

    /** Listen to Firestore collection changes filtered by current user */
    private fun listenToFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        listener = collectionRef
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChecklistVM", "Firestore listen failed", error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents
                    ?.mapNotNull { it.toObject(ChecklistItem::class.java) }
                    ?: emptyList()
                _checklistItems.value = items
            }
    }

    /** Add a new checklist item with userId */
    fun addItem(name: String, description: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val newItem = ChecklistItem(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            userId = currentUser.uid
        )

        val currentList = _checklistItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(newItem)
        _checklistItems.value = currentList

        collectionRef.document(newItem.id)
            .set(newItem)
            .addOnSuccessListener { Log.d("ChecklistVM", "Item added successfully") }
            .addOnFailureListener { e -> Log.e("ChecklistVM", "Failed to add item", e) }
    }

    /** Update the status of an item and sync to Firestore */
    fun updateItemStatus(itemId: String, newStatus: String) {
        val currentList = _checklistItems.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == itemId }

        if (index != -1) {
            val updatedItem = currentList[index].copy(status = newStatus)
            currentList[index] = updatedItem
            _checklistItems.value = currentList

            collectionRef.document(itemId)
                .update("status", newStatus)
                .addOnSuccessListener { Log.d("ChecklistVM", "Status updated successfully") }
                .addOnFailureListener { e -> Log.e("ChecklistVM", "Failed to update status", e) }
        } else {
            Log.w("ChecklistVM", "Item ID not found: $itemId")
        }
    }

    /** Calculate completion percentage based on Approved items */
    fun getCompletionPercentage(): Int {
        val items = _checklistItems.value ?: return 0
        if (items.isEmpty()) return 0
        val completed = items.count { it.status == "Approved" }
        return (completed / items.size.toFloat() * 100).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
