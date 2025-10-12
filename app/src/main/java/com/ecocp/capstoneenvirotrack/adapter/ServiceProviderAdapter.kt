package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.ServiceProvider

class ServiceProviderAdapter(
    private val providers: List<ServiceProvider>,
    private val onBookClick: (ServiceProvider) -> Unit
) : RecyclerView.Adapter<ServiceProviderAdapter.ServiceProviderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_provider, parent, false)
        return ServiceProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceProviderViewHolder, position: Int) {
        val provider = providers[position]
        holder.bind(provider)
        holder.btnBook.setOnClickListener { onBookClick(provider) }
    }

    override fun getItemCount(): Int = providers.size

    class ServiceProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvProviderName)
        private val tvCompany: TextView = itemView.findViewById(R.id.tvProviderCompany)
        private val tvType: TextView = itemView.findViewById(R.id.tvProviderType)
        private val tvContact: TextView = itemView.findViewById(R.id.tvProviderContact)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvProviderEmail)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvProviderAddress)
        val btnBook: Button = itemView.findViewById(R.id.btnBookProvider)

        fun bind(provider: ServiceProvider) {
            tvName.text = provider.name
            tvCompany.text = provider.companyName
            tvType.text = "Type: ${provider.type}"
            tvContact.text = "Contact: ${provider.contactNumber}"
            tvEmail.text = "Email: ${provider.email}"
            tvAddress.text = "Address: ${provider.address}"
        }
    }
}
