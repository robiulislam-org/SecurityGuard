package com.securityguard.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllLocations(): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): LiveData<LocationEntity?>

    @Query("DELETE FROM location_history")
    suspend fun clearAll()
}
