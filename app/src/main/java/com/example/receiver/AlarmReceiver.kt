package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.engine.ActionExecutor
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        Log.d(TAG, "AlarmReceiver woke up for schedule $scheduleId")

        // Acquire brief partial wake lock for safe action execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NightOff:ExecutionWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(35_000L) // 35 seconds max safeguard
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ActionExecutor.execute(context, scheduleId)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing schedule $scheduleId: ${e.message}", e)
            } finally {
                try {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
