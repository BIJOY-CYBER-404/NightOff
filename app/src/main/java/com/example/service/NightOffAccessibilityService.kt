package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NightOffAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var pendingAction: PendingAction = PendingAction.NONE

    enum class PendingAction {
        NONE,
        CLEAR_RECENTS,
        TOGGLE_WIFI_OFF,
        TOGGLE_DATA_OFF,
        TOGGLE_BLUETOOTH_OFF,
        POWER_OFF,
        RESTART
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "NightOffAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || pendingAction == PendingAction.NONE) return

        val rootNode = rootInActiveWindow ?: event.source ?: return

        when (pendingAction) {
            PendingAction.CLEAR_RECENTS -> {
                if (attemptClearRecents(rootNode)) {
                    pendingAction = PendingAction.NONE
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 400)
                }
            }
            PendingAction.TOGGLE_WIFI_OFF -> {
                if (attemptToggleQuickSetting(rootNode, listOf("wi-fi", "wifi", "internet", "ওয়াই-ফাই", "ইন্টারনেট"))) {
                    pendingAction = PendingAction.NONE
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 500)
                }
            }
            PendingAction.TOGGLE_DATA_OFF -> {
                if (attemptToggleQuickSetting(rootNode, listOf("mobile data", "cellular data", "data", "মোবাইল ডাটা", "মোবাইল ডেটা", "ডেটা"))) {
                    pendingAction = PendingAction.NONE
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 500)
                }
            }
            PendingAction.TOGGLE_BLUETOOTH_OFF -> {
                if (attemptToggleQuickSetting(rootNode, listOf("bluetooth", "ব্লুটুথ", "bt"))) {
                    pendingAction = PendingAction.NONE
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 500)
                }
            }
            PendingAction.POWER_OFF -> {
                if (attemptClickPowerMenu(rootNode, isRestart = false)) {
                    pendingAction = PendingAction.NONE
                }
            }
            PendingAction.RESTART -> {
                if (attemptClickPowerMenu(rootNode, isRestart = true)) {
                    pendingAction = PendingAction.NONE
                }
            }
            PendingAction.NONE -> {}
        }
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
        pendingAction = PendingAction.CLEAR_RECENTS
        val ok = performGlobalAction(GLOBAL_ACTION_RECENTS)

        Thread {
            for (attempt in 1..15) {
                try {
                    Thread.sleep(200)
                    val root = rootInActiveWindow ?: continue
                    if (attemptClearRecents(root)) {
                        pendingAction = PendingAction.NONE
                        Thread.sleep(300)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        break
                    }

                    // On attempt 4 and 8, scroll recents horizontally to reveal 'Clear All' if off-screen (Pixel/Stock Launcher)
                    if (attempt == 4 || attempt == 8) {
                        scrollRecentsLeft(root)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Recents clear loop error: ${e.message}")
                }
            }

            // Fallback: If 'Clear All' button wasn't found, perform multiple swipe up gestures to dismiss recent cards
            if (pendingAction == PendingAction.CLEAR_RECENTS) {
                dismissRecentCardsBySwipe()
                pendingAction = PendingAction.NONE
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }.start()

        return ok
    }

    private fun attemptClearRecents(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Known resource IDs across launchers
        val knownIds = listOf(
            "com.android.systemui:id/clear_all_button",
            "com.android.systemui:id/clear_all",
            "com.sec.android.app.launcher:id/clear_all_button",
            "com.sec.android.app.launcher:id/clear_all",
            "com.google.android.apps.nexuslauncher:id/clear_all_button",
            "com.google.android.apps.nexuslauncher:id/clear_all",
            "com.miui.home:id/clear_all",
            "com.miui.home:id/clear_all_button",
            "com.oppo.launcher:id/clear_all",
            "com.vivo.launcher:id/clear_all",
            "com.oneplus.launcher:id/clear_all",
            "com.huawei.android.launcher:id/clear_all"
        )

        for (id in knownIds) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!matchingNodes.isNullOrEmpty()) {
                for (matched in matchingNodes) {
                    if (performClickHierarchy(matched)) return true
                }
            }
        }

        // 2. Multilingual keywords
        val keywords = listOf(
            "clear all", "close all", "dismiss all", "clean all", "remove all", "clear", "close",
            "সব মুছুন", "সব বন্ধ করুন", "সব সাফ করুন", "সাফ করুন", "মুছে ফেলুন", "বন্ধ করুন",
            "limpiar todo", "cerrar todo", "tout effacer", "alles schließen", "alles löschen"
        )
        return findAndClickNode(rootNode, keywords)
    }

    private fun scrollRecentsLeft(rootNode: AccessibilityNodeInfo) {
        // Try scrolling scrollable views or dispatch swipe right gesture to scroll recents left
        try {
            val scrollables = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.apps.nexuslauncher:id/overview_panel")
            if (!scrollables.isNullOrEmpty()) {
                scrollables[0].performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            } else {
                dispatchSwipeGesture(startX = 100f, startY = 800f, endX = 900f, endY = 800f)
            }
        } catch (_: Exception) {}
    }

    private fun dismissRecentCardsBySwipe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (i in 1..4) {
                dispatchSwipeGesture(startX = 500f, startY = 1200f, endX = 500f, endY = 200f)
                try { Thread.sleep(200) } catch (_: Exception) {}
            }
        }
    }

    fun turnOffWifiViaQuickSettings(): Boolean {
        pendingAction = PendingAction.TOGGLE_WIFI_OFF
        val ok = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val root = rootInActiveWindow ?: continue
                    if (attemptToggleQuickSetting(root, listOf("wi-fi", "wifi", "internet", "ওয়াই-ফাই", "ইন্টারনেট"))) {
                        pendingAction = PendingAction.NONE
                        Thread.sleep(400)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        break
                    }
                } catch (_: Exception) {}
            }
            if (pendingAction == PendingAction.TOGGLE_WIFI_OFF) {
                pendingAction = PendingAction.NONE
            }
        }.start()

        return ok
    }

    fun turnOffMobileDataViaQuickSettings(): Boolean {
        pendingAction = PendingAction.TOGGLE_DATA_OFF
        val ok = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val root = rootInActiveWindow ?: continue
                    if (attemptToggleQuickSetting(root, listOf("mobile data", "cellular data", "data", "মোবাইল ডাটা", "মোবাইল ডেটা", "ডেটা"))) {
                        pendingAction = PendingAction.NONE
                        Thread.sleep(400)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        break
                    }
                } catch (_: Exception) {}
            }
            if (pendingAction == PendingAction.TOGGLE_DATA_OFF) {
                pendingAction = PendingAction.NONE
            }
        }.start()

        return ok
    }

    fun turnOffBluetoothViaQuickSettings(): Boolean {
        pendingAction = PendingAction.TOGGLE_BLUETOOTH_OFF
        val ok = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val root = rootInActiveWindow ?: continue
                    if (attemptToggleQuickSetting(root, listOf("bluetooth", "ব্লুটুথ", "bt"))) {
                        pendingAction = PendingAction.NONE
                        Thread.sleep(400)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        break
                    }
                } catch (_: Exception) {}
            }
            if (pendingAction == PendingAction.TOGGLE_BLUETOOTH_OFF) {
                pendingAction = PendingAction.NONE
            }
        }.start()

        return ok
    }

    fun triggerPowerOff(): Boolean {
        pendingAction = PendingAction.POWER_OFF
        val ok = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val root = rootInActiveWindow ?: continue
                    if (attemptClickPowerMenu(root, isRestart = false)) {
                        pendingAction = PendingAction.NONE
                        break
                    }
                } catch (_: Exception) {}
            }
            if (pendingAction == PendingAction.POWER_OFF) {
                pendingAction = PendingAction.NONE
            }
        }.start()

        return ok
    }

    fun triggerRestart(): Boolean {
        pendingAction = PendingAction.RESTART
        val ok = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

        Thread {
            for (attempt in 1..10) {
                try {
                    Thread.sleep(300)
                    val root = rootInActiveWindow ?: continue
                    if (attemptClickPowerMenu(root, isRestart = true)) {
                        pendingAction = PendingAction.NONE
                        break
                    }
                } catch (_: Exception) {}
            }
            if (pendingAction == PendingAction.RESTART) {
                pendingAction = PendingAction.NONE
            }
        }.start()

        return ok
    }

    private fun attemptToggleQuickSetting(rootNode: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        return findAndToggleTile(rootNode, keywords)
    }

    private fun findAndToggleTile(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()

        for (kw in keywords) {
            if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
                if (performClickHierarchy(node)) return true
            }
        }

        // Check if there is a switch widget inside panel
        if (node.className?.toString()?.contains("Switch", ignoreCase = true) == true ||
            node.className?.toString()?.contains("ToggleButton", ignoreCase = true) == true) {
            if (node.isChecked) {
                return performClickHierarchy(node)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndToggleTile(child, keywords)) return true
        }
        return false
    }

    private fun attemptClickPowerMenu(rootNode: AccessibilityNodeInfo, isRestart: Boolean): Boolean {
        val keywords = if (isRestart) {
            listOf("restart", "reboot", "পুনরারম্ভ", "পুনরায় চালু", "রিবুট", "reiniciar", "redémarrer")
        } else {
            listOf("power off", "shut down", "shutdown", "poweroff", "পাওয়ার অফ", "বন্ধ করুন", "শাট ডাউন", "apagado", "éteindre")
        }

        val clicked = findAndClickNode(rootNode, keywords)
        if (clicked) {
            // Check for second confirmation tap after 500ms
            handler.postDelayed({
                val confirmRoot = rootInActiveWindow
                if (confirmRoot != null) {
                    findAndClickNode(confirmRoot, keywords)
                }
            }, 500)
            return true
        }
        return false
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()

        for (kw in keywords) {
            if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
                if (performClickHierarchy(node)) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNode(child, keywords)) return true
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

    private fun dispatchSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 200))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    companion object {
        private const val TAG = "AccessibilityService"
        var instance: NightOffAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }
}
