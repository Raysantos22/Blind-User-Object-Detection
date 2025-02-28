package com.surendramaran.yolov5.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MediaButtonReceiver"
        const val OUR_MEDIA_BUTTON_EVENT = "com.surendramaran.yolov5.MEDIA_BUTTON_EVENT"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "MediaButtonReceiver onReceive: ${intent?.action}")

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            Log.d(TAG, "Received media button event: keyCode=${event?.keyCode}, action=${event?.action}")

            if (event?.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        Log.d(TAG, "Intercepting media button press")
                        // Send our custom broadcast
                        context?.sendBroadcast(Intent(OUR_MEDIA_BUTTON_EVENT))
                        // Prevent other apps from receiving this
                        abortBroadcast()
                    }
                }
            }
        }
    }
}