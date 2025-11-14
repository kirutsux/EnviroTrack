package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class SP_TaskUpdateDetails : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_task_update_details, container, false)

        val btnSave = view.findViewById<View>(R.id.btnSave)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val btnUpload = view.findViewById<View>(R.id.btnUploadFile)

        btnSave.setOnClickListener {
            Toast.makeText(requireContext(), "Task status updated successfully!", Toast.LENGTH_SHORT).show()
            // TODO: Firestore update logic here
        }

        btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnUpload.setOnClickListener {
            Toast.makeText(requireContext(), "File upload clicked!", Toast.LENGTH_SHORT).show()
            // TODO: Integrate file picker here
        }

        return view
    }
}
