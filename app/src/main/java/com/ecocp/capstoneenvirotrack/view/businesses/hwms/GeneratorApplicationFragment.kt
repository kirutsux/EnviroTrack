package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.GeneratorRepository
import com.google.firebase.auth.FirebaseAuth
import java.lang.Exception


class GeneratorApplicationFragment : Fragment() {


    // UI
    private lateinit var flipper: ViewFlipper
    private lateinit var tvStepsTitle: TextView
    private lateinit var btnNextStep: Button
    private lateinit var btnBackStep: Button
    private lateinit var btnFinalize: Button

    // Step 1 fields
    private lateinit var etCompany: EditText
    private lateinit var etManagingHead: EditText
    private lateinit var etEstablishmentName: EditText
    private lateinit var etMobileNumber: EditText
    private lateinit var etNatureOfBusiness: EditText
    private lateinit var etPsicNumber: EditText
    private lateinit var etDateEstablished: EditText
    private lateinit var etNoEmployees: EditText
    private lateinit var etPcoName: EditText
    private lateinit var etPcoMobile: EditText
    private lateinit var etPcoTel: EditText
    private lateinit var etPcoAccredNo: EditText
    private lateinit var etPcoAccredDate: EditText

    // Permit/product/waste containers
    private lateinit var llPermitsContainer: LinearLayout
    private lateinit var btnAddPermit: Button
    private lateinit var llProductsContainer: LinearLayout
    private lateinit var btnAddProduct: Button
    private lateinit var llWastesContainer: LinearLayout
    private lateinit var btnAddWaste: Button

    // Required docs container (step 5)
    private lateinit var llRequiredDocs: LinearLayout

    private val repository = GeneratorRepository()
    private val auth = FirebaseAuth.getInstance()

    // Required doc keys (these must be provided to enable finalize)
    private val requiredDocKeys = listOf(
        "affidavit",
        "waste_management_plan",
        "pco_accreditation",
        "storage_photos"
    )

    // Map key -> picked Uri
    private val docUris = mutableMapOf<String, Uri?>()

    // used to figure out which doc key the user is currently picking
    private var currentPickingKey: String? = null

    // file picker launcher
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val key = currentPickingKey ?: return@registerForActivityResult
        try {
            if (!validateFile(uri)) {
                return@registerForActivityResult
            }
            docUris[key] = uri
            // update UI: find the row for this key and set name
            updateDocRowUi(key, uri)
            checkFinalizeEnabled()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "File validation error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            currentPickingKey = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_generator_application, container, false)

        // top-level UI
        flipper = v.findViewById(R.id.viewFlipperSteps)
        tvStepsTitle = v.findViewById(R.id.tvStepsTitle)
        btnNextStep = v.findViewById(R.id.btnNextStep)
        btnBackStep = v.findViewById(R.id.btnBackStep)
        btnFinalize = v.findViewById(R.id.btnFinalize)

        // Step 1 fields
        etCompany = v.findViewById(R.id.etCompany)
        etManagingHead = v.findViewById(R.id.etManagingHead)
        etEstablishmentName = v.findViewById(R.id.etEstablishmentName)
        etMobileNumber = v.findViewById(R.id.etMobileNumber)
        etNatureOfBusiness = v.findViewById(R.id.etNatureOfBusiness)
        etPsicNumber = v.findViewById(R.id.etPsicNumber)
        etDateEstablished = v.findViewById(R.id.etDateEstablished)
        etNoEmployees = v.findViewById(R.id.etNoEmployees)
        etPcoName = v.findViewById(R.id.etPcoName)
        etPcoMobile = v.findViewById(R.id.etPcoMobile)
        etPcoTel = v.findViewById(R.id.etPcoTel)
        etPcoAccredNo = v.findViewById(R.id.etPcoAccredNo)
        etPcoAccredDate = v.findViewById(R.id.etPcoAccredDate)

        // containers & add buttons
        llPermitsContainer = v.findViewById(R.id.llPermitsContainer)
        btnAddPermit = v.findViewById(R.id.btnAddPermit)
        llProductsContainer = v.findViewById(R.id.llProductsContainer)
        btnAddProduct = v.findViewById(R.id.btnAddProduct)
        llWastesContainer = v.findViewById(R.id.llWastesContainer)
        btnAddWaste = v.findViewById(R.id.btnAddWaste)
        llRequiredDocs = v.findViewById(R.id.llRequiredDocs)

        // step navigation
        btnNextStep.setOnClickListener { onNextStep() }
        btnBackStep.setOnClickListener { onBackStep() }
        btnFinalize.setOnClickListener { onFinalizeClicked() }

        // dynamic rows
        btnAddPermit.setOnClickListener { addPermitRow(null) }
        btnAddProduct.setOnClickListener { addProductRow(null, null) }
        btnAddWaste.setOnClickListener { addWasteRow(null) }

        // init: add one permit row by default
        addPermitRow(null)
        // add one waste profile by default
        addWasteRow(null)
        // add one product optional row placeholder (not mandatory)
        // addProductRow(null, null)  // optional

        // build required doc rows UI
        buildRequiredDocsUi()

        updateStepTitle()
        checkFinalizeEnabled()

        return v
    }

