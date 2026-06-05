package com.securityguard.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.securityguard.app.data.LocationEntity
import com.securityguard.app.data.SecurityDatabase
import com.securityguard.app.utils.FirebaseConfig
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: SecurityDatabase
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val CHANNEL_ID = "location_tracking_channel"
    private val NOTIFICATION_ID = 1002

    private var lastLat = 0.0
    private var lastLon = 0.0

    // ডিভাইসের unique ID (Firebase-এ data সেভ করতে)
    private val deviceId by lazy {
        android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                handleNewLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = SecurityDatabase.getDatabase(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("লোকেশন ট্র্যাকিং সক্রিয়"))
        Log.d(TAG, "LocationTrackingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5 * 60 * 1000L)
            .setMinUpdateDistanceMeters(50f)   // ৫০ মিটার নড়লে update
            .setMinUpdateIntervalMillis(60_000L) // সর্বনিম্ন ১ মিনিট
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}")
        }
    }

    private fun handleNewLocation(location: Location) {
        val latDiff = Math.abs(location.latitude - lastLat)
        val lonDiff = Math.abs(location.longitude - lastLon)

        // ন্যূনতম ৫০ মিটার পরিবর্তন হলে সেভ করো
        if (latDiff < 0.0005 && lonDiff < 0.0005 && lastLat != 0.0) return

        lastLat = location.latitude
        lastLon = location.longitude

        // Notification update করো
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, buildNotification(
            "শেষ আপডেট: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
        ))

        serviceScope.launch {
            try {
                // Room DB-তে সেভ করো
                db.locationDao().insert(
                    LocationEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Firebase-এও সেভ করো (real-time tracking)
                FirebaseConfig.saveLocationData(deviceId, location.latitude, location.longitude, System.currentTimeMillis())

                Log.d(TAG, "Location saved: ${location.latitude}, ${location.longitude}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving location: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "লোকেশন ট্র্যাকিং", NotificationManager.IMPORTANCE_LOW).apply {
                description = "ব্যাকগ্রাউন্ডে লোকেশন ট্র্যাক করে"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecurityGuard — লোকেশন")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { fusedLocationClient.removeLocationUpdates(locationCallback) } catch (_: Exception) {}
        serviceJob.cancel()
        Log.d(TAG, "LocationTrackingService destroyed")
    }

    companion object {
        private const val TAG = "LocationTrackingService"
    }
}
