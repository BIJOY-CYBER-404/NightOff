package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.SmartSleepReceiver
import java.util.Calendar

object SmartSleepDetector {
    private const val TAG = "SmartSleepDetector"

    fun scheduleSmartCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SmartSleepReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check every 15 minutes during potential night hours
        val triggerTime = System.currentTimeMillis() + (15 * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled smart sleep background check in 15 mins")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling smart sleep check", e)
        }
    }

    fun isInsideNightWindow(startHour: Int = 22, endHour: Int = 5): Boolean {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            // e.g. 22:00 to 05:00
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }
    }
}
