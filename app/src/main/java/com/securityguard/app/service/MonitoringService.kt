package com.securityguard.app.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.content.Context
import android.location.LocationManager
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MonitoringService : Service() {
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Log.d("MonitoringService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MonitoringService", "Service started")
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startMonitoring() {
        Thread {
            while (true) {
                try {
                    trackScreenTime()
                    trackLocation()
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    Log.e("MonitoringService", "Error in monitoring: ${e.message}")
                }
            }
        }.start()
    }

    private fun trackScreenTime() {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (24 * 60 * 60 * 1000)
            val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            
            if (usageStatsList != null && usageStatsList.isNotEmpty()) {
                for (stats in usageStatsList) {
                    Log.d("ScreenTime", "${stats.packageName}: ${stats.totalTimeInForeground}ms")
                }
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error tracking screen time: ${e.message}")
        }
    }

    private fun trackLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                location?.let {
                    Log.d("Location", "Lat: ${it.latitude}, Lon: ${it.longitude}")
                }
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error tracking location: ${e.message}")
        }
    }
}