    private fun updateStepTitle() {
        val step = flipper.displayedChild + 1
        tvStepsTitle.text = "Generator Application (Step $step of 5)"
    }

    private fun onNextStep() {
        val current = flipper.displayedChild
        // basic validation at certain steps (e.g., step 1 required fields)
        if (current == 0) {
            val company = etCompany.text.toString().trim()
            val waste = etNatureOfBusiness.text.toString().trim()
            if (company.isEmpty()) {
                Toast.makeText(requireContext(), "Company is required", Toast.LENGTH_SHORT).show()
                return
            }
            // allow move
        }
        if (current < flipper.childCount - 1) {
            flipper.showNext()
            updateStepTitle()
        }
    }

    private fun onBackStep() {
        if (flipper.displayedChild > 0) {
            flipper.showPrevious()
            updateStepTitle()
        }
    }

    // add a permit row to llPermitsContainer. supply existing data if editing
    private fun addPermitRow(existing: Map<String, String>?) {
        val row = layoutInflater.inflate(R.layout.item_permit, llPermitsContainer, false)
        val etType = row.findViewById<EditText>(R.id.etPermitType)
        val etNo = row.findViewById<EditText>(R.id.etPermitNo)
        val etDate = row.findViewById<EditText>(R.id.etDateIssued)
        val etExpiry = row.findViewById<EditText>(R.id.etExpiryDate)
        val etPlace = row.findViewById<EditText>(R.id.etPlaceOfIssuance)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemovePermit)

        existing?.let {
            etType.setText(it["type"] ?: "")
            etNo.setText(it["id"] ?: "")
            etDate.setText(it["dateIssued"] ?: "")
            etExpiry.setText(it["expiryDate"] ?: "")
            etPlace.setText(it["place"] ?: "")
        }

