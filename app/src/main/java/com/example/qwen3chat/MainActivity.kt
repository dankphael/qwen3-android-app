package com.example.qwen3chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.qwen3chat.databinding.ActivityMainBinding
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), ModelDownloadService.DownloadListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val DEFAULT_MODEL_KEY = "qwen3.5-0.8b"
        private const val CAROUSEL_INTERVAL_MS = 4000L

        /** Shared model instance — set once the model is loaded into memory. */
        @Volatile
        var sharedChatViewModel: ChatViewModel? = null
            private set
    }

    enum class DownloadButtonState { IDLE, DOWNLOADING, COMPLETE }

    private lateinit var binding: ActivityMainBinding
    private lateinit var carouselAdapter: CarouselAdapter
    private var downloadButtonState = DownloadButtonState.IDLE

    private val viewModel: ChatViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val deviceCapability by lazy { DeviceCapability(applicationContext) }

    private var currentModelMetadata: ModelMetadata? = null
    private var pendingDownloadMetadata: ModelMetadata? = null

    // Request POST_NOTIFICATIONS permission before starting foreground service
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingDownloadMetadata?.let { startModelDownloadInternal(it) }
        pendingDownloadMetadata = null
    }

    // Carousel auto-scroll
    private val carouselHandler = Handler(Looper.getMainLooper())
    private var carouselRunnable: Runnable? = null
    private var isCarouselRunning = false

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation with NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        setupCarousel()
        setupDownloadButton()

        initializeModel()
    }

    override fun onResume() {
        super.onResume()
        ModelDownloadService.listener = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        ModelDownloadService.listener = null
        stopCarousel()
    }

    override fun onDestroy() {
        ModelDownloadService.listener = null
        viewModel.cancelGeneration()
        stopCarousel()
        super.onDestroy()
    }

    /** Free native memory when the system asks us to trim memory. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            if (!viewModel.isGenerating.value) {
                viewModel.unloadModel()
                sharedChatViewModel = null
            }
        }
    }

    private fun setupCarousel() {
        carouselAdapter = CarouselAdapter(CarouselCard.CARDS)
        binding.viewPagerCarousel.adapter = carouselAdapter
        binding.viewPagerCarousel.setPageTransformer(CarouselAdapter.CarouselScaleTransformer())
        binding.viewPagerCarousel.offscreenPageLimit = 1

        // Page indicator dots
        com.google.android.material.tabs.TabLayoutMediator(
            binding.tabLayoutDots,
            binding.viewPagerCarousel
        ) { _, _ -> }.attach()

        // Pause auto-scroll when user swipes
        binding.viewPagerCarousel.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING) {
                        stopCarousel()
                    } else if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) {
                        startCarousel()
                    }
                }
            }
        )
    }

    private fun setupDownloadButton() {
        binding.btnDownload.setOnClickListener {
            when (downloadButtonState) {
                DownloadButtonState.IDLE -> {
                    currentModelMetadata?.let { startModelDownload(it) }
                }
                DownloadButtonState.DOWNLOADING -> {
                    val intent = Intent(this, ModelDownloadService::class.java).apply {
                        action = ModelDownloadService.ACTION_CANCEL
                    }
                    startService(intent)
                    updateDownloadButton(DownloadButtonState.IDLE)
                }
                DownloadButtonState.COMPLETE -> {
                    currentModelMetadata?.let { loadModelIntoMemory(it) }
                }
            }
        }
    }

    private fun updateDownloadButton(state: DownloadButtonState) {
        downloadButtonState = state
        val (text, bgRes, textColorRes) = when (state) {
            DownloadButtonState.IDLE -> Triple("Download", R.drawable.bg_download_button_idle, R.color.on_primary)
            DownloadButtonState.DOWNLOADING -> Triple("Cancel", R.drawable.bg_download_button_cancel, R.color.on_surface)
            DownloadButtonState.COMPLETE -> Triple("Open", R.drawable.bg_download_button_complete, R.color.on_primary)
        }
        binding.btnDownload.apply {
            setText(text)
            setBackgroundResource(bgRes)
            setTextColor(ContextCompat.getColor(this@MainActivity, textColorRes))
            // Morph animation
            animate().scaleX(0.95f).scaleY(0.95f).setDuration(125).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(125).start()
            }.start()
        }
    }

    private fun startCarousel() {
        if (isCarouselRunning) return
        isCarouselRunning = true
        carouselRunnable = object : Runnable {
            override fun run() {
                if (!isCarouselRunning) return
                val count = carouselAdapter.itemCount
                if (count > 0) {
                    val current = binding.viewPagerCarousel.currentItem
                    binding.viewPagerCarousel.currentItem = (current + 1) % count
                }
                carouselHandler.postDelayed(this, CAROUSEL_INTERVAL_MS)
            }
        }
        carouselHandler.postDelayed(carouselRunnable!!, CAROUSEL_INTERVAL_MS)
    }

    private fun stopCarousel() {
        isCarouselRunning = false
        carouselRunnable?.let { carouselHandler.removeCallbacks(it) }
        carouselRunnable = null
    }

    private fun initializeModel() {
        modelPreferences.migrateFromV1(this)

        val selectedModelKey = modelPreferences.getSelectedModelKey()

        if (selectedModelKey == null && !modelPreferences.isFirstLaunchDone()) {
            val intent = Intent(this, ModelSelectionActivity::class.java)
            startActivityForResult(intent, ModelSelectionActivity.REQUEST_CODE)
            return
        }

        val key = selectedModelKey ?: DEFAULT_MODEL_KEY
        proceedWithModel(key)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ModelSelectionActivity.REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedKey = data?.getStringExtra(ModelSelectionActivity.EXTRA_SELECTED_MODEL_KEY)
                ?: DEFAULT_MODEL_KEY
            proceedWithModel(selectedKey)
        } else if (requestCode == ModelSelectionActivity.REQUEST_CODE && resultCode != RESULT_OK) {
            proceedWithModel(DEFAULT_MODEL_KEY)
        }
    }

    private fun proceedWithModel(modelKey: String) {
        val metadata = try {
            ModelMetadata.findByKey(modelKey)
        } catch (e: IllegalArgumentException) {
            ModelMetadata.findByKey(DEFAULT_MODEL_KEY)
        }
        currentModelMetadata = metadata

        modelPreferences.saveSelectedModelKey(metadata.key)
        if (!modelPreferences.isFirstLaunchDone()) {
            modelPreferences.markFirstLaunchDone()
        }

        if (isModelDownloaded(metadata)) {
            loadModelIntoMemory(metadata)
        } else {
            startModelDownload(metadata)
        }
    }

    private fun isModelDownloaded(metadata: ModelMetadata): Boolean {
        return ModelDownloadService.isModelDownloaded(this, metadata.filename)
    }

    private fun startModelDownload(metadata: ModelMetadata) {
        binding.downloadContainer.visibility = View.VISIBLE
        binding.progressSection.visibility = View.VISIBLE
        binding.downloadStatusText.visibility = View.GONE
        binding.progressFill.layoutParams.width = 0
        binding.progressFill.requestLayout()
        binding.tvPercentage.text = "0%"
        binding.tvSpeed.text = ""
        binding.tvEta.text = ""

        updateDownloadButton(DownloadButtonState.DOWNLOADING)

        startCarousel()

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadMetadata = metadata
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        startModelDownloadInternal(metadata)
    }

    private fun startModelDownloadInternal(metadata: ModelMetadata) {
        val intent = Intent(this, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_START
            putExtra(ModelDownloadService.EXTRA_MODEL_KEY, metadata.key)
            putExtra(ModelDownloadService.EXTRA_MODEL_URL, metadata.downloadUrl)
            putExtra(ModelDownloadService.EXTRA_MODEL_FILENAME, metadata.filename)
            if (metadata.sha256 != null) {
                putExtra(ModelDownloadService.EXTRA_MODEL_SHA256, metadata.sha256)
            }
        }
        startForegroundService(intent)
    }

    // --- DownloadListener callbacks ---

    override fun onProgress(downloaded: Long, total: Long) {
        runOnUiThread {
            binding.downloadContainer.visibility = View.VISIBLE
            binding.progressSection.visibility = View.VISIBLE
            binding.downloadStatusText.visibility = View.GONE

            if (total > 0) {
                val progressFloat = downloaded.toFloat() / total.toFloat()
                val progressPercent = (progressFloat * 100).toInt()

                val containerWidth = (binding.progressFill.parent as? View)?.width ?: 0
                if (containerWidth > 0) {
                    binding.progressFill.layoutParams.width = (containerWidth * progressFloat).toInt()
                    binding.progressFill.requestLayout()
                }

                binding.tvPercentage.text = "$progressPercent%"

                val downloadedMB = downloaded / (1024.0 * 1024.0)
                binding.tvSpeed.text = String.format("%.1f MB downloaded", downloadedMB)

                val remainingBytes = total - downloaded
                val remainingMB = remainingBytes / (1024.0 * 1024.0)
                binding.tvEta.text = String.format("%.0f MB remaining", remainingMB)
            } else {
                val downloadedMB = downloaded / (1024.0 * 1024.0)
                binding.tvSpeed.text = String.format("%.1f MB downloaded", downloadedMB)
                binding.tvEta.text = ""
            }
        }
    }

    override fun onComplete() {
        runOnUiThread {
            stopCarousel()
            updateDownloadButton(DownloadButtonState.COMPLETE)
            currentModelMetadata?.let { loadModelIntoMemory(it) }
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            stopCarousel()
            updateDownloadButton(DownloadButtonState.IDLE)
            binding.downloadContainer.visibility = View.GONE
            Toast.makeText(this, "Download failed: $message", Toast.LENGTH_LONG).show()
        }
    }

    // --- Model loading ---

    private fun loadModelIntoMemory(metadata: ModelMetadata) {
        if (viewModel.isReady) return

        binding.downloadContainer.visibility = View.VISIBLE
        binding.progressSection.visibility = View.GONE
        binding.downloadStatusText.visibility = View.VISIBLE
        binding.downloadStatusText.text = "Loading model into memory..."

        startCarousel()

        viewModel.loadModel(metadata,
            onReady = {
                stopCarousel()
                binding.downloadContainer.visibility = View.GONE
                sharedChatViewModel = viewModel
            },
            onError = { errorMsg ->
                stopCarousel()
                binding.downloadContainer.visibility = View.GONE
                Toast.makeText(this, "Failed to load model: $errorMsg", Toast.LENGTH_LONG).show()
            }
        )
    }
}
