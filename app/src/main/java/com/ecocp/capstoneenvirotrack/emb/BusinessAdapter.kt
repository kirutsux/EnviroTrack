package com.ecocp.capstoneenvirotrack.emb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.ecocp.capstoneenvirotrack.R

class BusinessAdapter(private val context: Context, private val businessList: List<Business>) : BaseAdapter() {

    override fun getCount(): Int = businessList.size

    override fun getItem(position: Int): Any = businessList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_business, parent, false)

        val business = businessList[position]

        val logo = view.findViewById<ImageView>(R.id.business_logo)
        val name = view.findViewById<TextView>(R.id.business_name)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        val progressText = view.findViewById<TextView>(R.id.progress_text)

        logo.setImageResource(business.logoResId)
        name.text = business.name
        progressBar.progress = business.progress
        progressText.text = "Progress ${business.progress}%"

        return view
    }
}