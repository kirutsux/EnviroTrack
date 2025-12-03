package com.ecocp.capstoneenvirotrack.view.all

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ecocp.capstoneenvirotrack.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import okhttp3.MediaType.Companion.toMediaTypeOrNull

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

            // 🩵 Fix the naming mismatch for Service Provider and HWMS
            module = when (module) {
                "service provider" -> "service_provider"
                "hwms" -> "hwm" // optional, in case your backend uses "hwm" not "hwms"
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
            params.gravity = android.view.Gravity.END
            bubble.setBackgroundResource(R.drawable.bg_user_bubble)
        } else {
            params.gravity = android.view.Gravity.START
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

    private fun callAiAssistant(question: String, module: String) {
        typingText.visibility = View.VISIBLE
        startTypingAnimation()
        scrollToBottom()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val json = JSONObject()
                json.put("question", question)
                json.put("module", module)

                val body = RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    json.toString()
                )

                val request = Request.Builder()
                    .url("http://10.0.2.2:3000/ask")
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                        lifecycleScope.launch(Dispatchers.Main) {
                            stopTypingAnimation()
                            typingText.visibility = View.GONE
                            addChatBubble("Failed to get response", isUser = false)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val respText = response.body?.string()
                        val answer = try {
                            JSONObject(respText).getString("answer")
                        } catch (e: Exception) {
                            "No answer found"
                        }

                        lifecycleScope.launch(Dispatchers.Main) {
                            stopTypingAnimation()
                            typingText.visibility = View.GONE
                            addChatBubble(answer, isUser = false)
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    stopTypingAnimation()
                    typingText.visibility = View.GONE
                    addChatBubble("Error occurred", isUser = false)
                }
            }
        }
    }

    private fun startTypingAnimation() {
        val alpha = AlphaAnimation(0.3f, 1.0f)
        alpha.duration = 600 // milliseconds
        alpha.repeatMode = Animation.REVERSE
        alpha.repeatCount = Animation.INFINITE
        typingText.startAnimation(alpha)
    }

    private fun stopTypingAnimation() {
        typingText.clearAnimation()
    }
}
