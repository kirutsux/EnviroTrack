package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.FilesAdapter
import com.ecocp.capstoneenvirotrack.model.UserFile
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class Files : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: MaterialTextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var adapter: FilesAdapter

    private val fullFileList = mutableListOf<UserFile>()
    private val displayedFileList = mutableListOf<UserFile>()

    private val storage = FirebaseStorage.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val folders = listOf(
        "accreditations",
        "accredited_providers",
        "cnc_applications",
        "crs_applications",
        "hazwaste_manifests_generator",
        "opms_discharge_permits",
        "opms_pto_applications",
        "profile_pictures",
        "reports"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files, container, false)

        // 🔙 Toolbar
        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        recyclerView = view.findViewById(R.id.recyclerFiles)
        emptyState = view.findViewById(R.id.tvEmptyState)
        searchEditText = view.findViewById(R.id.etSearchFile)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FilesAdapter(displayedFileList)
        recyclerView.adapter = adapter

        // 🔍 Search Listener
        setupSearchListener()

        fetchAllFiles()

        return view
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFiles(s.toString())
            }
        })
    }

    private fun filterFiles(query: String) {
        val lowerQuery = query.lowercase()

        val filtered = if (lowerQuery.isEmpty()) {
            fullFileList
        } else {
            fullFileList.filter { file ->
                file.name.lowercase().contains(lowerQuery) ||
                        file.category.lowercase().contains(lowerQuery)
            }
        }

        displayedFileList.clear()
        displayedFileList.addAll(filtered)
        adapter.notifyDataSetChanged()
        toggleEmptyState()
    }

    private fun fetchAllFiles() {
        uid?.let { userId ->
            var remainingFolders = folders.size

            folders.forEach { folder ->
                val userFolderRef = storage.reference.child("$folder/$userId")

                userFolderRef.listAll()
                    .addOnSuccessListener { result ->
                        result.items.forEach { item ->
                            item.downloadUrl.addOnSuccessListener { uri ->
                                val file = UserFile(
                                    name = item.name,
                                    url = uri.toString(),
                                    category = folder
                                )
                                fullFileList.add(file)
                                displayedFileList.add(file)
                                adapter.notifyDataSetChanged()
                                toggleEmptyState()
                            }
                        }
                    }
                    .addOnCompleteListener {
                        remainingFolders--
                        if (remainingFolders == 0) toggleEmptyState()
                    }
                    .addOnFailureListener {
                        remainingFolders--
                        if (remainingFolders == 0) toggleEmptyState()
                    }
            }
        }
    }

    private fun toggleEmptyState() {
        emptyState.visibility = if (displayedFileList.isEmpty()) View.VISIBLE else View.GONE
    }
}
