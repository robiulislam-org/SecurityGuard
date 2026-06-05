package com.securityguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.securityguard.app.service.BackgroundTaskService
import com.securityguard.app.service.LocationTrackingService
import com.securityguard.app.service.MonitoringService
import com.securityguard.app.service.UninstallProtectionService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Device booted — starting all SecurityGuard services")

        // ফোন রিস্টার্ট হলে সব service স্বয়ংক্রিয়ভাবে চালু হবে
        listOf(
            MonitoringService::class.java,
            LocationTrackingService::class.java,
            UninstallProtectionService::class.java
        ).forEach { serviceClass ->
            val serviceIntent = Intent(context, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        // BackgroundTaskService foreground নয়
        context.startService(Intent(context, BackgroundTaskService::class.java))

        Log.d("BootReceiver", "All services started successfully")
    }
}
