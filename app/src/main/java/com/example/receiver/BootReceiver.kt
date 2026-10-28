package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NightOffApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as NightOffApplication
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app.repository.restoreAllAlarms()
                    Log.d(TAG, "Successfully restored all enabled schedules after boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore alarms after boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
