package com.example.update

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the version code from a remote JSON file hosted on GitHub Pages
 * (https://bijoy-cyber-404.github.io/NightOff/version.json) and compares it
 * with the current BuildConfig.VERSION_CODE to determine if an update dialog
 * should be triggered.
 */
class GitHubPagesUpdateChecker(
    private val githubPagesUrl: String = DEFAULT_GITHUB_PAGES_URL,
    private val fallbackRawUrl: String = DEFAULT_FALLBACK_RAW_URL
) {
    companion object {
        private const val TAG = "GitHubPagesUpdater"

        // Primary GitHub Pages endpoint
        const val DEFAULT_GITHUB_PAGES_URL =
            "https://bijoy-cyber-404.github.io/NightOff/version.json"

        // Fallback endpoint in case GitHub Pages is building or offline
        const val DEFAULT_FALLBACK_RAW_URL =
            "https://raw.githubusercontent.com/BIJOY-CYBER-404/NightOff/main/version.json"

        // Singleton instance for app-wide usage
        val instance = GitHubPagesUpdateChecker()
    }

    /**
     * Fetches version info from GitHub Pages and compares the remote version code
     * with local BuildConfig.VERSION_CODE.
     *
     * @return [AppUpdateInfo] with hasUpdate set to true if remote versionCode > local versionCode.
     */
    suspend fun checkForUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        // Step 1: Try GitHub Pages URL
        val pagesResult = fetchFromRemoteUrl(githubPagesUrl)
        if (pagesResult != null) {
            return@withContext pagesResult
        }

        // Step 2: Fallback to GitHub raw repo URL if GitHub Pages returned non-200 or failed
        val rawResult = fetchFromRemoteUrl(fallbackRawUrl)
        if (rawResult != null) {
            return@withContext rawResult
        }

        // Step 3: Default to no update when remote cannot be reached
        Log.i(TAG, "Could not reach remote version endpoint; assuming current version is up to date.")
        return@withContext AppUpdateInfo(
            hasUpdate = false,
            latestVersionName = BuildConfig.VERSION_NAME,
            latestVersionCode = BuildConfig.VERSION_CODE,
            downloadUrl = "",
            releaseNotes = "You are running the latest version of NightOff."
        )
    }

    /**
     * Checks remote version code asynchronously and triggers the provided callback on the Main thread
     * if an update is required.
     */
    fun checkAndTriggerDialog(
        scope: CoroutineScope,
        onUpdateAvailable: (AppUpdateInfo) -> Unit,
        onAlreadyLatest: (() -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            val updateInfo = checkForUpdate()
            withContext(Dispatchers.Main) {
                if (updateInfo.hasUpdate) {
                    Log.i(TAG, "Update detected (remote code: ${updateInfo.latestVersionCode} > local: ${BuildConfig.VERSION_CODE}). Triggering update dialog.")
                    onUpdateAvailable(updateInfo)
                } else {
                    onAlreadyLatest?.invoke()
                }
            }
        }
    }

    /**
     * Performs HTTP GET request, parses the version JSON, and compares version codes.
     */
    private fun fetchFromRemoteUrl(urlString: String): AppUpdateInfo? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NightOff-Updater/${BuildConfig.VERSION_NAME}")
            }

            val statusCode = connection.responseCode
            if (statusCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val json = JSONObject(response)

                // Read version code and version name
                val remoteVersionCode = json.optInt("latestVersionCode", json.optInt("versionCode", 0))
                val remoteVersionName = json.optString("latestVersionName", json.optString("versionName", "1.0")).removePrefix("v").trim()
                val releaseNotes = json.optString("releaseNotes", "New stability improvements and bug fixes.")
                val downloadUrl = json.optString(
                    "downloadUrl",
                    "https://github.com/BIJOY-CYBER-404/NightOff/releases/latest/download/NightOff.apk"
                )
                val isForceUpdate = json.optBoolean("forceUpdate", false)

                // Comparison with current BuildConfig.VERSION_CODE
                val currentVersionCode = BuildConfig.VERSION_CODE
                val isCodeHigher = remoteVersionCode > currentVersionCode
                val isNameHigher = isVersionNameHigher(remoteVersionName, BuildConfig.VERSION_NAME)
                val hasUpdate = isCodeHigher || isNameHigher

                Log.d(TAG, "Fetched version from $urlString -> remoteCode: $remoteVersionCode, currentCode: $currentVersionCode, hasUpdate: $hasUpdate")

                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = remoteVersionName,
                    latestVersionCode = remoteVersionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isForceUpdate = isForceUpdate
                )
            } else {
                Log.w(TAG, "Server responded with HTTP $statusCode for URL: $urlString")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching remote version JSON from $urlString: ${e.message}")
            null
        }
    }

    /**
     * Semantic version comparison helper (e.g., 1.13 > 1.12).
     */
    private fun isVersionNameHigher(remote: String, local: String): Boolean {
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
}
