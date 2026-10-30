package com.example.ui.components

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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.SleepAction
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightWarning

private val sortedSleepActions = SleepAction.values().sortedBy { it.executionOrder }

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
        sortedSleepActions.forEach { action ->
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
    val icon = remember(action) { getActionIcon(action) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isDisabled) { onToggle() }
            .testTag("action_toggle_${action.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && !isDisabled) Color(0xFFF0F9FF) else LightCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected && !isDisabled) LightPrimary.copy(alpha = 0.5f) else LightCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected && !isDisabled) LightPrimary.copy(alpha = 0.12f) else LightSurfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(action.titleRes),
                    tint = if (isDisabled) LightTextSecondary.copy(alpha = 0.5f)
                    else if (isSelected) LightPrimary
                    else LightTextSecondary,
                    modifier = Modifier.padding(9.dp)
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
                            color = if (isDisabled) LightTextSecondary.copy(alpha = 0.5f) else LightTextPrimary
                        )
                    )

                    if (action.requiresRoot) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isRootAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (isRootAvailable) "Elevated ✓" else "Root Only",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRootAvailable) LightSuccess else LightWarning,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(action.descRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDisabled) LightTextSecondary.copy(alpha = 0.4f) else LightTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Checkbox(
                checked = isSelected && !isDisabled,
                onCheckedChange = { if (!isDisabled) onToggle() },
                enabled = !isDisabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = LightPrimary,
                    checkmarkColor = Color.White
                )
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
