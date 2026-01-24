package com.example.nirmaanfieldapp.ui.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.nirmaanfieldapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * AttendanceFragment - Handles employee attendance for construction field workers
 *
 * Features:
 * - Offline-first attendance tracking
 * - GPS location capture on punch in/out
 * - Runtime permission handling
 * - Live time display
 * - Local storage using SharedPreferences
 * - Field-friendly UI with large buttons
 */
class AttendanceFragment : Fragment() {

    private companion object {
        const val PREFS_NAME = "attendance_prefs"
        const val KEY_PUNCH_IN_TIME = "punch_in_time"
        const val KEY_PUNCH_IN_LAT = "punch_in_lat"
        const val KEY_PUNCH_IN_LNG = "punch_in_lng"
        const val KEY_PUNCH_OUT_TIME = "punch_out_time"
        const val KEY_PUNCH_OUT_LAT = "punch_out_lat"
        const val KEY_PUNCH_OUT_LNG = "punch_out_lng"
        const val KEY_IS_PUNCHED_IN = "is_punched_in"
        const val UPDATE_TIME_INTERVAL_MS = 1000L
    }

    // View references
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvLocationStatus: TextView
    private lateinit var btnPunchIn: MaterialButton
    private lateinit var btnPunchOut: MaterialButton
    private lateinit var cardAttendanceSummary: MaterialCardView
    private lateinit var tvPunchInTime: TextView
    private lateinit var tvPunchOutTime: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var tvPunchInPhotoLabel: TextView
    private lateinit var ivPunchInPhoto: ImageView
    private lateinit var cardGpsLocation: MaterialCardView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView

    // Location client
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Photo storage
    private var punchInPhotoBitmap: Bitmap? = null

