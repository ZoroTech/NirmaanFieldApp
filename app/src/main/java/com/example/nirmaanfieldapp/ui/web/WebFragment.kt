package com.example.nirmaanfieldapp.ui.web

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nirmaanfieldapp.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

/**
 * WebFragment - Reusable WebView screen for displaying web content
 *
 * Features:
 * - Dynamic URL and title from arguments
 * - JavaScript, DOM storage, and caching enabled
 * - Loading progress indicator
 * - Offline detection with user-friendly message
 * - Back press handling (navigate within WebView history)
 * - Proper lifecycle management and cleanup
 *
 * Usage:
 * Pass "url" and "title" as arguments when navigating to this fragment
 */
class WebFragment : Fragment() {

    private companion object {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
    }

    // View references
    private lateinit var toolbar: MaterialToolbar
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardOfflineMessage: MaterialCardView

    // Fragment arguments
    private var pageUrl: String? = null
    private var pageTitle: String? = null

    // Back press handler
    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve arguments
        arguments?.let {
            pageUrl = it.getString(ARG_URL)
            pageTitle = it.getString(ARG_TITLE)
        }

        // Setup back press handler
        backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_web, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)

        // Setup toolbar
        setupToolbar()

        // Check internet connectivity
        if (isNetworkAvailable()) {
            // Setup and load WebView
            setupWebView()
            loadUrl()
        } else {
            // Show offline message
            showOfflineMessage()
        }
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)
        cardOfflineMessage = view.findViewById(R.id.cardOfflineMessage)
    }

    /**
     * Setup toolbar with title and back navigation
     */
    private fun setupToolbar() {
        // Set dynamic title or default
        toolbar.title = pageTitle ?: getString(R.string.web_view_title)

        // Setup back button click listener
        toolbar.setNavigationOnClickListener {
            handleBackPress()
        }
    }

    /**
     * Setup WebView with required settings and clients
     */
    private fun setupWebView() {
        webView.apply {
            // Enable JavaScript
            settings.javaScriptEnabled = true

            // Enable DOM storage
            settings.domStorageEnabled = true

            // Enable caching
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

            // Additional recommended settings
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            // Set WebViewClient to handle page loading
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Show progress bar when page starts loading
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Hide progress bar when page finishes loading
                    progressBar.visibility = View.GONE
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    // Hide progress bar on error
                    progressBar.visibility = View.GONE
                }
            }

            // Set WebChromeClient to handle progress updates
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    // Update progress bar
                    progressBar.progress = newProgress

                    // Hide progress bar when complete
                    if (newProgress == 100) {
                        progressBar.visibility = View.GONE
                    } else {
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * Load URL in WebView
     */
    private fun loadUrl() {
        pageUrl?.let { url ->
            webView.loadUrl(url)
        } ?: run {
            // No URL provided - show error or navigate back
            showOfflineMessage()
        }
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Show offline message and hide WebView
     */
    private fun showOfflineMessage() {
        webView.visibility = View.GONE
        cardOfflineMessage.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    /**
     * Handle back press - navigate in WebView history or pop fragment
     */
    private fun handleBackPress() {
        if (::webView.isInitialized && webView.canGoBack()) {
            // WebView has history - go back
            webView.goBack()
        } else {
            // No WebView history - pop fragment
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Cleanup WebView to prevent memory leaks
        webView.apply {
            webView.stopLoading()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove back press callback
        backPressCallback.remove()
    }
}
