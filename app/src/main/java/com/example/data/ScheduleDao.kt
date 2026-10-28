package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.NightSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM night_schedules ORDER BY isQuickTimer DESC, hour ASC, minute ASC")
    fun getAllSchedules(): Flow<List<NightSchedule>>

    @Query("SELECT * FROM night_schedules WHERE isQuickTimer = 1 LIMIT 1")
    fun getActiveQuickTimer(): Flow<NightSchedule?>

    @Query("SELECT * FROM night_schedules WHERE isQuickTimer = 1 LIMIT 1")
    suspend fun getActiveQuickTimerSync(): NightSchedule?

    @Query("SELECT * FROM night_schedules WHERE isEnabled = 1")
    suspend fun getEnabledSchedules(): List<NightSchedule>

    @Query("SELECT * FROM night_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): NightSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: NightSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: NightSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: NightSchedule)

    @Query("DELETE FROM night_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("DELETE FROM night_schedules WHERE isQuickTimer = 1")
    suspend fun clearQuickTimers()
}
