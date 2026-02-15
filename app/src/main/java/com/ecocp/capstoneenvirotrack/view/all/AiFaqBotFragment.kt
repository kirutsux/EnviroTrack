package com.ecocp.capstoneenvirotrack.view.all

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.api.AskRequest
import com.ecocp.capstoneenvirotrack.api.AskResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class AiFaqBotFragment : Fragment() {

    private lateinit var moduleSpinner: Spinner
    private lateinit var questionEditText: EditText
    private lateinit var askButton: Button
    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var typingText: TextView

    private val modules = listOf("SMR", "OPMS", "CNC", "HWMS", "PCO", "CRS", "Service Provider")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_faq_bot, container, false)

        moduleSpinner = view.findViewById(R.id.moduleSpinner)
        questionEditText = view.findViewById(R.id.questionEditText)
        askButton = view.findViewById(R.id.askButton)
        chatLayout = view.findViewById(R.id.chatLayout)
        scrollView = view.findViewById(R.id.scrollView)

        // Typing indicator
        typingText = TextView(requireContext())
        typingText.text = "Enviro Assistant is typing..."
        typingText.setTextColor(Color.DKGRAY)
        typingText.visibility = View.GONE
        chatLayout.addView(typingText)

        setupSpinner()
        setupButton()

        return view
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modules)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moduleSpinner.adapter = adapter
    }

    private fun setupButton() {
        askButton.setOnClickListener {
            val question = questionEditText.text.toString().trim()
            var module = moduleSpinner.selectedItem.toString().lowercase()

            // Fix naming mismatch for Service Provider and HWMS
            module = when (module) {
                "service provider" -> "service_provider"
                "hwms" -> "hwm" // optional: match backend naming
                else -> module
            }

            if (question.isNotEmpty()) {
                addChatBubble(question, isUser = true)
                questionEditText.setText("")
                callAiAssistant(question, module)
            } else {
                Toast.makeText(requireContext(), "Please type a question", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addChatBubble(text: String, isUser: Boolean) {
        val bubble = TextView(requireContext())
        bubble.text = text
        bubble.setTextColor(Color.BLACK)
        bubble.setPadding(24, 16, 24, 16)
        bubble.textSize = 16f

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 8, 16, 8)

        if (isUser) {
            params.gravity = Gravity.END
            bubble.setBackgroundResource(R.drawable.bg_user_bubble)
        } else {
            params.gravity = Gravity.START
            bubble.setBackgroundResource(R.drawable.bg_ai_bubble)
        }

        bubble.layoutParams = params
        chatLayout.addView(bubble)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    // ----------------- Retrofit AI Call -----------------
    private fun callAiAssistant(question: String, module: String) {
        typingText.visibility = View.VISIBLE
        startTypingAnimation()
        scrollToBottom()

        val request = AskRequest(question, module)

        RetrofitClient.instance.askAI(request)
            .enqueue(object : Callback<AskResponse> {
                override fun onResponse(call: Call<AskResponse>, response: Response<AskResponse>) {
                    stopTypingAnimation()
                    typingText.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val answer = response.body()!!.answer
                        addChatBubble(answer, isUser = false)
                    } else {
                        addChatBubble("Failed to get response", isUser = false)
                    }
                }

                override fun onFailure(call: Call<AskResponse>, t: Throwable) {
                    stopTypingAnimation()
                    typingText.visibility = View.GONE
                    addChatBubble("Error: ${t.message}", isUser = false)
                }
            })
    }

    private fun startTypingAnimation() {
        val alpha = AlphaAnimation(0.3f, 1.0f)
        alpha.duration = 600
        alpha.repeatMode = Animation.REVERSE
        alpha.repeatCount = Animation.INFINITE
        typingText.startAnimation(alpha)
    }

    private fun stopTypingAnimation() {
        typingText.clearAnimation()
    }
}
