package com.BlindDetection.feature.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.BlindDetection.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for direct SMS location sharing
 */
class LocationSmsHelper(private val context: Context) {
    private val TAG = "LocationSmsHelper"

    // Check SMS permission
    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request SMS permission if within an Activity
    private fun requestSmsPermission(activity: Activity?) {
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Format location URL as plain text to avoid SMS delivery issues
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return Formatted location text for SMS
     */
    fun formatLocationForSms(latitude: Double, longitude: Double): String {
        val mapLink = "maps.google.com/?q=$latitude,$longitude"
        return "EMERGENCY: My current location coordinates: $latitude, $longitude. View on Google Maps: $mapLink"
    }

    /**
     * Send location SMS to a specific phone number
     * @param phoneNumber Recipient's phone number
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param isEmergency Whether this is an emergency message
     * @return True if SMS was sent successfully
     */
    fun sendLocationSms(
        phoneNumber: String,
        latitude: Double,
        longitude: Double,
        isEmergency: Boolean = false
    ): Boolean {
        if (!hasSmsPermission()) {
            Log.e(TAG, "SMS permission not granted")

            // Try to request permission if context is an activity
            if (context is Activity) {
                requestSmsPermission(context)
            }

            return false
        }

        // Format message in a way that avoids SMS delivery issues
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val prefix = if (isEmergency) "EMERGENCY ALERT!" else "Location update"
        val message =
            "$prefix at $timestamp. Coordinates: $latitude, $longitude. View on Google Maps: maps.google.com/?q=$latitude,$longitude"

        try {
            // Get SmsManager instance based on Android version
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split message if needed (to handle potential length issues)
            val messageParts = smsManager.divideMessage(message)

            if (messageParts.size > 1) {
                // Send as multipart if message is long
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    null,
                    null
                )
            } else {
                // Send as single message
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

            Log.d(TAG, "SMS sent successfully to $phoneNumber")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            return false
        }
    }

    /**
     * Send location to all guardians using the GuardianManager
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param isEmergency Whether this is an emergency message
     * @return Number of guardians that were messaged
     */
    fun sendLocationToAllGuardians(
        latitude: Double,
        longitude: Double,
        isEmergency: Boolean = false
    ): Int {
        if (!hasSmsPermission()) {
            Log.e(TAG, "SMS permission not granted")

            // Try to request permission if context is an activity
            if (context is Activity) {
                requestSmsPermission(context)
            }

            return 0
        }

        val guardianManager = GuardianManager(context)
        val guardians = guardianManager.getGuardians()

        if (guardians.isEmpty()) {
            Log.d(TAG, "No guardians found")
            return 0
        }

        // Format message in a way that avoids SMS delivery issues
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val prefix = if (isEmergency) "EMERGENCY ALERT!" else "Location update"
        val message =
            "$prefix at $timestamp. Coordinates: $latitude, $longitude. View on Google Maps: maps.google.com/?q=$latitude,$longitude"

        return guardianManager.sendSmsToAllGuardians(message, isEmergency)
    }

    /**
     * Show dialog to send location SMS to a specific number
     * @param latitude Location latitude
     * @param longitude Location longitude
     */
    fun showSendSmsDialog(latitude: Double, longitude: Double) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_send_sms, null)
        val phoneNumberInput = dialogView.findViewById<EditText>(R.id.phoneNumberInput)

        AlertDialog.Builder(context)
            .setTitle("Send Location via SMS")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val phoneNumber = phoneNumberInput.text.toString().trim()
                if (phoneNumber.isNotEmpty()) {
                    val success = sendLocationSms(phoneNumber, latitude, longitude)
                    if (success) {
                        Toast.makeText(
                            context,
                            "Location SMS sent successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to send SMS. Please check permissions.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please enter a valid phone number",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 1002
    }
}