package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NightOffViewModel
import com.example.ui.components.OnboardingDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightOffTheme
import com.example.ui.theme.NightPrimary
import com.example.ui.theme.NightSecondary
import com.example.ui.theme.NightSuccess
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.NightTextPrimary
import com.example.ui.theme.NightTextSecondary
import com.example.ui.theme.NightWarning
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: NightOffViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("nightoff_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_lang", "en") ?: "en"
        LocaleHelper.applyLanguage(this, savedLang)
        viewModel.setLanguage(savedLang)

        setContent {
            NightOffTheme {
                MainScreen(
                    viewModel = viewModel,
                    intentTab = intent?.getStringExtra("EXTRA_NAV_TAB"),
                    onChangeLanguage = { newLang ->
                        prefs.edit().putString("selected_lang", newLang).apply()
                        viewModel.setLanguage(newLang)
                        LocaleHelper.applyLanguage(this, newLang)
                        recreate()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check all permissions every time the app opens as required
        viewModel.refreshPermissions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NightOffViewModel,
    intentTab: String?,
    onChangeLanguage: (String) -> Unit
) {
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (intentTab == "history") 3 else 0)
    }

    val activeTimer by viewModel.activeQuickTimer.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val executionLogs by viewModel.executionLogs.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
    val selectedPresetMinutes by viewModel.selectedPresetMinutes.collectAsStateWithLifecycle()
    val customMinutes by viewModel.customMinutes.collectAsStateWithLifecycle()
    val quickTimerActions by viewModel.quickTimerActions.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    // Permission Launchers
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    // First-launch onboarding check
    var showOnboarding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // If basic permissions are missing, show onboarding
        if (!permissions.isReadyForBasicSleep) {
            showOnboarding = true
        }
    }

    // Back handler to navigate back to Timer tab if on subtab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NightPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = NightPrimary,
                                modifier = Modifier.padding(7.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NightTextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NightTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Readiness Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (permissions.isReadyForBasicSleep) NightSuccess.copy(alpha = 0.15f) else NightWarning.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable { selectedTab = 2 }
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (permissions.isReadyForBasicSleep) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (permissions.isReadyForBasicSleep) NightSuccess else NightWarning,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (permissions.isReadyForBasicSleep) "Ready" else "Checklist",
                                color = if (permissions.isReadyForBasicSleep) NightSuccess else NightWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // In-app Language Switcher (EN / বাংলা)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NightSurfaceVariant,
                        modifier = Modifier
                            .clickable {
                                val next = if (currentLang == "en") "bn" else "en"
                                onChangeLanguage(next)
                            }
                            .padding(end = 8.dp)
                            .testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = NightPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLang == "en") "বাংলা" else "EN",
                                color = NightTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NightSurface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItem(0, R.string.nav_timer, Icons.Default.Bedtime, "timer_tab"),
                    NavigationItem(1, R.string.nav_schedules, Icons.Default.Alarm, "schedules_tab"),
                    NavigationItem(2, R.string.nav_permissions, Icons.Default.Security, "permissions_tab"),
                    NavigationItem(3, R.string.nav_history, Icons.Default.History, "history_tab")
                )

                items.forEach { item ->
                    val isSelected = selectedTab == item.index
                    val hasWarning = item.index == 2 && !permissions.isReadyForBasicSleep

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = item.index },
                        icon = {
                            if (hasWarning) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = NightWarning)
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = stringResource(item.labelRes))
                                }
                            } else {
                                Icon(item.icon, contentDescription = stringResource(item.labelRes))
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF082F49),
                            selectedTextColor = NightPrimary,
                            indicatorColor = NightPrimary,
                            unselectedIconColor = NightTextSecondary,
                            unselectedTextColor = NightTextSecondary
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TimerScreen(
                    activeTimer = activeTimer,
                    permissions = permissions,
                    selectedPresetMinutes = selectedPresetMinutes,
                    customMinutes = customMinutes,
                    selectedActions = quickTimerActions,
                    onSelectPreset = { viewModel.selectPresetMinutes(it) },
                    onCustomMinutesChange = { viewModel.setCustomMinutes(it) },
                    onToggleAction = { viewModel.toggleQuickAction(it) },
                    onStartTimer = { viewModel.startQuickTimer() },
                    onCancelTimer = { viewModel.cancelQuickTimer() }
                )
                1 -> SchedulesScreen(
                    schedules = allSchedules,
                    isRootAvailable = permissions.isRooted,
                    onToggleSchedule = { sched, enabled -> viewModel.toggleSchedule(sched, enabled) },
                    onSaveSchedule = { sched -> viewModel.saveSchedule(sched) },
                    onDeleteSchedule = { sched -> viewModel.deleteSchedule(sched) },
                    onTestRunNow = { scheduleId -> viewModel.runNow(scheduleId) }
                )
                2 -> PermissionsScreen(
                    permissions = permissions,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestBluetoothPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                    onRefresh = { viewModel.refreshPermissions() }
                )
                3 -> HistoryScreen(
                    logs = executionLogs,
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
        }
    }

    if (showOnboarding) {
        OnboardingDialog(
            onDismiss = { showOnboarding = false },
            onOpenPermissions = {
                showOnboarding = false
                selectedTab = 2
            }
        )
    }
}

private data class NavigationItem(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)
