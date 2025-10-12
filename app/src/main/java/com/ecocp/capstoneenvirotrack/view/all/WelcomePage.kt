package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class WelcomePage : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment that contains your Login and Register buttons
        return inflater.inflate(R.layout.welcome_page, container, false) // Ensure this layout exists
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogin: Button = view.findViewById(R.id.btnLogin) // Ensure this ID is in fragment_welcome.xml
        val btnRegister: Button = view.findViewById(R.id.btnRegister) // Ensure this ID is in fragment_welcome.xml

        btnLogin.setOnClickListener {
            // Use NavController to navigate to LoginFragment
            // Make sure R.id.action_welcomeFragment_to_loginFragment is defined in your nav graph
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }

        btnRegister.setOnClickListener {
            // Use NavController to navigate to RegistrationFragment
            // Make sure R.id.action_welcomeFragment_to_registrationFragment is defined in your nav graph
            findNavController().navigate(R.id.action_welcomeFragment_to_registrationFragment)
        }
    }
}