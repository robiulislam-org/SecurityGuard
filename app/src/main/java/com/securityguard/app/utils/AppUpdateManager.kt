package com.securityguard.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

object AppUpdateManager {
    private val TAG = "AppUpdateManager"
    private var updateRef: DatabaseReference? = null
    
    data class UpdateInfo(
        val latestVersion: String = "",
        val currentVersion: String = "",
        val downloadUrl: String = "",
        val updateMessage: String = "",
        val isForceUpdate: Boolean = false
    )
    
    fun initialize() {
        try {
            updateRef = FirebaseDatabase.getInstance().reference.child("app_updates")
            Log.d(TAG, "AppUpdateManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AppUpdateManager: ${e.message}")
        }
    }
    
    fun checkForUpdates(context: Context, callback: (UpdateInfo) -> Unit) {
        try {
            updateRef?.child("latest_version")?.get()?.addOnSuccessListener { snapshot ->
                val latestVersion = snapshot.value as? String ?: "1.0.0"
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                val downloadUrl = snapshot.parent?.child("download_url")?.value as? String ?: ""
                val updateMessage = snapshot.parent?.child("message")?.value as? String ?: "New update available!"
                val isForceUpdate = snapshot.parent?.child("force_update")?.value as? Boolean ?: false
                
                val hasUpdate = isVersionNewer(latestVersion, currentVersion)
                
                Log.d(TAG, "Current: $currentVersion, Latest: $latestVersion, Has Update: $hasUpdate")
                
                val updateInfo = UpdateInfo(
                    latestVersion = latestVersion,
                    currentVersion = currentVersion,
                    downloadUrl = downloadUrl,
                    updateMessage = updateMessage,
                    isForceUpdate = isForceUpdate && hasUpdate
                )
                
                callback(updateInfo)
            }?.addOnFailureListener { e ->
                Log.e(TAG, "Error checking for updates: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in checkForUpdates: ${e.message}")
        }
    }
    
    private fun isVersionNewer(latestVersion: String, currentVersion: String): Boolean {
        return try {
            val latest = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(latest.size, current.size)) {
                val l = latest.getOrNull(i) ?: 0
                val c = current.getOrNull(i) ?: 0
                
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions: ${e.message}")
            false
        }
    }
    
    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            context.startActivity(intent)
            Log.d(TAG, "Opening download URL: $downloadUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update: ${e.message}")
        }
    }
}
