package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.scheduler.AlarmScheduler
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val targetTime = intent.getLongExtra("EXTRA_TARGET_TIME", 0L)
        if (scheduleId == -1L) return

        Log.d(TAG, "ReminderReceiver received warning trigger for schedule $scheduleId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val schedule = db.scheduleDao().getScheduleById(scheduleId)

                // Verify schedule is still enabled and valid
                if (schedule != null && schedule.isEnabled) {
                    NotificationHelper.showReminderNotification(
                        context = context,
                        scheduleId = schedule.id,
                        scheduleTitle = schedule.title,
                        targetTimeMillis = if (targetTime > 0) targetTime else System.currentTimeMillis() + (10 * 60 * 1000L)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing reminder: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}
