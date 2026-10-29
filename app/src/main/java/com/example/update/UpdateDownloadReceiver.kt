package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import java.io.File

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val prefs = context.getSharedPreferences("nightoff_update_prefs", Context.MODE_PRIVATE)
            val expectedId = prefs.getLong("pending_download_id", -2L)
            val fileName = prefs.getString("pending_apk_filename", null)

            if (downloadId != -1L && downloadId == expectedId && fileName != null) {
                Log.d("UpdateReceiver", "Download complete for APK: $fileName")
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val apkFile = File(downloadDir, fileName)

                if (apkFile.exists()) {
                    AppUpdateManager.installApk(context, apkFile)
                }
            }
        }
    }
}
