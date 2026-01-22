package com.example.nirmaanfieldapp.ui.splash

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashFragment - Entry point for the Nirmaan Construction Field Management App
 *
 * Displays the app logo, name, and loading indicator while checking user authentication status.
 * Automatically navigates to the appropriate screen after a brief delay.
 */
class SplashFragment : Fragment() {

    // SharedPreferences constants
    private companion object {
        const val PREFS_NAME = "nirmaan_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val SPLASH_DELAY_MS = 2000L // 2 seconds
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the splash layout
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start splash timer and navigation logic
        navigateAfterDelay()
    }

    /**
     * Displays splash screen for specified duration, then navigates based on login status
     * Uses lifecycleScope to ensure coroutine is cancelled if fragment is destroyed
     */
    private fun navigateAfterDelay() {
        // Use lifecycleScope to automatically cancel coroutine when fragment is destroyed
        // This prevents memory leaks and navigation after fragment is no longer active
        lifecycleScope.launch {
            // Wait for splash delay
            delay(SPLASH_DELAY_MS)

            // Check if user is logged in
            val isLoggedIn = checkLoginStatus()

            // Navigate to appropriate destination
            navigateToNextScreen(isLoggedIn)
        }
    }

    /**
     * Checks if user is currently logged in using SharedPreferences
     *
     * @return true if user is logged in, false otherwise
     */
    private fun checkLoginStatus(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Navigates to the next screen based on login status
     *
     * @param isLoggedIn true to navigate to dashboard, false to navigate to login
     */
    private fun navigateToNextScreen(isLoggedIn: Boolean) {
        // Ensure fragment is still added to avoid IllegalStateException
        if (!isAdded) return

        try {
            val navController = findNavController()

            if (findNavController().currentDestination?.id != R.id.splashFragment) {
                return
            }

            if (isLoggedIn) {
                // User is logged in - navigate to dashboard
                navController.navigate(R.id.action_splashFragment_to_dashboardFragment)
            } else {
                // User is not logged in - navigate to login screen
                navController.navigate(R.id.action_splashFragment_to_loginFragment)
            }
        } catch (e: Exception) {
            // Handle navigation error gracefully
            e.printStackTrace()
        }
    }
}
