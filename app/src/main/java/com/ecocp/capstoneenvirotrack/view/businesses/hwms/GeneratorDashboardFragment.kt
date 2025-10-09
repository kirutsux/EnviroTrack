package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentGeneratorDashboardBinding
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.GeneratorDashboardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class GeneratorDashboardFragment : Fragment() {

    private var _binding: FragmentGeneratorDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: GeneratorDashboardAdapter
    private val applications = mutableListOf<GeneratorApplication>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GeneratorDashboardAdapter(applications)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Swipe-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            fetchApplications()
        }

        // Add button
        binding.btnAddApplication.setOnClickListener {
            navigateToFragment(GeneratorApplicationFragment())
        }

        // Initial load
        fetchApplications()
    }

    private fun fetchApplications() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.swipeRefresh.isRefreshing = true

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", uid)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                applications.clear()

                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No applications found.", Toast.LENGTH_SHORT).show()
                }

                for (doc in snapshot.documents) {
                    // ✅ Handle Timestamp for dateSubmitted
                    val timestamp = doc.get("dateSubmitted")
                    val formattedDate = when (timestamp) {
                        is Timestamp -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(timestamp.toDate())
                        is Date -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(timestamp)
                        is String -> timestamp
                        else -> "N/A"
                    }

                    val app = GeneratorApplication(
                        id = doc.id,
                        companyName = doc.getString("companyName") ?: "N/A",
                        establishmentName = doc.getString("establishmentName") ?: "N/A",
                        pcoName = doc.getString("pcoName") ?: "N/A",
                        natureOfBusiness = doc.getString("natureOfBusiness") ?: "N/A",
                        status = doc.getString("status") ?: "Pending",
                        dateSubmitted = formattedDate
                    )

                    applications.add(app)
                }

                adapter.notifyDataSetChanged()
                binding.swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, fragment)
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ✅ Minimal data model (only for dashboard)
data class GeneratorApplication(
    val id: String,
    val companyName: String,
    val establishmentName: String,
    val pcoName: String,
    val natureOfBusiness: String,
    val status: String,
    val dateSubmitted: String
)
