package com.BlindDetection.feature.home

// Import necessary libraries


import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.BlindDetection.R
import com.BlindDetection.databinding.ActivityMainBinding
import com.BlindDetection.feature.home.Constants.LABELS_PATH
import com.BlindDetection.feature.home.Constants.MODEL_PATH

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// MainActivity class implementing DetectorListener interface
class MainActivity : AppCompatActivity(), DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private var isFrontCamera = false
    private var isFlashOn = false
    private var flashMode: FlashMode = FlashMode.OFF
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var vibrator: Vibrator
    private lateinit var headsetController: HeadsetButtonController
    private var isHeadsetControllerRegistered = false
    private lateinit var simpleHeadsetReceiver: SimpleHeadsetReceiver

    private var pressCount = 0
    private var lastPressTime = 0L
    private val MULTI_PRESS_TIMEOUT = 500L // Time window for multi-press detection
    private val handler = Handler(Looper.getMainLooper())

    // Single headset receiver implementation
    private var currentLanguage = "en" // Default to English



    private lateinit var locationTracker: LocationTracker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationTracker = LocationTracker(this)
        if (hasAllPermissions()) {
            // If we have all permissions, start with guardian setup
            startGuardianSetup()
        } else {
            // Request permissions first
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Initialize the share location button
//        setupLocationButton()
        requestLocationPermissions()
        addLocationSharingButton()


        initializeServices()
        checkHeadsetStatus() // Add this line
        setupHeadsetReceiver()
        setupAudioFocus()
        initializeDetector()
        setupUI()
        initializeCamera()
        Log.d(TAG, "onCreate completed")

    }
    // Add this to your MainActivity.kt after setupCameraButtons() method
    private fun setupViewAllObjectsButton() {
        // Create CardView
        val cardView = CardView(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 32
                rightMargin = 32
            }
            radius = resources.getDimension(R.dimen.card_corner_radius) // Or use a specific value like 8f
            cardElevation = resources.getDimension(R.dimen.card_elevation) // Or use a specific value like 4f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white)) // Or any color you want
        }

        // Create button inside CardView
        val viewAllObjectsButton = Button(this).apply {
            setText("VIEW\nOBJECT")
            textSize = 10f // Smaller text size
            setCompoundDrawablesWithIntrinsicBounds(0,  0, 0, 0)
//            setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_preview_24, 0, 0, 0)

            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0) // Compact padding
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            contentDescription = "View all detectable objects"
        }

        // Add button to CardView
        val container = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            addView(viewAllObjectsButton)
        }

        cardView.addView(container)
        binding.cameraContainer.addView(cardView)

        viewAllObjectsButton.setOnClickListener {
            openObjectsListActivity()
        }
    }
    private fun openObjectsListActivity() {
        val intent = Intent(this, ObjectsListActivity::class.java)
        startActivity(intent)
    }
    private fun initializeServices() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Add this to your MainActivity class

    private fun setupHeadsetReceiver() {
        Log.d(TAG, "Setting up headset receiver")

        // Register for media button events with AudioManager
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(this@MainActivity, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mediaButtonIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        audioManager.registerMediaButtonEventReceiver(pendingIntent)
        Log.d(TAG, "Registered media button event receiver")

        // Check headset status
        val isHeadsetConnected = audioManager.isWiredHeadsetOn
        Log.d(TAG, "Headset connected: $isHeadsetConnected")
    }
    private val headsetButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Headset button receiver: ${intent?.action}")
            when (intent?.action) {
                HeadsetButtonController.ACTION_SINGLE_PRESS -> {
                    Log.d(TAG, "Processing single press")
                    if (requestAudioFocus()) {
                        // Play tutorial message
                        val tutorialMessage = """
                        Welcome to the object detection system. 
                        Single press plays this tutorial.
                        Double press scans objects in front of you.
                        Triple press activates voice commands.
                        You can say 'what's in front' or 'what do you see' to detect objects.
                        Say 'stop listening' to end voice recognition.
                    """.trimIndent()

                        // Provide haptic feedback
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(100)
                        }

                        detector.speak(tutorialMessage, true)
                    }
                }

                HeadsetButtonController.ACTION_DOUBLE_PRESS -> {
                    Log.d(TAG, "Processing double press")
                    if (requestAudioFocus()) {
                        // Provide haptic feedback - double pulse
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
                        }

                        // Scan and announce objects
                        detector.speak("Scanning objects in front of you", true)
                        detector.announceAllObjects(true)
                    }
                }

                HeadsetButtonController.ACTION_TRIPLE_PRESS -> {
                    Log.d(TAG, "Processing triple press")
                    if (requestAudioFocus()) {
                        // Provide haptic feedback - triple pulse
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100, 100, 100), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
                        }

                        // Activate voice recognition
                        detector.toggleVoiceRecognition()
                    }
                }
            }
        }
    }
    private val microphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MicrophoneButtonReceiver.ACTION_SINGLE_PRESS -> {
                    // Play tutorial message
                    val tutorialMessage = """
                Welcome to the object detection system. 
                Single press for this tutorial.
                Double press to scan objects directly in front of you.
                Triple press to activate voice commands.
                Four presses to send your location to guardians.
                Say "what's in front" or "what do you see" to detect objects.
                Say "stop listening" to end voice recognition.
            """.trimIndent()
                    detector.speak(tutorialMessage, true)
                }
                MicrophoneButtonReceiver.ACTION_DOUBLE_PRESS -> {
                    // Scan objects directly in front
                    if (requestAudioFocus()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        detector.speak("Scanning objects directly in front", true)
                        detector.announceAllObjects(true)
                    }
                }
                MicrophoneButtonReceiver.ACTION_TRIPLE_PRESS -> {
                    // Activate voice recognition
                    if (requestAudioFocus()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        detector.toggleVoiceRecognition()
                    }
                }
                MicrophoneButtonReceiver.ACTION_QUADRUPLE_PRESS -> {
                    // Send emergency location
                    if (requestAudioFocus()) {
                        // Distinct vibration pattern for emergency
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(
                                longArrayOf(0, 100, 100, 100, 100, 100, 100, 200), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100, 100, 200), -1)
                        }

                        // Speak and send location
                        detector.speak("Sending emergency location to your guardians", true)

                        if (locationTracker.isLocationAvailable()) {
                            if (locationTracker.hasGuardians()) {
                                locationTracker.shareLocationWithGuardians(true) // Emergency parameter
                            } else {
                                detector.speak("No guardians found. Please add guardians first.", true)
                                locationTracker.showManageGuardiansDialog()
                            }
                        } else {
                            detector.speak("Getting location. Please try again in a moment.", true)
                        }
                    }
                }
            }
        }
    }

    private fun setupAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> detector.stopListening()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> detector.pauseListening()
                        AudioManager.AUDIOFOCUS_GAIN -> detector.resumeListening()
                    }
                }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!)
        }
    }

    private fun initializeDetector() {
        // Add TensorFlow GPU dependency in your build.gradle first:
        // implementation 'org.tensorflow:tensorflow-lite-gpu:2.9.0'

        // Create the detector with GPU enabled by default
        detector = Detector(
            baseContext,
            MODEL_PATH,
            LABELS_PATH,
            this,
            vibrator,
            isGpu = true  // Add this parameter to enable GPU
        )
        detector.setup()
    }
    private fun setupUI() {
        setupVoiceCommandButton()
        setupDebugButton()
        setupCameraButtons()
        addLocationSharingButton()
        setupViewAllObjectsButton() // Add this line

        // Add manage guardians button
        addManageGuardiansButton()
    }

    private fun setupVoiceCommandButton() {
        val voiceCommandButton = ImageButton(this).apply {
            setImageResource(R.color.colorBlack)
//            setImageResource(R.color.colorBlack)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE // Make it invisible

        }
        binding.cameraContainer.addView(voiceCommandButton)
        voiceCommandButton.setOnClickListener {
            if (requestAudioFocus()) {
                detector.toggleVoiceRecognition()
            }
        }
    }

    private fun setupDebugButton() {
        val debugButton = ImageButton(this).apply {
            setBackgroundResource(android.R.drawable.ic_dialog_info)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                bottomMargin = 32
            }
            visibility = View.GONE // Make it invisible

        }
        binding.cameraContainer.addView(debugButton)
        debugButton.setOnClickListener {
            detector.announceAllObjects()
        }
    }

    private fun setupCameraButtons() {
//        findViewById<ImageButton>(R.id.openGalleryButton).setOnClickListener { openGallery() }
        findViewById<ImageButton>(R.id.cameraButton).setOnClickListener { takePhoto() }
        findViewById<ImageButton>(R.id.flipCameraButton).setOnClickListener { flipCamera() }
        findViewById<ToggleButton>(R.id.flashToggleButton).setOnClickListener { toggleFlash() }
    }

    private fun initializeCamera() {
        imageCapture = createImageCapture()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    // Method to open the gallery to select images
//    private fun openGallery() {
//        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
//    }

    // Method to create preview for camera
    private fun createPreview(): Preview {
        return Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()
    }

    // Method to create image analyzer
    private fun createImageAnalyzer(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
    }

    // Method to create image capture
    private fun createImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    // Method to set flash mode
    private fun setFlashMode() {
        // Enable/disable torch based on flash mode
        val cameraControl = camera?.cameraControl
        cameraControl?.enableTorch(isFlashOn)

        // Rebind camera use cases with new flash mode
        val cameraProvider = cameraProvider ?: return
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider.unbindAll()

            // Create instances of the necessary use cases
            preview = createPreview()
            imageAnalyzer = createImageAnalyzer()
            imageCapture = createImageCapture()

            // Build the camera with the new use cases
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture
            )

            // Set the flash mode based on the current state
            cameraControl?.enableTorch(isFlashOn)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    // Enum to represent flash mode
    enum class FlashMode {
        ON, OFF;

        // Method to toggle flash mode
        fun toggle(): FlashMode {
            return if (this == ON) OFF else ON
        }
    }

    // Method to check if flash is available
    private fun isFlashAvailable(): Boolean {
        val cameraProvider = cameraProvider ?: return false
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
            .build()

        return try {
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector
            )
            val flashInfo = camera.cameraInfo
            flashInfo.hasFlashUnit()
        } catch (exc: Exception) {
            false
        }
    }

    // Method to toggle flash mode
    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        cameraProvider?.unbindAll() // Unbind existing use cases
        setFlashMode()
        startCamera() // Rebind use cases with new flash mode
    }

    // Method to flip between front and back camera
    private fun flipCamera() {
        isFrontCamera = !isFrontCamera
        startCamera()
    }

    // Method to start the camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    // Method to bind camera use cases
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
        } else {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        }

        preview = createPreview()

        imageAnalyzer = createImageAnalyzer()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            if (!isDetectionPaused) {
                val bitmapBuffer =
                    Bitmap.createBitmap(
                        imageProxy.width,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )
                imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
                imageProxy.close()

                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                    if (isFrontCamera) {
                        postScale(
                            -1f,
                            1f,
                            imageProxy.width.toFloat(),
                            imageProxy.height.toFloat()
                        )
                    }
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                    matrix, true
                )

                detector.detect(rotatedBitmap)
            } else {
                // Just close the image when detection is paused
                imageProxy.close()
            }
        }
        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer,
                imageCapture // Include imageCapture here
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    // Method to capture photo
    private fun takePhoto() {
        // Use imageCapture instead of imageAnalyzer
        val imageCapture = imageCapture

        // Define image folder to save the captured image
        val imageFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Images"
        )
        if (!imageFolder.exists()) {
            imageFolder.mkdir()
        }

        // Generate file name for the captured image
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val fileName = "IMG_$timeStamp.jpg"
        val imageFile = File(imageFolder, fileName)

        // Define output options for image capture
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        // Capture image
        imageCapture?.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val message = "Photo Capture Succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
    }

    // Method to check if all required permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request permission launcher to request camera permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startCamera()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown: keyCode=$keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                handleButtonPress()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleButtonPress() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPressTime > MULTI_PRESS_TIMEOUT) {
            pressCount = 0
            handler.removeCallbacksAndMessages(null)
        }

        pressCount++
        lastPressTime = currentTime
        Log.d(TAG, "Button press count: $pressCount")

        // Remove any pending processing
        handler.removeCallbacksAndMessages(null)

        // Schedule processing after timeout
        handler.postDelayed({
            when (pressCount) {
                1 -> {
                    Log.d(TAG, "Single press - Tutorial")
                    if (requestAudioFocus()) {
                        // Vibrate once
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(100)
                        }

                        // Play tutorial in both languages
                        val tutorialMessage = """
                    Welcome to the object detection system.
                    Single press plays this tutorial.
                    Double press scans objects in front of you.
                    Triple press activates voice commands.
                    Four presses sends your location to guardians.
                    Five presses starts automatic scanning.
                    Six presses stops automatic scanning.
                    Say 'stop listening' to end voice recognition.
                    
//                    Maligayang pagdating sa sistema ng pagtukoy ng bagay.
//                    Isang pindot para sa gabay na ito.
//                    Dalawang pindot para ma-scan ang mga bagay sa harap mo.
//                    Tatlong pindot para sa boses na utos.
//                    Apat na pindot para ipadala ang iyong lokasyon sa mga guardian.
//                    Limang pindot para simulan ang awtomatikong pag-scan.
//                    Anim na pindot para ihinto ang awtomatikong pag-scan.
//                    Sabihin ang 'tumigil sa pakikinig' para matapos ang voice recognition.
                """.trimIndent()
                        detector.speak(tutorialMessage, true)
                    }
                }
                2 -> {
                    Log.d(TAG, "Double press - Manual scan")
                    if (requestAudioFocus()) {
                        // Stop automatic scan if it's running
                        stopAutomaticScan()

                        // Vibrate twice
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
                        }

                        // Announce scan in both languages
                        detector.speak("Scanning objects in front of you. Sina-scan ang mga bagay sa harap mo.", true)
                        detector.announceAllObjects(true)
                    }
                }
                3 -> {
                    Log.d(TAG, "Triple press - Voice recognition")
                    if (requestAudioFocus()) {
                        // Stop automatic scan if it's running
                        stopAutomaticScan()

                        // Vibrate three times
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100, 100, 100), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
                        }

                        // Activate voice recognition
                        detector.toggleVoiceRecognition()
                    }
                }
                4 -> {
                    Log.d(TAG, "Quadruple press - Emergency location sharing")
                    if (requestAudioFocus()) {
                        // Stop automatic scan if it's running
                        stopAutomaticScan()

                        // Vibrate four times with a distinct pattern for emergency
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(
                                longArrayOf(0, 100, 100, 100, 100, 100, 100, 200), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100, 100, 200), -1)
                        }

                        // Speak confirmation and send location
                        detector.speak("Sending emergency location to your guardians. Ipinapadala ang iyong lokasyon sa mga guardian.", true)

                        // Share location with guardians
                        if (locationTracker.isLocationAvailable()) {
                            if (locationTracker.hasGuardians()) {
                                // Emergency message with location
                                locationTracker.shareLocationWithGuardians(true) // Added parameter for emergency
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "No guardians found. Please add guardians first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                detector.speak("No guardians found. Please add guardians first.", true)
                                // Show guardian dialog automatically
                                locationTracker.showManageGuardiansDialog()
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Getting location... Please try again in a moment",
                                Toast.LENGTH_SHORT
                            ).show()
                            detector.speak("Getting location. Please try again in a moment.", true)
                        }
                    }
                }
                5 -> {
                    Log.d(TAG, "Five presses - Resume scanning")
                    if (requestAudioFocus()) {
                        // Vibrate pattern for resuming scan
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(
                                longArrayOf(0, 100, 50, 100, 50, 100, 50, 100, 50), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 100, 50, 100, 50), -1)
                        }

                        // Resume automatic scanning and detection
                        resumeObjectDetection()
                        detector.speak("Scanning resumed.", true)
                    }
                }
                6 -> {
                    Log.d(TAG, "Six presses - Stop all scanning")
                    if (requestAudioFocus()) {
                        // Vibrate pattern for completely stopping scan
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(
                                longArrayOf(0, 200, 100, 200, 100, 200), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
                        }

                        // Stop all scanning completely
                        pauseObjectDetection()
                        detector.speak("All scanning stopped. App is now silent. Tap 5 times to resume.", true)
                    }
                }
            }
            pressCount = 0
        }, MULTI_PRESS_TIMEOUT)
    }
    private var isDetectionPaused = false
    private fun pauseObjectDetection() {
        // Stop automatic scanning if active
        stopAutomaticScan()

        // Pause detector operations
        isDetectionPaused = true
        detector.pauseAllAnnouncements()

        // Visual feedback (optional)
        Toast.makeText(
            this@MainActivity,
            "All scanning stopped. App is now silent.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun resumeObjectDetection() {
        // Resume detector operations
        isDetectionPaused = false
        detector.resumeAllAnnouncements()

        // Visual feedback (optional)
        Toast.makeText(
            this@MainActivity,
            "Scanning resumed",
            Toast.LENGTH_SHORT
        ).show()
    }

    private var isAutomaticScanActive = false
    private val automaticScanHandler = Handler(Looper.getMainLooper())
    private val automaticScanRunnable = object : Runnable {
        override fun run() {
            if (isAutomaticScanActive) {
                detector.announceAllObjects(true)
                automaticScanHandler.postDelayed(this, 5000) // Scan every 5 seconds
            }
        }
    }

    private fun startAutomaticScan() {
        if (!isAutomaticScanActive) {
            isAutomaticScanActive = true

            // Provide initial scan immediately
            detector.announceAllObjects(true)

            // Then schedule recurring scans
            automaticScanHandler.postDelayed(automaticScanRunnable, 5000)

            // Visual feedback (optional)
            Toast.makeText(
                this@MainActivity,
                "Automatic scanning started",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopAutomaticScan() {
        if (isAutomaticScanActive) {
            isAutomaticScanActive = false
            automaticScanHandler.removeCallbacks(automaticScanRunnable)

            // Visual feedback (optional)
            Toast.makeText(
                this@MainActivity,
                "Automatic scanning stopped",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    // Override onKeyUp to handle button release
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyUp: keyCode=$keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    // onDestroy method called when the activity is destroyed
    override fun onDestroy() {
        // Stop location tracking
        stopAutomaticScan()
        handler.removeCallbacksAndMessages(null)
        if (::locationTracker.isInitialized) {
            locationTracker.stopTracking()
        }

        // Your existing cleanup code
        super.onDestroy()
//        stopAutomaticScan()
//        handler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
        detector.clear()
        cameraExecutor.shutdown()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: ${intent?.action}")

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            Log.d(TAG, "Media button event: ${event?.keyCode}")

            if (event?.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (requestAudioFocus()) {
                            // Provide haptic feedback
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(100)
                            }

                            // Start detection
                            detector.speak("Scanning objects in front of you", true)
                            detector.announceAllObjects(true)
                        }
                    }
                }
            }
        }
    }
    // onResume method called when the activity is resumed
    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun checkHeadsetStatus() {
        val isHeadsetConnected = audioManager.isWiredHeadsetOn
        Log.d(TAG, "Headset connected: $isHeadsetConnected")
    }
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val SMS_PERMISSION_REQUEST_CODE = 1002

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,  // Added SMS permission
            Manifest.permission.INTERNET
        )
    }
    // Method of DetectorListener interface to handle empty detection
    override fun onEmptyDetect() {

        binding.overlay.invalidate()
    }
    // Add this method to set up the share location button
//    private fun setupLocationButton() {
//        // Create a share location button
//        val shareLocationButton = Button(this).apply {
//            text = "Share Location"
//            setBackgroundResource(android.R.drawable.btn_default)
//            alpha = 0.8f
//
//            // Position at bottom right corner
//            val params = ConstraintLayout.LayoutParams(
//                ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                ConstraintLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
//                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//                bottomMargin = 32
//                rightMargin = 32
//            }
//            layoutParams = params
//
//            // Set up click listener
//            setOnClickListener {
//                if (locationTracker.isLocationAvailable()) {
//                    // Share the location
//                    locationTracker.shareLocation()
//                } else {
//                    Toast.makeText(
//                        this@MainActivity,
//                        "Getting location... Please try again in a moment",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//
//        // Add the button to the camera container
//        binding.cameraContainer.addView(shareLocationButton)
//    }

    private fun addLocationSharingButton() {
        // Create CardView container
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius) // You may need to define this dimension
            cardElevation = resources.getDimension(R.dimen.card_elevation) // You may need to define this dimension
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary)) // Use your app's primary color

            // Position the CardView
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomMargin = 50
                marginEnd = 50
            }
            layoutParams = params
        }

        // Create the button inside the CardView
        val shareLocationButton = Button(this).apply {
            text = "Share Location"
            background = ContextCompat.getDrawable(context, R.drawable.rounded_button_bg) // Create this drawable
            setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
            setPadding(24, 12, 24, 12)

            // No need for alpha since we're using a CardView

            // Set click listener
            setOnClickListener {
                if (locationTracker.isLocationAvailable()) {
                    if (locationTracker.hasGuardians()) {
                        // Share with guardians
                        locationTracker.shareLocationWithGuardians()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No guardians found. Please add guardians first.",
                            Toast.LENGTH_SHORT
                        ).show()
                        locationTracker.showManageGuardiansDialog()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Getting location... Please try again in a moment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Add the button to the CardView
        cardView.addView(shareLocationButton)

        // Add the CardView to your layout
        binding.cameraContainer.addView(cardView)
    }
    private fun addManageGuardiansButton() {
        // Create CardView container
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius) // You may need to define this dimension
            cardElevation = resources.getDimension(R.dimen.card_elevation) // You may need to define this dimension
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent)) // Use your app's accent color

            // Position the CardView
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                bottomMargin = 50
                marginStart = 50
            }
            layoutParams = params
        }

        // Create the button inside the CardView
        val manageGuardiansButton = Button(this).apply {
            text = "Manage Guardians"
            background = ContextCompat.getDrawable(context, R.drawable.rounded_button_bg) // Create this drawable
            setTextColor(ContextCompat.getColor(context, R.color.colorWhite))
            setPadding(24, 12, 24, 12)

            // Set click listener
            setOnClickListener {
                locationTracker.showManageGuardiansDialog()
            }
        }

        // Add the button to the CardView
        cardView.addView(manageGuardiansButton)

        // Add the CardView to your layout
        binding.cameraContainer.addView(cardView)
    }

    // Add this method to request location permissions
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, start tracking
            startLocationTracking()
        }
    }
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Start guardian setup process, then initialize the camera
     */
    private fun startGuardianSetup() {
        // Show guardian setup dialog if needed
        locationTracker.showInitialSetupIfNeeded {
            // Once guardian setup is complete, start location tracking and camera
            startLocationTracking()
            initializeCamera()
        }
    }

    // 7. Add method to start location tracking
    private fun startLocationTracking() {
        if (hasLocationPermission()) {
            locationTracker.startTracking()
        } else {
            requestLocationPermission()
        }
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All permissions granted, start guardian setup
                    startGuardianSetup()
                } else {
                    Toast.makeText(
                        this,
                        "Permissions required for app functionality",
                        Toast.LENGTH_LONG
                    ).show()
                    // Try to initialize camera anyway with limited functionality
                    initializeCamera()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking()
                }
            }
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // SMS permission granted, try sharing again
                    locationTracker.shareLocationWithGuardians()
                }
            }
        }
    }


    // Add this to your setupHeadsetReceiver() method
//
//    private val REQUIRED_PERMISSIONS = mutableListOf(
//        Manifest.permission.CAMERA,
//        Manifest.permission.RECORD_AUDIO  // Add permission for voice recognition
//    ).toTypedArray()

    // Method of DetectorListener interface to handle detection results

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }
}