package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    // Primary: raw version.json file from repo main branch
    private const val RAW_VERSION_URL =
        "https://raw.githubusercontent.com/BIJOY-CYBER-404/NightOff/main/version.json"

    // Primary direct APK mirror from repo main branch
    const val RAW_APK_URL =
        "https://raw.githubusercontent.com/BIJOY-CYBER-404/NightOff/main/NightOff.apk"

    // GitHub Pages APK mirror
    const val PAGES_APK_URL =
        "https://bijoy-cyber-404.github.io/NightOff/NightOff.apk"

    // Fallback: GitHub Releases direct download
    const val GITHUB_RELEASE_DOWNLOAD =
        "https://github.com/BIJOY-CYBER-404/NightOff/releases/latest/download/NightOff.apk"

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
                    RAW_APK_URL
                )
                val fallbackDownloadUrl = json.optString(
                    "fallbackDownloadUrl",
                    GITHUB_RELEASE_DOWNLOAD
                )
                val githubRepo = json.optString(
                    "githubRepo",
                    "https://github.com/BIJOY-CYBER-404/NightOff"
                )
                val isForce = json.optBoolean("forceUpdate", false)

                val hasUpdate = latestCode > BuildConfig.VERSION_CODE || isVersionHigher(latestName, BuildConfig.VERSION_NAME)

                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = latestName,
                    latestVersionCode = latestCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = notes,
                    isForceUpdate = isForce,
                    fallbackDownloadUrl = fallbackDownloadUrl,
                    githubRepo = githubRepo
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "version.json fetch notice: ${e.message}")
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
     * Downloads APK directly via HTTP streaming with automatic redirect handling (301/302/307),
     * real-time progress callbacks, and multi-mirror fallback (raw GitHub, GitHub Pages, releases).
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidates = linkedSetOf<String>()
        if (updateInfo.downloadUrl.isNotBlank()) candidates.add(updateInfo.downloadUrl)
        candidates.add(RAW_APK_URL)
        candidates.add(PAGES_APK_URL)
        if (updateInfo.fallbackDownloadUrl.isNotBlank()) candidates.add(updateInfo.fallbackDownloadUrl)
        candidates.add(GITHUB_RELEASE_DOWNLOAD)

        var lastError: Exception? = null

        for (targetUrl in candidates) {
            try {
                Log.d(TAG, "Starting streaming APK download from: $targetUrl")
                val downloadedFile = performStreamingDownload(
                    context = context,
                    initialUrl = targetUrl,
                    versionName = updateInfo.latestVersionName,
                    onProgress = onProgress
                )
                if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 50000) {
                    Log.i(TAG, "Download succeeded: ${downloadedFile.absolutePath} (${downloadedFile.length()} bytes)")
                    return@withContext Result.success(downloadedFile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mirror download attempt failed for $targetUrl: ${e.message}")
                lastError = e
            }
        }

        val errorMsg = lastError?.message ?: "Failed to download update APK from all available mirrors."
        Result.failure(Exception(errorMsg, lastError))
    }

    private fun performStreamingDownload(
        context: Context,
        initialUrl: String,
        versionName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit
    ): File? {
        var currentUrl = initialUrl
        var connection: HttpURLConnection? = null
        var redirects = 0
        val maxRedirects = 6

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) NightOff-Updater/$versionName")
                setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            }

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308) {
                val location = connection.getHeaderField("Location")
                if (location != null) {
                    currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                    redirects++
                    connection.disconnect()
                    continue
                }
            }

            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("Server returned HTTP $code (${connection.responseMessage}) for $currentUrl")
            }
            break
        }

        val activeConn = connection ?: throw IOException("Could not connect to $initialUrl")
        val totalBytes = activeConn.contentLengthLong

        val updateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "downloads")
        if (!updateDir.exists()) updateDir.mkdirs()
        val apkFile = File(updateDir, "NightOff-v$versionName.apk")
        if (apkFile.exists()) apkFile.delete()

        activeConn.inputStream.use { input ->
            FileOutputStream(apkFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (totalBytes > 0) {
                        (totalBytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    onProgress(totalBytesRead, totalBytes, progress)
                }
                output.flush()
            }
        }
        activeConn.disconnect()
        return apkFile
    }

    /**
     * Enqueues download of the new APK via system DownloadManager into app external files directory.
     */
    fun startDownload(context: Context, downloadUrl: String, versionName: String): Long {
        val fileName = "NightOff-v$versionName.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("NightOff Update (v$versionName)")
            setDescription("Downloading updated APK to app directory...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setRequiresCharging(false)
            }
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        context.getSharedPreferences("nightoff_update_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("pending_download_id", downloadId)
            .putString("pending_apk_filename", fileName)
            .apply()

        return downloadId
    }

    /**
     * Installs APK using FileProvider and Android Package Installer.
     * Smoothly checks Unknown Sources permission on Android 8.0+.
     */
    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "Cannot install APK, file does not exist: ${file.absolutePath}")
            Toast.makeText(context, "APK file not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 8.0+ (API 26), verify if app can request package installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Please allow NightOff to install unknown apps to proceed with update",
                    Toast.LENGTH_LONG
                ).show()
                val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(manageIntent)
            }
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

    /**
     * Safely opens URL in the default browser.
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
