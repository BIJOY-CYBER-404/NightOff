package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NightOffApplication
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActionNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val action = intent.action ?: return

        Log.d(TAG, "ActionNotificationReceiver action: $action, scheduleId: $scheduleId")
        if (scheduleId == -1L) return

        val app = context.applicationContext as NightOffApplication
        val repository = app.repository

        NotificationHelper.dismissReminderNotification(context, scheduleId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_CANCEL -> {
                        val schedule = app.database.scheduleDao().getScheduleById(scheduleId)
                        if (schedule != null) {
                            if (schedule.isQuickTimer) {
                                repository.cancelQuickTimer()
                            } else {
                                repository.toggleSchedule(schedule, false)
                            }
                        }
                    }
                    ACTION_DELAY_10 -> {
                        repository.delaySchedule10Min(scheduleId)
                    }
                    ACTION_RUN_NOW -> {
                        repository.runNow(scheduleId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ActionNotifReceiver"
        const val ACTION_CANCEL = "com.example.nightoff.ACTION_CANCEL"
        const val ACTION_DELAY_10 = "com.example.nightoff.ACTION_DELAY_10"
        const val ACTION_RUN_NOW = "com.example.nightoff.ACTION_RUN_NOW"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
    }
}
