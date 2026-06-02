package com.securityguard.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BackgroundTaskService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("BackgroundTaskService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundTaskService", "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
