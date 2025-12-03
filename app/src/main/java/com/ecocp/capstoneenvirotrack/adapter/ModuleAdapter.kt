package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R

class ModuleAdapter(
    private val modules: List<Pair<String, String>>,
    private val onItemClick: (String, String) -> Unit
): RecyclerView.Adapter<ModuleAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModuleName: TextView = view.findViewById(R.id.tvModuleName)
        val card: CardView = view.findViewById(R.id.cardModule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val(name,_) = modules[position]
        holder.tvModuleName.text = name
        holder.card.setOnClickListener { onItemClick(name, modules[position].second) }
    }

    override fun getItemCount(): Int = modules.size
}