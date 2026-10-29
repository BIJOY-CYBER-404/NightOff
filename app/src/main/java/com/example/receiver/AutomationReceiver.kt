package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_START_PROFILE = "com.example.ACTION_START_PROFILE"
        const val EXTRA_PROFILE_NAME = "EXTRA_PROFILE_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_START_PROFILE) {
            val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Study night"
            val db = AppDatabase.getDatabase(context)

            CoroutineScope(Dispatchers.IO).launch {
                val scheduleDao = db.scheduleDao()
                val all = scheduleDao.getAllSchedulesSync()
                val match = all.find { it.title.contains(profileName, ignoreCase = true) }
                if (match != null) {
                    com.example.engine.ActionExecutor.execute(context, match.id)
                } else {
                    // Fallback to quick timer with 30 min if profile not found
                    com.example.data.ScheduleRepository(context, scheduleDao, db.executionLogDao())
                        .startQuickTimer(30, com.example.model.SleepAction.defaultActions())
                }
            }
        }
    }
}
