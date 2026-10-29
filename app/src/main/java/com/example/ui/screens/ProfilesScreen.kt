package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightCard
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSecondary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import kotlinx.coroutines.launch

data class PresetProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultMinutes: Int,
    val actions: Set<SleepAction>
)

val defaultPresetProfiles = listOf(
    PresetProfile(
        id = "study_night",
        title = "Study Night",
        subtitle = "Quiet focus with auto-mute and media pause",
        icon = Icons.Default.Psychology,
        defaultMinutes = 45,
        actions = setOf(SleepAction.STOP_MEDIA, SleepAction.CLOSE_APPS, SleepAction.MUTE_SOUND, SleepAction.TURN_OFF_WIFI)
    ),
    PresetProfile(
        id = "movie_night",
        title = "Movie Night",
        subtitle = "Sleep timer after watching late movies",
        icon = Icons.Default.AutoAwesome,
        defaultMinutes = 60,
        actions = setOf(SleepAction.STOP_MEDIA, SleepAction.MUTE_SOUND, SleepAction.LOCK_SCREEN)
    ),
    PresetProfile(
        id = "work_night",
        title = "Work Night",
        subtitle = "Full disconnect: Network off, mute, screen lock",
        icon = Icons.Default.Smartphone,
        defaultMinutes = 30,
        actions = setOf(
            SleepAction.STOP_MEDIA,
            SleepAction.CLOSE_APPS,
            SleepAction.TURN_OFF_WIFI,
            SleepAction.TURN_OFF_MOBILE_DATA,
            SleepAction.MUTE_SOUND,
            SleepAction.LOCK_SCREEN
        )
    )
)

@Composable
fun ProfilesScreen(
    schedules: List<NightSchedule>,
    onStartProfile: (Int, Set<SleepAction>) -> Unit,
    onSaveProfile: (NightSchedule) -> Unit,
    onDeleteProfile: (NightSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSmartDetectionEnabled by remember { mutableStateOf(true) }
    var smartWindowText by remember { mutableStateOf("10:00 PM - 05:00 AM") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                ProfilesHeaderCard()
            }

            item {
                SmartSleepDetectionCard(
                    isEnabled = isSmartDetectionEnabled,
                    windowText = smartWindowText,
                    onToggle = { isSmartDetectionEnabled = it }
                )
            }

            item {
                Text(
                    text = "Presets & Profiles",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            items(defaultPresetProfiles, key = { it.id }) { preset ->
                PresetProfileCard(
                    preset = preset,
                    onStart = {
                        onStartProfile(preset.defaultMinutes, preset.actions)
                        Toast.makeText(context, "Started '${preset.title}' (${preset.defaultMinutes}m)", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                AutomationShortcutCard(context = context)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfilesHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Profiles & Automations",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "One-tap presets and smart sleep rules",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartSleepDetectionCard(
    isEnabled: Boolean,
    windowText: String,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
        border = BorderStroke(1.5.dp, Color(0xFFBAE6FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Sleep Detection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LightPrimary
                    ),
                    modifier = Modifier.testTag("smart_sleep_toggle")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Automatically detects when you fall asleep inside your night window ($windowText) based on motion and inactivity, with a 5-minute confirmation warning.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun PresetProfileCard(
    preset: PresetProfile,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LightPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = preset.icon,
                            contentDescription = null,
                            tint = LightPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "${preset.defaultMinutes} min duration • ${preset.actions.size} actions",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LightTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("start_preset_${preset.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun AutomationShortcutCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = BorderStroke(1.dp, LightCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tasker & Google Assistant Integration",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trigger profiles externally from Tasker, Macrodroid, or Voice Shortcuts using broadcast intent:\n• Action: com.example.ACTION_START_PROFILE\n• Extra: EXTRA_PROFILE_NAME = \"Study Night\"",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}
