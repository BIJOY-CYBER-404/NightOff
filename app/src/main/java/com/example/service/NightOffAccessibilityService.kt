package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NightOffAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "NightOffAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if needed
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

    fun clearRecentsAndHome(): Boolean {
        // First invoke recents
        val recentsSuccess = performGlobalAction(GLOBAL_ACTION_RECENTS)
        // Give short delay to let recents UI draw, then try to find "Clear all" button
        try {
            Thread.sleep(400)
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val found = findAndClickClearAll(rootNode)
                if (found) {
                    Thread.sleep(300)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during recents clearance: ${e.message}")
        }
        // Fallback to going home to minimize all active apps
        performGlobalAction(GLOBAL_ACTION_HOME)
        return recentsSuccess
    }

    private fun findAndClickClearAll(node: AccessibilityNodeInfo): Boolean {
        val clearKeywords = listOf(
            "clear all", "close all", "dismiss all",
            "সব মুছুন", "সব বন্ধ করুন", "সব সাফ করুন",
            "limpiar todo", "tout effacer", "alles schließen"
        )

        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()

        for (kw in clearKeywords) {
            if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
                if (node.isClickable) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    parent = parent.parent
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickClearAll(child)) {
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
