package com.example.engine

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.model.ActionResult
import com.example.model.ExecutionLog
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.receiver.NightOffDeviceAdminReceiver
import com.example.service.NightOffAccessibilityService
import com.example.util.NotificationHelper
import com.example.util.RootUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ActionExecutor {
    private const val TAG = "ActionExecutor"

    suspend fun execute(context: Context, scheduleId: Long) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val schedule = database.scheduleDao().getScheduleById(scheduleId)

        // Cancel rule verification (Requirement 4):
        // "if the user cancels or turns off the schedule before it runs, NO action is executed. Verify state again right before execution."
        if (schedule == null) {
            Log.d(TAG, "Schedule $scheduleId no longer exists, aborting execution.")
            NotificationHelper.dismissReminderNotification(context, scheduleId)
            return@withContext
        }

        if (!schedule.isEnabled) {
            Log.d(TAG, "Schedule $scheduleId is disabled, aborting execution.")
            NotificationHelper.dismissReminderNotification(context, scheduleId)
            return@withContext
        }

        NotificationHelper.dismissReminderNotification(context, scheduleId)

        val selectedActions = schedule.getActionList()
            .sortedBy { it.executionOrder }

        val results = mutableListOf<ActionResult>()

        for (action in selectedActions) {
            val result = executeSingleAction(context, action)
            results.add(result)
            Log.d(TAG, "Executed ${action.name}: ${result.isSuccess} (${result.message})")

            // Safe pause between actions to allow OS / audio focus transitions
            try {
                Thread.sleep(250)
            } catch (_: Exception) {}
        }

        // If it was a one-time schedule or quick timer, disable or remove it
        if (schedule.isQuickTimer) {
            database.scheduleDao().deleteSchedule(schedule)
        } else if (schedule.getDaysList().isEmpty()) {
            database.scheduleDao().updateSchedule(schedule.copy(isEnabled = false))
        }

        // Save execution log
        val successCount = results.count { it.isSuccess }
        val totalCount = results.size
        val status = when {
            successCount == totalCount -> "SUCCESS"
            successCount > 0 -> "PARTIAL"
            else -> "FAILED"
        }

        val formattedDetails = results.joinToString("\n") { res ->
            val icon = if (res.isSuccess) "✓" else "✕"
            "$icon ${res.action.name.replace("_", " ")}: ${res.message}"
        }

        val log = ExecutionLog(
            scheduleId = schedule.id,
            scheduleTitle = schedule.title.ifBlank { "Sleep Shutdown" },
            timestamp = System.currentTimeMillis(),
            totalActions = totalCount,
            successActions = successCount,
            status = status,
            actionResultsFormatted = formattedDetails
        )
        database.executionLogDao().insertLog(log)

        // Show execution result notification
        NotificationHelper.showExecutionResultNotification(
            context = context,
            scheduleTitle = schedule.title.ifBlank { "Sleep Shutdown" },
            successCount = successCount,
            totalCount = totalCount,
            detailsText = formattedDetails
        )
    }

    private fun executeSingleAction(context: Context, action: SleepAction): ActionResult {
        return when (action) {
            SleepAction.STOP_MEDIA -> stopMediaPlayback(context)
            SleepAction.CLOSE_APPS -> closeAppsAndRecents(context)
            SleepAction.TURN_OFF_WIFI -> turnOffWifi(context)
            SleepAction.TURN_OFF_MOBILE_DATA -> turnOffMobileData()
            SleepAction.TURN_OFF_BLUETOOTH -> turnOffBluetooth(context)
            SleepAction.MUTE_SOUND -> muteSoundAndDnd(context)
            SleepAction.LOCK_SCREEN -> lockScreen(context)
            SleepAction.POWER_OFF -> powerOffPhone()
            SleepAction.RESTART_PHONE -> restartPhone()
        }
    }

    private fun stopMediaPlayback(context: Context): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                    .build()
                audioManager.requestAudioFocus(focusRequest)
                // Abandon focus shortly after to pause players permanently
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            ActionResult(SleepAction.STOP_MEDIA, true, "Media focus acquired & paused")
        } catch (e: Exception) {
            ActionResult(SleepAction.STOP_MEDIA, false, "Failed to pause media: ${e.message}")
        }
    }

    private fun closeAppsAndRecents(context: Context): ActionResult {
        var accessibilityCleared = false
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            accessibilityCleared = accessibilityService.clearRecentsAndHome()
        }

        // Also call killBackgroundProcesses for eligible processes
        var killedCount = 0
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = am.runningAppProcesses ?: emptyList()
            for (proc in runningApps) {
                if (proc.pkgList != null) {
                    for (pkg in proc.pkgList) {
                        if (pkg != context.packageName) {
                            am.killBackgroundProcesses(pkg)
                            killedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "killBackgroundProcesses notice: ${e.message}")
        }

        return if (accessibilityCleared) {
            ActionResult(
                SleepAction.CLOSE_APPS,
                true,
                "Recents cleared & home navigated ($killedCount bg apps signalled)"
            )
        } else if (accessibilityService == null) {
            ActionResult(
                SleepAction.CLOSE_APPS,
                true,
                "Accessibility service not enabled; signalled $killedCount background processes"
            )
        } else {
            ActionResult(
                SleepAction.CLOSE_APPS,
                true,
                "Home navigated ($killedCount background processes signaled)"
            )
        }
    }

    private fun turnOffWifi(context: Context): ActionResult {
        // 1. Try root if available
        if (RootUtil.isRootAvailable()) {
            val rootOk = RootUtil.toggleWifi(false)
            if (rootOk) {
                return ActionResult(SleepAction.TURN_OFF_WIFI, true, "Wi-Fi disabled via Root command")
            }
        }

        // 2. On Android 9 and lower: WifiManager.setWifiEnabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return try {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val success = wifiManager?.setWifiEnabled(false) ?: false
                ActionResult(
                    SleepAction.TURN_OFF_WIFI,
                    success,
                    if (success) "Wi-Fi turned off" else "Could not disable Wi-Fi"
                )
            } catch (e: Exception) {
                ActionResult(SleepAction.TURN_OFF_WIFI, false, "Wifi error: ${e.message}")
            }
        }

        // 3. Android 10+ (Q+) restricts third-party apps from toggling Wi-Fi directly without root/system privileges
        return ActionResult(
            SleepAction.TURN_OFF_WIFI,
            false,
            "Android 10+ restricts direct Wi-Fi toggling without Root/Shizuku"
        )
    }

    private fun turnOffMobileData(): ActionResult {
        if (RootUtil.isRootAvailable()) {
            val rootOk = RootUtil.toggleMobileData(false)
            return if (rootOk) {
                ActionResult(SleepAction.TURN_OFF_MOBILE_DATA, true, "Mobile data disabled via Root")
            } else {
                ActionResult(SleepAction.TURN_OFF_MOBILE_DATA, false, "Root command failed for mobile data")
            }
        }
        return ActionResult(
            SleepAction.TURN_OFF_MOBILE_DATA,
            false,
            "Mobile data toggle requires Root or Shizuku on modern Android"
        )
    }

    private fun turnOffBluetooth(context: Context): ActionResult {
        if (RootUtil.isRootAvailable()) {
            val rootOk = RootUtil.toggleBluetooth(false)
            if (rootOk) {
                return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth disabled via Root")
            }
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, false, "No Bluetooth adapter found")
            }

            if (!adapter.isEnabled) {
                return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth already off")
            }

            @Suppress("DEPRECATION")
            val disabled = adapter.disable()
            if (disabled) {
                ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth radio disabled")
            } else {
                ActionResult(
                    SleepAction.TURN_OFF_BLUETOOTH,
                    false,
                    "Android restricted Bluetooth toggle (requires Root on newer builds)"
                )
            }
        } catch (e: SecurityException) {
            ActionResult(SleepAction.TURN_OFF_BLUETOOTH, false, "Missing BLUETOOTH_CONNECT permission")
        } catch (e: Exception) {
            ActionResult(SleepAction.TURN_OFF_BLUETOOTH, false, "Bluetooth error: ${e.message}")
        }
    }

    private fun muteSoundAndDnd(context: Context): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Mute standard audio streams
            val streams = intArrayOf(
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_SYSTEM
            )
            for (stream in streams) {
                try {
                    audioManager.setStreamVolume(stream, 0, 0)
                } catch (_: Exception) {}
            }

            // Set DND if permission granted
            var dndSet = false
            if (notifManager.isNotificationPolicyAccessGranted) {
                notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                dndSet = true
            }

            // Set ringer mode silent
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (_: Exception) {}

            ActionResult(
                SleepAction.MUTE_SOUND,
                true,
                if (dndSet) "All sounds muted & DND activated" else "Audio streams muted (grant DND for full silence)"
            )
        } catch (e: Exception) {
            ActionResult(SleepAction.MUTE_SOUND, false, "Failed to mute audio: ${e.message}")
        }
    }

    private fun lockScreen(context: Context): ActionResult {
        // 1. Accessibility Service GLOBAL_ACTION_LOCK_SCREEN (API 28+)
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null && accessibilityService.lockScreen()) {
            return ActionResult(SleepAction.LOCK_SCREEN, true, "Screen locked via Accessibility Service")
        }

        // 2. Device Admin lockNow()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = NightOffDeviceAdminReceiver.getComponentName(context)
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            return try {
                dpm.lockNow()
                ActionResult(SleepAction.LOCK_SCREEN, true, "Screen locked via Device Admin")
            } catch (e: Exception) {
                ActionResult(SleepAction.LOCK_SCREEN, false, "Device Admin lock failed: ${e.message}")
            }
        }

        return ActionResult(
            SleepAction.LOCK_SCREEN,
            false,
            "Enable Accessibility Service or Device Admin to lock screen"
        )
    }

    private fun powerOffPhone(): ActionResult {
        if (!RootUtil.isRootAvailable()) {
            return ActionResult(
                SleepAction.POWER_OFF,
                false,
                "Power Off requires Root or Shizuku privileges"
            )
        }
        val executed = RootUtil.powerOffPhone()
        return ActionResult(
            SleepAction.POWER_OFF,
            executed,
            if (executed) "Shutdown signal dispatched" else "Root shutdown command failed"
        )
    }

    private fun restartPhone(): ActionResult {
        if (!RootUtil.isRootAvailable()) {
            return ActionResult(
                SleepAction.RESTART_PHONE,
                false,
                "Restart requires Root or Shizuku privileges"
            )
        }
        val executed = RootUtil.restartPhone()
        return ActionResult(
            SleepAction.RESTART_PHONE,
            executed,
            if (executed) "Reboot signal dispatched" else "Root reboot command failed"
        )
    }
}
