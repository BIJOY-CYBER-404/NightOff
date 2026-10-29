package com.example.util

import android.app.AlarmManager
import android.content.Context
import android.os.Build

object AlarmSafeChecker {

    data class AlarmStatus(
        val hasAlarmWithin12Hours: Boolean,
        val nextAlarmTimeMillis: Long,
        val formattedTime: String
    )

    fun checkUpcomingAlarm(context: Context): AlarmStatus {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return AlarmStatus(false, 0L, "None")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val alarmClock = alarmManager.nextAlarmClock
            if (alarmClock != null) {
                val triggerTime = alarmClock.triggerTime
                val now = System.currentTimeMillis()
                val diff = triggerTime - now
                val twelveHours = 12 * 60 * 60 * 1000L

                val hasAlarmWithin12 = diff in 1..twelveHours
                val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(triggerTime))

                return AlarmStatus(hasAlarmWithin12, triggerTime, timeStr)
            }
        }

        return AlarmStatus(false, 0L, "No upcoming alarm")
    }
}
