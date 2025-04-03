package com.BlindDetection.feature.home

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.VideoProfile.isPaused
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

//class Detector(
//    private val context: Context,
//    private val modelPath: String,
//    private val labelPath: String,
//    private val detectorListener: DetectorListener
//) {
//
//    private var interpreter: Interpreter? = null
//    private var labels = mutableListOf<String>()
//    private lateinit var textToSpeech: TextToSpeech
//    private var lastSpokenTime = 0L
//    private val SPEAK_DELAY = 5000L // Reduced delay for more responsive feedback
//    private var lastDistances = mutableMapOf<String, Float>()
//
//    private var tensorWidth = 0
//    private var tensorHeight = 0
//    private var numChannel = 0
//    private var numElements = 0
//
//    init {
//        initTextToSpeech()
//    }
//
//    private fun initTextToSpeech() {
//        textToSpeech = TextToSpeech(context) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                textToSpeech.language = Locale.US
//                textToSpeech.setSpeechRate(1.0f)
//            }
//        }
//    }
//
//    private fun calculateDistance(box: BoundingBox): Float {
//        val REFERENCE_HEIGHT = 1.7f // Average human height in meters
//        val boxHeight = box.h
//
//        // Calculate distance using the principle of similar triangles
//        // This formula assumes the camera's field of view and sensor size are calibrated
//        return (REFERENCE_HEIGHT / boxHeight) * 0.8f // Adjustment factor for better accuracy
//    }
//
//    private fun checkDistanceChange(objectName: String, currentDistance: Float): String? {
//        val previousDistance = lastDistances[objectName]
//        if (previousDistance != null) {
//            val change = currentDistance - previousDistance
//            return when {
//                abs(change) < 0.5f -> null // Ignore small changes
//                change > 0 -> "moving away"
//                else -> "getting closer"
//            }
//        }
//        return null
//    }
//
//    private fun speakDetection(boxes: List<BoundingBox>) {
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastSpokenTime < SPEAK_DELAY) return
//
//        boxes.forEach { box ->
//            val distance = box.calculateDistance()
//            val movement = checkDistanceChange(box.clsName, distance)
//
//            // Update stored distance
//            lastDistances[box.clsName] = distance
//
//            // Create spoken message
//            val message = buildString {
//                append("${box.clsName} at ${String.format("%.1f", distance)} meters")
//                if (movement != null) {
//                    append(", $movement")
//                }
//                when {
//                    distance <= 1.0f -> append(", Warning! Very close!")
//                    distance <= 2.0f -> append(", Approaching close range")
//                }
//            }
//
//            // Prioritize warnings for close objects
//            val queueMode = if (distance <= 2.0f) {
//                TextToSpeech.QUEUE_FLUSH
//            } else {
//                TextToSpeech.QUEUE_ADD
//            }
//
//            textToSpeech.speak(message, queueMode, null, null)
//        }
//        lastSpokenTime = currentTime
//    }
//
//    private val imageProcessor = ImageProcessor.Builder()
//        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
//        .add(CastOp(INPUT_IMAGE_TYPE))
//        .build()
//
//    fun setup() {
//        val model = FileUtil.loadMappedFile(context, modelPath)
//        val options = Interpreter.Options()
//        options.numThreads = 4
//        interpreter = Interpreter(model, options)
//
//        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
//        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return
//
//        tensorWidth = inputShape[1]
//        tensorHeight = inputShape[2]
//        numChannel = outputShape[1]
//        numElements = outputShape[2]
//
//        try {
//            val inputStream: InputStream = context.assets.open(labelPath)
//            val reader = BufferedReader(InputStreamReader(inputStream))
//
//            var line: String? = reader.readLine()
//            while (line != null && line != "") {
//                labels.add(line)
//                line = reader.readLine()
//            }
//
//            reader.close()
//            inputStream.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
//
//    fun detect(frame: Bitmap) {
//        interpreter ?: return
//        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return
//
//        var inferenceTime = SystemClock.uptimeMillis()
//
//        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)
//        val tensorImage = TensorImage(DataType.FLOAT32)
//        tensorImage.load(resizedBitmap)
//        val processedImage = imageProcessor.process(tensorImage)
//        val imageBuffer = processedImage.buffer
//
//        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
//        interpreter?.run(imageBuffer, output.buffer)
//
//        val bestBoxes = bestBox(output.floatArray)
//        if (bestBoxes == null) {
//            detectorListener.onEmptyDetect()
//            return
//        }
//
//        // Speak the detection results
//        speakDetection(bestBoxes)
//
//        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
//        detectorListener.onDetect(bestBoxes, inferenceTime)
//    }
//
//
//    fun clear() {
//        interpreter?.close()
//        interpreter = null
//        textToSpeech.shutdown()
//    }
//
//    private fun bestBox(array: FloatArray) : List<BoundingBox>? {
//
//        val boundingBoxes = mutableListOf<BoundingBox>()
//        for (c in 0 until numElements) {
//            val confidences = (4 until numChannel).map { array[c + numElements * it] }
//            val cnf = confidences.max()
//            if (cnf > CONFIDENCE_THRESHOLD) {
//                val cls = confidences.indexOf(cnf)
//                val clsName = labels[cls]
//                val cx = array[c] // 0
//                val cy = array[c + numElements] // 1
//                val w = array[c + numElements * 2]
//                val h = array[c + numElements * 3]
//                val x1 = cx - (w/2F)
//                val y1 = cy - (h/2F)
//                val x2 = cx + (w/2F)
//                val y2 = cy + (h/2F)
//                if (x1 < 0F || x1 > 1F) continue
//                if (y1 < 0F || y1 > 1F) continue
//                if (x2 < 0F || x2 > 1F) continue
//                if (y2 < 0F || y2 > 1F) continue
//
//                boundingBoxes.add(
//                    BoundingBox(
//                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
//                        cx = cx, cy = cy, w = w, h = h,
//                        cnf = cnf, cls = cls, clsName = clsName
//                    )
//                )
//            }
//        }
//
//        if (boundingBoxes.isEmpty()) return null
//
//        return applyNMS(boundingBoxes)
//    }
//
//    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
//        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
//        val selectedBoxes = mutableListOf<BoundingBox>()
//
//        while(sortedBoxes.isNotEmpty()) {
//            val first = sortedBoxes.first()
//            selectedBoxes.add(first)
//            sortedBoxes.remove(first)
//
//            val iterator = sortedBoxes.iterator()
//            while (iterator.hasNext()) {
//                val nextBox = iterator.next()
//                val iou = calculateIoU(first, nextBox)
//                if (iou >= IOU_THRESHOLD) {
//                    iterator.remove()
//                }
//            }
//        }
//
//        return selectedBoxes
//    }
//
//    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
//        val x1 = maxOf(box1.x1, box2.x1)
//        val y1 = maxOf(box1.y1, box2.y1)
//        val x2 = minOf(box1.x2, box2.x2)
//        val y2 = minOf(box1.y2, box2.y2)
//        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
//        val box1Area = box1.w * box1.h
//        val box2Area = box2.w * box2.h
//        return intersectionArea / (box1Area + box2Area - intersectionArea)
//    }
//
//    interface DetectorListener {
//        fun onEmptyDetect()
//        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
//    }
//
//    companion object {
//        private const val INPUT_MEAN = 0f
//        private const val INPUT_STANDARD_DEVIATION = 255f
//        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
//        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
//        private const val CONFIDENCE_THRESHOLD = 0.75F
//        private const val IOU_THRESHOLD = 0.5F
//        private const val VERY_CLOSE_THRESHOLD = 1.0f
//        private const val CLOSE_THRESHOLD = 2.0f
//        private const val MEDIUM_THRESHOLD = 3.0f
//        private const val DISTANCE_CHANGE_THRESHOLD = 0.5f
//    }
//}

