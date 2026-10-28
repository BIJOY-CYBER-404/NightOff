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
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

@Composable
fun OnboardingDialog(
    onOpenPermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissible to ensure all permissions are granted */ },
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
                        text = "Required Permission Setup",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "To ensure NightOff can successfully pause media, close apps, silence calls, and lock your screen without failing, please grant the required permissions below:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightTextSecondary,
                        lineHeight = 18.sp
                    )
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Alarm,
                    title = "Exact Alarms",
                    desc = "Required to trigger bedtime actions at the precise moment"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "10-Min Warning Notification",
                    desc = "Required for pre-warning alerts and snooze options"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.SettingsAccessibility,
                    title = "Accessibility Service",
                    desc = "Required to close recent apps and lock screen"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.Lock,
                    title = "Device Admin",
                    desc = "Required for instant hardware screen lock"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.DoNotDisturbOn,
                    title = "Do Not Disturb",
                    desc = "Required to mute ringers while sleeping"
                )

                OnboardingPermissionRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "Ignore Battery Optimization",
                    desc = "Required to prevent Android from delaying sleep alarms"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_continue_button")
            ) {
                Text(
                    text = "Grant Permissions Now",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
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
