package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.receiver.NightOffDeviceAdminReceiver
import com.example.service.NightOffAccessibilityService

data class PermissionState(
    val hasExactAlarm: Boolean,
    val hasNotification: Boolean,
    val hasAccessibility: Boolean,
    val hasDeviceAdmin: Boolean,
    val hasDnd: Boolean,
    val hasIgnoreBatteryOpt: Boolean,
    val hasBluetooth: Boolean,
    val isRealRooted: Boolean,
    val isVirtualRootEnabled: Boolean
) {
    val isReadyForBasicSleep: Boolean
        get() = hasExactAlarm && (hasAccessibility || hasDeviceAdmin || isVirtualRootEnabled)

    val isElevatedAvailable: Boolean
        get() = isRealRooted || isVirtualRootEnabled

    val areAllRequiredGranted: Boolean
        get() = hasExactAlarm && hasNotification && hasIgnoreBatteryOpt && hasDnd && (hasAccessibility || hasDeviceAdmin)

    val requiredGrantedCount: Int
        get() = (if (hasExactAlarm) 1 else 0) +
                (if (hasNotification) 1 else 0) +
                (if (hasIgnoreBatteryOpt) 1 else 0) +
                (if (hasDnd) 1 else 0) +
                (if (hasAccessibility) 1 else 0) +
                (if (hasDeviceAdmin) 1 else 0)

    val totalRequiredCount: Int = 6
}

object PermissionHelper {

    fun checkAll(context: Context): PermissionState {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

        val hasNotification = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val hasAccessibility = NightOffAccessibilityService.isRunning

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = NightOffDeviceAdminReceiver.getComponentName(context)
        val hasDeviceAdmin = dpm?.isAdminActive(adminComponent) ?: false

        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val hasDnd = notifManager?.isNotificationPolicyAccessGranted ?: false

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val hasIgnoreBatteryOpt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        val hasBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val isRealRooted = RootUtil.isRealRootAvailable()
        val isVirtualRoot = RootUtil.isVirtualRootEnabled(context)

        return PermissionState(
            hasExactAlarm = hasExactAlarm,
            hasNotification = hasNotification,
            hasAccessibility = hasAccessibility,
            hasDeviceAdmin = hasDeviceAdmin,
            hasDnd = hasDnd,
            hasIgnoreBatteryOpt = hasIgnoreBatteryOpt,
            hasBluetooth = hasBluetooth,
            isRealRooted = isRealRooted,
            isVirtualRootEnabled = isVirtualRoot
        )
    }

    fun getExactAlarmIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getDeviceAdminIntent(context: Context): Intent {
        val adminComponent = NightOffDeviceAdminReceiver.getComponentName(context)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "NightOff requires Device Admin permission to turn off and lock your screen when you fall asleep."
            )
            // Note: DO NOT add FLAG_ACTIVITY_NEW_TASK here.
            // Android's DeviceAdminAdd activity explicitly checks for FLAG_ACTIVITY_NEW_TASK
            // and auto-finishes immediately if present.
        }
    }

    fun getDeviceAdminSettingsIntent(): Intent {
        return try {
            Intent("android.app.action.DEVICE_ADMIN_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun getDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
