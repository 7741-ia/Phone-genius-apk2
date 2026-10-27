package com.example.phonegenius.data.engine

import com.example.phonegenius.data.model.*
import java.util.UUID

object AlertsEngine {
    data class Candidate(
        val category: AlertCategory,
        val priority: AlertPriority,
        val message: String
    )

    fun detectAlerts(s: DeviceSnapshot, t: Thresholds): List<Candidate> {
        val out = mutableListOf<Candidate>()
        val lvl = s.battery.levelPct.value
        val charging = s.battery.charging.value == true

        if (lvl != null && !charging) {
            if (lvl <= t.batteryCritical) {
                out.add(
                    Candidate(
                        AlertCategory.BATTERY_LOW,
                        AlertPriority.CRITIQUE,
                        "Attention, votre batterie est à $lvl %. Branchez votre chargeur."
                    )
                )
            } else if (lvl <= t.batteryLow) {
                out.add(
                    Candidate(
                        AlertCategory.BATTERY_LOW,
                        AlertPriority.ELEVEE,
                        "Attention, votre batterie est à $lvl %. Vous pouvez activer le mode économie d'énergie."
                    )
                )
            }
        }

        val drain = s.battery.drainPctPerHour.value
        if (drain != null && drain >= t.drainPctPerHourHigh) {
            out.add(
                Candidate(
                    AlertCategory.BATTERY_DRAIN,
                    AlertPriority.MOYENNE,
                    "Décharge rapide estimée à %.1f %% par heure.".format(drain)
                )
            )
        }

        val sp = RulesEngine.storageFreePct(s)
        if (sp != null && sp <= t.storageFreePctLow) {
            out.add(
                Candidate(
                    AlertCategory.STORAGE,
                    if (sp <= t.storageFreePctCritical) AlertPriority.CRITIQUE else AlertPriority.ELEVEE,
                    "Stockage presque saturé : %.1f %% libre.".format(sp)
                )
            )
        }

        val rp = RulesEngine.ramFreePct(s)
        if (rp != null && rp <= t.ramFreePctLow) {
            out.add(
                Candidate(
                    AlertCategory.MEMORY,
                    AlertPriority.MOYENNE,
                    "Mémoire disponible faible : %.0f %%.".format(rp)
                )
            )
        }

        val temp = s.battery.temperatureC.value
        if (temp != null && temp >= t.tempHigh) {
            out.add(
                Candidate(
                    AlertCategory.TEMPERATURE,
                    if (temp >= t.tempCritical) AlertPriority.CRITIQUE else AlertPriority.ELEVEE,
                    "Température de batterie élevée : %.1f °C.".format(temp)
                )
            )
        }

        val data = s.data.mobileTodayMb.value
        if (data != null && data >= t.dataDailyMbHigh) {
            out.add(
                Candidate(
                    AlertCategory.DATA,
                    AlertPriority.MOYENNE,
                    "Forte consommation de données aujourd'hui : $data Mo."
                )
            )
        }

        if (s.permissions.notifications.value == false) {
            out.add(
                Candidate(
                    AlertCategory.PERMISSION,
                    AlertPriority.FAIBLE,
                    "Les notifications sont désactivées : vous ne recevrez pas les alertes."
                )
            )
        }

        return out
    }

    fun evaluateAlerts(
        s: DeviceSnapshot,
        t: Thresholds,
        settings: AlertSettings,
        history: List<AlertEvent>,
        now: Long = System.currentTimeMillis()
    ): List<AlertEvent> {
        val cooldown = settings.cooldownMinutes * 60_000L
        return detectAlerts(s, t)
            .filter { settings.enabled[it.category] != false }
            .filter { candidate ->
                history.none { h ->
                    h.category == candidate.category && (now - h.createdAt) < cooldown
                }
            }
            .mapIndexed { index, candidate ->
                AlertEvent(
                    id = "${now.toString(36)}-$index",
                    category = candidate.category,
                    priority = candidate.priority,
                    message = candidate.message,
                    createdAt = now,
                    adapter = s.adapter,
                    read = false
                )
            }
    }
}
