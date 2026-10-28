package com.example.model

import com.example.R

enum class SleepAction(
    val titleRes: Int,
    val descRes: Int,
    val executionOrder: Int,
    val requiresRoot: Boolean = false,
    val requiresAccessibility: Boolean = false,
    val requiresDeviceAdmin: Boolean = false,
    val requiresDnd: Boolean = false
) {
    STOP_MEDIA(
        titleRes = R.string.action_stop_media,
        descRes = R.string.action_stop_media_desc,
        executionOrder = 1
    ),
    CLOSE_APPS(
        titleRes = R.string.action_close_apps,
        descRes = R.string.action_close_apps_desc,
        executionOrder = 2,
        requiresAccessibility = true
    ),
    TURN_OFF_WIFI(
        titleRes = R.string.action_turn_off_wifi,
        descRes = R.string.action_turn_off_wifi_desc,
        executionOrder = 3
    ),
    TURN_OFF_MOBILE_DATA(
        titleRes = R.string.action_turn_off_mobile_data,
        descRes = R.string.action_turn_off_mobile_data_desc,
        executionOrder = 4,
        requiresRoot = true
    ),
    TURN_OFF_BLUETOOTH(
        titleRes = R.string.action_turn_off_bluetooth,
        descRes = R.string.action_turn_off_bluetooth_desc,
        executionOrder = 5
    ),
    MUTE_SOUND(
        titleRes = R.string.action_mute_sound,
        descRes = R.string.action_mute_sound_desc,
        executionOrder = 6,
        requiresDnd = true
    ),
    LOCK_SCREEN(
        titleRes = R.string.action_lock_screen,
        descRes = R.string.action_lock_screen_desc,
        executionOrder = 7,
        requiresAccessibility = true,
        requiresDeviceAdmin = true
    ),
    POWER_OFF(
        titleRes = R.string.action_power_off,
        descRes = R.string.action_power_off_desc,
        executionOrder = 8,
        requiresRoot = true
    ),
    RESTART_PHONE(
        titleRes = R.string.action_restart,
        descRes = R.string.action_restart_desc,
        executionOrder = 9,
        requiresRoot = true
    );

    companion object {
        fun defaultActions(): Set<SleepAction> = setOf(
            STOP_MEDIA,
            CLOSE_APPS,
            TURN_OFF_WIFI,
            TURN_OFF_BLUETOOTH,
            MUTE_SOUND,
            LOCK_SCREEN
        )
    }
}

data class ActionResult(
    val action: SleepAction,
    val isSuccess: Boolean,
    val message: String
)
