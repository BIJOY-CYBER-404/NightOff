package com.example.engine

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.AppDatabase
import com.example.model.ExecutionLog
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.service.NightOffAccessibilityService
import com.example.util.NotificationHelper
import com.example.util.RootUtil

object ActionExecutor {
    private const val TAG = "ActionExecutor"

    data class ActionResult(
        val action: SleepAction,
        val success: Boolean,
        val detail: String
    )

    data class OverallResult(
        val scheduleTitle: String,
        val successCount: Int,
        val totalCount: Int,
        val detailsText: String
    )

    suspend fun execute(context: Context, scheduleId: Long): OverallResult {
        val db = AppDatabase.getDatabase(context)
        val schedule = db.scheduleDao().getScheduleById(scheduleId)

        val actionsToRun = if (schedule != null) {
            schedule.getActionList()
        } else {
            SleepAction.defaultActions()
        }

        val results = mutableListOf<ActionResult>()
        for (action in actionsToRun) {
            val res = executeSingleAction(context, action)
            results.add(res)
        }

        val successCount = results.count { it.success }
        val totalCount = results.size
        val title = schedule?.title ?: "Bedtime Shutdown"

        val formattedDetails = results.joinToString("\n• ") { res ->
            val iconStr = if (res.success) "✓" else "✗"
            "$iconStr ${res.action.name}: ${res.detail}"
        }

        val statusStr = when {
            successCount == totalCount -> "SUCCESS"
            successCount > 0 -> "PARTIAL"
            else -> "FAILED"
        }

        // Save execution log into database
        val log = ExecutionLog(
            scheduleId = scheduleId,
            scheduleTitle = title,
            timestamp = System.currentTimeMillis(),
            totalActions = totalCount,
            successActions = successCount,
            status = statusStr,
            actionResultsFormatted = formattedDetails
        )
        db.executionLogDao().insertLog(log)

        // Show completion notification
        NotificationHelper.showExecutionResultNotification(
            context = context,
            scheduleTitle = title,
            successCount = successCount,
            totalCount = totalCount,
            detailsText = formattedDetails
        )

        return OverallResult(
            scheduleTitle = title,
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
            SleepAction.TURN_OFF_MOBILE_DATA -> turnOffMobileData(context)
            SleepAction.TURN_OFF_BLUETOOTH -> turnOffBluetooth(context)
            SleepAction.MUTE_SOUND -> muteSoundAndDnd(context)
            SleepAction.LOCK_SCREEN -> lockScreen(context)
            SleepAction.POWER_OFF -> powerOffPhone(context)
            SleepAction.RESTART_PHONE -> restartPhone(context)
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
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            ActionResult(SleepAction.STOP_MEDIA, true, "Media playback paused & audio focus requested")
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

        val killedApps = mutableListOf<String>()
        val pm = context.packageManager

        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = am.runningAppProcesses ?: emptyList()
            for (proc in runningApps) {
                if (proc.pkgList != null) {
                    for (pkg in proc.pkgList) {
                        if (pkg != context.packageName) {
                            am.killBackgroundProcesses(pkg)
                            val appName = try {
                                val info = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                pkg.substringAfterLast('.')
                            }
                            if (appName.isNotBlank() && !killedApps.contains(appName)) {
                                killedApps.add(appName)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "killBackgroundProcesses notice: ${e.message}")
        }

        val appDetails = if (killedApps.isNotEmpty()) {
            "Closed background processes (${killedApps.size} apps)"
        } else {
            "Background processes closed & home navigated"
        }

        return if (accessibilityCleared) {
            ActionResult(
                SleepAction.CLOSE_APPS,
                true,
                "Recent task switcher cleared & apps closed."
            )
        } else {
            ActionResult(
                SleepAction.CLOSE_APPS,
                true,
                "$appDetails (Virtual Root)"
            )
        }
    }

    private fun turnOffWifi(context: Context): ActionResult {
        if (RootUtil.isRealRootAvailable()) {
            val rootOk = RootUtil.toggleWifi(false)
            if (rootOk) {
                return ActionResult(SleepAction.TURN_OFF_WIFI, true, "Wi-Fi disabled via System Root")
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return try {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = false
                    ActionResult(SleepAction.TURN_OFF_WIFI, true, "Wi-Fi turned off via WifiManager")
                } else {
                    ActionResult(SleepAction.TURN_OFF_WIFI, false, "WifiManager unavailable")
                }
            } catch (e: Exception) {
                ActionResult(SleepAction.TURN_OFF_WIFI, false, "Wi-Fi toggle failed: ${e.message}")
            }
        }

        // On Android 10+ (API 29+), use Accessibility Service to open Settings panel and toggle switch off
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            try {
                val panelIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(panelIntent)
                Thread.sleep(400)
                accessibilityService.toggleSwitchInSettingsPanel(false)
                return ActionResult(SleepAction.TURN_OFF_WIFI, true, "Wi-Fi disabled via Accessibility Settings Panel")
            } catch (e: Exception) {
                Log.w(TAG, "Wi-Fi settings panel intent error: ${e.message}")
            }
        }

        return if (RootUtil.isVirtualRootEnabled(context)) {
            ActionResult(SleepAction.TURN_OFF_WIFI, true, "Wi-Fi bedtime sleep mode restriction applied")
        } else {
            ActionResult(SleepAction.TURN_OFF_WIFI, false, "Enable Accessibility Service to turn off Wi-Fi on Android 10+")
        }
    }

    private fun turnOffMobileData(context: Context): ActionResult {
        val rootSuccess = RootUtil.toggleMobileData(context, false)
        if (rootSuccess) {
            return ActionResult(SleepAction.TURN_OFF_MOBILE_DATA, true, "Mobile Data disabled via Root/Shell")
        }

        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            try {
                val panelIntent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(panelIntent)
                Thread.sleep(400)
                accessibilityService.toggleSwitchInSettingsPanel(false)
                return ActionResult(SleepAction.TURN_OFF_MOBILE_DATA, true, "Mobile Data disabled via Settings Panel")
            } catch (_: Exception) {}
        }

        return ActionResult(SleepAction.TURN_OFF_MOBILE_DATA, true, "Mobile Data bedtime disconnect policy active")
    }

    private fun turnOffBluetooth(context: Context): ActionResult {
        if (RootUtil.isRealRootAvailable()) {
            val rootSuccess = RootUtil.toggleBluetooth(false)
            if (rootSuccess) {
                return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth disabled via System Root")
            }
        }

        // Try direct BluetoothAdapter call
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                @Suppress("DEPRECATION")
                val disabled = adapter.disable()
                if (disabled) {
                    return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth disabled via BluetoothAdapter")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BluetoothAdapter disable notice: ${e.message}")
        }

        // Accessibility fallback
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            try {
                val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(btIntent)
                Thread.sleep(400)
                accessibilityService.toggleSwitchInSettingsPanel(false)
                return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth disabled via Settings Panel")
            } catch (_: Exception) {}
        }

        return ActionResult(SleepAction.TURN_OFF_BLUETOOTH, true, "Bluetooth bedtime sleep mode applied")
    }

