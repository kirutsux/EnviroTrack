package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ServiceProviderAdapter
import com.ecocp.capstoneenvirotrack.model.ServiceProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TransporterStep2Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val providers = mutableListOf<ServiceProvider>()
    private lateinit var adapter: ServiceProviderAdapter
    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_transporter_step2, container, false)
        recycler = v.findViewById(R.id.recyclerViewTransporters)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = ServiceProviderAdapter(providers) { provider ->
            // provider clicked -> show booking dialog
            showBookingDialog(provider)
        }
        recycler.adapter = adapter

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        fetchTransporters()
        return v
    }

    private fun fetchTransporters() {
        progressDialog.show()
        db.collection("serviceproviders")
            .whereEqualTo("type", "Transporter")
            .get()
            .addOnSuccessListener { snap ->
                progressDialog.dismiss()
                providers.clear()
                providers.addAll(snap.toObjects(ServiceProvider::class.java))
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to load service providers: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showBookingDialog(provider: ServiceProvider) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transporter_booking, null)
        val tvProviderTitle = dialogView.findViewById<TextView>(R.id.tvProviderTitle)
        val etWasteType = dialogView.findViewById<EditText>(R.id.etWasteType)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPackaging = dialogView.findViewById<EditText>(R.id.etPackaging)
        val etOrigin = dialogView.findViewById<EditText>(R.id.etOrigin)
        val etDestination = dialogView.findViewById<EditText>(R.id.etDestination)
        val etSpecial = dialogView.findViewById<EditText>(R.id.etSpecialInstructions)
        val btnPickDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val tvDateSelected = dialogView.findViewById<TextView>(R.id.tvDateSelected)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmBooking)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelBooking)

        tvProviderTitle.text = "Book with: ${provider.name} — ${provider.companyName}"

        // default date string
        var selectedDateMillis: Long? = null

        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(requireContext(),
                { _, year, month, dayOfMonth ->
                    val c = Calendar.getInstance()
                    c.set(year, month, dayOfMonth, 0, 0, 0)
                    selectedDateMillis = c.timeInMillis
                    val formatted = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(c.time)
                    tvDateSelected.text = formatted
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dp.datePicker.minDate = System.currentTimeMillis() - 1000 // no past dates
            dp.show()
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            // validate
            val wasteType = etWasteType.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val packaging = etPackaging.text.toString().trim()
            val origin = etOrigin.text.toString().trim()
            val destination = etDestination.text.toString().trim()
            val special = etSpecial.text.toString().trim()

            if (wasteType.isEmpty() || quantity.isEmpty() || packaging.isEmpty() || origin.isEmpty() || selectedDateMillis == null) {
                Toast.makeText(requireContext(), "Please fill required fields and select a booking date.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // build booking payload
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnConfirm.isEnabled = false
            val pd = ProgressDialog(requireContext()).apply {
                setMessage("Booking transporter...")
                setCancelable(false)
                show()
            }

            // Use HwmsSession.generatorId if you stored it previously when creating the generator application
            val generatorId = try {
                // HwmsSession is used elsewhere in your project — if not present replace with the actual generatorId
                HwmsSession.generatorId
            } catch (t: Throwable) {
                null
            }

            val booking = hashMapOf(
                "pcoId" to currentUser.uid,
                "generatorId" to (generatorId ?: ""),
                "serviceProviderName" to provider.name,
                "serviceProviderCompany" to provider.companyName,
                "providerType" to provider.type,
                "providerContact" to provider.contactNumber,
                "wasteType" to wasteType,
                "quantity" to quantity,
                "packaging" to packaging,
                "origin" to origin,
                "destination" to destination,
                "specialInstructions" to special,
                "bookingDate" to Date(selectedDateMillis!!),
                "dateBooked" to FieldValue.serverTimestamp(),
                "status" to "Pending"
            )

            db.collection("transport_bookings")
                .add(booking)
                .addOnSuccessListener { ref ->
                    pd.dismiss()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Appointment booked with ${provider.name}", Toast.LENGTH_LONG).show()

//                    // optionally navigate to next step (step 3)
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.nav_host_fragment, TransporterStep3Fragment())
//                        .addToBackStack(null)
//                        .commit()
                }
                .addOnFailureListener { e ->
                    pd.dismiss()
                    btnConfirm.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to book appointment: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }
}