    // Time update handler
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    // Current action type (punch in or punch out)
    private var currentAction: AttendanceAction = AttendanceAction.NONE

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - proceed with location capture
            captureLocationAndSaveAttendance()
        } else {
            // Permission denied - show message
            Toast.makeText(
                requireContext(),
                getString(R.string.location_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            updateLocationStatus(getString(R.string.location_permission_required))
            resetButtonStates()
        }
    }

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

    // Camera launcher - TakePicturePreview
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Photo captured successfully
            punchInPhotoBitmap = bitmap
            displayPunchInPhoto(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Setup time update
        setupTimeUpdate()

        // Setup click listeners
        setupClickListeners()

        // Restore attendance state from SharedPreferences
        restoreAttendanceState()

        // Update date immediately
        updateDateTime()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        tvLocationStatus = view.findViewById(R.id.tvLocationStatus)
        btnPunchIn = view.findViewById(R.id.btnPunchIn)
        btnPunchOut = view.findViewById(R.id.btnPunchOut)
        cardAttendanceSummary = view.findViewById(R.id.cardAttendanceSummary)
        tvPunchInTime = view.findViewById(R.id.tvPunchInTime)
        tvPunchOutTime = view.findViewById(R.id.tvPunchOutTime)
        tvTotalDuration = view.findViewById(R.id.tvTotalDuration)
        tvPunchInPhotoLabel = view.findViewById(R.id.tvPunchInPhotoLabel)
        ivPunchInPhoto = view.findViewById(R.id.ivPunchInPhoto)
        cardGpsLocation = view.findViewById(R.id.cardGpsLocation)
        tvLatitude = view.findViewById(R.id.tvLatitude)
        tvLongitude = view.findViewById(R.id.tvLongitude)
    }

    /**
     * Setup live time update using Handler
     */
    private fun setupTimeUpdate() {
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateDateTime()
                // Schedule next update
                timeUpdateHandler.postDelayed(this, UPDATE_TIME_INTERVAL_MS)
            }
        }
        // Start time updates
        timeUpdateHandler.post(timeUpdateRunnable)
    }

    /**
     * Update date and time display
     */
    private fun updateDateTime() {
        val currentDate = Calendar.getInstance()

        // Format date: "Wednesday, Jan 22, 2026"
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(currentDate.time)

        // Format time: "09:45 AM"
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        tvCurrentTime.text = timeFormat.format(currentDate.time)
    }

    /**
     * Setup click listeners for buttons
     */
    private fun setupClickListeners() {
        btnPunchIn.setOnClickListener {
            handlePunchIn()
        }

        btnPunchOut.setOnClickListener {
            handlePunchOut()
        }
    }

    /**
     * Handle Punch In button click
     * First requests camera permission, then opens camera for photo capture
     */
    private fun handlePunchIn() {
        currentAction = AttendanceAction.PUNCH_IN

        // Check for camera permission
        if (checkCameraPermission()) {
            // Permission already granted - open camera
            openCamera()
        } else {
            // Request camera permission
            requestCameraPermission()
        }
    }

    /**
     * Handle Punch Out button click
     */
    private fun handlePunchOut() {
        currentAction = AttendanceAction.PUNCH_OUT
        updateLocationStatus(getString(R.string.location_fetching))

        // Check for location permission
        if (checkLocationPermission()) {
            // Permission already granted - capture location
            captureLocationAndSaveAttendance()
        } else {
            // Request permission
            requestLocationPermission()
        }
    }

    /**
     * Check if location permission is granted
     */
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permission
     */
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
     * Open camera using TakePicturePreview contract
     */
    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    /**
     * Display captured photo in ImageView
     */
    private fun displayPunchInPhoto(bitmap: Bitmap) {
        tvPunchInPhotoLabel.visibility = View.VISIBLE
        ivPunchInPhoto.visibility = View.VISIBLE
        ivPunchInPhoto.setImageBitmap(bitmap)

        // After photo is displayed, proceed with location and attendance
        updateLocationStatus(getString(R.string.location_fetching))

        // Check for location permission
        if (checkLocationPermission()) {
            // Permission already granted - capture location
            captureLocationAndSaveAttendance()
        } else {
            // Request permission
            requestLocationPermission()
        }
    }

    /**
     * Capture current location and save attendance
     */
    @SuppressLint("MissingPermission")
    private fun captureLocationAndSaveAttendance() {
        // Disable buttons during location capture
        btnPunchIn.isEnabled = false
        btnPunchOut.isEnabled = false

        // Get last known location (faster and works offline)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Location captured successfully
                    handleLocationSuccess(location)
                } else {
                    // Location is null - try last known location as fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            handleLocationSuccess(lastLocation)
                        } else {
                            handleLocationFailure()
                        }
                    }.addOnFailureListener {
                        handleLocationFailure()
                    }
                }
            }
            .addOnFailureListener {
                handleLocationFailure()
            }
    }

    /**
     * Handle successful location capture
     */
    private fun handleLocationSuccess(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val timestamp = System.currentTimeMillis()

        // Save attendance based on action type
        when (currentAction) {
            AttendanceAction.PUNCH_IN -> {
                savePunchIn(latitude, longitude, timestamp)
                // Display GPS coordinates for Punch In only
                displayGpsCoordinates(latitude, longitude)
            }
            AttendanceAction.PUNCH_OUT -> {
                savePunchOut(latitude, longitude, timestamp)
            }
            AttendanceAction.NONE -> {
                // Should not happen
            }
        }

        // Update UI
        updateLocationStatus(getString(R.string.location_captured))

        // Reset action
        currentAction = AttendanceAction.NONE
    }

    /**
     * Handle location capture failure
     */
    private fun handleLocationFailure() {
        updateLocationStatus(getString(R.string.location_failed))
        Toast.makeText(
            requireContext(),
            getString(R.string.location_capture_error),
            Toast.LENGTH_LONG
        ).show()
        resetButtonStates()
        currentAction = AttendanceAction.NONE
    }

    /**
     * Save Punch In attendance to SharedPreferences
     */
    private fun savePunchIn(latitude: Double, longitude: Double, timestamp: Long) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong(KEY_PUNCH_IN_TIME, timestamp)
            putString(KEY_PUNCH_IN_LAT, latitude.toString())
            putString(KEY_PUNCH_IN_LNG, longitude.toString())
            putBoolean(KEY_IS_PUNCHED_IN, true)
            // Clear punch out data from previous day if exists
            remove(KEY_PUNCH_OUT_TIME)
            remove(KEY_PUNCH_OUT_LAT)
            remove(KEY_PUNCH_OUT_LNG)
            apply()
        }

        // Show success message
        Toast.makeText(
            requireContext(),
            getString(R.string.punch_in_success),
            Toast.LENGTH_SHORT
        ).show()

        // Update UI state
        btnPunchIn.isEnabled = false
        btnPunchOut.isEnabled = true

        // Update attendance summary
        updateAttendanceSummary()
    }

    /**
     * Save Punch Out attendance to SharedPreferences
     */
    private fun savePunchOut(latitude: Double, longitude: Double, timestamp: Long) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong(KEY_PUNCH_OUT_TIME, timestamp)
            putString(KEY_PUNCH_OUT_LAT, latitude.toString())
            putString(KEY_PUNCH_OUT_LNG, longitude.toString())
            putBoolean(KEY_IS_PUNCHED_IN, false)
            apply()
        }

        // Show success message
        Toast.makeText(
            requireContext(),
            getString(R.string.punch_out_success),
            Toast.LENGTH_SHORT
        ).show()

        // Update UI state
        btnPunchIn.isEnabled = true
        btnPunchOut.isEnabled = false

        // Update attendance summary
        updateAttendanceSummary()
    }

    /**
     * Restore attendance state from SharedPreferences
     */
    private fun restoreAttendanceState() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPunchedIn = prefs.getBoolean(KEY_IS_PUNCHED_IN, false)

        // Check if stored attendance is from today
        val punchInTime = prefs.getLong(KEY_PUNCH_IN_TIME, 0L)
        val isToday = isSameDay(punchInTime, System.currentTimeMillis())

        if (isToday && punchInTime > 0) {
            // Restore state
            btnPunchIn.isEnabled = !isPunchedIn
            btnPunchOut.isEnabled = isPunchedIn

            if (isPunchedIn) {
                updateLocationStatus(getString(R.string.location_already_captured))
            }

            // Update attendance summary
            updateAttendanceSummary()

            // Restore GPS coordinates display if punch in exists
            restoreGpsCoordinatesDisplay()
        } else {
            // Different day or no data - reset state
            resetAttendanceForNewDay()
        }
    }

    /**
     * Reset attendance state for a new day
     */
    private fun resetAttendanceForNewDay() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        btnPunchIn.isEnabled = true
        btnPunchOut.isEnabled = false
        cardAttendanceSummary.visibility = View.GONE
        tvPunchInPhotoLabel.visibility = View.GONE
        ivPunchInPhoto.visibility = View.GONE
        cardGpsLocation.visibility = View.GONE
        punchInPhotoBitmap = null
    }

    /**
     * Update attendance summary card
     */
    private fun updateAttendanceSummary() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val punchInTime = prefs.getLong(KEY_PUNCH_IN_TIME, 0L)
        val punchOutTime = prefs.getLong(KEY_PUNCH_OUT_TIME, 0L)

        if (punchInTime > 0) {
            // Show summary card
            cardAttendanceSummary.visibility = View.VISIBLE

            // Format punch in time
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvPunchInTime.text = getString(
                R.string.punch_in_time_format,
                timeFormat.format(Date(punchInTime))
            )

            if (punchOutTime > 0) {
                // Show punch out time
                tvPunchOutTime.visibility = View.VISIBLE
                tvPunchOutTime.text = getString(
                    R.string.punch_out_time_format,
                    timeFormat.format(Date(punchOutTime))
                )

                // Calculate and show duration
                val durationMs = punchOutTime - punchInTime
                val hours = (durationMs / (1000 * 60 * 60)) % 24
                val minutes = (durationMs / (1000 * 60)) % 60

                tvTotalDuration.visibility = View.VISIBLE
                tvTotalDuration.text = getString(
                    R.string.duration_format,
                    hours,
                    minutes
                )
            } else {
                // Hide punch out info
                tvPunchOutTime.visibility = View.GONE
                tvTotalDuration.visibility = View.GONE
            }
        } else {
            // Hide summary card
            cardAttendanceSummary.visibility = View.GONE
        }
    }

    /**
     * Update location status text
     */
    private fun updateLocationStatus(status: String) {
        tvLocationStatus.text = status
    }

    /**
     * Display GPS coordinates for Punch In
     * Only visible during and after Punch In operation
     *
     * @param latitude The latitude value from GPS
     * @param longitude The longitude value from GPS
     */
    private fun displayGpsCoordinates(latitude: Double, longitude: Double) {
        // Format coordinates to 6 decimal places for accuracy
        val formattedLatitude = String.format("%.6f", latitude)
        val formattedLongitude = String.format("%.6f", longitude)

        // Update UI elements with GPS data
        tvLatitude.text = "Latitude: $formattedLatitude"
        tvLongitude.text = "Longitude: $formattedLongitude"

        // Show the GPS location card
        cardGpsLocation.visibility = View.VISIBLE

        // Save GPS data to SharedPreferences for persistence
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_punch_in_lat_display", formattedLatitude)
            putString("last_punch_in_lng_display", formattedLongitude)
            apply()
        }
    }

    /**
     * Restore GPS coordinates display if punch in exists today
     */
    private fun restoreGpsCoordinatesDisplay() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val punchInTime = prefs.getLong(KEY_PUNCH_IN_TIME, 0L)

        // Only show GPS card if punch in happened today
        if (punchInTime > 0 && isSameDay(punchInTime, System.currentTimeMillis())) {
            val savedLat = prefs.getString("last_punch_in_lat_display", null)
            val savedLng = prefs.getString("last_punch_in_lng_display", null)

            if (savedLat != null && savedLng != null) {
                tvLatitude.text = "Latitude: $savedLat"
                tvLongitude.text = "Longitude: $savedLng"
                cardGpsLocation.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Reset button states to default
     */
    private fun resetButtonStates() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPunchedIn = prefs.getBoolean(KEY_IS_PUNCHED_IN, false)

        btnPunchIn.isEnabled = !isPunchedIn
        btnPunchOut.isEnabled = isPunchedIn
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        if (timestamp1 == 0L) return false

        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop time updates when view is destroyed
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)

        // Clear photo bitmap to prevent memory leaks
        punchInPhotoBitmap = null
    }

    /**
     * Enum to track current attendance action
     */
    private enum class AttendanceAction {
        NONE,
        PUNCH_IN,
        PUNCH_OUT
    }
}
