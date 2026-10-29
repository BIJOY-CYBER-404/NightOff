package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Update Available",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "v${updateInfo.latestVersionName} is now ready",
                        color = LightPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current: v${BuildConfig.VERSION_NAME}",
                            fontSize = 11.sp,
                            color = LightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Latest: v${updateInfo.latestVersionName}",
                            fontSize = 11.sp,
                            color = LightPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Release Notes:",
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    fontSize = 12.sp
                )

                Text(
                    text = updateInfo.releaseNotes.ifBlank { "Performance improvements and bedtime automation enhancements." },
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏳ Download started in notifications. The installation will launch once complete.",
                        color = LightPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDownloading = true
                    Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show()
                    try {
                        AppUpdateManager.startDownload(
                            context = context,
                            downloadUrl = updateInfo.downloadUrl,
                            versionName = updateInfo.latestVersionName
                        )
                    } catch (e: Exception) {
                        // Fallback: Open browser to download
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("download_update_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDownloading) "Downloading..." else "Update Now",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = if (!updateInfo.isForceUpdate) {
            {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_update_button")
                ) {
                    Text("Later", color = LightTextSecondary, fontSize = 12.sp)
                }
            }
        } else null,
        containerColor = LightCard,
        shape = RoundedCornerShape(18.dp)
    )
}
