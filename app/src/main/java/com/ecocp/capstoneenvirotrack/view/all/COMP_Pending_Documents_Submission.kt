package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R

class COMP_Pending_Documents_Submission : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.pco_pending_documents_submission,
            container,
            false
        )
    }

}