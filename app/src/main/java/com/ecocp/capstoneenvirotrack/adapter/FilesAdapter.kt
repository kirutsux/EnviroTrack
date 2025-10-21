package com.ecocp.capstoneenvirotrack.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.UserFile

class FilesAdapter(private val fileList: List<UserFile>) :
    RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.tvFileName)
        val fileCategory: TextView = itemView.findViewById(R.id.tvFileCategory)
        val btnView: Button = itemView.findViewById(R.id.btnViewFile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_card, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.fileName.text = file.name
        holder.fileCategory.text = "📁 ${file.category}"

        holder.btnView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = fileList.size
}
