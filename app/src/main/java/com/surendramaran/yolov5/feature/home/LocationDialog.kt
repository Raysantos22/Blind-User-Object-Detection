package com.surendramaran.yolov5.feature.home

import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.appcompat.app.AlertDialog
import com.surendramaran.yolov5.R

class LocationDialog(private val context: Context, private val locationTracker: LocationTracker) {

    /**
     * Show the location sharing dialog
     */
    fun showLocationDialog() {
        // Use a custom layout for the dialog
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_location_sharing, null, false
        )

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Share Location")
            .create()

        // Set up button click listeners
        dialogView.findViewById<Button>(R.id.btnShareLocation).setOnClickListener {
            locationTracker.shareLocation()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnOpenMaps).setOnClickListener {
            locationTracker.openLocationInGoogleMaps()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSendSms).setOnClickListener {
            sendLocationViaSms()
            dialog.dismiss()
        }

        // Update location URL text
        val locationUrlText = dialogView.findViewById<TextView>(R.id.tvLocationUrl)
        locationTracker.getLocationUrl()?.let { url ->
            locationUrlText.text = url
        } ?: run {
            locationUrlText.text = "Location not available yet"
        }

        // Show the dialog
        dialog.show()
    }

    /**
     * Send location via SMS
     */
    private fun sendLocationViaSms() {
        val locationUrl = locationTracker.getLocationUrl() ?: return

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", "Here is my current location: $locationUrl")
        }

        if (smsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(smsIntent)
        }
    }

    /**
     * Create the custom dialog layout if it doesn't exist
     */
    companion object {
        fun createLayoutIfNeeded(context: Context) {
            // This would typically be in an XML file, but we can create it programmatically
            // if it doesn't exist in your project already

            // Check if the layout resource exists
            try {
                context.resources.getIdentifier(
                    "dialog_location_sharing", "layout", context.packageName
                )
            } catch (e: Exception) {
                // Create the layout programmatically
                var layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // Root layout
                val dialogLayout = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                    layoutParams = layoutParams
                }

                // Location URL text
                val locationUrlText = TextView(context).apply {
                    id = R.id.tvLocationUrl
                    layoutParams = layoutParams
                    setText("Location URL will appear here")
                    setPadding(0, 0, 0, 32)
                }
                dialogLayout.addView(locationUrlText)

                // Share location button
                val shareLocationButton = Button(context).apply {
                    id = R.id.btnShareLocation
                    layoutParams = layoutParams
                    text = "Share via Apps"
                }
                dialogLayout.addView(shareLocationButton)

                // Open in Maps button
                val openMapsButton = Button(context).apply {
                    id = R.id.btnOpenMaps
                    layoutParams = layoutParams
                    text = "Open in Google Maps"
                }
                dialogLayout.addView(openMapsButton)

                // Send SMS button
                val sendSmsButton = Button(context).apply {
                    id = R.id.btnSendSms
                    layoutParams = layoutParams
                    text = "Send via SMS"
                }
                dialogLayout.addView(sendSmsButton)

                // In a real implementation, you would save this as an XML layout file
                // But for this example, we're just demonstrating the structure
            }
        }
    }
}