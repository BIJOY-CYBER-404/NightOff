package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "night_schedules")
data class NightSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String = "", // e.g. "1,2,3,4,5,6,7" (1=Monday ... 7=Sunday) or "" for one-time
    val isEnabled: Boolean = true,
    val isQuickTimer: Boolean = false,
    val targetTimeMillis: Long = 0L,
    val actionsJson: String = "", // comma-separated SleepAction enum names
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getActionList(): Set<SleepAction> {
        if (actionsJson.isBlank()) return SleepAction.defaultActions()
        return actionsJson.split(",")
            .mapNotNull { name ->
                try {
                    SleepAction.valueOf(name.trim())
                } catch (e: Exception) {
                    null
                }
            }
            .toSet()
    }

    fun getDaysList(): Set<Int> {
        if (daysOfWeek.isBlank()) return emptySet()
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }
}
