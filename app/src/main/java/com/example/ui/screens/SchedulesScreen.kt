package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchedulesScreen(
    schedules: List<NightSchedule>,
    isRootAvailable: Boolean,
    onToggleSchedule: (NightSchedule, Boolean) -> Unit,
    onSaveSchedule: (NightSchedule) -> Unit,
    onDeleteSchedule: (NightSchedule) -> Unit,
    onTestRunNow: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<NightSchedule?>(null) }

    val savedNonQuickSchedules = schedules.filter { !it.isQuickTimer }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
    ) {
        if (savedNonQuickSchedules.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = NightSurfaceVariant,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = NightPrimary,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.no_schedules_yet),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = NightTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        editingSchedule = null
                        showDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NightPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_first_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF082F49))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.add_schedule),
                        color = Color(0xFF082F49),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.schedules_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(savedNonQuickSchedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { isEnabled -> onToggleSchedule(schedule, isEnabled) },
                        onEdit = {
                            editingSchedule = schedule
                            showDialog = true
                        },
                        onDelete = { onDeleteSchedule(schedule) },
                        onRunNow = { onTestRunNow(schedule.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingSchedule = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_schedule_fab"),
            containerColor = NightPrimary,
            contentColor = Color(0xFF082F49)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_schedule))
        }
    }

    if (showDialog) {
        ScheduleEditDialog(
            initialSchedule = editingSchedule,
            isRootAvailable = isRootAvailable,
            onDismiss = { showDialog = false },
            onSave = { updated ->
                onSaveSchedule(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: NightSchedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRunNow: () -> Unit
) {
    val amPm = if (schedule.hour < 12) "AM" else "PM"
    val displayHour = when {
        schedule.hour == 0 -> 12
        schedule.hour > 12 -> schedule.hour - 12
        else -> schedule.hour
    }
    val timeFormatted = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, schedule.minute, amPm)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_card_${schedule.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) NightCard else NightSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (schedule.isEnabled) NightCardBorder else Color.Transparent
        )
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
                Column {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (schedule.isEnabled) NightTextPrimary else NightTextSecondary
                        )
                    )
                    Text(
                        text = schedule.title.ifBlank { "Bedtime Schedule" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NightPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF082F49),
                        checkedTrackColor = NightPrimary,
                        uncheckedThumbColor = NightTextSecondary,
                        uncheckedTrackColor = NightSurfaceVariant
                    ),
                    modifier = Modifier.testTag("schedule_switch_${schedule.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day indicators
            DaysOfWeekRow(scheduleDays = schedule.getDaysList())

            Spacer(modifier = Modifier.height(10.dp))

            // Action count & buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${schedule.getActionList().size} actions configured",
                    style = MaterialTheme.typography.bodySmall.copy(color = NightTextSecondary)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRunNow,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run now",
                            tint = NightSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = NightTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NightError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekRow(scheduleDays: Set<Int>) {
    val dayLabels = listOf(
        1 to "M",
        2 to "T",
        3 to "W",
        4 to "T",
        5 to "F",
        6 to "S",
        7 to "S"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dayLabels.forEach { (isoDay, label) ->
            val isActive = scheduleDays.contains(isoDay)
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = if (isActive) NightPrimary.copy(alpha = 0.25f) else Color(0xFF1E293B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) NightPrimary else NightTextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditDialog(
    initialSchedule: NightSchedule?,
    isRootAvailable: Boolean,
    onDismiss: () -> Unit,
    onSave: (NightSchedule) -> Unit
) {
    var title by remember { mutableStateOf(initialSchedule?.title ?: "Bedtime Shutdown") }
    var selectedDays by remember { mutableStateOf(initialSchedule?.getDaysList() ?: setOf(1, 2, 3, 4, 5)) }
    var selectedActions by remember { mutableStateOf(initialSchedule?.getActionList() ?: SleepAction.defaultActions()) }

    val initialHour = initialSchedule?.hour ?: 23
    val initialMinute = initialSchedule?.minute ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchedule == null) stringResource(R.string.add_schedule) else stringResource(R.string.edit_schedule),
                fontWeight = FontWeight.Bold,
                color = NightTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.schedule_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NightPrimary,
                        unfocusedBorderColor = NightCardBorder
                    ),
                    singleLine = true
                )

                // Time Picker
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = NightSurfaceVariant,
                            selectorColor = NightPrimary,
                            containerColor = NightCard
                        )
                    )
                }

                // Days selector
                Text(
                    text = stringResource(R.string.days_repeat),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NightTextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                    days.forEach { (iso, label) ->
                        val isSelected = selectedDays.contains(iso)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - iso else selectedDays + iso
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NightPrimary else NightSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF082F49) else NightTextPrimary
                                )
                            }
                        }
                    }
                }

                // Action toggles
                Text(
                    text = stringResource(R.string.select_actions),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NightTextPrimary
                    )
                )

                ActionSelector(
                    selectedActions = selectedActions,
                    isRootAvailable = isRootAvailable,
                    onToggleAction = { action ->
                        selectedActions = if (selectedActions.contains(action)) {
                            selectedActions - action
                        } else {
                            selectedActions + action
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysString = selectedDays.sorted().joinToString(",")
                    val actionsString = selectedActions.joinToString(",") { it.name }

                    val scheduleToSave = (initialSchedule ?: NightSchedule(
                        title = title,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )).copy(
                        title = title,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        daysOfWeek = daysString,
                        isEnabled = true,
                        isQuickTimer = false,
                        actionsJson = actionsString
                    )

                    onSave(scheduleToSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NightPrimary),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = Color(0xFF082F49),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = NightTextSecondary)
            }
        },
        containerColor = NightCard
    )
}
