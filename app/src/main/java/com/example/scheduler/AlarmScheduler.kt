package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.NightSchedule
import com.example.receiver.AlarmReceiver
import com.example.receiver.ReminderReceiver
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"

    fun schedule(context: Context, schedule: NightSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerTime = computeNextTriggerMillis(schedule)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Computed trigger time is in the past: $triggerTime")
            return
        }

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - missing permission")
                return
            }
        }

        // 1. Schedule Execution Alarm
        val execIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }
        val execPendingIntent = PendingIntent.getBroadcast(
            context,
            (schedule.id * 100).toInt(),
            execIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                execPendingIntent
            )
            Log.d(TAG, "Scheduled execution for schedule ${schedule.id} at $triggerTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
        }

        // 2. Schedule 10-minute warning reminder
        val tenMinutesMillis = 10 * 60 * 1000L
        val reminderTime = if (triggerTime - System.currentTimeMillis() > tenMinutesMillis) {
            triggerTime - tenMinutesMillis
        } else if (triggerTime - System.currentTimeMillis() > 2 * 60 * 1000L) {
            // If less than 10 mins but more than 2 mins, warn 2 mins prior
            triggerTime - 2 * 60 * 1000L
        } else {
            0L // Too short to show a useful pre-warning
        }

        if (reminderTime > System.currentTimeMillis()) {
            val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, schedule.id)
                putExtra("EXTRA_TARGET_TIME", triggerTime)
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                (schedule.id * 100 + 1).toInt(),
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    reminderPendingIntent
                )
                Log.d(TAG, "Scheduled reminder for schedule ${schedule.id} at $reminderTime")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException scheduling reminder alarm", e)
            }
        }
    }

    fun cancel(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel Execution
        val execIntent = Intent(context, AlarmReceiver::class.java)
        val execPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 100).toInt(),
            execIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (execPendingIntent != null) {
            alarmManager.cancel(execPendingIntent)
            execPendingIntent.cancel()
        }

        // Cancel Reminder
        val reminderIntent = Intent(context, ReminderReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 100 + 1).toInt(),
            reminderIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (reminderPendingIntent != null) {
            alarmManager.cancel(reminderPendingIntent)
            reminderPendingIntent.cancel()
        }

        Log.d(TAG, "Cancelled alarms for schedule $scheduleId")
    }

    fun computeNextTriggerMillis(schedule: NightSchedule): Long {
        if (schedule.isQuickTimer && schedule.targetTimeMillis > System.currentTimeMillis()) {
            return schedule.targetTimeMillis
        }

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysSet = schedule.getDaysList() // e.g. 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun

        if (daysSet.isEmpty()) {
            // One-time schedule: if time already passed today, schedule for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            // Recurring schedule on specific days
            // Map Android Calendar.DAY_OF_WEEK to 1..7 (Mon..Sun)
            // Calendar: SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5, FRIDAY=6, SATURDAY=7
            fun calendarDayToIso(dayOfWeek: Int): Int {
                return when (dayOfWeek) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
            }

            for (dayOffset in 0..7) {
                val candidate = (target.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                val candidateIsoDay = calendarDayToIso(candidate.get(Calendar.DAY_OF_WEEK))

                if (daysSet.contains(candidateIsoDay)) {
                    if (candidate.timeInMillis > now.timeInMillis) {
                        return candidate.timeInMillis
                    }
                }
            }
            // Fallback
            target.add(Calendar.DAY_OF_YEAR, 1)
            return target.timeInMillis
        }
    }
}
