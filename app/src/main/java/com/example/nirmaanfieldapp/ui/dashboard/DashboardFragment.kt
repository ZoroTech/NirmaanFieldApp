package com.example.nirmaanfieldapp.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * DashboardFragment - Main dashboard for the Nirmaan Field App
 *
 * Features:
 * - Grid layout with 4 feature cards
 * - Navigation to Attendance and DPR screens
 * - Logout functionality with session management
 * - Offline banner for field-ready UX
 * - Clean XML-based UI with MaterialCardView
 * - Professional construction field management interface
 */
class DashboardFragment : Fragment() {

    // SharedPreferences constants
    private companion object {
        const val PREFS_NAME = "nirmaan_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // View references for dashboard cards
    private lateinit var cardAttendance: MaterialCardView
    private lateinit var cardDpr: MaterialCardView
    private lateinit var cardTasks: MaterialCardView
    private lateinit var cardMaterials: MaterialCardView

    // View references for logout and offline banner
    private lateinit var btnLogout: MaterialButton
    private lateinit var cardOfflineBanner: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all views
        initializeViews(view)

        // Setup click listeners for all cards and buttons
        setupClickListeners()

        // Check connectivity and show/hide offline banner
        checkConnectivityAndUpdateUI()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        // Dashboard cards
        cardAttendance = view.findViewById(R.id.cardAttendance)
        cardDpr = view.findViewById(R.id.cardDpr)
        cardTasks = view.findViewById(R.id.cardTasks)
        cardMaterials = view.findViewById(R.id.cardMaterials)

        // Logout button and offline banner
        btnLogout = view.findViewById(R.id.btnLogout)
        cardOfflineBanner = view.findViewById(R.id.cardOfflineBanner)
    }

    /**
     * Setup click listeners for all dashboard cards and buttons
     */
    private fun setupClickListeners() {
        // Logout Button - Clear session and navigate to login
        btnLogout.setOnClickListener {
            handleLogout()
        }

        // Attendance Card - Navigate to Attendance screen
        cardAttendance.setOnClickListener {
            navigateToAttendance()
        }

        // DPR Card - Navigate to DPR screen
        cardDpr.setOnClickListener {
            navigateToDpr()
        }

        // Tasks → WebView
        cardTasks.setOnClickListener {
            val bundle = bundleOf(
                "url" to "\n" + "https://nirmaan-frontend.vercel.app/task",
                "title" to "Tasks"
            )
            findNavController().navigate(R.id.webFragment, bundle)
        }

        // Materials → WebView
        cardMaterials.setOnClickListener {
            val bundle = bundleOf(
                "url" to "https://nirmaan-frontend.vercel.app/invoice",
                "title" to "Materials"
            )
            findNavController().navigate(R.id.webFragment, bundle)
        }
    }

    /**
     * Navigate to Attendance screen using Navigation Component
     */
    private fun navigateToAttendance() {
        try {
            findNavController().navigate(R.id.action_dashboardFragment_to_attendanceFragment)
        } catch (e: Exception) {
            // Handle navigation error gracefully
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Unable to navigate to Attendance",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Navigate to DPR (Daily Progress Report) screen using Navigation Component
     */
    private fun navigateToDpr() {
        try {
            findNavController().navigate(R.id.action_dashboardFragment_to_dprFragment)
        } catch (e: Exception) {
            // Handle navigation error gracefully
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Unable to navigate to DPR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Show a "Coming Soon" toast for features not yet implemented
     *
     * @param featureName The name of the feature that is coming soon
     */
    private fun showComingSoonToast(featureName: String) {
        Toast.makeText(
            requireContext(),
            "$featureName - ${getString(R.string.coming_soon)}",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Handle logout action
     *
     * Steps:
     * 1. Clear login session from SharedPreferences
     * 2. Show success message
     * 3. Navigate to LoginFragment with cleared back stack
     */
    private fun handleLogout() {
        // Clear login session from SharedPreferences
        clearLoginSession()

        // Show logout success message
        Toast.makeText(
            requireContext(),
            getString(R.string.logout_success),
            Toast.LENGTH_SHORT
        ).show()

        // Navigate to LoginFragment and clear back stack
        navigateToLogin()
    }

    /**
     * Clear login session by removing the is_logged_in flag from SharedPreferences
     */
    private fun clearLoginSession() {
        val sharedPreferences = requireContext().getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    /**
     * Navigate to LoginFragment and clear navigation back stack
     * Uses NavOptions to remove DashboardFragment from back stack
     */
    private fun navigateToLogin() {
        try {
            // Create NavOptions to clear back stack
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.dashboardFragment, inclusive = true)
                .build()

            // Navigate to LoginFragment
            findNavController().navigate(
                R.id.loginFragment,
                null,
                navOptions
            )
        } catch (e: Exception) {
            // Handle navigation error gracefully
            e.printStackTrace()
        }
    }

    /**
     * Check internet connectivity and update offline banner visibility
     *
     * Uses ConnectivityManager and NetworkCapabilities to detect internet connection
     * Shows offline banner when no internet is available
     * Hides offline banner when internet is available
     */
    private fun checkConnectivityAndUpdateUI() {
        val isConnected = isNetworkAvailable()

        // Show offline banner if not connected, hide if connected
        cardOfflineBanner.visibility = if (isConnected) View.GONE else View.VISIBLE
    }

    /**
     * Check if network is available and has internet capability
     *
     * @return true if internet is available, false otherwise
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if network has internet capability
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