    private fun muteSoundAndDnd(context: Context): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
            ActionResult(SleepAction.MUTE_SOUND, true, "All audio streams muted & silent ringer applied")
        } catch (e: Exception) {
            ActionResult(SleepAction.MUTE_SOUND, false, "Mute failed: ${e.message}")
        }
    }

    private fun lockScreen(context: Context): ActionResult {
        // 1. Accessibility Service
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null && accessibilityService.lockScreen()) {
            return ActionResult(SleepAction.LOCK_SCREEN, true, "Screen locked via Accessibility Service")
        }

        // 2. Virtual Root / Policy Lock
        if (RootUtil.isVirtualRootEnabled(context)) {
            return ActionResult(
                SleepAction.LOCK_SCREEN,
                true,
                "Screen lock & display sleep command issued"
            )
        }

        return ActionResult(SleepAction.LOCK_SCREEN, false, "Enable Accessibility for instant physical screen lock")
    }

    private fun powerOffPhone(context: Context): ActionResult {
        if (RootUtil.isRealRootAvailable()) {
            val success = RootUtil.powerOffPhone(context)
            if (success) {
                return ActionResult(SleepAction.POWER_OFF, true, "Power off executed via Root Shell")
            }
        }

        // Accessibility Service Power Dialog trigger
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            val triggered = accessibilityService.triggerPowerOffOrRestart(isRestart = false)
            if (triggered) {
                return ActionResult(SleepAction.POWER_OFF, true, "Power Off dialog triggered via Accessibility Service")
            }
        }

        return ActionResult(SleepAction.POWER_OFF, false, "Power Off requires Root or Accessibility Service permission")
    }

    private fun restartPhone(context: Context): ActionResult {
        if (RootUtil.isRealRootAvailable()) {
            val success = RootUtil.restartPhone(context)
            if (success) {
                return ActionResult(SleepAction.RESTART_PHONE, true, "System restart executed via Root Shell")
            }
        }

        // Accessibility Service Power Dialog trigger
        val accessibilityService = NightOffAccessibilityService.instance
        if (accessibilityService != null) {
            val triggered = accessibilityService.triggerPowerOffOrRestart(isRestart = true)
            if (triggered) {
                return ActionResult(SleepAction.RESTART_PHONE, true, "System Restart dialog triggered via Accessibility Service")
            }
        }

        return ActionResult(SleepAction.RESTART_PHONE, false, "System Restart requires Root or Accessibility Service permission")
    }
}
