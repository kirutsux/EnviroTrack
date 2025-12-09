package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.annotation.SuppressLint
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
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpInboxBinding
import com.ecocp.capstoneenvirotrack.model.InboxItem
import com.ecocp.capstoneenvirotrack.model.Message
import com.ecocp.capstoneenvirotrack.model.PCOMessages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("ClassName")
class SP_Inbox : Fragment() {

    private lateinit var binding: FragmentSpInboxBinding
    private lateinit var adapter: InboxAdapter
    private val pcoList = mutableListOf<InboxItem>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().getReference("chats")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
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
        binding = FragmentSpInboxBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = InboxAdapter(pcoList) { pco ->
            val bundle = Bundle().apply {
                putString("providerId", pco.id)
                putString("providerName", pco.name)
                putString("providerImage", pco.imageUrl)
            }
            findNavController().navigate(R.id.action_spInbox_to_chatFragment, bundle)
        }

        binding.inboxRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.inboxRecyclerView.adapter = adapter

        loadPCOs()
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPCOs() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .whereEqualTo("userType", "pco")
            .get()
            .addOnSuccessListener { result ->
                pcoList.clear()

                for (doc in result) {
                    val pcoId = doc.id
                    val pcoName = doc.getString("firstName") ?: "Unknown"
                    val imageUrl = doc.getString("profileImageUrl") ?: ""
                    val contact = doc.getString("phoneNumber") ?: ""
                    val email = doc.getString("email") ?: ""
                    val address = doc.getString("address") ?: ""

                    val pco = PCOMessages(
                        id = pcoId,
                        name = pcoName,
                        description = "Loading last message...",
                        imageUrl = imageUrl,
                        status = "active",
                        contact = contact,
                        email = email,
                        address = address
                    )

                    pcoList.add(pco)
                    fetchLastMessage(currentUserId, pco)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun fetchLastMessage(currentUserId: String, pco: PCOMessages) {
        val chatId = "${minOf(currentUserId, pco.id)}_${maxOf(currentUserId, pco.id)}"
        val messagesRef = realtimeDb.child(currentUserId).child(chatId).child("messages")

        messagesRef.get().addOnSuccessListener { snapshot ->
            var latestMessage = "No messages yet"
            var latestTimeStamp = ""

            for (msgSnapshot in snapshot.children) {
                val message = msgSnapshot.getValue(Message::class.java)
                if (message != null && message.timestamp > latestTimeStamp) {
                    latestTimeStamp = message.timestamp
                    latestMessage = message.message
                }
            }
            updatePCOLastMessage(pco.id, latestMessage)
        }.addOnFailureListener {
            updatePCOLastMessage(pco.id, "No messages yet")
        }
    }

    private fun updatePCOLastMessage(pcoId: String, message: String) {
        val index = pcoList.indexOfFirst { it.id == pcoId }
        if (index != -1) {
            val current = pcoList[index] as PCOMessages
            pcoList[index] = current.copy(description = message)
            adapter.notifyItemChanged(index)
        }
    }
}
