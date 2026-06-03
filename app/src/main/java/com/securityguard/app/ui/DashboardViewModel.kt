package com.securityguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.securityguard.app.data.AppUsageEntity
import com.securityguard.app.data.LocationEntity
import com.securityguard.app.data.SecurityAlertEntity
import com.securityguard.app.data.SecurityDatabase
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SecurityDatabase.getDatabase(application)
    val allUsage: LiveData<List<AppUsageEntity>>
    val latestLocation: LiveData<LocationEntity?>
    val unresolvedAlerts: LiveData<List<SecurityAlertEntity>>

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        allUsage = db.appUsageDao().getUsageFromTime(todayStart)
        latestLocation = db.locationDao().getLatestLocation()
        unresolvedAlerts = db.securityAlertDao().getUnresolvedAlerts()
    }
}
