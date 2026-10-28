package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_logs")
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduleTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalActions: Int,
    val successActions: Int,
    val status: String, // "SUCCESS", "PARTIAL", "FAILED"
    val actionResultsFormatted: String // formatted newline-separated strings
)
