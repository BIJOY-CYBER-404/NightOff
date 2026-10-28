package com.example.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import android.util.Log
import com.example.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * AppIntegrityGuard provides robust anti-tampering, anti-decompilation, and hook detection
 * to protect NightOff against repackaging, memory tampering, and unauthorized modifications.
 */
object AppIntegrityGuard {
    private const val TAG = "AppIntegrityGuard"

    data class SecurityStatus(
        val isAuthenticPackage: Boolean,
        val isDebuggerAbsent: Boolean,
        val isTracerAbsent: Boolean,
        val isHookFrameworkAbsent: Boolean
    ) {
        val isSecure: Boolean
            get() = isAuthenticPackage && isDebuggerAbsent && isTracerAbsent && isHookFrameworkAbsent
    }

    /**
     * Runs full anti-tamper security analysis.
     */
    fun verifyEnvironment(context: Context): SecurityStatus {
        val isAuthenticPkg = context.packageName == BuildConfig.APPLICATION_ID
        val isDebuggerAbsent = !Debug.isDebuggerConnected() && !Debug.waitingForDebugger()
        val isTracerAbsent = !detectTracerPid()
        val isHookFrameworkAbsent = !detectHookingFrameworks()

        if (!isAuthenticPkg) {
            Log.w(TAG, "Security alert: Package name mismatch detected.")
        }
        if (!isDebuggerAbsent || !isTracerAbsent) {
            Log.w(TAG, "Security alert: Debugging or tracing active.")
        }
        if (!isHookFrameworkAbsent) {
            Log.w(TAG, "Security alert: Hooking framework detected.")
        }

        return SecurityStatus(
            isAuthenticPackage = isAuthenticPkg,
            isDebuggerAbsent = isDebuggerAbsent,
            isTracerAbsent = isTracerAbsent,
            isHookFrameworkAbsent = isHookFrameworkAbsent
        )
    }

    /**
     * Checks if a ptracer or debugger is attached via Linux /proc/self/status.
     */
    private fun detectTracerPid(): Boolean {
        return try {
            val reader = BufferedReader(FileReader("/proc/self/status"))
            var line: String?
            var tracerPid = 0
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("TracerPid:")) {
                    val parts = line!!.split(":")
                    if (parts.size > 1) {
                        tracerPid = parts[1].trim().toIntOrNull() ?: 0
                    }
                    break
                }
            }
            reader.close()
            tracerPid > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Detects Frida, Xposed, substrate, and binary hook artifacts.
     */
    private fun detectHookingFrameworks(): Boolean {
        val suspiciousArtifacts = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/framework/XposedBridge.jar",
            "/system/lib/libxposed_art.so",
            "/system/lib64/libxposed_art.so",
            "/data/data/de.robv.android.xposed.installer",
            "/system/xbin/ku.sud",
            "/data/adb/modules"
        )

        for (path in suspiciousArtifacts) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {}
        }

        // Check loaded libraries in memory for Frida/Substrate
        return try {
            val mapsReader = BufferedReader(FileReader("/proc/self/maps"))
            var line: String?
            var hookFound = false
            while (mapsReader.readLine().also { line = it } != null) {
                val lower = line!!.lowercase()
                if (lower.contains("frida") || lower.contains("xposed") || lower.contains("substrate")) {
                    hookFound = true
                    break
                }
            }
            mapsReader.close()
            hookFound
        } catch (_: Exception) {
            false
        }
    }
}
