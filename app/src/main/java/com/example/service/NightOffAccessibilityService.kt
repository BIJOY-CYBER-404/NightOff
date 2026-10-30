package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NightOffAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "NightOffAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing
    }

    override fun onInterrupt() {
        Log.d(TAG, "NightOffAccessibilityService interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun showPowerMenu(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    fun triggerPowerOffOrRestart(isRestart: Boolean): Boolean {
        val menuOpened = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        if (!menuOpened) return false

        // Wait for power menu dialog to appear, then search and click "Power off" or "Restart"
        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val rootNode = rootInActiveWindow ?: continue
                    val targetKeywords = if (isRestart) {
                        listOf("restart", "reboot", "পুনরারম্ভ", "পুনরায় চালু", "reiniciar", "redémarrer", "neu starten")
                    } else {
                        listOf("power off", "shut down", "shutdown", "poweroff", "পাওয়ার অফ", "বন্ধ করুন", "apagado", "éteindre", "ausschalten")
                    }

                    if (findAndClickNode(rootNode, targetKeywords)) {
                        Log.d(TAG, "Successfully clicked power menu item (restart=$isRestart)")
                        // Short pause, then confirm if a second confirmation dialog appears ("Tap again to restart/power off")
                        Thread.sleep(500)
                        val confirmRoot = rootInActiveWindow
                        if (confirmRoot != null) {
                            findAndClickNode(confirmRoot, targetKeywords)
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Power menu click attempt error: ${e.message}")
                }
            }
        }.start()

        return true
    }

    fun clearRecentsAndHome(): Boolean {
        // 1. Trigger Recents UI
        val recentsSuccess = performGlobalAction(GLOBAL_ACTION_RECENTS)
        
        // 2. Poll across multiple frames as Recents launcher animation runs
        Thread {
            var clicked = false
            for (attempt in 1..12) {
                try {
                    Thread.sleep(250)
                    val rootNode = rootInActiveWindow ?: continue
                    
                    // Search by known resource IDs first
                    if (clickClearAllById(rootNode)) {
                        clicked = true
                        Log.d(TAG, "Cleared recents via resource ID on attempt $attempt")
                        break
                    }
                    
                    // Search by text keywords
                    if (findAndClickClearAllByText(rootNode)) {
                        clicked = true
                        Log.d(TAG, "Cleared recents via text search on attempt $attempt")
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Clear recents poll error: ${e.message}")
                }
            }
            
            // Short delay, then return to home screen
            try { Thread.sleep(300) } catch (_: Exception) {}
            performGlobalAction(GLOBAL_ACTION_HOME)
        }.start()

        return recentsSuccess
    }

    private fun clickClearAllById(node: AccessibilityNodeInfo): Boolean {
        val knownIds = listOf(
            "com.android.systemui:id/clear_all_button",
            "com.android.systemui:id/clear_all",
            "com.sec.android.app.launcher:id/clear_all_button",
            "com.sec.android.app.launcher:id/clear_all",
            "com.google.android.apps.nexuslauncher:id/clear_all_button",
            "com.miui.home:id/clear_all",
            "com.miui.home:id/clear_all_button",
            "com.oppo.launcher:id/clear_all",
            "com.vivo.launcher:id/clear_all",
            "com.oneplus.launcher:id/clear_all",
            "com.huawei.android.launcher:id/clear_all"
        )

        for (id in knownIds) {
            val matchingNodes = node.findAccessibilityNodeInfosByViewId(id)
            if (!matchingNodes.isNullOrEmpty()) {
                for (matched in matchingNodes) {
                    if (performClickHierarchy(matched)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun findAndClickClearAllByText(node: AccessibilityNodeInfo): Boolean {
        val clearKeywords = listOf(
            "clear all", "close all", "dismiss all", "clean all", "remove all",
            "সব মুছুন", "সব বন্ধ করুন", "সব সাফ করুন", "সাফ করুন", "মুছে ফেলুন", "বন্ধ করুন",
            "limpiar todo", "cerrar todo", "tout effacer", "alles schließen", "alles löschen"
        )
        return findAndClickNode(node, clearKeywords)
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()

        for (kw in keywords) {
            if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
                if (performClickHierarchy(node)) {
                    return true
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNode(child, keywords)) {
                return true
            }
        }
        return false
    }

    private fun performClickHierarchy(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    fun toggleSwitchInSettingsPanel(turnOn: Boolean): Boolean {
        // Find and toggle switch widget in Settings panel or Quick Settings
        val rootNode = rootInActiveWindow ?: return false
        return findAndToggleSwitch(rootNode, turnOn)
    }

    private fun findAndToggleSwitch(node: AccessibilityNodeInfo, turnOn: Boolean): Boolean {
        if (node.className?.toString()?.contains("Switch", ignoreCase = true) == true ||
            node.className?.toString()?.contains("ToggleButton", ignoreCase = true) == true) {
            val isChecked = node.isChecked
            if (isChecked != turnOn) {
                return performClickHierarchy(node)
            }
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndToggleSwitch(child, turnOn)) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "AccessibilityService"
        var instance: NightOffAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }
}
