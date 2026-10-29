package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    // Primary: raw version.json file from repo main branch
    private const val RAW_VERSION_URL =
        "https://raw.githubusercontent.com/BIJOY-CYBER-404/NightOff/main/version.json"

    // Fallback: GitHub Releases API
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/BIJOY-CYBER-404/NightOff/releases/latest"

    // Direct Browser Fallback
    const val GITHUB_RELEASES_WEB =
        "https://github.com/BIJOY-CYBER-404/NightOff/releases"

    /**
     * Checks for updates by querying version.json on GitHub Pages and comparing versionCode.
     */
    suspend fun checkForUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        // 1. Primary: Use GitHubPagesUpdateChecker to query GitHub Pages version.json
        val pagesResult = GitHubPagesUpdateChecker.instance.checkForUpdate()
        if (pagesResult.hasUpdate) {
            return@withContext pagesResult
        }

        // 2. Try raw version.json if GitHub Pages was not updated
        val jsonResult = fetchFromVersionJson()
        if (jsonResult != null && jsonResult.hasUpdate) {
            return@withContext jsonResult
        }

        // 3. Fallback: current version is up to date
        return@withContext pagesResult
    }

    private fun fetchFromVersionJson(): AppUpdateInfo? {
        return try {
            val url = URL(RAW_VERSION_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                useCaches = false
            }

            if (connection.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val json = JSONObject(response)
                val latestName = json.optString("latestVersionName", "1.0").removePrefix("v")
                val latestCode = json.optInt("latestVersionCode", 1)
                val notes = json.optString("releaseNotes", "New bug fixes and performance improvements.")
                val downloadUrl = json.optString(
                    "downloadUrl",
                    "https://github.com/BIJOY-CYBER-404/NightOff/releases/latest/download/NightOff.apk"
                )
                val isForce = json.optBoolean("forceUpdate", false)

                val hasUpdate = latestCode > BuildConfig.VERSION_CODE || isVersionHigher(latestName, BuildConfig.VERSION_NAME)

                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = latestName,
                    latestVersionCode = latestCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = notes,
                    isForceUpdate = isForce
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "version.json fetch notice: ${e.message}")
            null
        }
    }

    private fun fetchFromGitHubReleasesApi(): AppUpdateInfo? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NightOff-Updater")
            }

            if (connection.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "v1.0").removePrefix("v").trim()
                val body = json.optString("body", "Performance improvements and bug fixes.")

                var apkDownloadUrl = GITHUB_RELEASES_WEB
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", GITHUB_RELEASES_WEB)
                            break
                        }
                    }
                }

                val hasUpdate = isVersionHigher(tagName, BuildConfig.VERSION_NAME)

                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName,
                    latestVersionCode = BuildConfig.VERSION_CODE + (if (hasUpdate) 1 else 0),
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = body
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub API fetch notice: ${e.message}")
            null
        }
    }

    private fun isVersionHigher(remote: String, local: String): Boolean {
        try {
            val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
            val count = maxOf(rParts.size, lParts.size)
            for (i in 0 until count) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Enqueues download of the new APK via DownloadManager.
     */
    fun startDownload(context: Context, downloadUrl: String, versionName: String): Long {
        val fileName = "NightOff-v$versionName.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("NightOff Update (v$versionName)")
            setDescription("Downloading updated APK...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setRequiresCharging(false)
            }
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        // Save download ID to preferences
        context.getSharedPreferences("nightoff_update_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("pending_download_id", downloadId)
            .putString("pending_apk_filename", fileName)
            .apply()

        return downloadId
    }

    /**
     * Installs APK using FileProvider and Android Package Installer.
     */
    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "Cannot install APK, file does not exist: ${file.absolutePath}")
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
