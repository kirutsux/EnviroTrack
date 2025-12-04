package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SmrFileListAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrSummaryBinding
import com.ecocp.capstoneenvirotrack.model.*
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ecocp.capstoneenvirotrack.utils.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SmrSummaryFragment : Fragment() {

    private var _binding: FragmentSmrSummaryBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var fileAdapter: SmrFileListAdapter

    private val filePickerLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { uploadFile(it) }
        }

    private val permissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pickFile()
            } else {
                Snackbar.make(
                    binding.root,
                    "Permission denied. Cannot access files.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
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

        fileAdapter = SmrFileListAdapter { url -> smrViewModel.removeFileUrl(url) }
        binding.recyclerAttachedFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAttachedFiles.adapter = fileAdapter

        smrViewModel.fileUrls.observe(viewLifecycleOwner) { urls ->
            fileAdapter.submitList(urls)
        }

        binding.btnAttachFile.setOnClickListener {
            checkPermissionAndPickFile()
        }

        // Observe the smr LiveData so summary updates automatically when modules change
        smrViewModel.smr.observe(viewLifecycleOwner) { smr ->
            // update UI from the latest smr
            displaySmrData(smr)
        }

        binding.btnSubmitSmr.setOnClickListener {
            submitSmrToFirebase()
        }
    }

    private fun checkPermissionAndPickFile() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pickFile()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun pickFile(){
        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )
    }

    private fun uploadFile(uri: Uri){
        val userId = FirebaseAuth.getInstance().currentUser?.uid?: run {
            Snackbar.make(binding.root, "User unauthorized.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val fileName = "smr_files/$userId/${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val storageRef: StorageReference = storage.reference.child(fileName)

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    smrViewModel.addFileUrl(downloadUrl.toString())
                    Snackbar.make(binding.root, "File uploaded successfully!", Snackbar.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Upload failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
            .addOnProgressListener{ taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount).toInt()
                binding.progressBar.progress = progress
            }
    }

    /** --- DISPLAY SUMMARY DATA --- **/
    @SuppressLint("SetTextI18n")
    private fun displaySmrData(smr: Smr) {
        // Module 1: General Info
        val module1Text = smr.generalInfo.generalInfoText()
        binding.module1Container.tvModuleTitle.text = "Module 1: General Information"
        binding.module1Container.tvModuleSummary.text = module1Text
        android.util.Log.d("SMRDisplay", "Module 1: $module1Text")

        // Module 2: Hazardous Waste
        val module2Text = smr.hazardousWastes.hazardousWasteText()
        binding.module2Container.tvModuleTitle.text = "Module 2: Hazardous Waste"
        binding.module2Container.tvModuleSummary.text = module2Text
        android.util.Log.d("SMRDisplay", "Module 2: $module2Text")

        // Module 3: Water Pollution
        val module3Text = smr.waterPollutionRecords.waterPollutionText()
        binding.module3Container.tvModuleTitle.text = "Module 3: Water Pollution"
        binding.module3Container.tvModuleSummary.text = module3Text
        android.util.Log.d("SMRDisplay", "Module 3: $module3Text")

        // Module 4: Air Pollution
        val module4Text = smr.airPollution.airPollutionText()
        binding.module4Container.tvModuleTitle.text = "Module 4: Air Pollution"
        binding.module4Container.tvModuleSummary.text = module4Text
        android.util.Log.d("SMRDisplay", "Module 4: $module4Text")

        // Module 5: Others
        val module5Text = smr.others.othersText()
        binding.module5Container.tvModuleTitle.text = "Module 5: Others"
        binding.module5Container.tvModuleSummary.text = module5Text
        android.util.Log.d("SMRDisplay", "Module 5: $module5Text")
    }


    /** --- SAVE TO FIREBASE --- **/
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
            .addOnSuccessListener {
                Snackbar.make(binding.root, "SMR successfully submitted!", Snackbar.LENGTH_SHORT)
                    .show()
                smrViewModel.clearSmr()
                smrViewModel.clearFiles()
                clearAllInputs() // clear all input fields

                // Navigate to SmrDashboardFragment
                findNavController().navigate(R.id.action_smrSummaryFragment_to_smrDashboardFragment)
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    binding.root,
                    "Failed to submit SMR: ${e.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
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
