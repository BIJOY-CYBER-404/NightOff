package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/BIJOY-CYBER-404/NightOff/releases/latest"
    private const val FALLBACK_GITHUB_URL = "https://github.com/BIJOY-CYBER-404/NightOff/releases"

    /**
     * Checks GitHub API for the latest release.
     */
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NightOff-AndroidApp")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "v1.0").removePrefix("v").trim()
                val body = json.optString("body", "Performance improvements and bug fixes.")
                
                var apkUrl = FALLBACK_GITHUB_URL
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", FALLBACK_GITHUB_URL)
                            break
                        }
                    }
                }

                val isNewer = isVersionNewer(tagName, BuildConfig.VERSION_NAME)
                return@withContext UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersionName = tagName,
                    downloadUrl = apkUrl,
                    releaseNotes = body
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check notice: ${e.message}")
        }

        return@withContext UpdateInfo(
            hasUpdate = false,
            latestVersionName = BuildConfig.VERSION_NAME,
            downloadUrl = FALLBACK_GITHUB_URL,
            releaseNotes = "You are using the latest version of NightOff."
        )
    }

    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

            val maxParts = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxParts) {
                val remote = remoteParts.getOrElse(i) { 0 }
                val current = currentParts.getOrElse(i) { 0 }
                if (remote > current) return true
                if (remote < current) return false
            }
        } catch (_: Exception) {}
        return false
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading NightOff Update...")
                setDescription("Fetching the latest version from GitHub")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NightOff-Update.apk")
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            manager?.enqueue(request)
        } catch (e: Exception) {
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
