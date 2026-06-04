package com.securityguard.app.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import android.util.Log

object FirebaseConfig {
    private val TAG = "FirebaseConfig"
    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    private var locationRef: DatabaseReference? = null
    
    fun initialize(context: Context) {
        try {
            // Firebase Realtime Database এ সংযোগ স্থাপন
            database = FirebaseDatabase.getInstance()
            database?.setPersistenceEnabled(true)
            
            // Firebase Authentication ইনিশিয়ালাইজ
            auth = FirebaseAuth.getInstance()
            
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }
    }
    
    fun getDatabase(): FirebaseDatabase? {
        return database ?: FirebaseDatabase.getInstance().also { 
            database = it 
        }
    }
    
    fun getAuth(): FirebaseAuth? {
        return auth ?: FirebaseAuth.getInstance().also { 
            auth = it 
        }
    }
    
    fun getLocationReference(deviceId: String): DatabaseReference? {
        return try {
            database?.reference?.child("devices")?.child(deviceId)?.child("location")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location reference: ${e.message}")
            null
        }
    }
    
    fun getDeviceReference(deviceId: String): DatabaseReference? {
        return try {
            database?.reference?.child("devices")?.child(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device reference: ${e.message}")
            null
        }
    }
    
    fun saveLocationData(deviceId: String, latitude: Double, longitude: Double, timestamp: Long) {
        try {
            val locationData = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "timestamp" to timestamp,
                "accuracy" to 0.0
            )
            
            getLocationReference(deviceId)?.setValue(locationData)
                ?.addOnSuccessListener {
                    Log.d(TAG, "Location saved successfully")
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Error saving location: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saving location: ${e.message}")
        }
    }
    
    fun getLocationData(deviceId: String, callback: (Double, Double, Long) -> Unit) {
        try {
            getLocationReference(deviceId)?.get()?.addOnSuccessListener { snapshot ->
                val latitude = snapshot.child("latitude").value as? Double ?: 0.0
                val longitude = snapshot.child("longitude").value as? Double ?: 0.0
                val timestamp = snapshot.child("timestamp").value as? Long ?: 0L
                callback(latitude, longitude, timestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
        }
    }
}
