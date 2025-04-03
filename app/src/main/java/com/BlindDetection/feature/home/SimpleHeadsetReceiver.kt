package com.BlindDetection.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.util.Log

class SimpleHeadsetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SimpleHeadsetReceiver"
    }

    private var buttonCallback: (() -> Unit)? = null

    fun setButtonCallback(callback: () -> Unit) {
        buttonCallback = callback
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received intent: ${intent?.action}")

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            Log.d(TAG, "KeyEvent received: keyCode=${event?.keyCode}, action=${event?.action}")

            if (event?.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        Log.d(TAG, "Button pressed - executing callback")
                        buttonCallback?.invoke()
                        abortBroadcast()
                    }
                }
            }
        }
    }
}