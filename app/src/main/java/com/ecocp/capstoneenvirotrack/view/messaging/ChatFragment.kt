package com.ecocp.capstoneenvirotrack.view.messaging

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ChatAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentChatBinding
import com.ecocp.capstoneenvirotrack.model.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("chats")

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()

    private var providerId: String = ""
    private var providerImageUrl: String = ""
    private var providerDisplayName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        // Hide bottom nav while chatting
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE

        providerId = arguments?.getString("providerId") ?: ""
        providerImageUrl = arguments?.getString("providerImage") ?: ""
        providerDisplayName = arguments?.getString("providerName") ?: "Provider"

        binding.providerName.text = providerDisplayName
        if (providerImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(providerImageUrl)
                .placeholder(R.drawable.sample_profile)
                .into(binding.providerImage)
        }

        binding.btnBack.setOnClickListener{ findNavController().navigateUp() }

        setupRecyclerView()
        setupSendButton()
        listenForMessages()

        return binding.root
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList, auth.currentUser?.uid ?: "")
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            val senderId = auth.currentUser?.uid ?: return@setOnClickListener
            if (messageText.isEmpty()) return@setOnClickListener

            val timestamp = SimpleDateFormat("MM-dd-yy", Locale.getDefault()).format(Date())
            val message = Message(
                senderId = senderId,
                receiverId = providerId,
                message = messageText,
                timestamp = timestamp
            )

            val chatId = generateChatId(senderId, providerId)

            val senderChatRef = dbRef.child(senderId).child(chatId).child("messages")
            val receiverChatRef = dbRef.child(providerId).child(chatId).child("messages")

            val newMsgKey = senderChatRef.push().key?:return@setOnClickListener
            senderChatRef.child(newMsgKey).setValue(message)
            receiverChatRef.child(newMsgKey).setValue(message)

            dbRef.child(senderId).child(chatId).child("lastMessage").setValue(messageText)
            dbRef.child(providerId).child(chatId).child("lastMessage").setValue(messageText)

            binding.messageInput.text.clear()
        }
    }

    private fun listenForMessages() {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = generateChatId(currentUserId, providerId)
        val chatRef = dbRef.child(currentUserId).child(chatId).child("messages")

        chatRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnapshot in snapshot.children) {
                    msgSnapshot.getValue(Message::class.java)?.let {
                        messageList.add(it)
                    }
                }
                chatAdapter.notifyDataSetChanged()
                binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        val sortedIds = listOf(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }
}
