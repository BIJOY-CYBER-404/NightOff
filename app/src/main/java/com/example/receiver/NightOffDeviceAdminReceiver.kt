package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class NightOffDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("NightOffDeviceAdmin", "Device Admin successfully activated")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("NightOffDeviceAdmin", "Device Admin deactivated")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling NightOff Device Admin will prevent hardware screen lock at bedtime."
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, NightOffDeviceAdminReceiver::class.java)
        }
    }
}
