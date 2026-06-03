package com.securityguard.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.securityguard.app.data.AppUsageEntity
import com.securityguard.app.data.LocationEntity
import com.securityguard.app.data.SecurityDatabase
import kotlinx.coroutines.*
import java.util.Calendar

class MonitoringService : Service() {

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var locationManager: LocationManager
    private lateinit var db: SecurityDatabase

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isMonitoring = false

    private val CHANNEL_ID = "security_guard_monitoring"
    private val NOTIFICATION_ID = 1001

    private var lastSavedLatitude = 0.0
    private var lastSavedLongitude = 0.0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            saveLocationToDb(location)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        db = SecurityDatabase.getDatabase(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        Log.d("MonitoringService", "Service created and foreground started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MonitoringService", "Service started onStartCommand")
        if (!isMonitoring) {
            isMonitoring = true
            startMonitoring()
            requestLocationUpdates()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Monitoring"
            val descriptionText = "Monitors device security and activity"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecurityGuard Active")
            .setContentText("আপনার ডিভাইস সুরক্ষিত রাখা হচ্ছে")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    trackScreenTime()
                    trackPassiveLocation()
                    delay(30000) // Poll every 30 seconds
                } catch (e: Exception) {
                    Log.e("MonitoringService", "Error in monitoring loop: ${e.message}")
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    300000L,
                    10f,
                    locationListener
                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    300000L,
                    10f,
                    locationListener
                )
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error requesting location updates: ${e.message}")
        }
    }

    private fun trackPassiveLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                location?.let { saveLocationToDb(it) }
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error tracking passive location: ${e.message}")
        }
    }

    private fun saveLocationToDb(location: Location) {
        val latDiff = Math.abs(location.latitude - lastSavedLatitude)
        val lonDiff = Math.abs(location.longitude - lastSavedLongitude)
        if (latDiff > 0.0001 || lonDiff > 0.0001) {
            lastSavedLatitude = location.latitude
            lastSavedLongitude = location.longitude
            
            serviceScope.launch {
                val entity = LocationEntity(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                )
                db.locationDao().insert(entity)
                Log.d("MonitoringService", "Saved location to DB: Lat: ${location.latitude}, Lon: ${location.longitude}")
            }
        }
    }

    private fun trackScreenTime() {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            )

            if (!usageStatsList.isNullOrEmpty()) {
                val pm = packageManager
                val entities = mutableListOf<AppUsageEntity>()
                for (stats in usageStatsList) {
                    if (stats.totalTimeInForeground > 0) {
                        var appName = stats.packageName
                        try {
                            val appInfo = pm.getApplicationInfo(stats.packageName, 0)
                            appName = pm.getApplicationLabel(appInfo).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            // Keep package name if app not found
                        }
                        
                        entities.add(
                            AppUsageEntity(
                                packageName = stats.packageName,
                                appName = appName,
                                usageTimeMs = stats.totalTimeInForeground,
                                timestamp = endTime
                            )
                        )
                    }
                }
                if (entities.isNotEmpty()) {
                    serviceScope.launch {
                        db.appUsageDao().insertAll(entities)
                        Log.d("MonitoringService", "Saved ${entities.size} app usage entries to DB")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error tracking screen time: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceJob.cancel()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error removing location updates: ${e.message}")
        }
        Log.d("MonitoringService", "Service destroyed")
    }
}
