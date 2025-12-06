package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SmrFileListAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrSummaryBinding
import com.ecocp.capstoneenvirotrack.model.AirPollution
import com.ecocp.capstoneenvirotrack.model.GeneralInfo
import com.ecocp.capstoneenvirotrack.model.Others
import com.ecocp.capstoneenvirotrack.model.Smr
import com.ecocp.capstoneenvirotrack.utils.airPollutionText
import com.ecocp.capstoneenvirotrack.utils.generalInfoText
import com.ecocp.capstoneenvirotrack.utils.hazardousWasteText
import com.ecocp.capstoneenvirotrack.utils.othersText
import com.ecocp.capstoneenvirotrack.utils.waterPollutionText
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class SmrSummaryFragment : Fragment() {

    private var _binding: FragmentSmrSummaryBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var fileAdapter: SmrFileListAdapter
    private var currentSmrDocumentId: String? = null
    private var statusListener: ListenerRegistration? = null

    private val filePickerLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { uploadFile(it) }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmrSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val smrId = arguments?.getString("smrId")
        if (smrId != null) {
            loadExistingSmr(smrId)
            binding.btnSubmitSmr.visibility = View.GONE
        } else {
            binding.tvStatus.visibility = View.GONE
            binding.btnEditSmr.visibility = View.GONE
            binding.tvRejectionReason.visibility = View.GONE

            smrViewModel.smr.observe(viewLifecycleOwner){ smr->
                displaySmrData(smr)
            }
        }
        Log.d("SmrSummaryFragment", "smrId: $smrId")

        fileAdapter = SmrFileListAdapter { url ->
            if (smrId == null) smrViewModel.removeFileUrl(url)
        }
        binding.recyclerAttachedFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAttachedFiles.adapter = fileAdapter

        if(smrId == null){
            smrViewModel.fileUrls.observe(viewLifecycleOwner) { urls ->
                fileAdapter.submitList(urls)
            }
        }

        binding.btnAttachFile.setOnClickListener {
            pickFile()
        }

        binding.btnSubmitSmr.setOnClickListener {
            submitSmrToFirebase()
        }

        binding.btnEditSmr.setOnClickListener {
            findNavController().navigate(R.id.action_smrSummaryFragment_to_module1GeneralInfoFragment)
        }
    }

    private fun loadExistingSmr(smrId: String) {
        firestore.collection("smr_submissions").document(smrId).get()
            .addOnSuccessListener { document ->
                val smr = document.toObject(Smr::class.java)?.copy(id = smrId)
                smr?.let {
                    displaySmrData(it)
                    fileAdapter.submitList(it.fileUrls)

                    binding.btnSubmitSmr.visibility = View.GONE
                    val initialStatus = document.getString("status") ?: "Pending"
                    val rejectionReason = document.getString("rejectionReason")?: ""
                    binding.tvStatus.text = "Status: $initialStatus"
                    binding.tvStatus.visibility = View.VISIBLE

                    if(initialStatus == "Rejected" && rejectionReason.isNotEmpty()){
                        binding.tvRejectionReason.text = "Rejection Reason: $rejectionReason"
                        binding.tvRejectionReason.visibility = View.VISIBLE
                    }else{
                        binding.tvRejectionReason.visibility = View.GONE
                    }

                    if (initialStatus == "Rejected") {
                        binding.btnEditSmr.visibility = View.VISIBLE
                        binding.btnAttachFile.visibility = View.VISIBLE
                    } else {
                        binding.btnEditSmr.visibility = View.GONE
                        binding.btnAttachFile.visibility = View.GONE

                        smrViewModel.smr.observe(viewLifecycleOwner){smr->
                            displaySmrData(smr)
                        }
                    }

                    currentSmrDocumentId = smrId
                    setupStatusListener(smrId)
                }?:run {
                    Snackbar.make(binding.root, "No SMR data found", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun pickFile() {
        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )
    }

    private fun uploadFile(uri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Snackbar.make(binding.root, "User unauthorized.", Snackbar.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.VISIBLE
            return
        }

        val fileName = "smr_files/$userId/${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val storageRef: StorageReference = storage.reference.child(fileName)

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    if(arguments?.getString("smrId") == null){
                        smrViewModel.addFileUrl(downloadUrl.toString())
                    }
                    Snackbar.make(
                        binding.root,
                        "File uploaded successfully!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Upload failed: ${e.message}", Snackbar.LENGTH_SHORT)
                    .show()
                binding.progressBar.visibility = View.GONE
            }
            .addOnProgressListener { taskSnapshot ->
                val progress =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                binding.progressBar.progress = progress
            }
    }

    /** --- DISPLAY SUMMARY DATA --- **/
    @SuppressLint("SetTextI18n")
    private fun displaySmrData(smr: Smr) {
        Log.d("SMRDisplay", "Displaying SMR: ${smr.id}, GeneralInfo: ${smr.generalInfo.establishmentName}")

        // Module 1: General Info
        val module1Text = smr.generalInfo.generalInfoText()
        binding.module1Container.tvModuleTitle.text = "Module 1: General Information"
        binding.module1Container.tvModuleSummary.text = module1Text
        Log.d("SMRDisplay", "Module 1: $module1Text")

        // Module 2: Hazardous Waste
        val module2Text = smr.hazardousWastes.hazardousWasteText()
        binding.module2Container.tvModuleTitle.text = "Module 2: Hazardous Waste"
        binding.module2Container.tvModuleSummary.text = module2Text
        Log.d("SMRDisplay", "Module 2: $module2Text")

        // Module 3: Water Pollution
        val module3Text = smr.waterPollutionRecords.waterPollutionText()
        binding.module3Container.tvModuleTitle.text = "Module 3: Water Pollution"
        binding.module3Container.tvModuleSummary.text = module3Text
        Log.d("SMRDisplay", "Module 3: $module3Text")

        // Module 4: Air Pollution
        val module4Text = smr.airPollution.airPollutionText()
        binding.module4Container.tvModuleTitle.text = "Module 4: Air Pollution"
        binding.module4Container.tvModuleSummary.text = module4Text
        Log.d("SMRDisplay", "Module 4: $module4Text")

        // Module 5: Others
        val module5Text = smr.others.othersText()
        binding.module5Container.tvModuleTitle.text = "Module 5: Others"
        binding.module5Container.tvModuleSummary.text = module5Text
        Log.d("SMRDisplay", "Module 5: $module5Text")

    }


    /** --- SAVE TO FIREBASE --- **/
    @SuppressLint("SetTextI18n")
    private fun submitSmrToFirebase() {
        val smr = smrViewModel.smr.value
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (smr == null || userUid == null) {
            Snackbar.make(binding.root, "No SMR data or user UID found.", Snackbar.LENGTH_SHORT)
                .show()
            return
        }

        val smrData = mapOf(
            "dateSubmitted" to Timestamp.now(),
            "uid" to userUid,
            "generalInfo" to smr.generalInfo,
            "hazardousWastes" to smr.hazardousWastes,
            "waterPollutionRecords" to smr.waterPollutionRecords,
            "airPollution" to smr.airPollution,
            "others" to smr.others,
            "fileUrls" to smr.fileUrls
        )

        firestore.collection("smr_submissions")
            .add(smrData)
            .addOnSuccessListener { documentReference ->
                currentSmrDocumentId = documentReference.id
                Snackbar.make(binding.root, "SMR successfully submitted!", Snackbar.LENGTH_SHORT)
                    .show()
                binding.btnSubmitSmr.visibility = View.GONE
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = "Status: Pending"

                setupStatusListener(currentSmrDocumentId!!)

            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    binding.root,
                    "Failed to submit SMR: ${e.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun setupStatusListener(documentId: String) {
        statusListener?.remove()
        statusListener = firestore.collection("smr_submissions").document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Snackbar.make(binding.root, "Error fetching SMR status.", Snackbar.LENGTH_SHORT)
                        .show()
                    return@addSnapshotListener
                }
                val status = snapshot?.getString("status") ?: "Pending"
                val rejectionReason = snapshot?.getString("rejectionReason") ?: ""
                binding.tvStatus.text = "Status: $status"
                binding.tvStatus.visibility = View.VISIBLE

                if(status == "Rejected" && rejectionReason.isNotEmpty()){
                    binding.tvRejectionReason.text = "Rejection Reason: $rejectionReason"
                    binding.tvRejectionReason.visibility = View.VISIBLE
                }else{
                    binding.tvRejectionReason.visibility = View.GONE
                }

                when (status) {
                    "Rejected" -> {
                        binding.btnEditSmr.visibility = View.VISIBLE
                    }

                    "Approved" -> {
                        binding.btnEditSmr.visibility = View.GONE
                        if (arguments?.getString("smrId") == null) {
                            clearAllInputs()
                        }
                    }

                    else -> {
                        binding.btnEditSmr.visibility = View.GONE
                    }
                }
            }
    }

    private fun updateSmrStatus(status: String, reason:String? = null){
        if (currentSmrDocumentId == null) {
            Snackbar.make(binding.root, "No SMR document ID available.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val updateData = mutableMapOf<String, Any>("status" to status)
        reason?.let{updateData["rejectionReason"]=it}

        firestore.collection("smr_submissions").document(currentSmrDocumentId!!)
            .update(updateData)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "SMR status updated to $status", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Failed to update status: ${it.message}", Snackbar.LENGTH_SHORT).show()
            }
    }


    /** --- CLEAR ALL MODULE INPUT FIELDS --- **/
    private fun clearAllInputs() {
        smrViewModel.updateGeneralInfo(GeneralInfo())
        smrViewModel.updateHazardousWastes(emptyList())
        smrViewModel.clearWaterPollutionRecords()
        smrViewModel.updateAirPollution(AirPollution())
        smrViewModel.updateOthers(Others())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
