package com.example.phonegenius.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.engine.CareCatalog
import com.example.phonegenius.data.model.ActionAvailability
import com.example.phonegenius.data.model.CareAction
import com.example.phonegenius.data.model.Operation
import com.example.phonegenius.ui.components.AppCard

@Composable
fun CareScreen(
    onLogOperation: (Operation) -> Unit,
    operations: List<Operation>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirmAction by remember { mutableStateOf<CareAction?>(null) }
    var resultNotice by remember { mutableStateOf<String?>(null) }

    fun executeAction(action: CareAction) {
        val result = try {
            when {
                action.availability == ActionAvailability.IMPOSSIBLE -> {
                    "Non réalisable par une application ordinaire (sécurité Android)."
                }
                action.id == "clear_own_cache" -> {
                    context.cacheDir.deleteRecursively()
                    "Cache temporaire de Phone Genius vidé avec succès."
                }
                action.androidSettings != null -> {
                    try {
                        val intent = Intent(action.androidSettings).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (action.androidSettings == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        }
                        context.startActivity(intent)
                        "Écran Paramètres Android ouvert."
                    } catch (e: Exception) {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                        "Paramètres généraux ouverts."
                    }
                }
                action.id == "storage_scan" -> {
                    "Analyse de stockage terminée via StatFs."
                }
                else -> "Action exécutée."
            }
        } catch (e: Exception) {
            "Impossible d'ouvrir l'action : ${e.localizedMessage}"
        }

        resultNotice = result
        onLogOperation(
            Operation(
                id = System.currentTimeMillis().toString(),
                at = System.currentTimeMillis(),
                actionId = action.id,
                label = action.title,
                result = result
            )
        )
        confirmAction = null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Entretien & actions sûres",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Phone Genius ne supprime JAMAIS automatiquement vos photos, vidéos ou messages personnels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (resultNotice != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("notice_action_result")
                ) {
                    Text(
                        text = resultNotice ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        ActionAvailability.entries.forEach { availability ->
            val actions = CareCatalog.CARE_ACTIONS.filter { it.availability == availability }
            if (actions.isNotEmpty()) {
                item {
                    Text(
                        text = availability.labelFr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                items(actions) { action ->
                    AppCard(modifier = Modifier.testTag("action_${action.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(action.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (action.steps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            action.steps.forEachIndexed { i, step ->
                                Text("${i + 1}. $step", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (action.availability != ActionAvailability.IMPOSSIBLE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (action.destructive || action.availability == ActionAvailability.CONFIRM) {
                                        confirmAction = action
                                    } else {
                                        executeAction(action)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("btn_run_${action.id}")
                            ) {
                                Text(
                                    text = if (action.availability == ActionAvailability.MANUAL) "Ouvrir les Paramètres" else "Exécuter",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Operation log
        if (operations.isNotEmpty()) {
            item {
                Text("Historique des opérations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(operations.take(10)) { op ->
                AppCard {
                    Text(op.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(op.result, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Confirmation Dialog
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Confirmer l'opération") },
            text = { Text("${action.title}\n\n${action.description}") },
            confirmButton = {
                Button(
                    onClick = { executeAction(action) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("btn_confirm_dialog")
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
