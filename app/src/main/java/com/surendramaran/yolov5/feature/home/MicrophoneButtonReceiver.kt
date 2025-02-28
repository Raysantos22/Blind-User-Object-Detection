package com.surendramaran.yolov5.feature.home

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
        const val ACTION_SINGLE_PRESS = "com.surendramaran.yolov5.SINGLE_PRESS"
        const val ACTION_DOUBLE_PRESS = "com.surendramaran.yolov5.DOUBLE_PRESS"
        const val ACTION_TRIPLE_PRESS = "com.surendramaran.yolov5.TRIPLE_PRESS"
        const val ACTION_QUADRUPLE_PRESS = "com.surendramaran.yolov5.QUADRUPLE_PRESS" // Add this line

        private const val DOUBLE_PRESS_DELAY = 300L // Time window for double press
        private const val TRIPLE_PRESS_DELAY = 500L // Time window for triple press
        private const val QUADRUPLE_PRESS_DELAY = 650L // Time window for quadruple press
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

        if (currentTime - lastPressTime > QUADRUPLE_PRESS_DELAY) {
            // Reset counter if too much time has passed
            pressCount = 0
        }

        pressCount++
        lastPressTime = currentTime

        when (pressCount) {
            1 -> {
                // Wait to see if there's a second press
                handler.postDelayed({
                    if (pressCount == 1) {
                        // Single press confirmed
                        context?.sendBroadcast(Intent(ACTION_SINGLE_PRESS))
                        pressCount = 0
                    }
                }, DOUBLE_PRESS_DELAY)
            }
            2 -> {
                // Wait to see if there's a third press
                handler.postDelayed({
                    if (pressCount == 2) {
                        // Double press confirmed
                        context?.sendBroadcast(Intent(ACTION_DOUBLE_PRESS))
                        pressCount = 0
                    }
                }, DOUBLE_PRESS_DELAY)
            }
            3 -> {
                // Wait to see if there's a fourth press
                handler.postDelayed({
                    if (pressCount == 3) {
                        // Triple press confirmed
                        context?.sendBroadcast(Intent(ACTION_TRIPLE_PRESS))
                        pressCount = 0
                    }
                }, DOUBLE_PRESS_DELAY)
            }
            4 -> {
                // Quadruple press detected
                context?.sendBroadcast(Intent(ACTION_QUADRUPLE_PRESS))
                pressCount = 0
            }
        }
    }
}