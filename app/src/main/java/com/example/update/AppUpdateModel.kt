package com.example.update

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false
)
