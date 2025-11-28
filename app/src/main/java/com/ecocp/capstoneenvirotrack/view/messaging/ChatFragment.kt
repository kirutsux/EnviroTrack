package com.ecocp.capstoneenvirotrack.view.messaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ChatAdapter
import com.ecocp.capstoneenvirotrack.model.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private lateinit var providerImage: ImageView
    private lateinit var providerName: TextView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatRecyclerView: androidx.recyclerview.widget.RecyclerView

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
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Hide bottom nav while chatting
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE

        providerImage = view.findViewById(R.id.providerImage)
        providerName = view.findViewById(R.id.providerName)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)

        val backButton: ImageButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener { findNavController().navigateUp() }

        providerId = arguments?.getString("providerId") ?: ""
        providerImageUrl = arguments?.getString("providerImage") ?: ""
        providerDisplayName = arguments?.getString("providerName") ?: "Provider"

        providerName.text = providerDisplayName
        if (providerImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(providerImageUrl)
                .placeholder(R.drawable.sample_profile)
                .into(providerImage)
        }

        setupRecyclerView()
        setupSendButton()
        listenForMessages()

        return view
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList, auth.currentUser?.uid ?: "")
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            val senderId = auth.currentUser?.uid ?: return@setOnClickListener
            if (messageText.isEmpty()) return@setOnClickListener

            val timestamp = SimpleDateFormat("MM-dd-yy", Locale.getDefault()).format(Date())
            val message = Message(
                senderId = senderId,
                receiverId = providerId,
                message = messageText,
                timestamp = timestamp,
                role = "",
                content = ""
            )

            // References for both sender and receiver
            val senderChatRef = dbRef.child(senderId).child(providerId)
            val receiverChatRef = dbRef.child(providerId).child(senderId)

            // Push message for both sides
            val newMsgKey = senderChatRef.child("messages").push().key ?: return@setOnClickListener
            senderChatRef.child("messages").child(newMsgKey).setValue(message)
            receiverChatRef.child("messages").child(newMsgKey).setValue(message)

            // Update lastMessage
            senderChatRef.child("lastMessage").setValue(messageText)
            receiverChatRef.child("lastMessage").setValue(messageText)

            messageInput.text.clear()
        }
    }

    private fun listenForMessages() {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatRef = dbRef.child(currentUserId).child(providerId).child("messages")

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnapshot in snapshot.children) {
                    msgSnapshot.getValue(Message::class.java)?.let {
                        messageList.add(it)
                    }
                }
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messageList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }
}
