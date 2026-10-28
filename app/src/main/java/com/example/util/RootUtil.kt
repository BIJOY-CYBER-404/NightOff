package com.example.util

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootUtil {
    private const val TAG = "RootUtil"

    @Volatile
    private var isRootChecked = false
    @Volatile
    private var isRootedCache = false

    fun isRootAvailable(): Boolean {
        if (isRootChecked) return isRootedCache

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

        isRootedCache = canExecuteSu
        isRootChecked = true
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

    fun powerOffPhone(): Boolean {
        // Try various standard shutdown commands for Android
        return executeRootCommand("reboot -p") ||
                executeRootCommand("svc power shutdown") ||
                executeRootCommand("/system/bin/reboot -p")
    }

    fun restartPhone(): Boolean {
        return executeRootCommand("reboot") ||
                executeRootCommand("/system/bin/reboot")
    }

    fun toggleWifi(enable: Boolean): Boolean {
        val cmd = if (enable) "svc wifi enable" else "svc wifi disable"
        return executeRootCommand(cmd)
    }

    fun toggleMobileData(enable: Boolean): Boolean {
        val cmd = if (enable) "svc data enable" else "svc data disable"
        return executeRootCommand(cmd)
    }

    fun toggleBluetooth(enable: Boolean): Boolean {
        val cmd = if (enable) "cmd bluetooth enable" else "cmd bluetooth disable"
        return executeRootCommand(cmd) || executeRootCommand(if (enable) "svc bluetooth enable" else "svc bluetooth disable")
    }
}
