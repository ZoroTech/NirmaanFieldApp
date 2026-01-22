package com.example.nirmaanfieldapp.ui.dpr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nirmaanfieldapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * DprFragment - Daily Progress Report Screen for Construction Field App
 *
 * Features:
 * - Offline-first DPR creation
 * - Camera integration with permission handling
 * - Photo capture and preview
 * - Local data persistence using SharedPreferences
 * - Field-friendly UI with large touch targets
 * - Auto-filled date field
 * - Input validation
 */
class DprFragment : Fragment() {

    private companion object {
        const val PREFS_NAME = "dpr_prefs"
        const val KEY_DPR_LIST = "dpr_list"
        const val SUBMIT_DELAY_MS = 1500L
        const val PHOTO_FILE_PREFIX = "DPR_"
        const val PHOTO_FILE_SUFFIX = ".jpg"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }

    // View references
    private lateinit var tvDprDate: TextView
    private lateinit var tilWorkDescription: TextInputLayout
    private lateinit var tilRemarks: TextInputLayout
    private lateinit var etWorkDescription: TextInputEditText
    private lateinit var etRemarks: TextInputEditText
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnSubmitDpr: MaterialButton
    private lateinit var cardPhotoPreview: MaterialCardView
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var fabRemovePhoto: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    // Photo capture variables
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - open camera
            openCamera()
        } else {
            // Permission denied - show message
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            // Photo captured successfully
            displayPhotoPreview(currentPhotoUri!!)
        } else {
            // Photo capture failed or cancelled
            currentPhotoUri = null
            currentPhotoFile?.delete()
            currentPhotoFile = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dpr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Set current date
        setCurrentDate()

        // Setup click listeners
        setupClickListeners()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        tvDprDate = view.findViewById(R.id.tvDprDate)
        tilWorkDescription = view.findViewById(R.id.tilWorkDescription)
        tilRemarks = view.findViewById(R.id.tilRemarks)
        etWorkDescription = view.findViewById(R.id.etWorkDescription)
        etRemarks = view.findViewById(R.id.etRemarks)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        btnSubmitDpr = view.findViewById(R.id.btnSubmitDpr)
        cardPhotoPreview = view.findViewById(R.id.cardPhotoPreview)
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview)
        fabRemovePhoto = view.findViewById(R.id.fabRemovePhoto)
        progressBar = view.findViewById(R.id.progressBar)
    }

    /**
     * Set current date in the date display field
     */
    private fun setCurrentDate() {
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        tvDprDate.text = dateFormat.format(currentDate.time)
    }

    /**
     * Setup click listeners for buttons
     */
    private fun setupClickListeners() {
        // Add Photo button
        btnAddPhoto.setOnClickListener {
            handleAddPhotoClick()
        }

        // Remove Photo button
        fabRemovePhoto.setOnClickListener {
            handleRemovePhotoClick()
        }

        // Submit DPR button
        btnSubmitDpr.setOnClickListener {
            handleSubmitDprClick()
        }

        // Clear error messages when user starts typing
        etWorkDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilWorkDescription.error = null
        }
    }

    /**
     * Handle Add Photo button click
     * Checks camera permission and opens camera
     */
    private fun handleAddPhotoClick() {
        when {
            // Check if camera permission is granted
            checkCameraPermission() -> {
                openCamera()
            }
            // Show rationale if needed
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_permission_rationale),
                    Toast.LENGTH_LONG
                ).show()
                // Request permission anyway
                requestCameraPermission()
            }
            // Request permission
            else -> {
                requestCameraPermission()
            }
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission
     */
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Open camera to capture photo
     * Creates a temporary file and launches camera intent
     */
    private fun openCamera() {
        try {
            // Create image file
            val photoFile = createImageFile()
            currentPhotoFile = photoFile

            // Get URI for the file using FileProvider
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}$AUTHORITY_SUFFIX",
                photoFile
            )
            currentPhotoUri = photoUri

            // Launch camera
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_open_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Create a temporary image file in the app's private storage
     */
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            "${PHOTO_FILE_PREFIX}${timestamp}_",
            PHOTO_FILE_SUFFIX,
            storageDir
        )
    }

    /**
     * Display photo preview after successful capture
     */
    private fun displayPhotoPreview(photoUri: Uri) {
        try {
            // Load image into ImageView
            ivPhotoPreview.setImageURI(photoUri)

            // Show preview card
            cardPhotoPreview.visibility = View.VISIBLE

            // Update button text
            btnAddPhoto.text = getString(R.string.retake_photo)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.photo_display_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Handle Remove Photo button click
     */
    private fun handleRemovePhotoClick() {
        // Delete photo file
        currentPhotoFile?.delete()

        // Clear references
        currentPhotoUri = null
        currentPhotoFile = null

        // Hide preview card
        cardPhotoPreview.visibility = View.GONE

        // Reset button text
        btnAddPhoto.text = getString(R.string.add_photo)

        Toast.makeText(
            requireContext(),
            getString(R.string.photo_removed),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Handle Submit DPR button click
     * Validates inputs and saves DPR locally
     */
    private fun handleSubmitDprClick() {
        // Clear previous errors
        tilWorkDescription.error = null

        // Get input values
        val workDescription = etWorkDescription.text.toString().trim()
        val remarks = etRemarks.text.toString().trim()

        // Validate work description
        if (workDescription.isEmpty()) {
            tilWorkDescription.error = getString(R.string.error_work_description_empty)
            etWorkDescription.requestFocus()
            return
        }

        // Proceed with submission
        submitDpr(workDescription, remarks)
    }

    /**
     * Submit DPR by saving data locally
     */
    private fun submitDpr(workDescription: String, remarks: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Show loading state
            setLoadingState(true)

            // Simulate processing delay
            delay(SUBMIT_DELAY_MS)

            // Check if fragment is still attached
            if (!isAdded) return@launch

            try {
                // Save DPR data
                saveDprLocally(workDescription, remarks)

                // Show success message
                Toast.makeText(
                    requireContext(),
                    getString(R.string.dpr_submit_success),
                    Toast.LENGTH_SHORT
                ).show()

                // Clear form
                clearForm()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.dpr_submit_error),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Hide loading state
                setLoadingState(false)
            }
        }
    }

    /**
     * Save DPR data locally using SharedPreferences
     * Stores data as JSON array
     */
    private fun saveDprLocally(workDescription: String, remarks: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Get existing DPR list
        val dprListJson = prefs.getString(KEY_DPR_LIST, "[]") ?: "[]"
        val dprArray = JSONArray(dprListJson)

        // Create new DPR object
        val dprObject = JSONObject().apply {
            put("id", generateDprId())
            put("date", tvDprDate.text.toString())
            put("timestamp", System.currentTimeMillis())
            put("workDescription", workDescription)
            put("remarks", remarks)
            put("photoUri", currentPhotoUri?.toString() ?: "")
            put("photoPath", currentPhotoFile?.absolutePath ?: "")
        }

        // Add new DPR to array
        dprArray.put(dprObject)

        // Save updated array
        prefs.edit().apply {
            putString(KEY_DPR_LIST, dprArray.toString())
            apply()
        }
    }

    /**
     * Generate unique DPR ID using timestamp
     */
    private fun generateDprId(): String {
        return "DPR_${System.currentTimeMillis()}"
    }

    /**
     * Clear form after successful submission
     */
    private fun clearForm() {
        // Clear text fields
        etWorkDescription.text?.clear()
        etRemarks.text?.clear()

        // Clear photo (but keep the file since it's saved)
        currentPhotoUri = null
        currentPhotoFile = null
        cardPhotoPreview.visibility = View.GONE
        btnAddPhoto.text = getString(R.string.add_photo)

        // Clear errors
        tilWorkDescription.error = null
        tilRemarks.error = null
    }

    /**
     * Set loading state for UI
     */
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Disable inputs
            etWorkDescription.isEnabled = false
            etRemarks.isEnabled = false
            btnAddPhoto.isEnabled = false
            btnSubmitDpr.isEnabled = false

            // Show progress bar
            progressBar.visibility = View.VISIBLE
        } else {
            // Enable inputs
            etWorkDescription.isEnabled = true
            etRemarks.isEnabled = true
            btnAddPhoto.isEnabled = true
            btnSubmitDpr.isEnabled = true

            // Hide progress bar
            progressBar.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up if needed
    }
}
