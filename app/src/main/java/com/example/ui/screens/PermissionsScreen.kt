package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightError
import com.example.ui.theme.NightPrimary
import com.example.ui.theme.NightSecondary
import com.example.ui.theme.NightSuccess
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.NightTextPrimary
import com.example.ui.theme.NightTextSecondary
import com.example.ui.theme.NightWarning
import com.example.util.PermissionHelper
import com.example.util.PermissionState

@Composable
fun PermissionsScreen(
    permissions: PermissionState,
    onRequestNotificationPermission: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
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
                            color = NightTextPrimary
                        )
                    )
                    Text(
                        text = stringResource(R.string.permissions_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(color = NightTextSecondary)
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NightPrimary)
                ) {
                    Text("Refresh", fontSize = 12.sp)
                }
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
                isRequired = true,
                icon = Icons.Default.SettingsAccessibility,
                onGrantClick = {
                    context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
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
                    context.startActivity(PermissionHelper.getDeviceAdminIntent(context))
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

        // Root / Shizuku Explanation Card
        item {
            RootExplanationCard(isRooted = permissions.isRooted)
        }

        // OEM Battery Optimization & Auto-start Restrictions Card
        item {
            OemBatteryGuideCard(context = context)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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
        colors = CardDefaults.cardColors(containerColor = NightCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) NightSuccess.copy(alpha = 0.3f) else NightCardBorder
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
                color = if (isGranted) NightSuccess.copy(alpha = 0.15f) else NightSurfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) NightSuccess else NightTextSecondary,
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
                            color = NightTextPrimary
                        )
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NightWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NightTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NightSuccess.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NightSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.btn_granted),
                            color = NightSuccess,
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
                        containerColor = if (isRequired) NightPrimary else NightSurfaceVariant,
                        contentColor = if (isRequired) Color(0xFF082F49) else NightTextPrimary
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
private fun RootExplanationCard(isRooted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NightSecondary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NightSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Power Off & Restart Limits",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NightTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.root_explanation),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NightTextSecondary,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isRooted) Color(0xFF065F46) else Color(0xFF1E293B)
            ) {
                Text(
                    text = if (isRooted) stringResource(R.string.root_detected) else stringResource(R.string.root_not_detected),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isRooted) Color(0xFF6EE7B7) else NightTextSecondary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OemBatteryGuideCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NightCardBorder)
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
                    tint = NightPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OEM Battery & Auto-start Guide",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NightTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Certain manufacturers (Xiaomi/MIUI, Samsung OneUI, Huawei EMUI, Oppo/Realme) aggressively kill background alarms. Ensure you:\n" +
                        "• Enable 'Autostart' for NightOff in system settings\n" +
                        "• Set Battery Saver to 'No restrictions'\n" +
                        "• Lock NightOff in Recent Apps",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NightTextSecondary,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    context.startActivity(PermissionHelper.getAppDetailsIntent(context))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Open App Info Settings",
                    color = NightPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
