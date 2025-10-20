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
import com.google.firebase.firestore.ktx.toObject

class Inbox : Fragment() {

    private lateinit var binding: FragmentInboxBinding
    private lateinit var adapter: InboxAdapter
    private val providerList = mutableListOf<Provider>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().getReference("chats")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle system back button
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

        // Back button in header
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

        firestore.collection("accredited_providers")
            .get()
            .addOnSuccessListener { result ->
                providerList.clear()
                for (doc in result) {
                    val provider = doc.toObject<Provider>().copy(id = doc.id)
                    val chatId = if (currentUserId < provider.id) {
                        "${currentUserId}_${provider.id}"
                    } else {
                        "${provider.id}_${currentUserId}"
                    }

                    realtimeDb.child(chatId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val lastMsg = snapshot.child("lastMessage").getValue(String::class.java)
                                val updatedProvider = provider.copy(
                                    description = lastMsg ?: "No messages yet"
                                )
                                val index = providerList.indexOfFirst { it.id == provider.id }
                                if (index >= 0) providerList[index] = updatedProvider else providerList.add(updatedProvider)
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
    }
}
