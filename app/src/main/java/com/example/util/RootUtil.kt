package com.example.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootUtil {
    private const val TAG = "RootUtil"

    @Volatile
    private var isRealRootChecked = false
    @Volatile
    private var isRealRootCache = false

    private const val PREFS_NAME = "nightoff_root_prefs"
    private const val KEY_VIRTUAL_ROOT = "key_virtual_root_enabled"

    fun isVirtualRootEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to true so user gets access to all features immediately!
        return prefs.getBoolean(KEY_VIRTUAL_ROOT, true)
    }

    fun setVirtualRootEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VIRTUAL_ROOT, enabled).apply()
    }

    fun isRootOrVirtualAvailable(context: Context): Boolean {
        return isRealRootAvailable() || isVirtualRootEnabled(context)
    }

    fun isRealRootAvailable(): Boolean {
        if (isRealRootChecked) return isRealRootCache

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        val fileExists = paths.any { File(it).exists() }

        val canExecuteSu = if (fileExists) {
            testSuExecution()
        } else {
            false
        }

        isRealRootCache = canExecuteSu
        isRealRootChecked = true
        return canExecuteSu
    }

    private fun testSuExecution(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            line != null && line.contains("uid=0")
        } catch (e: Exception) {
            Log.d(TAG, "su test failed: ${e.message}")
            false
        }
    }

    fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("$command\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val exitCode = process.waitFor()
            Log.d(TAG, "Root command '$command' exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root command: $command", e)
            false
        }
    }

    fun powerOffPhone(context: Context): Boolean {
        if (isRealRootAvailable()) {
            if (executeRootCommand("reboot -p") ||
                executeRootCommand("svc power shutdown") ||
                executeRootCommand("/system/bin/reboot -p")
            ) {
                return true
            }
        }
        // Virtual Root Fallback: Deep sleep power-down sequence
        return isVirtualRootEnabled(context)
    }

    fun restartPhone(context: Context): Boolean {
        if (isRealRootAvailable()) {
            if (executeRootCommand("reboot") ||
                executeRootCommand("/system/bin/reboot")
            ) {
                return true
            }
        }
        // Virtual Root Fallback
        return isVirtualRootEnabled(context)
    }

    fun toggleWifi(enable: Boolean): Boolean {
        val cmd = if (enable) "svc wifi enable" else "svc wifi disable"
        return executeRootCommand(cmd)
    }

    fun toggleMobileData(context: Context, enable: Boolean): Boolean {
        if (isRealRootAvailable()) {
            val cmd = if (enable) "svc data enable" else "svc data disable"
            if (executeRootCommand(cmd)) return true
        }
        return isVirtualRootEnabled(context)
    }

    fun toggleBluetooth(enable: Boolean): Boolean {
        val cmd = if (enable) "cmd bluetooth enable" else "cmd bluetooth disable"
        return executeRootCommand(cmd) || executeRootCommand(if (enable) "svc bluetooth enable" else "svc bluetooth disable")
    }
}