        btnRemove.setOnClickListener {
            llPermitsContainer.removeView(row)
        }
        llPermitsContainer.addView(row)
    }

    private fun addProductRow(name: String?, desc: String?) {
        val row = layoutInflater.inflate(R.layout.item_product, llProductsContainer, false)
        val etName = row.findViewById<EditText>(R.id.etProductName)
        val etDesc = row.findViewById<EditText>(R.id.etServiceDesc)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveProduct)

        name?.let { etName.setText(it) }
        desc?.let { etDesc.setText(it) }

        btnRemove.setOnClickListener { llProductsContainer.removeView(row) }
        llProductsContainer.addView(row)
    }

    private fun addWasteRow(existing: Map<String, String>?) {
        val row = layoutInflater.inflate(R.layout.item_waste_profile, llWastesContainer, false)
        val etName = row.findViewById<EditText>(R.id.etWasteName)
        val etNature = row.findViewById<EditText>(R.id.etNature)
        val etCatalogue = row.findViewById<EditText>(R.id.etCatalogue)
        val etDetails = row.findViewById<EditText>(R.id.etWasteDetails)
        val etPractice = row.findViewById<EditText>(R.id.etCurrentPractice)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveWaste)

        existing?.let {
            etName.setText(it["wasteName"] ?: "")
            etNature.setText(it["nature"] ?: "")
            etCatalogue.setText(it["catalogue"] ?: "")
            etDetails.setText(it["details"] ?: "")
            etPractice.setText(it["practice"] ?: "")
        }

        btnRemove.setOnClickListener { llWastesContainer.removeView(row) }
        llWastesContainer.addView(row)
    }

    // build rows for required docs (step 5)
    private fun buildRequiredDocsUi() {
        llRequiredDocs.removeAllViews()
        for (key in requiredDocKeys) {
            val row = layoutInflater.inflate(R.layout.item_required_doc, llRequiredDocs, false)
            val tvLabel = row.findViewById<TextView>(R.id.tvDocLabel)
            val tvName = row.findViewById<TextView>(R.id.tvDocName)
            val btnPick = row.findViewById<Button>(R.id.btnPickDoc)
            val btnRemove = row.findViewById<Button>(R.id.btnRemoveDoc)

            tvLabel.text = when (key) {
                "affidavit" -> "* Duly notarized affidavit"
                "waste_management_plan" -> "* Description of existing waste management plan"
                "pco_accreditation" -> "* Pollution control officer accreditation certificate"
                "storage_photos" -> "* Photographs of the hazardous waste storage area"
                else -> "* Required file"
            }

            tvName.text = docUris[key]?.lastPathSegment ?: "No file"

            btnPick.setOnClickListener {
                currentPickingKey = key
                // allow only pdf / images; we will validate further in callback
                pickFileLauncher.launch("*/*")
            }

            btnRemove.setOnClickListener {
                docUris.remove(key)
                tvName.text = "No file"
                checkFinalizeEnabled()
            }

            // attach a tag mapping so `updateDocRowUi` can find this tv later (we'll tag the view)
            row.tag = key
            llRequiredDocs.addView(row)
        }
    }

    private fun updateDocRowUi(key: String, uri: Uri) {
        // find child with tag == key
        for (i in 0 until llRequiredDocs.childCount) {
            val child = llRequiredDocs.getChildAt(i)
            if (child.tag == key) {
                val tvName = child.findViewById<TextView>(R.id.tvDocName)
                tvName.text = uri.lastPathSegment
                break
            }
        }
    }

    // checks each required key exists in docUris and is non-null
    private fun checkFinalizeEnabled() {
        val allPresent = requiredDocKeys.all { docUris[it] != null }
        btnFinalize.isEnabled = allPresent
    }

    // validate selected file: mime and size (<= 20MB)
    private fun validateFile(uri: Uri): Boolean {
        val resolver: ContentResolver = requireContext().contentResolver
        val mime = resolver.getType(uri) ?: ""
        val allowed = setOf("application/pdf", "image/jpeg", "image/png", "image/gif")
        if (!allowed.contains(mime)) {
            Toast.makeText(requireContext(), "Invalid file type: $mime. Allowed: pdf, jpg, png, gif", Toast.LENGTH_LONG).show()
            return false
        }
        // check size
        var sizeOk = true
        try {
            val pfd = resolver.openFileDescriptor(uri, "r")
            val size = pfd?.statSize ?: -1L
            pfd?.close()
            val max = 20L * 1024L * 1024L
            if (size > max) {
                Toast.makeText(requireContext(), "File too large (${size} bytes). Max 20MB", Toast.LENGTH_LONG).show()
                sizeOk = false
            }
        } catch (e: Exception) {
            // if we cannot obtain size, we will allow but warn (rare)
            Toast.makeText(requireContext(), "Could not validate file size; try another file.", Toast.LENGTH_SHORT).show()
            sizeOk = false
        }
        return sizeOk
    }

    // gather permits from llPermitsContainer
    private fun collectPermits(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until llPermitsContainer.childCount) {
            val row = llPermitsContainer.getChildAt(i)
            val etType = row.findViewById<EditText>(R.id.etPermitType)
            val etNo = row.findViewById<EditText>(R.id.etPermitNo)
            val etDate = row.findViewById<EditText>(R.id.etDateIssued)
            val etExpiry = row.findViewById<EditText>(R.id.etExpiryDate)
            val etPlace = row.findViewById<EditText>(R.id.etPlaceOfIssuance)

            val type = etType.text.toString().trim()
            if (type.isEmpty()) continue // skip empty rows
            val map = mapOf(
                "type" to type,
                "id" to etNo.text.toString().trim(),
                "dateIssued" to etDate.text.toString().trim(),
                "expiryDate" to etExpiry.text.toString().trim(),
                "place" to etPlace.text.toString().trim()
            )
            list.add(map)
        }
        return list
    }

    private fun collectProducts(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until llProductsContainer.childCount) {
            val row = llProductsContainer.getChildAt(i)
            val etName = row.findViewById<EditText>(R.id.etProductName)
            val etDesc = row.findViewById<EditText>(R.id.etServiceDesc)
            val name = etName.text.toString().trim()
            if (name.isEmpty()) continue
            list.add(mapOf("productName" to name, "serviceDescription" to etDesc.text.toString().trim()))
        }
        return list
    }

    private fun collectWastes(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until llWastesContainer.childCount) {
            val row = llWastesContainer.getChildAt(i)
            val etName = row.findViewById<EditText>(R.id.etWasteName)
            val etNature = row.findViewById<EditText>(R.id.etNature)
            val etCatalogue = row.findViewById<EditText>(R.id.etCatalogue)
            val etDetails = row.findViewById<EditText>(R.id.etWasteDetails)
            val etPractice = row.findViewById<EditText>(R.id.etCurrentPractice)

            val name = etName.text.toString().trim()
            if (name.isEmpty()) continue
            list.add(
                mapOf(
                    "wasteName" to name,
                    "nature" to etNature.text.toString().trim(),
                    "catalogue" to etCatalogue.text.toString().trim(),
                    "details" to etDetails.text.toString().trim(),
                    "currentPractice" to etPractice.text.toString().trim()
                )
            )
        }
        return list
    }

    // FINALIZE: upload docs & create Firestore doc
    private fun onFinalizeClicked() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Build payload
        val payload = mutableMapOf<String, Any>(
            "companyName" to etCompany.text.toString().trim(),
            "managingHead" to etManagingHead.text.toString().trim(),
            "establishmentName" to etEstablishmentName.text.toString().trim(),
            "mobileNumber" to etMobileNumber.text.toString().trim(),
            "natureOfBusiness" to etNatureOfBusiness.text.toString().trim(),
            "psicNumber" to etPsicNumber.text.toString().trim(),
            "dateOfEstablishment" to etDateEstablished.text.toString().trim(),
            "noOfEmployees" to (etNoEmployees.text.toString().trim().toIntOrNull() ?: 0),
            "pcoName" to etPcoName.text.toString().trim(),
            "pcoMobileNumber" to etPcoMobile.text.toString().trim(),
            "pcoTelephoneNumber" to etPcoTel.text.toString().trim(),
            "pcoAccreditationNumber" to etPcoAccredNo.text.toString().trim(),
            "pcoDateOfAccreditation" to etPcoAccredDate.text.toString().trim(),
            "permits" to collectPermits(),
            "products" to collectProducts(),
            "wastes" to collectWastes(),
            "status" to "submitted"
        )

        // ensure required docs present
        val missing = requiredDocKeys.filter { docUris[it] == null }
        if (missing.isNotEmpty()) {
            Toast.makeText(requireContext(), "Please upload all required documents before finalizing.", Toast.LENGTH_LONG).show()
            return
        }

        // prepare docUris map (non-null)
        val toUpload = docUris.filterValues { it != null }.mapValues { it.value!! }

        // disable finalize to prevent duplicate taps
        btnFinalize.isEnabled = false
        Toast.makeText(requireContext(), "Submitting application... please wait", Toast.LENGTH_SHORT).show()

        repository.submitGeneratorApplication(uid, payload, toUpload,
            onSuccess = { appId ->
                Toast.makeText(requireContext(), "Application submitted (id=$appId)", Toast.LENGTH_LONG).show()
                // store in session (for later manifest use)
                HwmsSession.generatorId = appId
                // Optionally navigate to next HWMS step (transporter selection)
                try {
                    // this nav action should exist in your manifest_nav_graph:
                    val navController = requireActivity().supportFragmentManager.findFragmentById(R.id.hwms_nav_host_fragment)
                    // instead of trying to programmatically navigate here (child nav), we will use parent navigation:
                    // If using child nav graph, call findNavController().navigate(R.id.action_generator_to_transporter)
                    val parentNav = requireActivity().findNavController(R.id.nav_host_fragment) // best effort, may vary
                } catch (e: Exception) {
                    // ignore nav attempt — your nav graph will control flow
                }
            },
            onFailure = { e ->
                btnFinalize.isEnabled = true
                Toast.makeText(requireContext(), "Submission failed: ${e.message}", Toast.LENGTH_LONG).show()
            })
    }
}
