package com.example.nirmaanfieldapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import com.google.android.material.button.MaterialButton

/**
 * DashboardFragment - Main dashboard for the Nirmaan Field App
 *
 * This fragment serves as the main hub after login
 */
class DashboardFragment : Fragment() {

    private lateinit var btnGoToAttendance: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        btnGoToAttendance = view.findViewById(R.id.btnGoToAttendance)

        // Setup click listeners
        btnGoToAttendance.setOnClickListener {
            navigateToAttendance()
        }
    }

    /**
     * Navigate to Attendance screen
     */
    private fun navigateToAttendance() {
        try {
            findNavController().navigate(R.id.action_dashboardFragment_to_attendanceFragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
