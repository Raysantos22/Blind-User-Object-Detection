package com.BlindDetection.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

// 1. First, update MicrophoneButtonReceiver to include quadruple press detection


class MicrophoneButtonReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MicrophoneButtonReceiver"
        const val ACTION_SINGLE_PRESS = "com.BlindDetection.SINGLE_PRESS"
        const val ACTION_DOUBLE_PRESS = "com.BlindDetection.DOUBLE_PRESS"
        const val ACTION_TRIPLE_PRESS = "com.BlindDetection.TRIPLE_PRESS"
        const val ACTION_QUADRUPLE_PRESS = "com.BlindDetection.QUADRUPLE_PRESS"
        const val ACTION_FIVE_PRESS = "com.BlindDetection.FIVE_PRESS" // Add this
        const val ACTION_SIX_PRESS = "com.BlindDetection.SIX_PRESS" // Add this

        private const val MULTI_PRESS_DELAY = 500L // Time window for multi-press detection
    }

    private var pressCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var lastPressTime = 0L

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

            if (event?.action == KeyEvent.ACTION_UP &&
                (event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                        event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {

                handleButtonPress(context)
                abortBroadcast() // Prevent other apps from receiving this
            }
        }
    }

    private fun handleButtonPress(context: Context?) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPressTime > MULTI_PRESS_DELAY) {
            // Reset counter if too much time has passed
            pressCount = 0
        }

        pressCount++
        lastPressTime = currentTime

        // Clear any pending handlers to avoid inconsistent states
        handler.removeCallbacksAndMessages(null)

        // Set a timer to process after delay
        handler.postDelayed({
            when (pressCount) {
                1 -> context?.sendBroadcast(Intent(ACTION_SINGLE_PRESS))
                2 -> context?.sendBroadcast(Intent(ACTION_DOUBLE_PRESS))
                3 -> context?.sendBroadcast(Intent(ACTION_TRIPLE_PRESS))
                4 -> context?.sendBroadcast(Intent(ACTION_QUADRUPLE_PRESS))
                5 -> context?.sendBroadcast(Intent(ACTION_FIVE_PRESS))
                6 -> context?.sendBroadcast(Intent(ACTION_SIX_PRESS))
            }
            pressCount = 0
        }, MULTI_PRESS_DELAY)
    }
}