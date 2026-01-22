package com.example.nirmaanfieldapp.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LoginFragment - Handles user authentication for the Nirmaan Field App
 *
 * Features:
 * - Mobile number and password input validation
 * - Loading state management
 * - SharedPreferences for login persistence
 * - Navigation to dashboard on successful login
 */
class LoginFragment : Fragment() {

    // SharedPreferences constants
    private companion object {
        const val PREFS_NAME = "nirmaan_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val LOGIN_DELAY_MS = 1500L
        const val MOBILE_NUMBER_LENGTH = 10
    }

    // View references
    private lateinit var tilMobileNumber: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etMobileNumber: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Set up click listeners
        setupClickListeners()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        tilMobileNumber = view.findViewById(R.id.tilMobileNumber)
        tilPassword = view.findViewById(R.id.tilPassword)
        etMobileNumber = view.findViewById(R.id.etMobileNumber)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        progressBar = view.findViewById(R.id.progressBar)
    }

    /**
     * Set up click listeners for interactive elements
     */
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        // Clear error messages when user starts typing
        etMobileNumber.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilMobileNumber.error = null
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
    }

    /**
     * Validates input fields and initiates login process
     */
    private fun attemptLogin() {
        // Clear previous errors
        tilMobileNumber.error = null
        tilPassword.error = null

        // Get input values
        val mobileNumber = etMobileNumber.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate inputs
        if (!validateInputs(mobileNumber, password)) {
            return
        }

        // Proceed with login
        performLogin()
    }

    /**
     * Validates mobile number and password inputs
     *
     * @param mobileNumber The mobile number entered by user
     * @param password The password entered by user
     * @return true if all validations pass, false otherwise
     */
    private fun validateInputs(mobileNumber: String, password: String): Boolean {
        var isValid = true

        // Validate mobile number
        if (mobileNumber.isEmpty()) {
            tilMobileNumber.error = getString(R.string.error_mobile_empty)
            isValid = false
        } else if (mobileNumber.length != MOBILE_NUMBER_LENGTH) {
            tilMobileNumber.error = getString(R.string.error_mobile_invalid)
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.error_password_empty)
            isValid = false
        }

        return isValid
    }

    /**
     * Performs login operation with loading state
     * Simulates network call and saves login state on success
     */
    private fun performLogin() {
        // Use viewLifecycleOwner.lifecycleScope to ensure coroutine is cancelled
        // when the view is destroyed, preventing memory leaks
        viewLifecycleOwner.lifecycleScope.launch {
            // Show loading state
            setLoadingState(true)

            // Simulate network delay (replace with actual API call in production)
            delay(LOGIN_DELAY_MS)

            // Check if fragment is still attached to avoid crashes
            if (!isAdded) return@launch

            // Simulate successful login
            handleLoginSuccess()
        }
    }

    /**
     * Handles successful login by saving state and navigating to dashboard
     */
    private fun handleLoginSuccess() {
        // Save login state to SharedPreferences
        saveLoginState()

        // Show success message
        Toast.makeText(
            requireContext(),
            getString(R.string.login_success),
            Toast.LENGTH_SHORT
        ).show()

        // Hide loading state
        setLoadingState(false)

        // Navigate to dashboard and remove login from back stack
        navigateToDashboard()
    }

    /**
     * Saves login state to SharedPreferences
     */
    private fun saveLoginState() {
        val sharedPreferences = requireContext().getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Navigates to DashboardFragment and removes LoginFragment from back stack
     */
    private fun navigateToDashboard() {
        // Ensure fragment is still added before navigating
        if (!isAdded) return

        try {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        } catch (e: Exception) {
            // Handle navigation error gracefully
            e.printStackTrace()
        }
    }

    /**
     * Manages loading state UI
     *
     * @param isLoading true to show loading state, false to hide
     */
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Disable input fields
            etMobileNumber.isEnabled = false
            etPassword.isEnabled = false

            // Disable and show loading on button
            btnLogin.isEnabled = false

            // Show progress bar
            progressBar.visibility = View.VISIBLE
        } else {
            // Enable input fields
            etMobileNumber.isEnabled = true
            etPassword.isEnabled = true

            // Enable button
            btnLogin.isEnabled = true

            // Hide progress bar
            progressBar.visibility = View.GONE
        }
    }
}
