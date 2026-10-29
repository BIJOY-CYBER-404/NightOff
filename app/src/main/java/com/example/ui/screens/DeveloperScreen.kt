package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateInfo
import kotlinx.coroutines.launch

@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                DeveloperHeaderCard(context = context)
            }

            item {
                InAppUpdateCard(context = context)
            }

            item {
                SpecialityKnowledgeCard()
            }

            item {
                AppDescriptionCard()
            }

            item {
                ContactWhatsAppCard(context = context)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DeveloperHeaderCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = LightPrimary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(2.dp, LightPrimary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_developer_photo),
                    contentDescription = "Developer Bijoy Kumar Sarkar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bijoy Kumar Sarkar",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = LightPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Lead Android Automation & Systems Specialist",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LightSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun InAppUpdateCard(context: Context) {
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "In-App Software Update",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                        )
                        Text(
                            text = "Current Version: v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LightTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LightPrimary
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            isChecking = true
                            scope.launch {
                                updateInfo = com.example.update.GitHubPagesUpdateChecker.instance.checkForUpdate()
                                isChecking = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (updateInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val info = updateInfo!!
                if (info.hasUpdate) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightPrimary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎉 New Update Available: v${info.latestVersionName}",
                                fontWeight = FontWeight.Bold,
                                color = LightPrimary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.releaseNotes,
                                color = LightTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    try {
                                        AppUpdateManager.startDownload(context, info.downloadUrl, info.latestVersionName)
                                    } catch (_: Exception) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Download & Install Update", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "✓ You are running the latest official release (v${BuildConfig.VERSION_NAME}).",
                        color = Color(0xFF166534),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialityKnowledgeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Specialities & Knowledge",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val skills = listOf(
                "Kotlin & Jetpack Compose Modern UI Architecture",
                "Android OS Low-Level System Services & Power Policy",
                "Root (su) Shell Operations & Kernel Optimizations",
                "Exact Alarm Clock Scheduling & Doze Bypass",
                "Clean Architecture & App Size Reduction"
            )

            skills.forEach { skill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = skill,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDescriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "About NightOff",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NightOff is an automated bedtime sleep scheduler for Android. It automates sleep tasks—pausing media, closing apps, silencing ringers, toggling network data, and locking your screen—so you can disconnect, save battery, and sleep peacefully.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun ContactWhatsAppCard(context: Context) {
    val whatsappGreen = Color(0xFF25D366)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Contact Developer",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
            )

            Text(
                text = "Get in touch directly with the developer for custom features, support, or developer inquiries.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF15803D),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Button(
                onClick = {
                    val url = "https://wa.me/8801605081296?text=Hello%20Bijoy,%20I'm%20using%20NightOff%20app"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("whatsapp_contact_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = whatsappGreen,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contact Developer on WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
