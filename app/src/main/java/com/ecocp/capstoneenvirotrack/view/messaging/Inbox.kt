package com.ecocp.capstoneenvirotrack.view.messaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.InboxAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentInboxBinding
import com.ecocp.capstoneenvirotrack.model.Provider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class Inbox : Fragment() {

    private lateinit var binding: FragmentInboxBinding
    private lateinit var adapter: InboxAdapter
    private val providerList = mutableListOf<Provider>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().getReference("chats")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInboxBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = InboxAdapter(providerList) { provider ->
            val bundle = Bundle().apply {
                putString("providerId", provider.id)
                putString("providerName", provider.name)
                putString("providerImage", provider.imageUrl)
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }

        binding.inboxRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.inboxRecyclerView.adapter = adapter

        loadProviders()
        return binding.root
    }

    private fun loadProviders() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("service_providers")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { result ->
                providerList.clear()

                for (doc in result) {
                    val providerId = doc.getString("uid") ?: continue
                    val providerName = doc.getString("name") ?: "Unknown"
                    val imageUrl = doc.getString("profileImageUrl") ?: ""
                    val contact = doc.getString("contactNumber") ?: ""
                    val email = doc.getString("email") ?: ""
                    val address = doc.getString("location") ?: ""

                    val provider = Provider(
                        id = providerId,
                        name = providerName,
                        description = "Loading last message...",
                        imageUrl = imageUrl,
                        status = "approved",
                        contact = contact,
                        email = email,
                        address = address
                    )

                    providerList.add(provider)
                    fetchLastMessage(currentUserId, provider)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun fetchLastMessage(currentUserId: String, provider: Provider) {
        // Check for both directions: currentUserId/providerId and providerId/currentUserId
        val path1 = realtimeDb.child(currentUserId).child(provider.id)
        val path2 = realtimeDb.child(provider.id).child(currentUserId)

        path1.child("lastMessage").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    updateProviderLastMessage(provider.id, snapshot.value.toString())
                } else {
                    // Try the reversed path
                    path2.child("lastMessage").get()
                        .addOnSuccessListener { reversedSnap ->
                            if (reversedSnap.exists()) {
                                updateProviderLastMessage(provider.id, reversedSnap.value.toString())
                            } else {
                                updateProviderLastMessage(provider.id, "No messages yet")
                            }
                        }
                }
            }
    }

    private fun updateProviderLastMessage(providerId: String, message: String) {
        val index = providerList.indexOfFirst { it.id == providerId }
        if (index != -1) {
            providerList[index] = providerList[index].copy(description = message)
            adapter.notifyItemChanged(index)
        }
    }
}
