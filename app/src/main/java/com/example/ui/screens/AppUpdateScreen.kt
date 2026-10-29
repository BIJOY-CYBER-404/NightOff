package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.theme.LightError
import com.example.ui.theme.LightOnPrimary
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightPrimaryContainer
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightTextTertiary
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf(DownloadState.IDLE) }
    var progressFloat by remember { mutableFloatStateOf(0f) }
    var bytesDownloaded by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    // Disable system back press if force update is mandatory
    BackHandler(enabled = !updateInfo.isForceUpdate) {
        downloadJob?.cancel()
        onDismiss()
    }

    // Infinite pulsing transition for update hero icon
    val infiniteTransition = rememberInfiniteTransition(label = "update_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Animated progress float
    val animatedProgress by animateFloatAsState(
        targetValue = progressFloat,
        animationSpec = tween(300),
        label = "progress_anim"
    )

    fun startDownloadProcess() {
        downloadJob?.cancel()
        downloadState = DownloadState.DOWNLOADING
        progressFloat = 0f
        bytesDownloaded = 0L
        totalBytes = 0L
        errorMessage = null

        downloadJob = scope.launch {
            val result = AppUpdateManager.downloadApkWithProgress(
                context = context,
                updateInfo = updateInfo
            ) { bytes, total, prog ->
                bytesDownloaded = bytes
                totalBytes = total
                progressFloat = prog
            }

            result.fold(
                onSuccess = { file ->
                    downloadedFile = file
                    downloadState = DownloadState.COMPLETED
                    progressFloat = 1f
                    Toast.makeText(context, "Download complete! Opening installer...", Toast.LENGTH_SHORT).show()
                    AppUpdateManager.installApk(context, file)
                },
                onFailure = { err ->
                    errorMessage = err.message ?: "Failed to download update APK"
                    downloadState = DownloadState.ERROR
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Software Update",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LightTextPrimary
                    )
                },
                navigationIcon = {
                    if (!updateInfo.isForceUpdate) {
                        IconButton(
                            onClick = {
                                downloadJob?.cancel()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("update_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LightTextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO ANIMATION & BADGE
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(120.dp)
            ) {
                // Outer pulsing halo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(LightPrimary.copy(alpha = pulseAlpha * 0.35f))
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE))
                            )
                        )
                )

                // Inner core with icon
                Surface(
                    shape = CircleShape,
                    color = LightPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (downloadState == DownloadState.COMPLETED) Icons.Default.CheckCircle
                            else if (downloadState == DownloadState.DOWNLOADING) Icons.Default.RocketLaunch
                            else Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Status chip: "UPDATE AVAILABLE"
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LightSuccess)
                    )
                    Text(
                        text = if (updateInfo.isForceUpdate) "CRITICAL UPDATE REQUIRED" else "NEW VERSION AVAILABLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Title & Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NightOff v${updateInfo.latestVersionName}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = LightTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "An updated release is ready to install with bug fixes and bedtime automation upgrades.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LightTextSecondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // 2. VERSION COMPARISON CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.dp, LightCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "INSTALLED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightTextTertiary,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextPrimary
                        )
                        Text(
                            text = "Build ${BuildConfig.VERSION_CODE}",
                            fontSize = 11.sp,
                            color = LightTextSecondary
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = LightSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = LightPrimary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }

                    // Latest
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LATEST RELEASE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightPrimary,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "v${updateInfo.latestVersionName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightPrimary
                        )
                        Text(
                            text = "Build ${updateInfo.latestVersionCode}",
                            fontSize = 11.sp,
                            color = LightSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. RELEASE NOTES CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.dp, LightCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "What's New in this Version",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LightTextPrimary
                        )
                    }

                    val notes = updateInfo.releaseNotes.ifBlank {
                        "• Dedicated full-screen in-app update experience\n• Direct mirror download with real-time download progress\n• Bug fixes and battery optimization improvements"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notes.lines().filter { it.isNotBlank() }.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(LightPrimary)
                                )
                                Text(
                                    text = line.removePrefix("•").removePrefix("-").trim(),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. DOWNLOAD & PROGRESS CARD
            when (downloadState) {
                DownloadState.IDLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { startDownloadProcess() },
                            colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("download_update_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download & Install Update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = LightOnPrimary
                            )
                        }

                        // Browser fallback button
                        OutlinedButton(
                            onClick = {
                                val url = if (updateInfo.downloadUrl.isNotBlank()) updateInfo.downloadUrl
                                else AppUpdateManager.RAW_APK_URL
                                AppUpdateManager.openInBrowser(context, url)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LightCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("browser_download_button")
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp), tint = LightTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download via Browser",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = LightTextSecondary
                            )
                        }

                        if (!updateInfo.isForceUpdate) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dismiss_update_page_button")
                            ) {
                                Text(
                                    text = "Remind Me Later",
                                    color = LightTextTertiary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                DownloadState.DOWNLOADING -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = LightPrimary
                                    )
                                    Text(
                                        text = "Downloading update...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = LightPrimary
                                    )
                                }

                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = LightPrimary
                                )
                            }

                            // Animated progress bar
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = LightPrimary,
                                trackColor = Color(0xFFE0F2FE),
                                strokeCap = StrokeCap.Round
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (bytesDownloaded > 0) formatBytes(bytesDownloaded) else "Connecting...",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (totalBytes > 0) formatBytes(totalBytes) else "Direct Mirror",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "⏳ Please keep NightOff open while download finishes. Package will launch automatically.",
                                fontSize = 11.sp,
                                color = LightTextSecondary,
                                lineHeight = 15.sp
                            )

                            OutlinedButton(
                                onClick = {
                                    downloadJob?.cancel()
                                    downloadState = DownloadState.IDLE
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Text("Cancel", color = LightError, fontSize = 12.sp)
                            }
                        }
                    }
                }

                DownloadState.COMPLETED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LightSuccess,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Text(
                                text = "Update Ready to Install!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF065F46)
                            )

                            Text(
                                text = "The latest APK was downloaded and verified. Tap below to proceed with installation.",
                                fontSize = 12.sp,
                                color = Color(0xFF047857),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    val file = downloadedFile
                                    if (file != null && file.exists()) {
                                        AppUpdateManager.installApk(context, file)
                                    } else {
                                        startDownloadProcess()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("install_update_button")
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Install Update Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            TextButton(
                                onClick = { startDownloadProcess() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = LightTextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-download package", fontSize = 11.sp, color = LightTextSecondary)
                            }
                        }
                    }
                }

                DownloadState.ERROR -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = LightError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Download Notice",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LightError
                                )
                            }

                            Text(
                                text = errorMessage ?: "Download could not be completed from mirror.",
                                fontSize = 12.sp,
                                color = Color(0xFF9F1239),
                                lineHeight = 16.sp
                            )

                            Text(
                                text = "Tip: You can retry the direct mirror or download the APK directly using your web browser.",
                                fontSize = 11.sp,
                                color = LightTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { startDownloadProcess() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("retry_download_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val url = if (updateInfo.downloadUrl.isNotBlank()) updateInfo.downloadUrl
                                        else AppUpdateManager.RAW_APK_URL
                                        AppUpdateManager.openInBrowser(context, url)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("open_browser_download_button")
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browser", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. SECURITY & REPO FOOTER
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LightSurface,
                border = BorderStroke(1.dp, LightCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = LightSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Verified GitHub Distribution",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                        Text(
                            text = "Signed by AI Studio build system. Compatible with your settings & schedules.",
                            fontSize = 10.sp,
                            color = LightTextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}
