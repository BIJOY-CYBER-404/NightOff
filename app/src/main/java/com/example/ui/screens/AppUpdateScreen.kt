package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdateManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "downloadProgressAnim")

    BackHandler(enabled = !updateInfo.isForceUpdate) {
        onDismiss()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = { Text("Software Update", fontWeight = FontWeight.Bold, color = LightTextPrimary) },
                navigationIcon = {
                    if (!updateInfo.isForceUpdate) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("back_from_update_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightTextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Hero Icon Surface
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = LightPrimary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Text(
                text = "New Update Available!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    fontSize = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Version v${updateInfo.latestVersionName} is ready to download and install. Update now for the latest features and stability improvements.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LightTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Version Comparison Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightCard),
                border = BorderStroke(1.dp, LightCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Version", fontSize = 11.sp, color = LightTextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "➔",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = LightPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Latest Version", fontSize = 11.sp, color = LightPrimary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("v${updateInfo.latestVersionName} (${updateInfo.latestVersionCode})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightPrimary)
                    }
                }
            }

            // Release Notes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightCard),
                border = BorderStroke(1.dp, LightCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Release Notes",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateInfo.releaseNotes.ifBlank { "General performance enhancements and bedtime automation updates." },
                        color = LightTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Download Progress Section if downloading or downloaded
            if (isDownloading || downloadedFile != null || downloadError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, LightCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    downloadedFile != null -> "Download Complete"
                                    downloadError != null -> "Download Failed"
                                    else -> "Downloading Update..."
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (downloadedFile != null) LightSuccess else if (downloadError != null) MaterialTheme.colorScheme.error else LightPrimary,
                                fontSize = 13.sp
                            )

                            if (isDownloading) {
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = LightPrimary
                                )
                            }
                        }

                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = LightPrimary,
                                trackColor = LightSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = downloadStatusText.ifBlank { "Streaming update APK..." },
                                fontSize = 11.sp,
                                color = LightTextSecondary
                            )
                        }

                        if (downloadError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = downloadError ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (downloadedFile != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    downloadedFile?.let { AppUpdateManager.installApk(context, it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("install_apk_button")
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install Update Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            if (downloadedFile == null) {
                Button(
                    onClick = {
                        isDownloading = true
                        downloadError = null
                        downloadProgress = 0f
                        scope.launch {
                            val result = AppUpdateManager.downloadApkWithProgress(
                                context = context,
                                updateInfo = updateInfo
                            ) { downloaded, total, prog ->
                                downloadProgress = prog
                                downloadStatusText = "${downloaded / 1024} KB / ${total / 1024} KB"
                            }
                            isDownloading = false
                            result.fold(
                                onSuccess = { file ->
                                    downloadedFile = file
                                    Toast.makeText(context, "Download complete! Ready to install.", Toast.LENGTH_LONG).show()
                                    AppUpdateManager.installApk(context, file)
                                },
                                onFailure = { err ->
                                    downloadError = err.message ?: "Download failed"
                                    try {
                                        AppUpdateManager.startDownload(context, updateInfo.downloadUrl, updateInfo.latestVersionName)
                                        Toast.makeText(context, "Started background download via DownloadManager", Toast.LENGTH_LONG).show()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_download_update_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloading) "Downloading Update..." else "Download & Update Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Fallback / Browser Download Button
            OutlinedButton(
                onClick = {
                    AppUpdateManager.openInBrowser(context, updateInfo.downloadUrl.ifBlank { updateInfo.githubRepo })
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("browser_download_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LightPrimary)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open in Browser / GitHub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (!updateInfo.isForceUpdate && downloadedFile == null && !isDownloading) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_update_page_button")
                ) {
                    Text("Remind Me Later", color = LightTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
