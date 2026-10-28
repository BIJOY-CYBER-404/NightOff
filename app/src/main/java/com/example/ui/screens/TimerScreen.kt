package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.ui.components.ActionSelector
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightError
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondary
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightWarning
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
    onSnoozeTimer: (Int) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                if (activeTimer != null) {
                    ActiveTimerHeroCard(
                        activeTimer = activeTimer,
                        onCancel = onCancelTimer,
                        onSnooze = onSnoozeTimer
                    )
                } else {
                    TimerPresetSelectorCard(
                        selectedPresetMinutes = selectedPresetMinutes,
                        customMinutes = customMinutes,
                        onSelectPreset = onSelectPreset,
                        onCustomMinutesChange = onCustomMinutesChange
                    )
                }
            }

            // Warning if exact alarm missing
            if (!permissions.hasExactAlarm) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightWarning.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = LightWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exact Alarm Permission Needed",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Required for precise bedtime execution.",
                                    color = Color(0xFFB45309),
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = {
                                    context.startActivity(PermissionHelper.getExactAlarmIntent())
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightWarning,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Header for Actions
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
                            color = LightTextPrimary
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "${selectedActions.size} active",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LightPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Action Selection Cards
            item {
                ActionSelector(
                    selectedActions = selectedActions,
                    isRootAvailable = permissions.isElevatedAvailable,
                    onToggleAction = onToggleAction
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Start Button
            if (activeTimer == null) {
                item {
                    Button(
                        onClick = onStartTimer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_sleep_timer_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.start_sleep_timer),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerHeroCard(
    activeTimer: NightSchedule,
    onCancel: () -> Unit,
    onSnooze: (Int) -> Unit
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBAE6FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val totalDuration = (activeTimer.targetTimeMillis - activeTimer.createdAt).coerceAtLeast(60_000L)
            val progress = (remainingMillis.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            val sweepAngle = progress * 360f

            // Clean animated circular dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    // Background track
                    drawCircle(
                        color = Color(0xFFE0F2FE),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Dynamic active arc
                    drawArc(
                        color = Color(0xFF0284C7),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", remainingMinutes, remainingSeconds),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = LightTextPrimary,
                            fontSize = 32.sp
                        )
                    )
                    Text(
                        text = "time left",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Runs sleep shutdown at $formattedTarget",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LightTextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.snooze_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSnooze(10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("snooze_10_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE0F2FE),
                            contentColor = LightPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.snooze_10_min),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onSnooze(30) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("snooze_30_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE0F2FE),
                            contentColor = LightPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.snooze_30_min),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("cancel_timer_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightError,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cancel_timer),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TimerPresetSelectorCard(
    selectedPresetMinutes: Int,
    customMinutes: Int,
    onSelectPreset: (Int) -> Unit,
    onCustomMinutesChange: (Int) -> Unit
) {
    val durationMinutes = if (selectedPresetMinutes == -1) customMinutes else selectedPresetMinutes
    val targetTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTarget = timeFormat.format(Date(targetTimeMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            color = LightTextPrimary
                        )
                    )
                    Text(
                        text = stringResource(R.string.quick_timer_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextSecondary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = formattedTarget,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets row
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

            // Custom slider
            AnimatedVisibility(
                visible = selectedPresetMinutes == -1,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Duration:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary)
                        )
                        Text(
                            text = "$customMinutes min",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightSecondary
                            )
                        )
                    }

                    Slider(
                        value = customMinutes.toFloat(),
                        onValueChange = { onCustomMinutesChange(it.toInt()) },
                        valueRange = 5f..180f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = LightPrimary,
                            activeTrackColor = LightPrimary,
                            inactiveTrackColor = LightSurfaceVariant
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
        color = if (isSelected) LightPrimary else LightSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) LightPrimary else LightCardBorder
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else LightTextPrimary,
                fontSize = 14.sp
            )
        }
    }
}
