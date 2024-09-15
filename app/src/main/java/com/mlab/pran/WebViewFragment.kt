package com.mlab.pran

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mlab.pran.databinding.FragmentWebViewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class WebViewFragment : Fragment() {
    private lateinit var binding: FragmentWebViewBinding
    private val viewModel by activityViewModels<MainViewModel>()

    private var permissionRequest: PermissionRequest? = null
    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "Permission granted: $isGranted")
            permissionRequest?.apply {
                if (isGranted) {
                    grant(resources)
//                    if(photoUri!= null)
//                        launcherCamera.launch(photoUri)
                }
                else deny()
            }
        }

    private var filePickerCallback: ValueCallback<Array<Uri>>? = null
    private val launcherFilePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "registerForActivityResult: ${it.data?.data}")
            it.data?.apply {
                clipData?.apply {
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until itemCount) {
                        uris.add(getItemAt(i).uri)
                    }
                    Log.d(TAG, "registerForActivityResult: $uris")
                    filePickerCallback?.onReceiveValue(uris.toTypedArray())
                } ?: data?.apply {
                    filePickerCallback?.onReceiveValue(arrayOf(this))
                }
            }
        }
    private val launcherSingleMediaPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            Log.d(TAG, "registerForActivityResult: $it")
            it?.let {
                filePickerCallback?.onReceiveValue(arrayOf(it))
            }
        }

    private val launcherMultipleMediaPicker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
            Log.d(TAG, "registerForActivityResult: $it")
            filePickerCallback?.onReceiveValue(it.toTypedArray())
        }

//    private var photoUri: Uri? = null
//    private val launcherCamera =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) {
//            Log.d(TAG, "registerForActivityResult: $it")
//            if (it && photoUri != null)
//                filePickerCallback?.onReceiveValue(listOfNotNull(photoUri).toTypedArray())
//            photoUri = null
//        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
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
                arguments?.getString("url")?.let {
                    webView.loadUrl(it)
                }
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
            val drawer = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
            if (drawer?.isDrawerOpen(GravityCompat.START) == true)
                drawer.close()
            else if (binding.webView.canGoBack())
                binding.webView.goBack()
            else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
                isEnabled = true
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val dbPath =
            context?.getDir("database", Context.MODE_PRIVATE)?.path

        binding.webView.apply {
            settings.apply {
                setSupportMultipleWindows(true)
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
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
//                allowFileAccessFromFileURLs = true
//                allowUniversalAccessFromFileURLs = true
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

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    return super.shouldInterceptRequest(view, request)
                }

                @Deprecated("Deprecated")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    viewModel.url = url
                    viewModel.setLastBrowsedLink(url)
                    when {
                        url.startsWith("intent://") ->
                            startActivity(Intent.parseUri(url, Intent.URI_INTENT_SCHEME))

                        url.startsWith("tel:") ->
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))

                        url.startsWith("mailto:") ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

                        url.contains("youtube.com/") || url.contains("youtu.be/") ->
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.google.android.youtube")
                            )

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
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    filePickerCallback = filePathCallback
                    val acceptTypes= fileChooserParams?.acceptTypes?.toList().orEmpty()
                    Log.d(TAG, "onShowFileChooser: $acceptTypes")
//                    val captureEnabled = fileChooserParams?.isCaptureEnabled ?: false
                    val isPhoto = acceptTypes.find {
                        it.contains("image/") || it == ".jpg" || it == ".jpeg" || it == ".png"
                    } != null
                    val isVideo = acceptTypes.find {
                        it.contains("video/") || it.equals(".mp4") || it == ".avi" || it == ".mkv"
                    } != null
//                    val capturePhoto = captureEnabled && isPhoto
//                    val captureVideo = captureEnabled && isVideo

                    when (fileChooserParams?.mode) {
                        FileChooserParams.MODE_OPEN -> {
//                            if(capturePhoto){
//                                val directory = File(context.filesDir, "camera_images")
//                                if (!directory.exists())
//                                    directory.mkdirs()
//                                val file = File(directory, "${System.currentTimeMillis()}.jpg")
////                                photoUri = Uri.fromFile(file)
//                                photoUri = FileProvider. getUriForFile (requireContext(), requireContext().applicationContext.packageName +".provider", file)
//
//                                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
//                                    == PackageManager.PERMISSION_GRANTED
//                                ) launcherCamera.launch(photoUri)
//                                else
//                                    permissionRequestLauncher.launch(android.Manifest.permission.CAMERA)
//                            }
//                            else
                            if (isPhoto)
                                launcherSingleMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            else if (isVideo)
                                launcherSingleMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            else
                                launcherFilePicker.launch(Intent.createChooser(Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT), "Select a file"))
                            isFilePickerActive = true
                        }
                        FileChooserParams.MODE_OPEN_MULTIPLE ->{
                            if(isPhoto)
                                launcherMultipleMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            else if(isVideo)
                                launcherMultipleMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            else {
                                launcherFilePicker.launch(
                                    Intent.createChooser(
                                        Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
                                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
                                        "Select files"
                                    )
                                )
                            }
                            isFilePickerActive = true
                        }

                    }
                    return true
//                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    permissionRequest = request
                    Log.d(TAG, "onPermissionRequest")
                    lifecycleScope.launch {
                        Log.d(TAG, request.origin.toString())
                        request.resources.forEach {
                            when (it) {
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                                    permissionRequestLauncher.launch(android.Manifest.permission.CAMERA)

                                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                                    permissionRequestLauncher.launch(android.Manifest.permission.RECORD_AUDIO)

                                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ->
                                    permissionRequestLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
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
    }

    override fun onResume() {
        super.onResume()
        arguments?.getString("url").website.apply {
            val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
            toolbar?.title = if (pageName == "Home") "PRAN-RFL Group" else pageName
        }
        if(!isFilePickerActive)
            binding.webView.loadUrl(viewModel.getLastBrowsedLink())
        else
            isFilePickerActive = false
    }

    override fun onPause() {
        super.onPause()
        binding.webView.url?.let {
            viewModel.setLastBrowsedLink(it, arguments?.getInt("index") ?: 0)
        }
    }

    companion object {
        private const val TAG = "WebViewFragment"
        private var isFilePickerActive = false
    }
}