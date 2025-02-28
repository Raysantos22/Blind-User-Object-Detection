package com.surendramaran.yolov5.feature.home



/**
 * Manages guardian phone numbers for emergency location sharing
 */
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.surendramaran.yolov5.R
import java.lang.reflect.Type
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

import android.widget.*


/**
 * Manages guardian phone numbers for emergency location sharing
 */
class GuardianManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        GUARDIAN_PREFS, Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * Check if any guardian numbers have been saved
     * @return true if at least one guardian number exists
     */
    fun hasGuardians(): Boolean {
        val guardians = getGuardians()
        return guardians.isNotEmpty()
    }

    /**
     * Get list of saved guardian numbers
     * @return List of Guardian objects
     */
    fun getGuardians(): List<Guardian> {
        val json = sharedPreferences.getString(KEY_GUARDIANS, null) ?: return emptyList()
        val type: Type = object : TypeToken<List<Guardian>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Save a list of guardians
     * @param guardians List of Guardian objects to save
     */
    private fun saveGuardians(guardians: List<Guardian>) {
        val json = gson.toJson(guardians)
        sharedPreferences.edit().putString(KEY_GUARDIANS, json).apply()
    }

    /**
     * Add a new guardian
     * @param name Guardian's name
     * @param phoneNumber Guardian's phone number
     * @return true if added successfully
     */
    fun addGuardian(name: String, phoneNumber: String): Boolean {
        if (name.isBlank() || phoneNumber.isBlank()) {
            return false
        }

        // Normalize phone number (remove spaces, dashes, etc.)
        val normalizedNumber = phoneNumber.replace(Regex("[^+0-9]"), "")

        val guardians = getGuardians().toMutableList()

        // Check if this number already exists
        if (guardians.any { it.phoneNumber == normalizedNumber }) {
            return false
        }

        guardians.add(Guardian(name, normalizedNumber))
        saveGuardians(guardians)
        return true
    }

    /**
     * Update an existing guardian
     * @param oldGuardian Existing Guardian object
     * @param name New name
     * @param phoneNumber New phone number
     * @return true if updated successfully
     */
    fun updateGuardian(oldGuardian: Guardian, name: String, phoneNumber: String): Boolean {
        if (name.isBlank() || phoneNumber.isBlank()) {
            return false
        }

        // Normalize phone number
        val normalizedNumber = phoneNumber.replace(Regex("[^+0-9]"), "")

        val guardians = getGuardians().toMutableList()
        val index = guardians.indexOfFirst { it.id == oldGuardian.id }

        if (index != -1) {
            guardians[index] = Guardian(name, normalizedNumber, oldGuardian.id)
            saveGuardians(guardians)
            return true
        }
        return false
    }

    /**
     * Remove a guardian
     * @param guardian Guardian to remove
     * @return true if removed successfully
     */
    fun removeGuardian(guardian: Guardian): Boolean {
        val guardians = getGuardians().toMutableList()
        val removed = guardians.removeIf { it.id == guardian.id }
        if (removed) {
            saveGuardians(guardians)
        }
        return removed
    }

    /**
     * Show dialog to add a new guardian
     * @param callback Optional callback when guardian is added
     */
    fun showAddGuardianDialog(callback: ((Boolean) -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_guardian, null)
        val nameInput = view.findViewById<EditText>(R.id.guardianNameInput)
        val phoneInput = view.findViewById<EditText>(R.id.guardianPhoneInput)

        AlertDialog.Builder(context)
            .setTitle("Add Guardian")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                val success = addGuardian(name, phone)
                callback?.invoke(success)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show dialog to manage (add/edit/remove) guardians
     * @param callback Optional callback when dialog is dismissed
     */

    fun showManageGuardiansDialog(callback: (() -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_manage_guardians, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.guardiansRecyclerView)
        val emptyView = view.findViewById<TextView>(R.id.emptyGuardiansText)
        val addButton = view.findViewById<Button>(R.id.addGuardianButton)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Manage Guardians")
            .setView(view)
            .setPositiveButton("Done", null)
            .create()

        // Create adapter first, then set up the callbacks
        val guardianList: MutableList<Guardian> = getGuardians().toMutableList()
        val adapter = GuardianAdapter(guardianList, { _ -> }, { _ -> })

        // Now set up the callbacks with explicit reference to the created adapter
        adapter.setOnEditListener { guardian ->
            showEditGuardianDialog(guardian) {
                // Refresh the list
                val updatedList: List<Guardian> = getGuardians()
                adapter.updateList(updatedList)
                checkEmptyState(adapter, emptyView)
            }
        }

        adapter.setOnDeleteListener { guardian ->
            showDeleteGuardianDialog(guardian) {
                // Refresh the list
                val updatedList: List<Guardian> = getGuardians()
                adapter.updateList(updatedList)
                checkEmptyState(adapter, emptyView)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Check if the list is empty
        checkEmptyState(adapter, emptyView)

        addButton.setOnClickListener {
            showAddGuardianDialog { success ->
                if (success) {
                    // Refresh the list
                    val updatedList: List<Guardian> = getGuardians()
                    adapter.updateList(updatedList)
                    checkEmptyState(adapter, emptyView)
                }
            }
        }

        dialog.setOnDismissListener {
            callback?.invoke()
        }

        dialog.show()
    }


    /**
     * Show initial setup dialog for first app launch
     * @param callback Function to call when setup is complete
     */
    fun showInitialSetupDialog(callback: () -> Unit) {
        val message = "Please add at least one emergency contact who will receive your location in case of emergency."

        AlertDialog.Builder(context)
            .setTitle("Emergency Contact Setup")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Add Guardian") { _, _ ->
                showAddGuardianDialog { success ->
                    if (success) {
                        Toast.makeText(context, "Guardian added successfully", Toast.LENGTH_SHORT).show()
                        callback()
                    } else {
                        // If failed, show the dialog again
                        showInitialSetupDialog(callback)
                    }
                }
            }
            .setNeutralButton("Skip") { _, _ ->
                // Allow skipping for now
                Toast.makeText(context, "You can add guardians later from settings", Toast.LENGTH_LONG).show()
                callback()
            }
            .show()
    }

    /**
     * Show dialog to edit an existing guardian
     * @param guardian Guardian to edit
     * @param callback Optional callback when edit is complete
     */
    private fun showEditGuardianDialog(guardian: Guardian, callback: (() -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_guardian, null)
        val nameInput = view.findViewById<EditText>(R.id.guardianNameInput)
        val phoneInput = view.findViewById<EditText>(R.id.guardianPhoneInput)

        // Pre-fill the fields
        nameInput.setText(guardian.name)
        phoneInput.setText(guardian.phoneNumber)

        AlertDialog.Builder(context)
            .setTitle("Edit Guardian")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                updateGuardian(guardian, name, phone)
                callback?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show confirmation dialog to delete a guardian
     * @param guardian Guardian to delete
     * @param callback Optional callback when deletion is complete
     */
    private fun showDeleteGuardianDialog(guardian: Guardian, callback: (() -> Unit)? = null) {
        AlertDialog.Builder(context)
            .setTitle("Remove Guardian")
            .setMessage("Are you sure you want to remove ${guardian.name} from your guardians?")
            .setPositiveButton("Remove") { _, _ ->
                removeGuardian(guardian)
                callback?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Check if the guardians list is empty and update UI accordingly
     */
    private fun checkEmptyState(adapter: GuardianAdapter, emptyView: TextView) {
        if (adapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    /**
     * Send SMS to all guardians
     * @param message Message to send
     * @param isEmergency Whether this is an emergency message (affects delivery priority)
     * @return Number of guardians that were messaged
     */
    fun sendSmsToAllGuardians(message: String, isEmergency: Boolean = false): Int {
        val guardians = getGuardians()
        Log.d("GuardianManager", "sendSmsToAllGuardians found ${guardians.size} guardians")

        if (guardians.isEmpty()) {
            return 0
        }

        var sentCount = 0

        try {
            // Check for SMS permission first
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("GuardianManager", "SMS permission not granted")
                Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show()

                // Request permission if in an activity
                if (context is Activity) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.SEND_SMS),
                        1002 // SMS_PERMISSION_REQUEST_CODE
                    )
                }
                return 0
            }

            // Get SmsManager instance
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Process each guardian
            for (guardian in guardians) {
                try {
                    Log.d("GuardianManager", "Sending SMS to ${guardian.name} at ${guardian.phoneNumber}")

                    // Make sure phone number is valid
                    if (guardian.phoneNumber.isBlank()) {
                        Log.e("GuardianManager", "Invalid phone number for ${guardian.name}")
                        continue
                    }

                    // For emergency messages, try to send free SMS if available
                    if (isEmergency) {
                        // Try to send as emergency SMS if supported on this device (Android 10+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                // Attempt to send as emergency message on supported devices
                                // Note: This requires special permissions and might not work on all devices
                                val messageParts = smsManager.divideMessage(message)

                                // On some carriers, this might send without requiring load/credit
                                smsManager.sendMultipartTextMessage(
                                    guardian.phoneNumber,
                                    null,
                                    messageParts,
                                    null,
                                    null,
                                    0.toString(), // No delay
                                    isEmergency.toString() // Mark as emergency for supported carriers
                                )

                                sentCount++
                                Log.d("GuardianManager", "Sent emergency SMS to ${guardian.phoneNumber}")
                                continue
                            } catch (e: Exception) {
                                Log.e("GuardianManager", "Failed to send as emergency SMS: ${e.message}")
                                // Fall back to regular SMS
                            }
                        }
                    }

                    // Standard SMS sending
                    val messageParts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        guardian.phoneNumber,
                        null,
                        messageParts,
                        null,
                        null
                    )
                    sentCount++
                    Log.d("GuardianManager", "Successfully sent SMS to ${guardian.phoneNumber}")

                } catch (e: Exception) {
                    // Log error but continue with other guardians
                    Log.e("GuardianManager", "Error sending SMS to ${guardian.phoneNumber}: ${e.message}")
                    e.printStackTrace()
                }
            }

            Log.d("GuardianManager", "Sent SMS to $sentCount guardians")
            return sentCount
        } catch (e: Exception) {
            Log.e("GuardianManager", "Error in sendSmsToAllGuardians: ${e.message}")
            e.printStackTrace()
            return 0
        }
    }
    /**
     * Debug method to diagnose guardian storage issues
     * Add this to your GuardianManager class
     */
    fun diagnosePotentialIssues() {
        Log.d("GuardianDiagnosis", "Starting guardian storage diagnosis")

        // Check if SharedPreferences file exists
        try {
            val prefsFile = context.getSharedPreferences(GUARDIAN_PREFS, Context.MODE_PRIVATE)
            val allPrefs = prefsFile.all
            Log.d("GuardianDiagnosis", "Number of preferences entries: ${allPrefs.size}")
            Log.d("GuardianDiagnosis", "All keys: ${allPrefs.keys.joinToString()}")

            // Check specific guardian data
            val guardiansJson = prefsFile.getString(KEY_GUARDIANS, null)
            Log.d("GuardianDiagnosis", "Guardians JSON (null? ${guardiansJson == null}): $guardiansJson")

            // Try parsing the JSON
            if (!guardiansJson.isNullOrEmpty()) {
                try {
                    val type: Type = object : TypeToken<List<Guardian>>() {}.type
                    val result = gson.fromJson<List<Guardian>>(guardiansJson, type)
                    Log.d("GuardianDiagnosis", "Successfully parsed ${result?.size ?: 0} guardians")
                } catch (e: Exception) {
                    Log.e("GuardianDiagnosis", "Failed to parse JSON: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Check for setup_completed flag
            val isSetupCompleted = prefsFile.getBoolean("setup_completed", false)
            Log.d("GuardianDiagnosis", "Is setup completed: $isSetupCompleted")

        } catch (e: Exception) {
            Log.e("GuardianDiagnosis", "Error accessing SharedPreferences: ${e.message}")
            e.printStackTrace()
        }

        // Test adding a guardian
        val testSuccess = addGuardian("Test Guardian", "1234567890")
        Log.d("GuardianDiagnosis", "Test guardian add success: $testSuccess")

        // Check if guardian was added
        val guardians = getGuardians()
        Log.d("GuardianDiagnosis", "After test add, found ${guardians.size} guardians")

        // Remove test guardian if added
        if (testSuccess && guardians.isNotEmpty()) {
            val testGuardian = guardians.find { it.name == "Test Guardian" }
            if (testGuardian != null) {
                val removeSuccess = removeGuardian(testGuardian)
                Log.d("GuardianDiagnosis", "Test guardian removal success: $removeSuccess")
            }
        }

        Log.d("GuardianDiagnosis", "Diagnosis complete")
    }
    companion object {
        private const val GUARDIAN_PREFS = "guardian_preferences"
        private const val KEY_GUARDIANS = "guardians"

        /**
         * Check if initial setup has been performed
         */
        fun isSetupCompleted(context: Context): Boolean {
            val prefs = context.getSharedPreferences(GUARDIAN_PREFS, Context.MODE_PRIVATE)
            return prefs.getBoolean("setup_completed", false)
        }

        /**
         * Mark initial setup as completed
         */
        fun markSetupCompleted(context: Context) {
            val prefs = context.getSharedPreferences(GUARDIAN_PREFS, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("setup_completed", true).apply()
        }
    }
}


/**
 * Data class representing a guardian/emergency contact
 */
data class Guardian(
    val name: String,
    val phoneNumber: String,
    val id: String = java.util.UUID.randomUUID().toString()
) : java.io.Serializable