package com.surendramaran.yolov5.feature.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple GPS Location Tracker with Guardian integration
 */
class LocationTracker(private val context: Context) {
    private val TAG = "LocationTracker"

    // Location variables
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var locationAvailable = false
    private var isTracking = false

    // GuardianManager for emergency contacts
    private val guardianManager = GuardianManager(context)

    // Location manager and listener
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // Location listener
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Store location
            currentLatitude = location.latitude
            currentLongitude = location.longitude
            locationAvailable = true

            // Save to preferences
            saveLocationToPreferences(location)

            Log.d(TAG, "Location updated: $currentLatitude, $currentLongitude")
        }

        // These methods are deprecated but still required for older Android versions
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            promptEnableLocation()
        }
    }

    /**
     * Start location tracking
     * @return True if tracking started successfully
     */
    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return false
        }

        // Check if GPS is enabled
        if (!isLocationEnabled()) {
            promptEnableLocation()
            return false
        }

        try {
            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener
            )

            // Also use network provider as fallback
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener
            )

            // Try to get last known location immediately
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastKnownLocation?.let {
                currentLatitude = it.latitude
                currentLongitude = it.longitude
                locationAvailable = true
                saveLocationToPreferences(it)
            }

            isTracking = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location tracking: ${e.message}")
            return false
        }
    }

    /**
     * Stop location tracking
     */
    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
            isTracking = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking: ${e.message}")
        }
    }

    /**
     * Check if location services are enabled
     */
    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Prompt user to enable location services
     */
    private fun promptEnableLocation() {
        Toast.makeText(
            context,
            "Please enable location services to use this feature",
            Toast.LENGTH_LONG
        ).show()

        // Open location settings
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open location settings: ${e.message}")
        }
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Save location to SharedPreferences
     */
    private fun saveLocationToPreferences(location: Location) {
        val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("latitude", location.latitude.toFloat())
            putFloat("longitude", location.longitude.toFloat())
            putLong("timestamp", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Get current location Google Maps URL
     * @return URL string or null if location not available
     */
    fun getLocationUrl(): String? {
        return if (locationAvailable) {
            "https://maps.google.com/?q=$currentLatitude,$currentLongitude"
        } else {
            // Try to retrieve from preferences
            val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("latitude", 0.0f)
            val lng = prefs.getFloat("longitude", 0.0f)

            if (lat != 0.0f && lng != 0.0f) {
                "https://maps.google.com/?q=$lat,$lng"
            } else {
                null
            }
        }
    }
    fun getLocationText(): String? {
        return if (locationAvailable) {
            "$currentLatitude,$currentLongitude"
        } else {
            // Try to retrieve from preferences
            val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("latitude", 0.0f)
            val lng = prefs.getFloat("longitude", 0.0f)


            if (lat != 0.0f && lng != 0.0f) {
                "$lat,$lng"
            } else {
                null
            }
        }
    }
    fun shareLocationWithGuardiansPlainText(isEmergency: Boolean = false): Int {
        if (!locationAvailable) {
            Toast.makeText(
                context,
                "Location not available yet. Please try again in a moment.",
                Toast.LENGTH_SHORT
            ).show()
            return 0
        }

        // Debug: Print current guardians
        val guardians = guardianManager.getGuardians()
        Log.d(TAG, "Number of guardians found: ${guardians.size}")

        // Create SMS helper
        val smsHelper = LocationSmsHelper(context)

        // Format message for better SMS delivery
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val prefix = if (isEmergency) "EMERGENCY ALERT! I need help!" else "My current location"
        val message = "$prefix at $currentTime. Coordinates: $currentLatitude, $currentLongitude. View on Google Maps: maps.google.com/?q=$currentLatitude,$currentLongitude"

        try {
            val sentCount = guardianManager.sendSmsToAllGuardians(message, isEmergency)

            if (sentCount > 0) {
                val toastMessage = if (isEmergency) {
                    "Emergency alert sent to $sentCount guardians"
                } else {
                    "Location shared with $sentCount guardians"
                }

                Toast.makeText(
                    context,
                    toastMessage,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "No guardians found. Please add guardians in settings.",
                    Toast.LENGTH_SHORT
                ).show()

                // Show guardian management dialog if no guardians found
                guardianManager.showManageGuardiansDialog()
            }

            return sentCount
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing location with guardians: ${e.message}")
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error sharing location: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            return 0
        }
    }
    /**
     * Share location via Android share intent
     */
    fun shareLocation() {
        val locationUrl = getLocationUrl()

        if (locationUrl != null) {
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "My Current Location")
                    putExtra(Intent.EXTRA_TEXT, "Here's my current location: $locationUrl")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Create the chooser and start activity
                val chooserIntent = Intent.createChooser(shareIntent, "Share Location via")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing location: ${e.message}")
                Toast.makeText(
                    context,
                    "Could not share location. Try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Location not available yet. Please wait a moment.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Open location directly in Google Maps
     */
    fun openLocationInGoogleMaps() {
        if (locationAvailable) {
            try {
                val uri =
                    Uri.parse("geo:$currentLatitude,$currentLongitude?q=$currentLatitude,$currentLongitude")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                mapIntent.setPackage("com.google.android.apps.maps")
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    // If Google Maps app is not installed, open in browser
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=$currentLatitude,$currentLongitude")
                    )
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(browserIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening maps: ${e.message}")
                Toast.makeText(
                    context,
                    "Could not open maps. Try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Location not available yet. Please wait a moment.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Send location to all guardians
     * @param isEmergency Whether this is an emergency location share
     * @return Number of guardians that were sent the message
     */
    fun shareLocationWithGuardians(isEmergency: Boolean = false): Int {
        val locationUrl = getLocationUrl()
        if (locationUrl == null) {
            Toast.makeText(
                context,
                "Location not available yet. Please try again in a moment.",
                Toast.LENGTH_SHORT
            ).show()
            return 0
        }

        // Debug: Print current guardians
        val guardians = guardianManager.getGuardians()
        Log.d(TAG, "Number of guardians found: ${guardians.size}")
        for (guardian in guardians) {
            Log.d(TAG, "Guardian: ${guardian.name}, Phone: ${guardian.phoneNumber}")
        }

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Create message based on whether this is an emergency
        val locationText = getLocationText()
        val message = if (isEmergency) {
            "EMERGENCY ALERT!  $currentTime: My current location at at google maps just copy and paste $locationText"
        } else {
            "EMERGENCY ALERT!  $currentTime: My current location at at google maps just copy and paste $locationText"
        }

        try {
            val sentCount = guardianManager.sendSmsToAllGuardians(message, isEmergency)

            if (sentCount > 0) {
                val toastMessage = if (isEmergency) {
                    "Emergency alert sent to $sentCount guardians"
                } else {
                    "Location shared with $sentCount guardians"
                }

                Toast.makeText(
                    context,
                    toastMessage,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "No guardians found. Please add guardians in settings.",
                    Toast.LENGTH_SHORT
                ).show()

                // Show guardian management dialog if no guardians found
                guardianManager.showManageGuardiansDialog()
            }

            return sentCount
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing location with guardians: ${e.message}")
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error sharing location: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            return 0
        }
    }

    /**
     * Show the guardian management dialog
     */
    fun showManageGuardiansDialog(callback: (() -> Unit)? = null) {
        guardianManager.showManageGuardiansDialog(callback)
    }

    /**
     * Show initial setup dialog for first app launch if no guardians exist
     * @param callback Function to call when setup is complete
     */
    fun showInitialSetupIfNeeded(callback: () -> Unit) {
        // Check if setup has been completed
        if (!GuardianManager.isSetupCompleted(context)) {
            guardianManager.showInitialSetupDialog {
                GuardianManager.markSetupCompleted(context)
                callback()
            }
        } else {
            // Check if we have any guardians, if not show the dialog
            if (!guardianManager.hasGuardians()) {
                guardianManager.showInitialSetupDialog {
                    callback()
                }
            } else {
                // Setup already done and we have guardians
                callback()
            }
        }
    }

    /**
     * Get the GuardianManager instance
     */
    fun getGuardianManager(): GuardianManager {
        return guardianManager
    }

    /**
     * Check if there are any guardians
     */
    fun hasGuardians(): Boolean {
        return guardianManager.hasGuardians()
    }

    /**
     * Check if location is available
     */
    fun isLocationAvailable(): Boolean = locationAvailable

    /**
     * Check if tracking is active
     */
    fun isTrackingActive(): Boolean = isTracking

    companion object {
        // Location update parameters
        private const val MIN_TIME_BETWEEN_UPDATES = 30000L // 30 seconds
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 meters
    }
}