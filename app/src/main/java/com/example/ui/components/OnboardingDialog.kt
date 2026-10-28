package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Welcome to NightOff",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Sleep shutdown automation",
                        color = LightTextSecondary,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "To pause media, silence calls, lock your screen, and protect your battery while you sleep, NightOff uses a few system permissions:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightTextSecondary,
                        lineHeight = 18.sp
                    )
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Alarm,
                    title = "Exact Alarms",
                    desc = "Triggers actions at the precise moment you pick"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "10-Min Warning Notification",
                    desc = "Gives you a heads-up warning before shutting down"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.SettingsAccessibility,
                    title = "Accessibility Service",
                    desc = "Closes recent apps, pauses media, and locks the screen"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Lock,
                    title = "Device Admin",
                    desc = "Immediate hardware screen lock"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.DoNotDisturbOn,
                    title = "Do Not Disturb",
                    desc = "Mutes ringers and alerts while sleeping"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "Ignore Battery Optimization",
                    desc = "Ensures Android does not delay sleep actions"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("onboarding_continue_button")
            ) {
                Text(
                    text = "Open Permissions Checklist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = LightSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Later", color = LightTextSecondary)
            }
        },
        containerColor = LightCard
    )
}

@Composable
private fun OnboardingPermissionRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE0F2FE)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LightPrimary,
                modifier = Modifier.padding(7.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = LightTextPrimary,
                fontSize = 13.sp
            )
            Text(
                text = desc,
                color = LightTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
