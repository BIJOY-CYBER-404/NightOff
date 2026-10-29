package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightWarning
import com.example.util.PermissionHelper
import com.example.util.PermissionState

@Composable
fun MandatoryPermissionGateScreen(
    permissions: PermissionState,
    onRequestNotificationPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress = (permissions.requiredGrantedCount.toFloat() / permissions.totalRequiredCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "permissionProgress")
    val allGranted = permissions.areAllRequiredGranted

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshPermissions()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HeaderSection(
                        grantedCount = permissions.requiredGrantedCount,
                        totalCount = permissions.totalRequiredCount,
                        progress = animatedProgress
                    )
                }

                // 1. Exact Alarm
                item {
                    PermissionRowCard(
                        title = "Exact Alarms (Precise Timing)",
                        description = "Triggers bedtime shutdown at the exact minute without delay",
                        isGranted = permissions.hasExactAlarm,
                        icon = Icons.Default.Alarm,
                        onGrant = {
                            context.startActivity(PermissionHelper.getExactAlarmIntent())
                        }
                    )
                }

                // 2. Notifications
                item {
                    PermissionRowCard(
                        title = "Notifications & Reminders",
                        description = "Sends the 10-minute warning alert with vibration, ring & snooze",
                        isGranted = permissions.hasNotification,
                        icon = Icons.Default.Notifications,
                        onGrant = onRequestNotificationPermission
                    )
                }

                // 3. Accessibility Service (With 2-step Restricted Settings unlock)
                item {
                    AccessibilityPermissionCard(
                        isGranted = permissions.hasAccessibility,
                        context = context
                    )
                }

                // 4. Device Admin Screen Lock (Result launcher to avoid auto-back + fallback settings)
                item {
                    DeviceAdminPermissionCard(
                        isGranted = permissions.hasDeviceAdmin,
                        context = context,
                        onGrant = {
                            try {
                                val intent = PermissionHelper.getDeviceAdminIntent(context)
                                deviceAdminLauncher.launch(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(PermissionHelper.getDeviceAdminSettingsIntent())
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                }
                            }
                        }
                    )
                }

                // 5. Battery Optimization
                item {
                    PermissionRowCard(
                        title = "Ignore Battery Optimization",
                        description = "Prevents Android OEM power killers from killing bedtime alarms",
                        isGranted = permissions.hasIgnoreBatteryOpt,
                        icon = Icons.Default.BatteryAlert,
                        onGrant = {
                            context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                        }
                    )
                }

                // 6. Do Not Disturb Access
                item {
                    PermissionRowCard(
                        title = "Do Not Disturb (DND) Access",
                        description = "Silences incoming calls, notifications, and ringers while you sleep",
                        isGranted = permissions.hasDnd,
                        icon = Icons.Default.DoNotDisturbOn,
                        onGrant = {
                            context.startActivity(PermissionHelper.getDndSettingsIntent())
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Sticky Action Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = LightCard,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (allGranted) "All Permissions Ready!" else "${permissions.requiredGrantedCount} of ${permissions.totalRequiredCount} Granted",
                            fontWeight = FontWeight.Bold,
                            color = if (allGranted) LightSuccess else LightTextPrimary,
                            fontSize = 13.sp
                        )

                        OutlinedButton(
                            onClick = onRefreshPermissions,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LightPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onContinue,
                        enabled = allGranted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_to_app_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (allGranted) LightSuccess else Color(0xFFCBD5E1),
                            contentColor = Color.White
                        )
                    ) {
                        if (allGranted) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue to NightOff", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text(
                                text = "Grant remaining permissions to continue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    grantedCount: Int,
    totalCount: Int,
    progress: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = LightPrimary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = LightTextPrimary,
                fontSize = 22.sp
            )
        )

        Text(
            text = "NightOff requires system permissions to automate bedtime tasks and preserve battery. All permissions must be granted to continue.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = LightTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Setup Progress",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary
                )
                Text(
                    text = "$grantedCount / $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (grantedCount == totalCount) LightSuccess else LightPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (grantedCount == totalCount) LightSuccess else LightPrimary,
                trackColor = LightSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) LightSuccess.copy(alpha = 0.35f) else LightCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isGranted) LightSuccess.copy(alpha = 0.12f) else LightSurfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) LightSuccess else LightTextSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LightSuccess.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LightSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Granted",
                            color = LightSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onGrant,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = "Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPermissionCard(
    isGranted: Boolean,
    context: Context
) {
    var expandedInstructions by remember { mutableStateOf(!isGranted) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) LightSuccess.copy(alpha = 0.35f) else LightCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isGranted) LightSuccess.copy(alpha = 0.12f) else LightSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsAccessibility,
                        contentDescription = null,
                        tint = if (isGranted) LightSuccess else LightTextSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accessibility Service",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Closes recent background apps and locks screen",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightSuccess.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LightSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Granted",
                                color = LightSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = "Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                // 2-Step Restricted Settings unlocker for Android 13/14/15/16
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightWarning.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⚠️ If Android shows \"App was denied access\":",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Tap 'Open App Info' -> Tap 3 dots (⋮) in upper-right -> Tap 'Allow restricted settings'.\n2. Then tap 'Open Accessibility' and turn NightOff ON.",
                            color = Color(0xFF78350F),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightWarning),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("1. Open App Info (Tap ⋮)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("2. Turn ON", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAdminPermissionCard(
    isGranted: Boolean,
    context: Context,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) LightSuccess.copy(alpha = 0.35f) else LightCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isGranted) LightSuccess.copy(alpha = 0.12f) else LightSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isGranted) LightSuccess else LightTextSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Admin Screen Lock",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Provides instant hardware screen lock when timer ends",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightSuccess.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LightSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Granted",
                                color = LightSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onGrant,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = "Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                // Fallback & explanation card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 Tap 'Grant' above to activate device admin. If your device auto-backs:",
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextPrimary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(PermissionHelper.getDeviceAdminSettingsIntent())
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("Open Admin Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightWarning),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("App Info (Tap ⋮)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
