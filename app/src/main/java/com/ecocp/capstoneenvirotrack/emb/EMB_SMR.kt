package com.ecocp.capstoneenvirotrack.emb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R


class EMB_SMR : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.emb_smr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val businessList = listOf(
            Business("Dunkin' Donuts", R.drawable.ic_launcher_background, 75),
            Business("McDonald's", R.drawable.ic_launcher_background, 78)
        )

        val listView: ListView = view.findViewById(R.id.business_list)
        listView.adapter = BusinessAdapter(requireContext(), businessList)
    }
}