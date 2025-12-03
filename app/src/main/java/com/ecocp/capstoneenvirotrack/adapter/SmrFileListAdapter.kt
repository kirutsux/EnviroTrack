package com.ecocp.capstoneenvirotrack.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemSmrFileBinding

class SmrFileListAdapter(
    private val onRemove: (String) -> Unit
): RecyclerView.Adapter<SmrFileListAdapter.FileViewHolder>() {

    private val files = mutableListOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newFiles: List<String>){
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemSmrFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(private val binding: ItemSmrFileBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(url: String){
            binding.tvFileName.text = url.substringAfterLast("/")
            binding.btnRemove.setOnClickListener{ onRemove(url) }
        }
    }
}