package com.example.phonegenius.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.engine.RulesEngine
import com.example.phonegenius.data.model.*
import com.example.phonegenius.ui.components.*
import com.example.phonegenius.ui.theme.AccentCyan
import com.example.phonegenius.ui.theme.PrimaryBlue

@Composable
fun DashboardScreen(
    snapshot: DeviceSnapshot?,
    reports: List<DiagnosticReport>,
    alerts: List<AlertEvent>,
    thresholds: Thresholds,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
        }
        return
    }

    val b = snapshot.battery
    val lvl = b.levelPct.value
    val sp = RulesEngine.storageFreePct(snapshot)
    val rp = RulesEngine.ramFreePct(snapshot)
    val temp = b.temperatureC.value
    val liveReport = RulesEngine.runDiagnostic(snapshot, thresholds, "analyse complète")
    val unreadAlerts = alerts.count { !it.read }
    val recos = liveReport.findings.filter { it.severity != Severity.OK && it.topic != Topic.CROSS }.take(3)

    val sevBat = when {
        lvl == null -> Severity.LOW
        lvl <= thresholds.batteryCritical -> Severity.CRITICAL
        lvl <= thresholds.batteryLow -> Severity.HIGH
        else -> Severity.OK
    }
    val sevSto = when {
        sp == null -> Severity.LOW
        sp <= thresholds.storageFreePctCritical -> Severity.CRITICAL
        sp <= thresholds.storageFreePctLow -> Severity.HIGH
        else -> Severity.OK
    }
    val sevRam = when {
        rp == null -> Severity.LOW
        rp <= thresholds.ramFreePctLow -> Severity.MEDIUM
        else -> Severity.OK
    }
    val sevTmp = when {
        temp == null -> Severity.LOW
        temp >= thresholds.tempCritical -> Severity.CRITICAL
        temp >= thresholds.tempHigh -> Severity.HIGH
        else -> Severity.OK
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tableau de bord",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("btn_refresh")) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("hero_status_card")
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(PrimaryBlue.copy(alpha = 0.2f), AccentCyan.copy(alpha = 0.1f))))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeverityBadge(severity = liveReport.worst)
                        SourceBadge(source = snapshot.adapter)
                    }
                    Text(text = liveReport.summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = onNavigateToDiagnostic,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_diagnose_now")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnostiquer")
                    }
                }
            }
        }

        AppCard {
            Text("Indicateurs système", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                CircularGauge(lvl?.toFloat(), 100f, "Batterie", if (lvl == null) "—" else "$lvl%", sevBat)
                CircularGauge(sp, 100f, "Stockage", if (sp == null) "—" else "%.0f%%".format(sp), sevSto)
                CircularGauge(rp, 100f, "RAM", if (rp == null) "—" else "%.0f%%".format(rp), sevRam)
                CircularGauge(temp, 60f, "Chauffe", if (temp == null) "—" else "%.0f°C".format(temp), sevTmp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f).testTag("card_alerts_stat")) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Alertes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$unreadAlerts non lue(s)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = onNavigateToAlerts, modifier = Modifier.testTag("btn_see_alerts")) {
                        Text("Voir →", fontSize = 11.sp)
                    }
                }
            }
            Card(modifier = Modifier.weight(1f).testTag("card_battery_stat")) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Batterie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (b.charging.value == true) "En charge" else "Sur batterie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("État: ${b.health.value ?: "N/A"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (recos.isNotEmpty()) {
            AppCard {
                Text("Recommandations prioritaires", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                recos.forEach { f ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (f.solutions.isNotEmpty()) {
                                Text(f.solutions.first(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        SeverityBadge(severity = f.severity)
                    }
                }
            }
        }
    }
}
