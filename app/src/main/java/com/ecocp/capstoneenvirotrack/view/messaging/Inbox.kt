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

        loadInbox()
        return binding.root
    }

    private fun loadInbox() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userChatsRef = realtimeDb.child(currentUserId)

        userChatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                providerList.clear()

                for (providerChatSnapshot in snapshot.children) {
                    val providerId = providerChatSnapshot.key ?: continue
                    val lastMsg = providerChatSnapshot.child("lastMessage").getValue(String::class.java) ?: "No messages yet"

                    firestore.collection("accredited_providers").document(providerId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val provider = doc.toObject<Provider>()?.copy(
                                id = providerId,
                                description = lastMsg
                            )
                            provider?.let {
                                if (providerList.none { p -> p.id == providerId }) {
                                    providerList.add(it)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
