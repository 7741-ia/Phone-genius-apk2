package com.example.phonegenius.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.model.DemoScenario
import com.example.phonegenius.data.model.ViewMode
import com.example.phonegenius.ui.components.AppCard

@Composable
fun SettingsScreen(
    useDemo: Boolean,
    onUseDemoChange: (Boolean) -> Unit,
    demoScenario: DemoScenario,
    onDemoScenarioChange: (DemoScenario) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var resetNotice by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Réglages & Confidentialité",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toutes vos données restent exclusivement sur cet appareil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (resetNotice != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resetNotice ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Data Source
        item {
            AppCard {
                Text("Source des données", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseDemoChange(false) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !useDemo,
                        onClick = { onUseDemoChange(false) },
                        modifier = Modifier.testTag("radio_native_mode")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Capteurs Android natifs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Données réelles lues via BatteryManager, StatFs et ActivityManager.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseDemoChange(true) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = useDemo,
                        onClick = { onUseDemoChange(true) },
                        modifier = Modifier.testTag("radio_demo_mode")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Mode Démonstration (fictif)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Simule différents profils de surchauffe ou saturation.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (useDemo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Scénario simulé :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    DemoScenario.entries.forEach { sc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDemoScenarioChange(sc) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = demoScenario == sc,
                                onClick = { onDemoScenarioChange(sc) },
                                modifier = Modifier.testTag("radio_scenario_${sc.name.lowercase()}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sc.labelFr, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Display Settings
        item {
            AppCard {
                Text("Affichage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thème sombre", fontSize = 14.sp)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChange,
                        modifier = Modifier.testTag("switch_dark_theme")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Mode de présentation :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { onViewModeChange(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("chip_viewmode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Privacy & Local reset
        item {
            AppCard {
                Text("Confidentialité & Sécurité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Aucun compte requis, aucune publicité, aucun traceur.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Aucune information personnelle n'est téléversée.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Les diagnostics et historiques sont stockés uniquement sur cet appareil.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_reset_history")
                ) {
                    Text("Effacer l'historique local")
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Effacer toutes les données ?") },
            text = { Text("Diagnostics, historiques et alertes enregistrés localement seront supprimés.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showResetDialog = false
                        resetNotice = "Historique local effacé avec succès."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Effacer définitivement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
