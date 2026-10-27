package com.example.phonegenius.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.phonegenius.data.engine.AlertsEngine
import com.example.phonegenius.data.engine.RulesEngine
import com.example.phonegenius.data.local.PhoneGeniusStorage
import com.example.phonegenius.data.model.*
import com.example.phonegenius.data.repository.DeviceSnapshotRepository
import com.example.phonegenius.service.AlertWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class PhoneGeniusUiState(
    val snapshot: DeviceSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val alerts: List<AlertEvent> = emptyList(),
    val reports: List<DiagnosticReport> = emptyList(),
    val operations: List<Operation> = emptyList(),
    val voiceLog: List<VoiceExchange> = emptyList(),
    val thresholds: Thresholds = Thresholds(),
    val alertSettings: AlertSettings = AlertSettings(),
    val useDemo: Boolean = false,
    val demoScenario: DemoScenario = DemoScenario.NORMAL,
    val isDarkTheme: Boolean = true,
    val viewMode: ViewMode = ViewMode.SIMPLE
)

class PhoneGeniusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceSnapshotRepository(application)
    private val storage = PhoneGeniusStorage(application)

    private val _uiState = MutableStateFlow(
        PhoneGeniusUiState(
            useDemo = storage.useDemo,
            demoScenario = storage.demoScenario,
            isDarkTheme = storage.isDarkTheme,
            viewMode = storage.viewMode,
            thresholds = storage.loadThresholds(),
            alerts = storage.loadAlerts()
        )
    )
    val uiState: StateFlow<PhoneGeniusUiState> = _uiState.asStateFlow()

    init {
        refreshSnapshot()
        scheduleBackgroundAlerts()
    }

    fun refreshSnapshot() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val snap = repository.readSnapshot(
                    useDemo = _uiState.value.useDemo,
                    scenario = _uiState.value.demoScenario
                )
                val currentAlerts = _uiState.value.alerts
                val freshAlerts = AlertsEngine.evaluateAlerts(
                    s = snap,
                    t = _uiState.value.thresholds,
                    settings = _uiState.value.alertSettings,
                    history = currentAlerts
                )
                val mergedAlerts = if (freshAlerts.isNotEmpty()) {
                    (freshAlerts + currentAlerts).take(50)
                } else currentAlerts

                storage.saveAlerts(mergedAlerts)

                _uiState.value = _uiState.value.copy(
                    snapshot = snap,
                    isLoading = false,
                    alerts = mergedAlerts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Erreur de lecture système"
                )
            }
        }
    }

    fun runDiagnostic(query: String): DiagnosticReport? {
        val snap = _uiState.value.snapshot ?: return null
        val report = RulesEngine.runDiagnostic(snap, _uiState.value.thresholds, query)
        val updatedReports = listOf(report) + _uiState.value.reports
        _uiState.value = _uiState.value.copy(reports = updatedReports)
        return report
    }

    fun markAlertsAsRead() {
        val updated = _uiState.value.alerts.map { it.copy(read = true) }
        storage.saveAlerts(updated)
        _uiState.value = _uiState.value.copy(alerts = updated)
    }

    fun updateThresholds(thresholds: Thresholds) {
        storage.saveThresholds(thresholds)
        _uiState.value = _uiState.value.copy(thresholds = thresholds)
        refreshSnapshot()
    }

    fun setDemoMode(useDemo: Boolean, scenario: DemoScenario = _uiState.value.demoScenario) {
        storage.useDemo = useDemo
        storage.demoScenario = scenario
        _uiState.value = _uiState.value.copy(useDemo = useDemo, demoScenario = scenario)
        refreshSnapshot()
    }

    fun setDarkTheme(dark: Boolean) {
        storage.isDarkTheme = dark
        _uiState.value = _uiState.value.copy(isDarkTheme = dark)
    }

    fun setViewMode(mode: ViewMode) {
        storage.viewMode = mode
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun logOperation(op: Operation) {
        _uiState.value = _uiState.value.copy(
            operations = listOf(op) + _uiState.value.operations
        )
    }

    fun addVoiceExchange(user: String, reply: String) {
        val exchange = VoiceExchange(
            id = System.currentTimeMillis().toString(),
            user = user,
            reply = reply
        )
        _uiState.value = _uiState.value.copy(
            voiceLog = listOf(exchange) + _uiState.value.voiceLog
        )
    }

    fun clearAllData() {
        storage.clearAll()
        _uiState.value = _uiState.value.copy(
            alerts = emptyList(),
            reports = emptyList(),
            operations = emptyList(),
            voiceLog = emptyList()
        )
        refreshSnapshot()
    }

    private fun scheduleBackgroundAlerts() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "PhoneGeniusPeriodicAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            /* Background worker scheduled */
        }
    }
}
