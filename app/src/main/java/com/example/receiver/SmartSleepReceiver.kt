package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NotificationHelper
import com.example.util.SmartSleepDetector

class SmartSleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check if inside night window
        if (SmartSleepDetector.isInsideNightWindow()) {
            // Send smart sleep confirmation notification: "Looks like you fell asleep. Shutting down in 5 minutes."
            NotificationHelper.showSmartSleepConfirmationNotification(context)
        }

        // Reschedule next check
        SmartSleepDetector.scheduleSmartCheck(context)
    }
}
