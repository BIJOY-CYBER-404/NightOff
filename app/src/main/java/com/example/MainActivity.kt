package com.example

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NightOffViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSuccess
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LightWarning
import com.example.ui.theme.NightOffTheme
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
        // Every time the app opens, permission checking runs in the background
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
        mutableIntStateOf(if (intentTab == "history") 2 else 0)
    }

    val activeTimer by viewModel.activeQuickTimer.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val executionLogs by viewModel.executionLogs.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
    val selectedPresetMinutes by viewModel.selectedPresetMinutes.collectAsStateWithLifecycle()
    val customMinutes by viewModel.customMinutes.collectAsStateWithLifecycle()
    val quickTimerActions by viewModel.quickTimerActions.collectAsStateWithLifecycle()

    val context = LocalContext.current

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

    val scope = rememberCoroutineScope()

    // Check for updates on startup via GitHub Pages version.json
    var pendingUpdateInfo by remember { mutableStateOf<com.example.update.AppUpdateInfo?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val updateChecker = com.example.update.GitHubPagesUpdateChecker.instance
            val info = updateChecker.checkForUpdate()
            if (info.hasUpdate) {
                pendingUpdateInfo = info
            }
        } catch (_: Exception) {}
    }

    val currentUpdate = pendingUpdateInfo
    if (currentUpdate != null) {
        com.example.ui.screens.AppUpdateScreen(
            updateInfo = currentUpdate,
            onDismiss = { pendingUpdateInfo = null }
        )
        return
    }

    // Refresh permissions every time user returns to app from system settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Enforce mandatory permission gate: cannot enter app until required permissions are granted
    if (!permissions.areAllRequiredGranted) {
        com.example.ui.screens.MandatoryPermissionGateScreen(
            permissions = permissions,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onRefreshPermissions = { viewModel.refreshPermissions() },
            onContinue = { viewModel.refreshPermissions() }
        )
        return
    }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightBackground,
        contentWindowInsets = androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = LightPrimary,
                                modifier = Modifier.padding(7.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = LightTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Protected Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LightSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Protected",
                                color = LightSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Update Page Button in TopBar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                                    try {
                                        val info = com.example.update.AppUpdateManager.checkForUpdate()
                                        pendingUpdateInfo = info
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Update check: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(end = 6.dp)
                            .testTag("update_topbar_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Software Update",
                                tint = LightPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update",
                                color = LightPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Developer Button in TopBar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSurfaceVariant,
                        modifier = Modifier
                            .clickable { selectedTab = 3 }
                            .padding(end = 8.dp)
                            .testTag("developer_topbar_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Developer",
                                tint = LightPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dev",
                                color = LightTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = LightSurface,
                tonalElevation = 4.dp
            ) {
                val items = listOf(
                    NavigationItem(0, R.string.nav_timer, Icons.Default.Bedtime, "timer_tab"),
                    NavigationItem(1, R.string.nav_schedules, Icons.Default.Alarm, "schedules_tab"),
                    NavigationItem(2, R.string.nav_history, Icons.Default.History, "history_tab")
                )

                items.forEach { item ->
                    val isSelected = selectedTab == item.index

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = item.index },
                        icon = {
                            Icon(item.icon, contentDescription = stringResource(item.labelRes))
                        },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = LightPrimary,
                            indicatorColor = LightPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
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
                    onCancelTimer = { viewModel.cancelQuickTimer() },
                    onSnoozeTimer = { viewModel.snoozeTimer(it) }
                )
                1 -> SchedulesScreen(
                    schedules = allSchedules,
                    isRootAvailable = permissions.isElevatedAvailable,
                    onToggleSchedule = { sched, enabled -> viewModel.toggleSchedule(sched, enabled) },
                    onSaveSchedule = { sched -> viewModel.saveSchedule(sched) },
                    onDeleteSchedule = { sched -> viewModel.deleteSchedule(sched) },
                    onTestRunNow = { scheduleId -> viewModel.runNow(scheduleId) }
                )
                2 -> HistoryScreen(
                    logs = executionLogs,
                    onClearHistory = { viewModel.clearHistory() }
                )
                3 -> com.example.ui.screens.DeveloperScreen()
            }
        }
    }
}

private data class NavigationItem(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)
