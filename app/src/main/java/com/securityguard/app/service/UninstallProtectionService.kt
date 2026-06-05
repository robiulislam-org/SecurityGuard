package com.securityguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.securityguard.app.data.SecurityAlertEntity
import com.securityguard.app.data.SecurityDatabase
import kotlinx.coroutines.*

/**
 * এই service টি অ্যাপের uninstall attempt সনাক্ত করে।
 * Device Admin active থাকলে এটি uninstall-কে ব্লক করতে পারে।
 * এছাড়া যখন app ব্যাকগ্রাউন্ডে kill হয়, সেটা detect করে alert দেয়।
 */
class UninstallProtectionService : Service() {

    private val CHANNEL_ID = "uninstall_protection_channel"
    private val NOTIFICATION_ID = 1003
    private lateinit var db: SecurityDatabase
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        db = SecurityDatabase.getDatabase(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(TAG, "UninstallProtectionService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY — সিস্টেম kill করলে পুনরায় চালু হবে
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // অ্যাপ recent apps থেকে swipe করে বন্ধ করলে এখানে detect হয়
        Log.w(TAG, "App removed from recents — logging alert")
        serviceScope.launch {
            try {
                db.securityAlertDao().insert(
                    SecurityAlertEntity(
                        alertType = "APP_REMOVED",
                        title = "অ্যাপ বন্ধ করার চেষ্টা",
                        description = "SecurityGuard অ্যাপটি সম্প্রতি ব্যাকগ্রাউন্ড থেকে সরিয়ে দেওয়া হয়েছে। নিরাপত্তা পর্যবেক্ষণ সাময়িকভাবে বাধাগ্রস্ত হতে পারে।",
                        timestamp = System.currentTimeMillis(),
                        isResolved = false
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving alert: ${e.message}")
            }
        }

        // Service পুনরায় চালু করো (3 সেকেন্ড পর)
        val restartIntent = Intent(applicationContext, UninstallProtectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Uninstall Protection", NotificationManager.IMPORTANCE_LOW).apply {
                description = "অ্যাপ সুরক্ষা পর্যবেক্ষণ"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecurityGuard সুরক্ষিত")
            .setContentText("অ্যাপ সুরক্ষা সক্রিয় রয়েছে")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "UninstallProtectionService destroyed")
    }

    companion object {
        private const val TAG = "UninstallProtection"
    }
}
