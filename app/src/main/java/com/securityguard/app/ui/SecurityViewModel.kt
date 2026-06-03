package com.securityguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.securityguard.app.data.SecurityAlertEntity
import com.securityguard.app.data.SecurityDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SecurityDatabase.getDatabase(application)
    val securityAlerts: LiveData<List<SecurityAlertEntity>> = db.securityAlertDao().getAllAlerts()

    fun resolveAlert(alertId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.securityAlertDao().resolveAlert(alertId)
        }
    }
}
