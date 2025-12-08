package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsStep1Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class HwmsStep1Fragment : Fragment() {

    private lateinit var binding: FragmentHwmsStep1Binding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    private var inventoryUri: Uri? = null
    private var storagePhotoUri: Uri? = null
    private var labAnalysisUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHwmsStep1Binding.inflate(inflater, container, false)

        // Add one waste entry by default
        addWasteRow(null)

        binding.btnAddWaste.setOnClickListener { addWasteRow(null) }
        binding.btnUploadInventory.setOnClickListener { pickFile(1001) }
        binding.btnUploadStoragePhoto.setOnClickListener { pickFile(1002) }
        binding.btnUploadLabAnalysis.setOnClickListener { pickFile(1003) }

        binding.btnSubmitStep1.setOnClickListener { saveWasteDetails() }

        return binding.root
    }

    private fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            when (requestCode) {
                1001 -> {
                    inventoryUri = uri
                    binding.tvInventoryStatus.text = "📄 File selected"
                }
                1002 -> {
                    storagePhotoUri = uri
                    binding.tvStoragePhotoStatus.text = "📸 Photo selected"
                }
                1003 -> {
                    labAnalysisUri = uri
                    binding.tvLabAnalysisStatus.text = "📑 File selected"
                }
            }
        }
    }

    private fun addWasteRow(existing: Map<String, String>?) {
        val row = layoutInflater.inflate(R.layout.item_waste_profile, binding.llWastesContainer, false)

        val etName = row.findViewById<EditText>(R.id.etWasteName)
        val etType = row.findViewById<EditText>(R.id.etWasteType)
        val etCode = row.findViewById<EditText>(R.id.etWasteCode)
        val etQuantity = row.findViewById<EditText>(R.id.etWasteQuantity)
        val etStorage = row.findViewById<EditText>(R.id.etStorageLocation)
        val tvDate = row.findViewById<TextView>(R.id.tvDateGenerated)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveWaste)

        // Date picker
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val c = Calendar.getInstance()
                    c.set(y, m, d)
                    val formatted = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(c.time)
                    tvDate.text = formatted
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        existing?.let {
            etName.setText(it["wasteName"] ?: "")
            etType.setText(it["wasteType"] ?: "")
            etCode.setText(it["wasteCode"] ?: "")
            etQuantity.setText(it["quantity"] ?: "")
            etStorage.setText(it["storageLocation"] ?: "")
            tvDate.text = it["dateGenerated"] ?: ""
        }

        btnRemove.setOnClickListener { binding.llWastesContainer.removeView(row) }
        binding.llWastesContainer.addView(row)
    }

    private fun saveWasteDetails() {
        val companyName = binding.etCompanyName.text.toString().trim()
        val companyAddress = binding.etCompanyAddress.text.toString().trim()
        val pcoName = binding.etPCOName.text.toString().trim()
        val pcoContact = binding.etPCOContact.text.toString().trim()
        val embRegNo = binding.etEmbRegNo.text.toString().trim()

        if (companyName.isEmpty() || companyAddress.isEmpty() || pcoName.isEmpty() ||
            pcoContact.isEmpty() || embRegNo.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all company & PCO fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val wasteList = mutableListOf<Map<String, Any>>()
        for (i in 0 until binding.llWastesContainer.childCount) {
            val view = binding.llWastesContainer.getChildAt(i)

            val wasteName = view.findViewById<EditText>(R.id.etWasteName).text.toString().trim()
            val wasteType = view.findViewById<EditText>(R.id.etWasteType).text.toString().trim()
            val wasteCode = view.findViewById<EditText>(R.id.etWasteCode).text.toString().trim()
            val quantity = view.findViewById<EditText>(R.id.etWasteQuantity).text.toString().trim()
            val storageLoc = view.findViewById<EditText>(R.id.etStorageLocation).text.toString().trim()
            val dateGen = view.findViewById<TextView>(R.id.tvDateGenerated).text.toString().trim()

            if (wasteName.isEmpty() || wasteType.isEmpty() || wasteCode.isEmpty() ||
                quantity.isEmpty() || storageLoc.isEmpty() || dateGen.isEmpty()) {
                Toast.makeText(requireContext(), "Please complete all waste entry fields.", Toast.LENGTH_SHORT).show()
                return
            }

            wasteList.add(
                mapOf(
                    "wasteName" to wasteName,
                    "wasteType" to wasteType,
                    "wasteCode" to wasteCode,
                    "quantity" to quantity,
                    "storageLocation" to storageLoc,
                    "dateGenerated" to dateGen
                )
            )
        }

        if (wasteList.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one waste entry.", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "userId" to userId,
            "companyName" to companyName,
            "companyAddress" to companyAddress,
            "pcoName" to pcoName,
            "pcoContact" to pcoContact,
            "embRegNo" to embRegNo,
            "wasteDetails" to wasteList,
            "status" to "Submitted",
            "timestamp" to FieldValue.serverTimestamp()
        )

        uploadFiles(data)
    }

    private fun uploadFiles(data: HashMap<String, Any>) {
        val uploads = mutableMapOf<String, Uri?>(
            "wasteInventory" to inventoryUri,
            "wasteStoragePhoto" to storagePhotoUri,
            "labAnalysis" to labAnalysisUri
        )

        val urls = mutableMapOf<String, String>()
        val totalFiles = uploads.values.count { it != null }
        if (totalFiles == 0) {
            saveToFirestore(data, urls)
            return
        }

        var uploaded = 0
        uploads.forEach { (key, uri) ->
            if (uri != null) {
                val ref = storage.reference.child("hwms_uploads/${userId}/${System.currentTimeMillis()}_${key}")
                ref.putFile(uri)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { fileUrl ->
                            urls[key] = fileUrl.toString()
                            uploaded++
                            if (uploaded == totalFiles) {
                                saveToFirestore(data, urls)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to upload $key", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun saveToFirestore(data: HashMap<String, Any>, fileUrls: Map<String, String>) {
        val finalData = data + fileUrls
        db.collection("HazardousWasteGenerator")
            .add(finalData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Waste generation details saved.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.TransporterStep2Fragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save waste details.", Toast.LENGTH_SHORT).show()
            }
    }
}
