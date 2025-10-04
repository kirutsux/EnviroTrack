package com.ecocp.capstoneenvirotrack.view.businesses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R

class PCOVerificationDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pco_verification_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in))

        val buttonOkay = view.findViewById<Button>(R.id.buttonOkay)
        val buttonLater = view.findViewById<Button>(R.id.buttonLater)

        buttonOkay.setOnClickListener {
            Toast.makeText(requireContext(), "Proceeding to PCO accreditation", Toast.LENGTH_SHORT).show()
            dismiss()
            findNavController().navigate(R.id.action_COMP_Dashboard_to_COMP_PCOAccreditation)
        }

        buttonLater.setOnClickListener {
            Toast.makeText(requireContext(), "You can verify your PCO accreditation later from settings", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}