interface DetectorListener {
    fun onEmptyDetect()
    fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
}

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener,
    private val vibrator: Vibrator? = null,
    private var isGpu: Boolean = true // Default to GPU enabled
) {
    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var lastSpokenTime = 0L
    private val SPEAK_DELAY = 2000L
    private var lastDistances = mutableMapOf<String, Float>()
    private var lastSpokenObjects = mutableMapOf<String, Long>()
    private var currentObjects = mutableListOf<BoundingBox>()
    private var isListening = false
    private var isSpeaking = false
    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0
    private val distanceHistory = mutableMapOf<String, ArrayDeque<Float>>()
    private val HISTORY_SIZE = 5 // Keep last 5 measurements for smoothing
    private var lastAnnouncedObjects = mutableMapOf<String, ObjectState>()
    private var isPaused = false
    private var gpuDelegate: GpuDelegate? = null

    private data class ObjectState(
        val distance: Float,
        val position: String,
        val timestamp: Long
    )

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    init {
        initInterpreter()
        if (vibrator != null) {
            initTextToSpeech()
            initSpeechRecognizer()
        }
    }
    fun setup() {
                val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        options.numThreads = 4
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initInterpreter() {
        val options = Interpreter.Options()

        if (isGpu) {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                gpuDelegate = GpuDelegate(delegateOptions)
                options.addDelegate(gpuDelegate)
            } else {
                // GPU not supported, fall back to CPU
                options.setNumThreads(4)
            }
        } else {
            // CPU mode
            options.setNumThreads(4)
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]

        // If input shape is in format of [1, 3, ..., ...]
        if (inputShape[1] == 3) {
            tensorWidth = inputShape[2]
            tensorHeight = inputShape[3]
        }

        numChannel = outputShape[1]
        numElements = outputShape[2]

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(1.0f)
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        // Vibrate to indicate start of speech
                        if (vibrator != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(50)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        // Vibrate to indicate end of speech
                        if (vibrator != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(100)
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        // Vibrate pattern to indicate error
                        if (vibrator != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
                            }
                        }
                    }
                })
            }
        }
    }

    // All your existing methods...

    private fun getSmoothedDistance(objectId: String, newDistance: Float): Float {
        val history = distanceHistory.getOrPut(objectId) { ArrayDeque(HISTORY_SIZE) }

        if (history.size >= HISTORY_SIZE) {
            history.removeFirst()
        }
        history.addLast(newDistance)

        // Return median of recent distances for more stability
        return history.sorted().let {
            if (it.size % 2 == 0) {
                (it[it.size/2] + it[it.size/2 - 1]) / 2
            } else {
                it[it.size/2]
            }
        }
    }

    private enum class VoiceAction {
        DESCRIBE, STOP, PAUSE, RESUME
    }

    private val voiceCommands = mapOf(
        "what's in front" to VoiceAction.DESCRIBE,
        "what is in front" to VoiceAction.DESCRIBE,
        "what do you see" to VoiceAction.DESCRIBE,
        "describe surroundings" to VoiceAction.DESCRIBE,
        "any objects" to VoiceAction.DESCRIBE,
        "what's around" to VoiceAction.DESCRIBE,
        "detect objects" to VoiceAction.DESCRIBE,
        "stop listening" to VoiceAction.STOP,
        "be quiet" to VoiceAction.STOP,
        "pause" to VoiceAction.PAUSE,
        "resume" to VoiceAction.RESUME,
        "ano ang nasa harap" to VoiceAction.DESCRIBE,
        "ano ang nakikita mo" to VoiceAction.DESCRIBE,
        "ano ang nasa paligid" to VoiceAction.DESCRIBE,
        "ilarawan ang paligid" to VoiceAction.DESCRIBE,
        "tumigil sa pakikinig" to VoiceAction.STOP
    )

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    vibratePattern(longArrayOf(0, 100, 50, 100)) // Double pulse for start
                    speak("Listening...", true)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { command ->
                        handleVoiceCommand(command.lowercase())
                    }
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that, please try again"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Please wait a moment and try again"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything, please try again"
                        else -> "Something went wrong, please try again"
                    }
                    vibratePattern(longArrayOf(0, 200, 100, 200, 100, 200)) // Error pattern
                    speak(errorMessage, true)
                }

                // Required overrides
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    vibratePattern(longArrayOf(0, 200)) // Single pulse for end
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleVoiceCommand(command: String) {
        val action = voiceCommands.entries.firstOrNull { (key, _) ->
            command.contains(key)
        }?.value

        when (action) {
            VoiceAction.DESCRIBE -> announceAllObjects(true)
            VoiceAction.STOP -> {
                isListening = false
                speechRecognizer.stopListening()
                speak("Voice recognition stopped", true)
            }
            VoiceAction.PAUSE -> {
                isPaused = true
                vibratePattern(longArrayOf(0, 100))
                speak("Pausing announcements", true)
            }
            VoiceAction.RESUME -> {
                isPaused = false
                vibratePattern(longArrayOf(0, 100, 50, 100))
                speak("Resuming announcements", true)
            }
            null -> speak("I didn't understand that command. Please try again.", true)
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }

    fun speak(text: String, force: Boolean = false) {
        if (this::textToSpeech.isInitialized && (!isSpeaking || force)) {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId")
            // Use higher priority for very close objects
            if (text.startsWith("Warning")) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageId")
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params, "messageId")
            }
        }
    }

    fun startListening() {
        if (!isSpeaking && this::speechRecognizer.isInitialized && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PH,fil-PH") // Support both English and Filipino
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to know? Ano ang gusto mong malaman?")
                }
                speechRecognizer.startListening(intent)
                vibratePattern(longArrayOf(0, 100, 50, 100))
                speak("Listening... Nakikinig...", true)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting voice recognition: ${e.message}")
                speak("Unable to start voice recognition. Hindi masimulan ang voice recognition.", true)
            }
        }
    }

    fun toggleVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
            speak("Voice recognition stopped", true)
            vibratePattern(longArrayOf(0, 200)) // Single pulse for stop
        } else {
            startListening()
        }
    }

    fun stopListening() {
        if (isListening && this::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            isListening = false
            speak("Voice recognition stopped", true)
        }
    }

    fun pauseListening() {
        isPaused = true
        vibratePattern(longArrayOf(0, 100))  // Short vibration to confirm pause
        speak("Pausing announcements", true)
    }

    fun resumeListening() {
        isPaused = false
        vibratePattern(longArrayOf(0, 100, 50, 100))  // Double vibration to confirm resume
        speak("Resuming announcements", true)
    }

    private fun getBilingualObjectDescription(box: BoundingBox, distance: Float): String {
        val position = when {
            box.cx < 0.4f -> "on your left (nasa kaliwa mo)"
            box.cx > 0.6f -> "on your right (nasa kanan mo)"
            else -> "in front of you (sa harap mo)"
        }

        val distanceDesc = when {
            distance <= 1.0f -> "very close (napakalapit)"
            distance <= 2.0f -> "nearby (malapit)"
            else -> "about ${String.format("%.1f", distance)} meters away (mga ${String.format("%.1f", distance)} metro ang layo)"
        }

        return "${box.clsName} $position, $distanceDesc"
    }

    fun announceAllObjects(immediate: Boolean = false) {
        if (currentObjects.isEmpty()) {
            announceNoObjectsDetected()
            return
        }

        val announcement = buildString {
            append("Detected objects (Mga nakitang bagay): ")
            currentObjects.sortedBy { it.calculateDistance() }.forEachIndexed { index, box ->
                if (index > 0) append(", ")
                append(getBilingualObjectDescription(box, box.calculateDistance()))
            }
        }

        speak(announcement, immediate)
    }

    private fun announceNoObjectsDetected() {
        speak("No objects detected. Path is clear. Walang nakitang bagay. Malinis ang daan.", true)
    }

    // Modified detect method to use GPU acceleration
    fun detect(frame: Bitmap) {
        interpreter ?: return
        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray)
        if (bestBoxes == null || bestBoxes.isEmpty()) {
            if (currentObjects.isNotEmpty()) {
                speak("", true)
                currentObjects.clear()
            }
            detectorListener.onEmptyDetect()
            return
        }

        // Only announce if objects have changed or significant distance changes
        val shouldAnnounce = shouldAnnounceChange(bestBoxes)

        currentObjects = bestBoxes.toMutableList()

        if (shouldAnnounce) {
            announceCurrentObjects()
        }

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        detectorListener.onDetect(bestBoxes, inferenceTime)
    }

    private fun announceCurrentObjects() {
        if (currentObjects.isEmpty()) {
            speak("Path is clear", true)
            return
        }

        // Group by type and annotate with smoothed distances
        val smoothedObjects = currentObjects.map { box ->
            val smoothedDistance = getSmoothedDistance(box.clsName, box.calculateDistance())
            box to smoothedDistance
        }.groupBy { it.first.clsName }

        val announcement = buildString {
            smoothedObjects.forEach { (type, objects) ->
                // Sort by smoothed distance
                val sortedObjects = objects.sortedBy { it.second }
                if (objects.size > 1) {
                    append("${objects.size} ${type}s, ")
                    val closestObject = sortedObjects.first()
                    append("closest one ${closestObject.first.getPositionDescription()} ")
                    append("at ${String.format("%.1f", closestObject.second)} meters")

                    if (closestObject.second <= VERY_CLOSE_THRESHOLD) {
                        append(" - very close!")
                    }
                } else {
                    val obj = sortedObjects.first()
                    append("$type ${obj.first.getPositionDescription()} ")
                    append("at ${String.format("%.1f", obj.second)} meters")

                    if (obj.second <= VERY_CLOSE_THRESHOLD) {
                        append(" - very close!")
                    }
                }
                append(". ")
            }
        }

        speak(announcement, true)
    }

    private fun shouldAnnounceChange(newBoxes: List<BoundingBox>): Boolean {
        val currentTime = System.currentTimeMillis()

        // Initialize map for objects that are truly new or significantly changed
        val changedObjects = mutableListOf<String>()

        for (box in newBoxes) {
            val objectId = box.clsName
            val smoothedDistance = getSmoothedDistance(objectId, box.calculateDistance())
            val currentPosition = box.getPositionDescription()

            val previousState = lastAnnouncedObjects[objectId]
            if (previousState == null) {
                // New object detected
                changedObjects.add(objectId)
                lastAnnouncedObjects[objectId] = ObjectState(smoothedDistance, currentPosition, currentTime)
                continue
            }

            // Calculate time since last announcement for this object
            val timeSinceLastAnnouncement = currentTime - previousState.timestamp

            // Check for significant changes
            val distanceChange = abs(smoothedDistance - previousState.distance)
            val positionChanged = currentPosition != previousState.position
            val isVeryClose = smoothedDistance <= VERY_CLOSE_THRESHOLD
            val wasVeryClose = previousState.distance <= VERY_CLOSE_THRESHOLD

            when {
                // Always announce new very close objects
                isVeryClose && !wasVeryClose -> {
                    changedObjects.add(objectId)
                }
                // Announce significant distance changes (> 1 meter)
                distanceChange >= SIGNIFICANT_DISTANCE_CHANGE -> {
                    changedObjects.add(objectId)
                }
                // Announce position changes after a delay
                positionChanged && timeSinceLastAnnouncement >= POSITION_CHANGE_DELAY -> {
                    changedObjects.add(objectId)
                }
                // Periodic updates for static objects
                timeSinceLastAnnouncement >= PERIODIC_ANNOUNCE_DELAY -> {
                    changedObjects.add(objectId)
                }
            }

            if (changedObjects.contains(objectId)) {
                lastAnnouncedObjects[objectId] = ObjectState(smoothedDistance, currentPosition, currentTime)
            }
        }

        // Clean up history for objects no longer detected
        val currentObjectIds = newBoxes.map { it.clsName }.toSet()
        distanceHistory.keys.toList().forEach { id ->
            if (id !in currentObjectIds) {
                distanceHistory.remove(id)
                lastAnnouncedObjects.remove(id)
            }
        }

        return changedObjects.isNotEmpty()
    }

    // Method to restart the interpreter with a specified GPU mode
    fun restart(isGpu: Boolean = true) {
        // Clean up existing interpreter and delegate
        interpreter?.close()
        gpuDelegate?.close()

        // Save the GPU mode preference
        this.isGpu = isGpu

        // Reinitialize interpreter with new settings
        initInterpreter()
    }

    // Additional helper methods
    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()
        for (c in 0 until numElements) {
            val confidences = (4 until numChannel).map { array[c + numElements * it] }
            val cnf = confidences.max()
            if (cnf > CONFIDENCE_THRESHOLD) {
                val cls = confidences.indexOf(cnf)
                val clsName = labels[cls]
                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = cnf, cls = cls, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null
        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    fun clear() {
        interpreter?.close()
        gpuDelegate?.close()  // Add this line
        interpreter = null
        gpuDelegate = null
        if (this::textToSpeech.isInitialized) {
            textToSpeech.shutdown()
        }
        if (this::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        lastDistances.clear()
        currentObjects.clear()
        distanceHistory.clear()
        lastAnnouncedObjects.clear()
    }

    companion object {
        private const val TAG = "Detector"
        private const val PERIODIC_ANNOUNCE_DELAY = 10000L
        private const val POSITION_CHANGE_DELAY = 2000L // 2 seconds minimum between position updates
        private const val SIGNIFICANT_DISTANCE_CHANGE = 1.0f // Announce if distance changes by 1 meter
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.75F
        private const val IOU_THRESHOLD = 0.5F
        private const val VERY_CLOSE_THRESHOLD = 1.0f
        private const val CLOSE_THRESHOLD = 2.0f
        private const val WARNING_DELAY = 3000L // Delay between warning announcements
    }
}