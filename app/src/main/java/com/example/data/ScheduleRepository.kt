package com.example.data

import android.content.Context
import com.example.engine.ActionExecutor
import com.example.model.ExecutionLog
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScheduleRepository(
    private val context: Context,
    private val scheduleDao: ScheduleDao,
    private val executionLogDao: ExecutionLogDao
) {
    val allSchedules: Flow<List<NightSchedule>> = scheduleDao.getAllSchedules()
    val activeQuickTimer: Flow<NightSchedule?> = scheduleDao.getActiveQuickTimer()
    val allExecutionLogs: Flow<List<ExecutionLog>> = executionLogDao.getAllLogs()

    suspend fun startQuickTimer(durationMinutes: Int, actions: Set<SleepAction>): Long {
        // Clear any previous quick timer
        val existing = scheduleDao.getActiveQuickTimerSync()
        if (existing != null) {
            AlarmScheduler.cancel(context, existing.id)
            scheduleDao.deleteSchedule(existing)
        }

        val targetMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val actionsString = actions.joinToString(",") { it.name }

        val schedule = NightSchedule(
            title = "Sleep Timer ($durationMinutes min)",
            hour = 0,
            minute = 0,
            daysOfWeek = "",
            isEnabled = true,
            isQuickTimer = true,
            targetTimeMillis = targetMillis,
            actionsJson = actionsString
        )

        val id = scheduleDao.insertSchedule(schedule)
        val saved = schedule.copy(id = id)
        AlarmScheduler.schedule(context, saved)
        return id
    }

    suspend fun cancelQuickTimer() {
        val existing = scheduleDao.getActiveQuickTimerSync()
        if (existing != null) {
            AlarmScheduler.cancel(context, existing.id)
            scheduleDao.deleteSchedule(existing)
        }
    }

    suspend fun snoozeActiveTimer(minutes: Int) {
        val active = scheduleDao.getActiveQuickTimerSync() ?: return
        AlarmScheduler.cancel(context, active.id)

        val baseTime = if (active.targetTimeMillis > System.currentTimeMillis()) {
            active.targetTimeMillis
        } else {
            System.currentTimeMillis()
        }
        val newTarget = baseTime + (minutes * 60 * 1000L)
        val updated = active.copy(targetTimeMillis = newTarget)

        scheduleDao.updateSchedule(updated)
        AlarmScheduler.schedule(context, updated)
    }

    suspend fun saveSchedule(schedule: NightSchedule): Long {
        val id = if (schedule.id == 0L) {
            scheduleDao.insertSchedule(schedule)
        } else {
            scheduleDao.updateSchedule(schedule)
            schedule.id
        }

        val updated = schedule.copy(id = id)
        if (updated.isEnabled) {
            AlarmScheduler.schedule(context, updated)
        } else {
            AlarmScheduler.cancel(context, id)
        }
        return id
    }

    suspend fun toggleSchedule(schedule: NightSchedule, isEnabled: Boolean) {
        val updated = schedule.copy(isEnabled = isEnabled)
        scheduleDao.updateSchedule(updated)
        if (isEnabled) {
            AlarmScheduler.schedule(context, updated)
        } else {
            AlarmScheduler.cancel(context, updated.id)
        }
    }

    suspend fun deleteSchedule(schedule: NightSchedule) {
        AlarmScheduler.cancel(context, schedule.id)
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun delaySchedule10Min(scheduleId: Long) {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return
        AlarmScheduler.cancel(context, scheduleId)

        val newTarget = (if (schedule.targetTimeMillis > System.currentTimeMillis()) {
            schedule.targetTimeMillis
        } else {
            System.currentTimeMillis()
        }) + (10 * 60 * 1000L)

        val updated = schedule.copy(targetTimeMillis = newTarget)
        scheduleDao.updateSchedule(updated)
        AlarmScheduler.schedule(context, updated)
    }

    fun runNow(scheduleId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            ActionExecutor.execute(context, scheduleId)
        }
    }

    suspend fun clearHistory() {
        executionLogDao.clearAllLogs()
    }

    suspend fun restoreAllAlarms() {
        val enabledList = scheduleDao.getEnabledSchedules()
        for (sched in enabledList) {
            AlarmScheduler.schedule(context, sched)
        }
    }
}
