package com.mlab.pran.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mlab.pran.R
import com.mlab.pran.Website
import com.mlab.pran.databinding.FragmentWebViewBinding
import com.mlab.pran.pageName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class WebViewFragment : Fragment() {
    private lateinit var binding: FragmentWebViewBinding
    private var url: String? = null

    private val permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        Log.d(TAG, "Permission granted: $it")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        setupListener()
    }

    private fun setupListener() {
        binding.apply {
            btnBackward.setOnClickListener {
                if (webView.canGoBack())
                    webView.goBack()
            }
            btnForward.setOnClickListener {
                if (webView.canGoForward())
                    webView.goForward()
            }
            btnHome.setOnClickListener {
                url?.let { webView.loadUrl(it) }
            }
            btnRefresh.setOnClickListener {
                binding.progressBar.visibility = View.VISIBLE
                webView.reload()
            }
            btnMenu.setOnClickListener {
                activity?.findViewById<DrawerLayout>(R.id.drawer_layout)?.open()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
                isEnabled = true
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        url = arguments?.getString("url")
        currentUrl = url
        val dbPath =
            context?.getDir("database", Context.MODE_PRIVATE)?.path

        binding.webView.apply {
            settings.apply {
                setSupportMultipleWindows(true)
                setSupportZoom(true)
                builtInZoomControls = true
                domStorageEnabled = true
                databaseEnabled = true
                databasePath = dbPath
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                useWideViewPort = true
                loadWithOverviewMode = true
                loadsImagesAutomatically = true
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                allowContentAccess = true
                allowFileAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }
            binding.progressBar.progress = progress
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    lifecycleScope.launch {
                        delay(500)
                        binding.progressBar.visibility = View.INVISIBLE
                    }
                }

                @Deprecated("Deprecated")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    currentUrl = url
                    when {
                        url.startsWith("tel:") ->
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))

                        url.startsWith("mailto:") ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

                        else -> {
                            view.loadUrl(url)
                            binding.progressBar.visibility = View.VISIBLE
                        }
                    }
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                // Grant permissions for cam
                override fun onPermissionRequest(request: PermissionRequest) {
                    Log.d(TAG, "onPermissionRequest")
                    lifecycleScope.launch {
                        Log.d(TAG, request.origin.toString())
                        if (request.toString() == "file:///") {
                            Log.d(TAG, "GRANTED")
                            request.grant(request.resources)
                        } else {
                            Log.d(TAG, "DENIED")
                            request.deny()
                        }
                    }
                }

            }
        }
        url?.let { binding.webView.loadUrl(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentUrl = null
    }
    companion object {
        private const val TAG = "WebViewFragment"
        var currentUrl: String? = null
    }
}