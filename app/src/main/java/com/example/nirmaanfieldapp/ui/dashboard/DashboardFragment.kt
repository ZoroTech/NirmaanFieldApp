package com.example.nirmaanfieldapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import com.google.android.material.card.MaterialCardView

/**
 * DashboardFragment - Main dashboard for the Nirmaan Field App
 *
 * Features:
 * - Grid layout with 4 feature cards
 * - Navigation to Attendance and DPR screens
 * - Placeholder toasts for upcoming features (Tasks, Materials)
 * - Clean XML-based UI with MaterialCardView
 * - Professional construction field management interface
 */
class DashboardFragment : Fragment() {

    // View references for dashboard cards
    private lateinit var cardAttendance: MaterialCardView
    private lateinit var cardDpr: MaterialCardView
    private lateinit var cardTasks: MaterialCardView
    private lateinit var cardMaterials: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all card views
        initializeViews(view)

        // Setup click listeners for all cards
        setupClickListeners()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        cardAttendance = view.findViewById(R.id.cardAttendance)
        cardDpr = view.findViewById(R.id.cardDpr)
        cardTasks = view.findViewById(R.id.cardTasks)
        cardMaterials = view.findViewById(R.id.cardMaterials)
    }

    /**
     * Setup click listeners for all dashboard cards
     */
    private fun setupClickListeners() {
        // Attendance Card - Navigate to Attendance screen
        cardAttendance.setOnClickListener {
            navigateToAttendance()
        }

        // DPR Card - Navigate to DPR screen
        cardDpr.setOnClickListener {
            navigateToDpr()
        }

        // Tasks Card - Show coming soon toast
        cardTasks.setOnClickListener {
            showComingSoonToast(getString(R.string.card_tasks_title))
        }

        // Materials Card - Show coming soon toast
        cardMaterials.setOnClickListener {
            showComingSoonToast(getString(R.string.card_materials_title))
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
}
