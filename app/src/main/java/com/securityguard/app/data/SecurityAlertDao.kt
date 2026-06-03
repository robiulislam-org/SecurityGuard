package com.securityguard.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SecurityAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: SecurityAlertEntity)

    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): LiveData<List<SecurityAlertEntity>>

    @Query("SELECT * FROM security_alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedAlerts(): LiveData<List<SecurityAlertEntity>>

    @Query("UPDATE security_alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun resolveAlert(alertId: Int)

    @Query("DELETE FROM security_alerts")
    suspend fun clearAll()
}
