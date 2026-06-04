package com.securityguard.app

import android.os.Bundle
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.securityguard.app.databinding.ActivityMainBinding
import com.securityguard.app.receiver.AdminReceiver
import com.securityguard.app.service.MonitoringService
import com.securityguard.app.service.BackgroundTaskService
import com.securityguard.app.service.LocationTrackingService
import com.securityguard.app.service.UninstallProtectionService
import com.securityguard.app.ui.PermissionActivity
import com.securityguard.app.utils.AppUpdateManager
import com.securityguard.app.utils.FirebaseConfig
import android.content.Intent
import android.util.Log
import android.os.Build
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAllPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            NavigationUI.setupWithNavController(bottomNav, navController)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation setup error: ${e.message}")
        }

        startAllServices()
        checkAndRequestDeviceAdmin()
        initializeFirebase()
        checkForUpdates()

        Log.d(TAG, "MainActivity created successfully")
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        )
        return isUsageStatsGranted() && permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isUsageStatsGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startAllServices() {
        try {
            // Monitoring Service
            val monitorIntent = Intent(this, MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(monitorIntent)
            } else {
                startService(monitorIntent)
            }
            Log.d(TAG, "Monitoring Service started")

            // Location Tracking Service
            val locationIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent)
            } else {
                startService(locationIntent)
            }
            Log.d(TAG, "Location Tracking Service started")

            // Uninstall Protection Service
            val protectionIntent = Intent(this, UninstallProtectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(protectionIntent)
            } else {
                startService(protectionIntent)
            }
            Log.d(TAG, "Uninstall Protection Service started")

            // Background Task Service
            val scanIntent = Intent(this, BackgroundTaskService::class.java)
            startService(scanIntent)
            Log.d(TAG, "Background Task Service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting services: ${e.message}")
        }
    }

    private fun checkAndRequestDeviceAdmin() {
        try {
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SecurityGuard needs admin access for enhanced security monitoring")
                }
                startActivity(intent)
                Log.d(TAG, "Requesting Device Admin")
            } else {
                Log.d(TAG, "Device Admin already active")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting device admin: ${e.message}")
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseConfig.initialize(this)
            AppUpdateManager.initialize()
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }
    }

    private fun checkForUpdates() {
        try {
            AppUpdateManager.checkForUpdates(this) { updateInfo ->
                if (updateInfo.latestVersion.isNotEmpty() && updateInfo.currentVersion < updateInfo.latestVersion) {
                    showUpdateDialog(updateInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}")
        }
    }

    private fun showUpdateDialog(updateInfo: AppUpdateManager.UpdateInfo) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("আপডেট উপলব্ধ")
            .setMessage("নতুন সংস্করণ ${updateInfo.latestVersion} উপলব্ধ\n\n${updateInfo.updateMessage}")
            .setPositiveButton("আপডেট করুন") { _, _ ->
                if (updateInfo.downloadUrl.isNotEmpty()) {
                    AppUpdateManager.downloadAndInstallUpdate(this, updateInfo.downloadUrl)
                }
            }
            .setNegativeButton("পরে") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(!updateInfo.isForceUpdate)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}
