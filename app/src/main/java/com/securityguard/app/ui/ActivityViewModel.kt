package com.securityguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.securityguard.app.data.AppUsageEntity
import com.securityguard.app.data.SecurityDatabase

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SecurityDatabase.getDatabase(application)
    val appUsageList: LiveData<List<AppUsageEntity>> = db.appUsageDao().getAllUsage()
}
