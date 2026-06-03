package com.securityguard.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.securityguard.app.data.SecurityAlertEntity
import com.securityguard.app.data.SecurityDatabase
import kotlinx.coroutines.*

class BackgroundTaskService : Service() {

    private lateinit var db: SecurityDatabase
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        db = SecurityDatabase.getDatabase(this)
        Log.d("BackgroundTaskService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundTaskService", "Service started onStartCommand")
        if (!isRunning) {
            isRunning = true
            startScanning()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startScanning() {
        serviceScope.launch {
            while (isActive) {
                try {
                    performSecurityScan()
                    delay(300000) // Scan every 5 minutes
                } catch (e: Exception) {
                    Log.e("BackgroundTaskService", "Error during scan: ${e.message}")
                }
            }
        }
    }

    private suspend fun performSecurityScan() {
        Log.d("BackgroundTaskService", "Starting security scan...")
        val pm = packageManager
        val packages: List<PackageInfo> = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            Log.e("BackgroundTaskService", "Failed to retrieve packages: ${e.message}")
            emptyList()
        }

        val suspiciousApps = mutableListOf<SecurityAlertEntity>()
        val currentTime = System.currentTimeMillis()

        for (pkg in packages) {
            val appInfo = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg.packageName, 0)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp || pkg.packageName == packageName) {
                continue
            }

            val requestedPermissions = pkg.requestedPermissions
            if (requestedPermissions != null) {
                val sensitivePerms = mutableListOf<String>()
                for (perm in requestedPermissions) {
                    if (perm == android.Manifest.permission.CAMERA ||
                        perm == android.Manifest.permission.RECORD_AUDIO ||
                        perm == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                        perm == android.Manifest.permission.READ_CALL_LOG
                    ) {
                        sensitivePerms.add(perm.substringAfterLast("."))
                    }
                }

                if (sensitivePerms.isNotEmpty()) {
                    val appLabel = pm.getApplicationLabel(appInfo).toString()
                    suspiciousApps.add(
                        SecurityAlertEntity(
                            alertType = "SUSPICIOUS_APP",
                            title = "সংবেদনশীল পারমিশন সনাক্ত হয়েছে",
                            description = "\"$appLabel\" অ্যাপটির কাছে এই পারমিশনগুলো আছে: ${sensitivePerms.joinToString(", ")}",
                            timestamp = currentTime,
                            isResolved = false
                        )
                    )
                }
            }
        }

        if (suspiciousApps.isNotEmpty()) {
            db.securityAlertDao().clearAll()
            for (alert in suspiciousApps) {
                db.securityAlertDao().insert(alert)
            }
            Log.d("BackgroundTaskService", "Security scan finished. Found ${suspiciousApps.size} apps with sensitive permissions.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
        Log.d("BackgroundTaskService", "Service destroyed")
    }
}
