package com.example.update

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false,
    val fallbackDownloadUrl: String = "",
    val githubRepo: String = "https://github.com/BIJOY-CYBER-404/NightOff"
)

