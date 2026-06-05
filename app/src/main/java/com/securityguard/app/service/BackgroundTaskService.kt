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

    // ইতিমধ্যে সেভ করা alert-এর key গুলো মনে রাখো (duplicate এড়াতে)
    private val seenAlertKeys = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        db = SecurityDatabase.getDatabase(this)
        Log.d(TAG, "BackgroundTaskService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startScanning()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScanning() {
        serviceScope.launch {
            // প্রথম scan টা ১৫ সেকেন্ড পরে শুরু করো (app load হতে সময় দাও)
            delay(15_000L)
            while (isActive) {
                try {
                    performSecurityScan()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during scan: ${e.message}")
                }
                delay(5 * 60 * 1000L) // প্রতি ৫ মিনিটে scan
            }
        }
    }

    private suspend fun performSecurityScan() {
        Log.d(TAG, "Starting security scan...")
        val pm = packageManager

        val packages: List<PackageInfo> = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve packages: ${e.message}")
            return
        }

        val currentTime = System.currentTimeMillis()
        val newAlerts = mutableListOf<SecurityAlertEntity>()

        // বিপজ্জনক permission-এর তালিকা
        val dangerousPerms = mapOf(
            android.Manifest.permission.CAMERA to "ক্যামেরা",
            android.Manifest.permission.RECORD_AUDIO to "মাইক্রোফোন",
            android.Manifest.permission.ACCESS_FINE_LOCATION to "সঠিক অবস্থান",
            android.Manifest.permission.READ_CALL_LOG to "কল লগ",
            android.Manifest.permission.READ_CONTACTS to "পরিচিতি",
            android.Manifest.permission.SEND_SMS to "SMS পাঠানো",
            android.Manifest.permission.READ_SMS to "SMS পড়া"
        )

        for (pkg in packages) {
            val appInfo = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg.packageName, 0)
                }
            } catch (e: PackageManager.NameNotFoundException) { continue }

            // System app ও নিজেকে বাদ দাও
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp || pkg.packageName == packageName) continue

            val requestedPerms = pkg.requestedPermissions ?: continue
            val foundPerms = dangerousPerms.entries
                .filter { (perm, _) -> requestedPerms.contains(perm) }
                .map { it.value }

            if (foundPerms.isEmpty()) continue

            val appLabel = pm.getApplicationLabel(appInfo).toString()
            val alertKey = "${pkg.packageName}:${foundPerms.sorted().joinToString(",")}"

            // ইতিমধ্যে এই alert insert করা হয়েছে কিনা চেক করো
            if (seenAlertKeys.contains(alertKey)) continue
            seenAlertKeys.add(alertKey)

            // risk level নির্ধারণ করো
            val riskLevel = when {
                foundPerms.size >= 4 -> "🔴 উচ্চ ঝুঁকি"
                foundPerms.size >= 2 -> "🟡 মাঝারি ঝুঁকি"
                else -> "🟢 কম ঝুঁকি"
            }

            newAlerts.add(
                SecurityAlertEntity(
                    alertType = "SUSPICIOUS_APP",
                    title = "$riskLevel — $appLabel",
                    description = "\"$appLabel\" অ্যাপটির কাছে এই সংবেদনশীল পারমিশনগুলো আছে: ${foundPerms.joinToString(", ")}। অ্যাপটি বিশ্বস্ত কিনা যাচাই করুন।",
                    timestamp = currentTime,
                    isResolved = false
                )
            )
        }

        // শুধু নতুন alerts insert করো — সব delete করো না!
        if (newAlerts.isNotEmpty()) {
            for (alert in newAlerts) {
                db.securityAlertDao().insert(alert)
            }
            Log.d(TAG, "Scan done. ${newAlerts.size} new alerts inserted.")
        } else {
            Log.d(TAG, "Scan done. No new suspicious apps found.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
        Log.d(TAG, "BackgroundTaskService destroyed")
    }

    companion object {
        private const val TAG = "BackgroundTaskService"
    }
}
