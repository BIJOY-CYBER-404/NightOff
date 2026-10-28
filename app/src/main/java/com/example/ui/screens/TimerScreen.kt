package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.ui.components.ActionSelector
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightError
import com.example.ui.theme.NightPrimary
import com.example.ui.theme.NightSecondary
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.NightTextPrimary
import com.example.ui.theme.NightTextSecondary
import com.example.ui.theme.NightWarning
import com.example.util.PermissionHelper
import com.example.util.PermissionState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimerScreen(
    activeTimer: NightSchedule?,
    permissions: PermissionState,
    selectedPresetMinutes: Int,
    customMinutes: Int,
    selectedActions: Set<SleepAction>,
    onSelectPreset: (Int) -> Unit,
    onCustomMinutesChange: (Int) -> Unit,
    onToggleAction: (SleepAction) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Active Sleep Timer Banner if running
            if (activeTimer != null) {
                ActiveTimerCard(
                    activeTimer = activeTimer,
                    onCancel = onCancelTimer
                )
            } else {
                QuickTimerConfigCard(
                    selectedPresetMinutes = selectedPresetMinutes,
                    customMinutes = customMinutes,
                    onSelectPreset = onSelectPreset,
                    onCustomMinutesChange = onCustomMinutesChange,
                    onStartTimer = onStartTimer
                )
            }
        }

        // Exact Alarm Permission Warning if missing
        if (!permissions.hasExactAlarm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NightWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NightWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Permission Needed",
                                fontWeight = FontWeight.Bold,
                                color = NightWarning,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Needed to accurately trigger actions while sleeping.",
                                color = NightTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(PermissionHelper.getExactAlarmIntent())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NightWarning)
                        ) {
                            Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Actions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.select_actions),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NightTextPrimary
                    )
                )

                Text(
                    text = "${selectedActions.size} selected",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NightPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Actions List
        item {
            ActionSelector(
                selectedActions = selectedActions,
                isRootAvailable = permissions.isRooted,
                onToggleAction = onToggleAction
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Start Button (sticky-like at end of scroll)
        if (activeTimer == null) {
            item {
                Button(
                    onClick = onStartTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_sleep_timer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightPrimary,
                        contentColor = Color(0xFF082F49)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.start_sleep_timer),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ActiveTimerCard(
    activeTimer: NightSchedule,
    onCancel: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeTimer.targetTimeMillis) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingMillis = (activeTimer.targetTimeMillis - currentTime).coerceAtLeast(0L)
    val remainingMinutes = remainingMillis / (60 * 1000L)
    val remainingSeconds = (remainingMillis % (60 * 1000L)) / 1000L

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTarget = timeFormat.format(Date(activeTimer.targetTimeMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_timer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C243B)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NightPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = NightPrimary,
                    modifier = Modifier.size(10.dp)
                ) {}
                Text(
                    text = stringResource(R.string.timer_running),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = NightPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Timer Display
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", remainingMinutes, remainingSeconds),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NightTextPrimary,
                    letterSpacing = 2.sp
                )
            )

            Text(
                text = "Target Shutdown: $formattedTarget",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NightTextSecondary
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_timer_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NightError,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cancel_timer),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickTimerConfigCard(
    selectedPresetMinutes: Int,
    customMinutes: Int,
    onSelectPreset: (Int) -> Unit,
    onCustomMinutesChange: (Int) -> Unit,
    onStartTimer: () -> Unit
) {
    val durationMinutes = if (selectedPresetMinutes == -1) customMinutes else selectedPresetMinutes
    val targetTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTarget = timeFormat.format(Date(targetTimeMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.quick_timer_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightTextPrimary
                        )
                    )
                    Text(
                        text = stringResource(R.string.quick_timer_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NightTextSecondary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NightSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = formattedTarget,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets row: 15m, 30m, 1h, Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = stringResource(R.string.preset_15_min),
                    minutes = 15,
                    isSelected = selectedPresetMinutes == 15,
                    onSelect = { onSelectPreset(15) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.preset_30_min),
                    minutes = 30,
                    isSelected = selectedPresetMinutes == 30,
                    onSelect = { onSelectPreset(30) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.preset_60_min),
                    minutes = 60,
                    isSelected = selectedPresetMinutes == 60,
                    onSelect = { onSelectPreset(60) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.preset_custom),
                    minutes = -1,
                    isSelected = selectedPresetMinutes == -1,
                    onSelect = { onSelectPreset(-1) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom slider if custom selected
            AnimatedVisibility(visible = selectedPresetMinutes == -1) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Sleep Duration:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = NightTextSecondary)
                        )
                        Text(
                            text = "$customMinutes minutes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightSecondary
                            )
                        )
                    }

                    Slider(
                        value = customMinutes.toFloat(),
                        onValueChange = { onCustomMinutesChange(it.toInt()) },
                        valueRange = 5f..180f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = NightSecondary,
                            activeTrackColor = NightSecondary,
                            inactiveTrackColor = NightSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    minutes: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("preset_${if (minutes == -1) "custom" else "${minutes}m"}"),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NightPrimary else NightSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NightPrimary else NightCardBorder
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF082F49) else NightTextPrimary,
                fontSize = 14.sp
            )
        }
    }
}
