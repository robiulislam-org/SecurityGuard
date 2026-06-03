package com.securityguard.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage ORDER BY usageTimeMs DESC")
    fun getAllUsage(): LiveData<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE timestamp >= :startTime ORDER BY usageTimeMs DESC")
    fun getUsageFromTime(startTime: Long): LiveData<List<AppUsageEntity>>

    @Query("DELETE FROM app_usage")
    suspend fun clearAll()
}
