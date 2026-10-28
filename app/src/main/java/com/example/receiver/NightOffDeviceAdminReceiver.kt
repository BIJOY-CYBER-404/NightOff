package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class NightOffDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("DeviceAdmin", "NightOff Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("DeviceAdmin", "NightOff Device Admin disabled")
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, NightOffDeviceAdminReceiver::class.java)
        }
    }
}
