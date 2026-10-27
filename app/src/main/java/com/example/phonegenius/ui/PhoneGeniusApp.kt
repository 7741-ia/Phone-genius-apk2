package com.example.phonegenius.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phonegenius.R
import com.example.phonegenius.data.model.DataSource
import com.example.phonegenius.service.VoiceAssistant
import com.example.phonegenius.ui.screens.*
import com.example.phonegenius.ui.theme.AccentCyan
import com.example.phonegenius.ui.theme.PhoneGeniusTheme
import com.example.phonegenius.ui.viewmodel.PhoneGeniusViewModel

enum class Screen(val titleRes: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Default.Speed),
    DIAGNOSTIC(R.string.nav_diagnostic, Icons.Default.Build),
    VOICE(R.string.nav_voice, Icons.Default.Mic),
    ALERTS(R.string.nav_alerts, Icons.Default.Notifications),
    CARE(R.string.nav_care, Icons.Default.CleaningServices),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneGeniusApp(
    viewModel: PhoneGeniusViewModel = viewModel()
) {
    val context = LocalContext.current
    val voiceAssistant = remember { VoiceAssistant(context) }

    DisposableEffect(Unit) {
        onDispose { voiceAssistant.shutdown() }
    }

    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    PhoneGeniusTheme(darkTheme = uiState.isDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "Phone",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Genius",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = AccentCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        if (uiState.useDemo) {
                            SuggestionChip(
                                onClick = { currentScreen = Screen.SETTINGS },
                                label = { Text("DÉMO", fontSize = 10.sp) },
                                modifier = Modifier.testTag("chip_topbar_demo")
                            )
                        } else if (uiState.snapshot != null && uiState.snapshot?.adapter == DataSource.NATIVE) {
                            SuggestionChip(
                                onClick = { currentScreen = Screen.SETTINGS },
                                label = { Text("RÉEL", fontSize = 10.sp) },
                                modifier = Modifier.testTag("chip_topbar_native")
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val unreadCount = uiState.alerts.count { !it.read }
                    Screen.entries.forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.ALERTS && unreadCount > 0) {
                                            Badge { Text("$unreadCount") }
                                        }
                                    }
                                ) {
                                    Icon(screen.icon, contentDescription = stringResource(screen.titleRes))
                                }
                            },
                            label = { Text(stringResource(screen.titleRes), fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        snapshot = uiState.snapshot,
                        reports = uiState.reports,
                        alerts = uiState.alerts,
                        thresholds = uiState.thresholds,
                        viewMode = uiState.viewMode,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onRefresh = { viewModel.refreshSnapshot() },
                        onNavigateToDiagnostic = { currentScreen = Screen.DIAGNOSTIC },
                        onNavigateToAlerts = { currentScreen = Screen.ALERTS }
                    )
                    Screen.DIAGNOSTIC -> DiagnosticScreen(
                        snapshot = uiState.snapshot,
                        thresholds = uiState.thresholds,
                        onNavigateToCare = { currentScreen = Screen.CARE }
                    )
                    Screen.VOICE -> VoiceScreen(
                        snapshot = uiState.snapshot,
                        thresholds = uiState.thresholds,
                        voiceAssistant = voiceAssistant
                    )
                    Screen.ALERTS -> AlertsScreen(
                        alerts = uiState.alerts,
                        alertSettings = uiState.alertSettings,
                        thresholds = uiState.thresholds,
                        onMarkAllRead = { viewModel.markAlertsAsRead() },
                        onRefreshAlerts = { viewModel.refreshSnapshot() },
                        onThresholdsChange = { viewModel.updateThresholds(it) }
                    )
                    Screen.CARE -> CareScreen(
                        onLogOperation = { viewModel.logOperation(it) },
                        operations = uiState.operations
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        useDemo = uiState.useDemo,
                        onUseDemoChange = { viewModel.setDemoMode(it) },
                        demoScenario = uiState.demoScenario,
                        onDemoScenarioChange = { viewModel.setDemoMode(uiState.useDemo, it) },
                        isDarkTheme = uiState.isDarkTheme,
                        onDarkThemeChange = { viewModel.setDarkTheme(it) },
                        viewMode = uiState.viewMode,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onClearAllHistory = { viewModel.clearAllData() }
                    )
                }
            }
        }
    }
}
