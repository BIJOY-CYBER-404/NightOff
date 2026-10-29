package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.ActionNotificationReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_REMINDERS = "channel_nightoff_reminders"
    const val CHANNEL_RESULTS = "channel_nightoff_results"

    const val NOTIFICATION_ID_REMINDER_BASE = 1000
    const val NOTIFICATION_ID_RESULT = 2000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Sleep Warning Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "10-minute warning before sleep actions are executed"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }

            val resultChannel = NotificationChannel(
                CHANNEL_RESULTS,
                "Execution Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Summaries of executed sleep shutdown actions"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    fun showReminderNotification(
        context: Context,
        scheduleId: Long,
        scheduleTitle: String,
        targetTimeMillis: Long
    ) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(targetTimeMillis))

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel Action
        val cancelIntent = Intent(context, ActionNotificationReceiver::class.java).apply {
            action = ActionNotificationReceiver.ACTION_CANCEL
            putExtra(ActionNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 1).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delay 10 Min Action
        val delayIntent = Intent(context, ActionNotificationReceiver::class.java).apply {
            action = ActionNotificationReceiver.ACTION_DELAY_10
            putExtra(ActionNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val delayPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 2).toInt(),
            delayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Run Now Action
        val runNowIntent = Intent(context, ActionNotificationReceiver::class.java).apply {
            action = ActionNotificationReceiver.ACTION_RUN_NOW
            putExtra(ActionNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val runNowPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 3).toInt(),
            runNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_nightoff_fg)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_message, formattedTime))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${scheduleTitle.ifBlank { "Sleep schedule" }}: NightOff will run sleep actions in 10 minutes at $formattedTime."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.action_btn_cancel), cancelPendingIntent)
            .addAction(0, context.getString(R.string.action_btn_delay_10), delayPendingIntent)
            .addAction(0, context.getString(R.string.action_btn_run_now), runNowPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_REMINDER_BASE + scheduleId.toInt(),
                builder.build()
            )
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    fun dismissReminderNotification(context: Context, scheduleId: Long) {
        NotificationManagerCompat.from(context).cancel(
            NOTIFICATION_ID_REMINDER_BASE + scheduleId.toInt()
        )
    }

    fun showExecutionResultNotification(
        context: Context,
        scheduleTitle: String,
        successCount: Int,
        totalCount: Int,
        detailsText: String
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "history")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.execution_result_title)
        val summary = context.getString(R.string.execution_result_summary, successCount, totalCount)

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_nightoff_fg)
            .setContentTitle("$title ($scheduleTitle)")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n$detailsText"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_RESULT,
                builder.build()
            )
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    fun showSmartSleepConfirmationNotification(context: Context) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            8888,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val awakeIntent = Intent(context, ActionNotificationReceiver::class.java).apply {
            action = "ACTION_IM_AWAKE"
        }
        val awakePending = PendingIntent.getBroadcast(
            context,
            8889,
            awakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val delayIntent = Intent(context, ActionNotificationReceiver::class.java).apply {
            action = "ACTION_DELAY_30"
        }
        val delayPending = PendingIntent.getBroadcast(
            context,
            8890,
            delayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_nightoff_fg)
            .setContentTitle("Looks like you fell asleep")
            .setContentText("Shutting down in 5 minutes unless you respond.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Looks like you fell asleep. Shutting down in 5 minutes. Tap I'm awake or Delay 30 min."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "I'm Awake", awakePending)
            .addAction(0, "Delay 30 Min", delayPending)

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_REMINDER_BASE + 999,
                builder.build()
            )
        } catch (e: SecurityException) {}
    }
}
