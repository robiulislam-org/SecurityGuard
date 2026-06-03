package com.securityguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_alerts")
data class SecurityAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alertType: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val isResolved: Boolean = false
)
