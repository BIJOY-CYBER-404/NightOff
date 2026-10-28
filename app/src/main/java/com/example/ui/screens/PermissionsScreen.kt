package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightError
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightWarning
import com.example.util.PermissionHelper
import com.example.util.PermissionState

@Composable
fun PermissionsScreen(
    permissions: PermissionState,
    onRequestNotificationPermission: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onToggleVirtualRoot: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRestrictedDialogFor by remember { mutableStateOf<String?>(null) } // "accessibility" or "device_admin"

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.permissions_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                        )
                        Text(
                            text = "Background automation readiness",
                            style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary)
                        )
                    }

                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightPrimary)
                    ) {
                        Text("Refresh", fontSize = 12.sp)
                    }
                }
            }

            // Virtual Root / Custom Elevated Mode Card
            item {
                VirtualRootCard(
                    isVirtualRootEnabled = permissions.isVirtualRootEnabled,
                    isRealRooted = permissions.isRealRooted,
                    onToggle = onToggleVirtualRoot
                )
            }

            // Restricted Settings Fix Card (Direct solution for "App was denied access" prompt)
            if (!permissions.hasAccessibility || !permissions.hasDeviceAdmin) {
                item {
                    RestrictedSettingsGuideCard(
                        context = context,
                        onOpenGuide = { showRestrictedDialogFor = "accessibility" }
                    )
                }
            }

            // Checklist Items
            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_exact_alarm),
                    description = stringResource(R.string.perm_exact_alarm_desc),
                    isGranted = permissions.hasExactAlarm,
                    isRequired = true,
                    icon = Icons.Default.Alarm,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getExactAlarmIntent())
                    }
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_notifications),
                    description = stringResource(R.string.perm_notifications_desc),
                    isGranted = permissions.hasNotification,
                    isRequired = true,
                    icon = Icons.Default.Notifications,
                    onGrantClick = onRequestNotificationPermission
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_accessibility),
                    description = stringResource(R.string.perm_accessibility_desc),
                    isGranted = permissions.hasAccessibility,
                    isRequired = !permissions.isVirtualRootEnabled,
                    icon = Icons.Default.SettingsAccessibility,
                    onGrantClick = {
                        // Show the guided 2-step unlock dialog to avoid the "App was denied access" wall
                        showRestrictedDialogFor = "accessibility"
                    }
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_device_admin),
                    description = stringResource(R.string.perm_device_admin_desc),
                    isGranted = permissions.hasDeviceAdmin,
                    isRequired = false,
                    icon = Icons.Default.Lock,
                    onGrantClick = {
                        showRestrictedDialogFor = "device_admin"
                    }
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_dnd),
                    description = stringResource(R.string.perm_dnd_desc),
                    isGranted = permissions.hasDnd,
                    isRequired = false,
                    icon = Icons.Default.DoNotDisturbOn,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getDndSettingsIntent())
                    }
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_battery_opt),
                    description = stringResource(R.string.perm_battery_opt_desc),
                    isGranted = permissions.hasIgnoreBatteryOpt,
                    isRequired = true,
                    icon = Icons.Default.BatteryAlert,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                    }
                )
            }

            item {
                PermissionChecklistItem(
                    title = stringResource(R.string.perm_bluetooth),
                    description = stringResource(R.string.perm_bluetooth_desc),
                    isGranted = permissions.hasBluetooth,
                    isRequired = false,
                    icon = Icons.Default.Bluetooth,
                    onGrantClick = onRequestBluetoothPermission
                )
            }

            // OEM Battery Optimization Card
            item {
                OemBatteryGuideCard(context = context)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal dialog showing 2 simple steps to unlock the restricted setting
    if (showRestrictedDialogFor != null) {
        RestrictedSettingsModalDialog(
            type = showRestrictedDialogFor!!,
            context = context,
            onDismiss = { showRestrictedDialogFor = null }
        )
    }
}

