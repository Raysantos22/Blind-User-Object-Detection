package com.BlindDetection.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

class HeadsetButtonController : BroadcastReceiver() {
    companion object {
        private const val TAG = "HeadsetBtnCtrl"  // Shorter tag for better log readability
        const val ACTION_SINGLE_PRESS = "com.BlindDetection.SINGLE_PRESS"
        const val ACTION_DOUBLE_PRESS = "com.BlindDetection.DOUBLE_PRESS"
        const val ACTION_TRIPLE_PRESS = "com.BlindDetection.TRIPLE_PRESS"

        private const val CLICK_TIMEOUT = 400L
    }

    private var clickCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var lastClickTime = 0L
    private var applicationContext: Context? = null
    private var lastKeyCode: Int = 0
    private var isProcessingClicks = false

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive called with action: ${intent?.action}")
        this.applicationContext = context?.applicationContext

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            Log.d(TAG, "Received KeyEvent - keyCode: ${keyEvent?.keyCode}, action: ${keyEvent?.action}, eventTime: ${keyEvent?.eventTime}")

            keyEvent?.let { event ->
                when (event.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        Log.d(TAG, "KEYCODE_HEADSETHOOK detected")
                        handleKeyEvent(event)
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        Log.d(TAG, "KEYCODE_MEDIA_PLAY_PAUSE detected")
                        handleKeyEvent(event)
                    }
                    else -> {
                        Log.d(TAG, "Unhandled key code: ${event.keyCode}")
                    }
                }
            }
            // Show a toast for debugging
            context?.let {
                handler.post {
                    Toast.makeText(it, "Headset button press detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleKeyEvent(event: KeyEvent) {
        Log.d(TAG, "Handling key event: ${event.keyCode}, action: ${event.action}")

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                Log.d(TAG, "Button DOWN at ${event.eventTime}")
                lastKeyCode = event.keyCode
            }
            KeyEvent.ACTION_UP -> {
                if (event.keyCode == lastKeyCode) {
                    Log.d(TAG, "Button UP at ${event.eventTime}")
                    processClick(event.eventTime)
                }
            }
        }

        // Always abort broadcast to prevent other apps from processing
        abortBroadcast()
    }

    private fun processClick(eventTime: Long) {
        Log.d(TAG, "Processing click at time: $eventTime")

        if (isProcessingClicks) {
            Log.d(TAG, "Already processing clicks, adding to count")
            clickCount++
            return
        }

        val timeSinceLastClick = eventTime - lastClickTime
        Log.d(TAG, "Time since last click: $timeSinceLastClick ms")

        if (timeSinceLastClick > CLICK_TIMEOUT) {
            Log.d(TAG, "Starting new click sequence")
            clickCount = 1
        } else {
            clickCount++
        }

        lastClickTime = eventTime
        isProcessingClicks = true

        Log.d(TAG, "Current click count: $clickCount")

        // Schedule processing of clicks
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            processClickSequence()
            isProcessingClicks = false
        }, CLICK_TIMEOUT)
    }

    private fun processClickSequence() {
        Log.d(TAG, "Processing click sequence. Final count: $clickCount")

        val intent = when (clickCount) {
            1 -> {
                Log.d(TAG, "Broadcasting single press")
                Intent(ACTION_SINGLE_PRESS)
            }
            2 -> {
                Log.d(TAG, "Broadcasting double press")
                Intent(ACTION_DOUBLE_PRESS)
            }
            3 -> {
                Log.d(TAG, "Broadcasting triple press")
                Intent(ACTION_TRIPLE_PRESS)
            }
            else -> {
                Log.d(TAG, "Invalid click count: $clickCount")
                null
            }
        }

        intent?.let {
            Log.d(TAG, "Sending broadcast for action: ${it.action}")
            applicationContext?.let { context ->
                context.sendBroadcast(it)
                // Show toast for debugging
                handler.post {
                    Toast.makeText(context,
                        when (clickCount) {
                            1 -> "Single press detected"
                            2 -> "Double press detected"
                            3 -> "Triple press detected"
                            else -> "Multiple presses detected"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        clickCount = 0
    }
}