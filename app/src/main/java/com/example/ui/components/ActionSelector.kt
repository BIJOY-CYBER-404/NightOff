package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SleepAction
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightError
import com.example.ui.theme.NightPrimary
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.NightTextPrimary
import com.example.ui.theme.NightTextSecondary
import com.example.ui.theme.NightWarning

@Composable
fun ActionSelector(
    selectedActions: Set<SleepAction>,
    isRootAvailable: Boolean,
    onToggleAction: (SleepAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SleepAction.values().sortedBy { it.executionOrder }.forEach { action ->
            val isSelected = selectedActions.contains(action)
            val isActionDisabled = action.requiresRoot && !isRootAvailable

            ActionItemRow(
                action = action,
                isSelected = isSelected,
                isDisabled = isActionDisabled,
                isRootAvailable = isRootAvailable,
                onToggle = {
                    if (!isActionDisabled) {
                        onToggleAction(action)
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionItemRow(
    action: SleepAction,
    isSelected: Boolean,
    isDisabled: Boolean,
    isRootAvailable: Boolean,
    onToggle: () -> Unit
) {
    val icon = getActionIcon(action)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isDisabled) { onToggle() }
            .testTag("action_toggle_${action.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && !isDisabled) NightSurfaceVariant else NightCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected && !isDisabled) NightPrimary.copy(alpha = 0.6f) else NightCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected && !isDisabled) NightPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(action.titleRes),
                    tint = if (isDisabled) NightTextSecondary.copy(alpha = 0.5f)
                    else if (isSelected) NightPrimary
                    else NightTextSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(action.titleRes),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDisabled) NightTextSecondary.copy(alpha = 0.5f) else NightTextPrimary
                        )
                    )

                    if (action.requiresRoot) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isRootAvailable) Color(0xFF065F46) else Color(0xFF451A03)
                        ) {
                            Text(
                                text = if (isRootAvailable) "Root ✓" else "Root Only",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRootAvailable) Color(0xFF6EE7B7) else NightWarning,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(action.descRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDisabled) NightTextSecondary.copy(alpha = 0.4f) else NightTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Checkbox(
                checked = isSelected && !isDisabled,
                onCheckedChange = { if (!isDisabled) onToggle() },
                enabled = !isDisabled
            )
        }
    }
}

private fun getActionIcon(action: SleepAction): ImageVector {
    return when (action) {
        SleepAction.STOP_MEDIA -> Icons.Default.StopCircle
        SleepAction.CLOSE_APPS -> Icons.Default.Close
        SleepAction.TURN_OFF_WIFI -> Icons.Default.WifiOff
        SleepAction.TURN_OFF_MOBILE_DATA -> Icons.Default.SignalCellularOff
        SleepAction.TURN_OFF_BLUETOOTH -> Icons.Default.BluetoothDisabled
        SleepAction.MUTE_SOUND -> Icons.Default.VolumeOff
        SleepAction.LOCK_SCREEN -> Icons.Default.Lock
        SleepAction.POWER_OFF -> Icons.Default.PowerSettingsNew
        SleepAction.RESTART_PHONE -> Icons.Default.RestartAlt
    }
}
