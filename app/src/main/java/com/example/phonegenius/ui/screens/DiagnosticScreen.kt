package com.example.phonegenius.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.engine.CareCatalog
import com.example.phonegenius.data.engine.RulesEngine
import com.example.phonegenius.data.model.*
import com.example.phonegenius.ui.components.*

@Composable
fun DiagnosticScreen(
    snapshot: DeviceSnapshot?,
    thresholds: Thresholds,
    onNavigateToCare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var currentReport by remember {
        mutableStateOf(
            if (snapshot != null) RulesEngine.runDiagnostic(snapshot, thresholds, "Analyse complète") else null
        )
    }

    val examples = listOf(
        "Mon téléphone chauffe beaucoup",
        "Ma batterie se vide rapidement",
        "Mon téléphone est devenu lent",
        "Je n'ai presque plus d'espace",
        "Analyse tous les problèmes"
    )

    fun executeDiagnostic(q: String) {
        if (q.isBlank() || snapshot == null) return
        currentReport = RulesEngine.runDiagnostic(snapshot, thresholds, q)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Diagnostic intelligent",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Moteur de règles 100 % local. Il ne conclut que sur des mesures disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            AppCard {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Ex: Mon téléphone chauffe en charge...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_diagnostic_query"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = { executeDiagnostic(queryText) },
                            enabled = queryText.isNotBlank() && snapshot != null,
                            modifier = Modifier.testTag("btn_submit_diagnostic")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Analyser")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(examples) { ex ->
                        SuggestionChip(
                            onClick = {
                                queryText = ex
                                executeDiagnostic(ex)
                            },
                            label = { Text(ex, fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("chip_example_${ex.take(6)}")
                        )
                    }
                }
            }
        }

        val report = currentReport
        if (report != null) {
            item {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeverityBadge(severity = report.worst)
                        SourceBadge(source = report.adapter)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.summary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (report.missing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mesures indisponibles : " + report.missing.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(report.findings) { f ->
                AppCard(modifier = Modifier.testTag("finding_${f.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(f.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SeverityBadge(severity = f.severity)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(f.observed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Confiance : ${f.confidence.labelFr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                    if (f.measures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        f.measures.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(m.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    SourceBadge(source = m.source)
                                }
                            }
                        }
                    }

                    if (f.causes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Causes possibles :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        f.causes.forEach { cause ->
                            Text("• $cause", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (f.solutions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Solutions recommandées :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        f.solutions.forEach { sol ->
                            Text("• $sol", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (f.actionIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            f.actionIds.forEach { actionId ->
                                val action = CareCatalog.getAction(actionId)
                                if (action != null) {
                                    OutlinedButton(
                                        onClick = { onNavigateToCare(action.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("btn_action_${action.id}")
                                    ) {
                                        Text(action.title, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
