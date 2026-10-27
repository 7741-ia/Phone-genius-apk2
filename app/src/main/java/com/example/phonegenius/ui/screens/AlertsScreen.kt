package com.example.phonegenius.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.model.*
import com.example.phonegenius.ui.components.AppCard
import com.example.phonegenius.ui.components.SourceBadge
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    alerts: List<AlertEvent>,
    alertSettings: AlertSettings,
    thresholds: Thresholds,
    onMarkAllRead: () -> Unit,
    onRefreshAlerts: () -> Unit,
    onThresholdsChange: (Thresholds) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Centre d'alertes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Surveillance locale intelligente. Aucun appel réseau ni télémétrie distante.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRefreshAlerts,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_check_alerts_now")
                ) {
                    Text("Vérifier maintenant")
                }
                OutlinedButton(
                    onClick = onMarkAllRead,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_mark_all_read")
                ) {
                    Text("Tout marquer comme lu")
                }
            }
        }

        // Alert List
        item {
            Text("Historique des alertes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (alerts.isEmpty()) {
            item {
                AppCard {
                    Text("Aucune alerte active.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(alerts) { al ->
                AppCard(modifier = Modifier.testTag("alert_item_${al.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!al.read) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(al.category.labelFr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        SourceBadge(source = al.adapter)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(al.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sdf.format(Date(al.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Threshold Configuration
        item {
            AppCard {
                Text("Seuils de déclenchement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Batterie critique : ${thresholds.batteryCritical} %", fontSize = 13.sp)
                    }
                    Slider(
                        value = thresholds.batteryCritical.toFloat(),
                        onValueChange = { onThresholdsChange(thresholds.copy(batteryCritical = it.toInt())) },
                        valueRange = 5f..25f,
                        steps = 4,
                        modifier = Modifier.testTag("slider_battery_critical")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Température max : ${thresholds.tempHigh.toInt()} °C", fontSize = 13.sp)
                    }
                    Slider(
                        value = thresholds.tempHigh,
                        onValueChange = { onThresholdsChange(thresholds.copy(tempHigh = it)) },
                        valueRange = 35f..55f,
                        steps = 4,
                        modifier = Modifier.testTag("slider_temp_high")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stockage critique : ${thresholds.storageFreePctCritical.toInt()} %", fontSize = 13.sp)
                    }
                    Slider(
                        value = thresholds.storageFreePctCritical,
                        onValueChange = { onThresholdsChange(thresholds.copy(storageFreePctCritical = it)) },
                        valueRange = 2f..15f,
                        steps = 3,
                        modifier = Modifier.testTag("slider_storage_critical")
                    )
                }
            }
        }
    }
}
