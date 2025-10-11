package com.ecocp.capstoneenvirotrack.view.businesses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.businesses.COMP_Profile
import com.ecocp.capstoneenvirotrack.view.businesses.cnc.COMP_CNC
import com.ecocp.capstoneenvirotrack.view.businesses.hwms.COMP_Hazwaste_Manifest
import com.ecocp.capstoneenvirotrack.view.businesses.opms.COMP_OPMS
import com.ecocp.capstoneenvirotrack.view.businesses.smr.COMP_SMR
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class COMP_Dashboard : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var cncCard: CardView
    private lateinit var smrCard: CardView
    private lateinit var opmsCard: CardView
    private lateinit var hazewasteCard: CardView
    private lateinit var pcoCard: CardView

    // Tracks whether user has accreditation
    private var isAccredited = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pco_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize cards
        cncCard = view.findViewById(R.id.cnc_card)
        smrCard = view.findViewById(R.id.smr_card)
        opmsCard = view.findViewById(R.id.opms_card)
        hazewasteCard = view.findViewById(R.id.hazewaste_card)
        pcoCard = view.findViewById(R.id.pco_card)

        // Check if user already has an accreditation record
        checkUserAccreditation()
    }

    private fun setupCardListeners() {
        // PCO card is always available (used to complete accreditation)
        pcoCard.setOnClickListener {
            navigateToFragment(COMP_PCO())
        }

        if (isAccredited) {
            // ✅ User has accreditation — unlock all cards
            cncCard.setOnClickListener { navigateToFragment(COMP_CNC()) }
            smrCard.setOnClickListener { navigateToFragment(COMP_SMR()) }
            opmsCard.setOnClickListener { navigateToFragment(COMP_OPMS()) }
            hazewasteCard.setOnClickListener { navigateToFragment(COMP_Hazwaste_Manifest()) }
        } else {
            // 🚫 User not accredited — disable other cards
            val lockMessage = "Please complete your accreditation first via the PCO section."

            cncCard.setOnClickListener { Toast.makeText(requireContext(), lockMessage, Toast.LENGTH_SHORT).show() }
            smrCard.setOnClickListener { Toast.makeText(requireContext(), lockMessage, Toast.LENGTH_SHORT).show() }
            opmsCard.setOnClickListener { Toast.makeText(requireContext(), lockMessage, Toast.LENGTH_SHORT).show() }
            hazewasteCard.setOnClickListener { Toast.makeText(requireContext(), lockMessage, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun checkUserAccreditation() {
        val user = auth.currentUser
        if (user == null) {
            Log.e("COMP_Dashboard", "No logged-in user.")
            return
        }

        val userId = user.uid

        // 🔍 Query the "accreditations" collection to find if any document has this UID
        db.collection("accreditations")
            .whereEqualTo("uid", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    isAccredited = true
                    Log.d("COMP_Dashboard", "✅ Accreditation found for user: $userId")
                } else {
                    isAccredited = false
                    Log.d("COMP_Dashboard", "⚠️ No accreditation found — showing dialog.")
                    val dialog = PCOVerificationDialogFragment()
                    dialog.show(childFragmentManager, "PCOVerificationDialog")
                }

                // Always set up the correct behavior for cards after check
                setupCardListeners()
            }
            .addOnFailureListener { e ->
                Log.e("COMP_Dashboard", "Error checking accreditation: ", e)
                Toast.makeText(requireContext(), "Failed to verify accreditation.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, fragment)
            addToBackStack(null)
        }
    }
}