@Composable
private fun RestrictedSettingsModalDialog(
    type: String,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = LightWarning,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Unlock 'App was denied access'",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                    )
                    Text(
                        text = "Android 13 / 14 / 15 Sideload Protection",
                        style = MaterialTheme.typography.bodySmall.copy(color = LightWarning, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Android displays \"App was denied access\" because this app was installed as an APK. Follow these 2 steps to allow it:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary, lineHeight = 20.sp)
                )

                // Step 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = LightPrimary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Step 1: Allow Restricted Settings",
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap below, then tap the three dots (⋮) in the top-right corner of App Info and choose 'Allow restricted settings'.",
                            color = LightTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open App Info (Tap ⋮ -> Allow)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = LightSuccess,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (type == "accessibility") "Step 2: Turn NightOff ON" else "Step 2: Activate Device Admin",
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (type == "accessibility") "Return here and tap below to open Accessibility > Downloaded apps > NightOff and turn it ON!" else "Tap below to activate device admin screen lock.",
                            color = LightTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (type == "accessibility") {
                                    context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                                } else {
                                    context.startActivity(PermissionHelper.getDeviceAdminIntent(context))
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (type == "accessibility") "Open Accessibility Settings" else "Open Device Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LightTextSecondary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = LightCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun VirtualRootCard(
    isVirtualRootEnabled: Boolean,
    isRealRooted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, LightPrimary.copy(alpha = 0.5f))
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
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = LightPrimary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = LightPrimary,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Virtual Root / Elevated Mode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                        )
                        Text(
                            text = if (isRealRooted) "Real Root (su) Detected" else "Auto Sleep Fallback Active",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isRealRooted) LightSuccess else LightPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Switch(
                    checked = isVirtualRootEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LightPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "When active, NightOff executes screen lock, media stop, and sleep shutdown without requiring physical root permissions or failing when system accessibility is restricted.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun RestrictedSettingsGuideCard(
    context: Context,
    onOpenGuide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, LightWarning.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = LightWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Fix 'App was denied access'",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    )
                    Text(
                        text = "Required for Android 13, 14 & 15 APKs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "When tapping NightOff in Accessibility, Android blocks it with 'App was denied access'. To unlock it:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF78350F),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Tap 'Open App Info' below.\n" +
                        "2. Tap the 3 dots (⋮) in the top-right corner.\n" +
                        "3. Tap 'Allow restricted settings' & enter PIN/fingerprint.\n" +
                        "4. Now you can turn NightOff Accessibility ON!",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF451A03),
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightWarning,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fix_restricted_settings_button")
                ) {
                    Text(
                        text = "1. Open App Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenGuide,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E))
                ) {
                    Text(text = "View Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionChecklistItem(
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) LightSuccess.copy(alpha = 0.3f) else LightCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isGranted) LightSuccess.copy(alpha = 0.12f) else LightSurfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) LightSuccess else LightTextSecondary,
                    modifier = Modifier.padding(9.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightTextSecondary,
                        fontSize = 12.sp
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                            text = stringResource(R.string.btn_granted),
                            color = LightSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRequired) LightPrimary else LightSurfaceVariant,
                        contentColor = if (isRequired) Color.White else LightTextPrimary
                    ),
                    modifier = Modifier.testTag("grant_btn_${title.take(6).lowercase()}")
                ) {
                    Text(
                        text = stringResource(R.string.btn_grant),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OemBatteryGuideCard(context: Context) {
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OEM Battery & Auto-start Guide",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Certain manufacturers (Xiaomi/MIUI, Samsung OneUI, Huawei, Realme, Vivo) may silence background alarms. To prevent delays:\n" +
                        "• Set App Battery Usage to 'Unrestricted'\n" +
                        "• Enable 'Autostart' in Settings if available",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                },
                colors = ButtonDefaults.buttonColors(containerColor = LightSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Open App Settings",
                    color = LightPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
