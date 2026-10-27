package com.example.phonegenius.data.engine

import com.example.phonegenius.data.model.*
import java.text.Normalizer
import java.util.UUID

object RulesEngine {
    val DEFAULT_THRESHOLDS = Thresholds()

    private val ALL_TOPICS = listOf(
        Topic.HEAT,
        Topic.BATTERY,
        Topic.SLOW,
        Topic.STORAGE,
        Topic.DATA,
        Topic.PERMISSIONS
    )

    private val KEYWORDS = mapOf(
        Topic.HEAT to listOf("chauf", "chaud", "températ", "brûl", "surchauf"),
        Topic.BATTERY to listOf("batter", "décharg", "decharg", "autonomie", "se vide", "charge"),
        Topic.SLOW to listOf("lent", "rame", "bloqu", "freez", "lag", "ram", "mémoire vive", "memoire vive"),
        Topic.STORAGE to listOf("espace", "stockage", "plein", "saturé", "sature", "mémoire pleine", "fichier"),
        Topic.DATA to listOf("donnée", "donnee", "forfait", "mobile", "internet", "data", "giga", "mo "),
        Topic.PERMISSIONS to listOf("autorisation", "permission", "accès", "acces")
    )

    private fun strip(s: String): String {
        val normalized = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun parseIntent(text: String): List<Topic> {
        val t = strip(" $text ")
        val scanRegex = "tous|tout|complet|global|analyse|general|scan".toRegex()
        val allCheck = ALL_TOPICS.any { tp ->
            KEYWORDS[tp]?.any { kw -> t.contains(strip(kw)) } == true
        }
        if (scanRegex.containsMatchIn(t) && !allCheck) {
            return ALL_TOPICS
        }
        if ("tous les problemes|tout analyser|analyse complete|diagnostic complet".toRegex().containsMatchIn(t)) {
            return ALL_TOPICS
        }
        val found = ALL_TOPICS.filter { tp ->
            KEYWORDS[tp]?.any { kw -> t.contains(strip(kw)) } == true
        }
        return if (found.isNotEmpty()) found else ALL_TOPICS
    }

    fun worstOf(list: List<Severity>): Severity {
        if (list.isEmpty()) return Severity.OK
        return list.maxByOrNull { it.rank } ?: Severity.OK
    }

    fun storageFreePct(s: DeviceSnapshot): Float? {
        val total = s.storage.totalGb
        val free = s.storage.freeGb
        return if (total.isAvailable && free.isAvailable && (total.value ?: 0f) > 0f) {
            ((free.value ?: 0f) / (total.value ?: 1f)) * 100f
        } else null
    }

    fun ramFreePct(s: DeviceSnapshot): Float? {
        val total = s.memory.totalMb
        val avail = s.memory.availableMb
        return if (total.isAvailable && avail.isAvailable && (total.value ?: 0L) > 0L) {
            ((avail.value ?: 0L).toFloat() / (total.value ?: 1L).toFloat()) * 100f
        } else null
    }

    private fun <T> measureLine(label: String, metric: Metric<T>, fmt: (T) -> String): MeasureLine {
        val v = metric.value
        val display = if (v != null && metric.source != DataSource.UNAVAILABLE) {
            fmt(v)
        } else {
            "Indisponible${if (metric.note != null) " — ${metric.note}" else ""}"
        }
        return MeasureLine(label, display, metric.source)
    }

    fun runDiagnostic(snapshot: DeviceSnapshot, thresholds: Thresholds, query: String): DiagnosticReport {
        val topics = parseIntent(query)
        val findings = mutableListOf<Finding>()

        for (tp in topics) {
            when (tp) {
                Topic.HEAT -> {
                    val temp = snapshot.battery.temperatureC
                    val charging = snapshot.battery.charging
                    val measures = listOf(
                        measureLine("Température batterie", temp) { "%.1f °C".format(it) },
                        measureLine("En charge", charging) { if (it) "Oui" else "Non" }
                    )
                    if (!temp.isAvailable) {
                        findings.add(
                            Finding(
                                id = "heat",
                                topic = Topic.HEAT,
                                title = "Chauffe : mesure de température indisponible",
                                severity = Severity.LOW,
                                confidence = Confidence.FAIBLE,
                                observed = "Impossible de lire la température sur cette source de données.",
                                measures = measures,
                                causes = listOf("Usage intensif (jeux, vidéo, GPS)", "Charge rapide", "Environnement chaud", "Application en boucle en arrière-plan"),
                                checks = listOf("Utiliser BatteryManager sur l'appareil", "Observer si la chauffe survient en charge ou pendant un usage précis"),
                                solutions = listOf("Retirer la coque pendant la charge", "Éviter l'exposition au soleil", "Fermer les applications lourdes non utilisées"),
                                actionIds = listOf("battery_usage", "battery_saver")
                            )
                        )
                    } else {
                        val tempVal = temp.value ?: 0f
                        val sev = when {
                            tempVal >= thresholds.tempCritical -> Severity.CRITICAL
                            tempVal >= thresholds.tempHigh -> Severity.HIGH
                            else -> Severity.OK
                        }
                        findings.add(
                            Finding(
                                id = "heat",
                                topic = Topic.HEAT,
                                title = if (sev == Severity.OK) "Température normale" else "Température batterie élevée",
                                severity = sev,
                                confidence = Confidence.ELEVEE,
                                observed = "Batterie à %.1f °C (seuil %.0f °C).".format(tempVal, thresholds.tempHigh),
                                measures = measures,
                                causes = if (sev == Severity.OK) emptyList() else listOf("Usage intensif du processeur", "Charge en cours ou charge rapide", "Signal réseau faible", "Température ambiante"),
                                checks = if (sev == Severity.OK) emptyList() else listOf("Consulter l'utilisation de la batterie par application", "Vérifier si la chauffe persiste au repos"),
                                solutions = if (sev == Severity.OK) listOf("Aucune action nécessaire") else listOf("Arrêter jeux / vidéo quelques minutes", "Débrancher le chargeur si la température dépasse 45 °C", "Activer l'économie d'énergie"),
                                actionIds = if (sev == Severity.OK) emptyList() else listOf("battery_usage", "battery_saver")
                            )
                        )
                    }
                }
                Topic.BATTERY -> {
                    val lvl = snapshot.battery.levelPct
                    val drain = snapshot.battery.drainPctPerHour
                    val health = snapshot.battery.health
                    val measures = listOf(
                        measureLine("Niveau", lvl) { "$it %" },
                        measureLine("Décharge estimée", drain) { "%.1f %%/h".format(it) },
                        measureLine("État", health) { it }
                    )
                    if (!lvl.isAvailable) {
                        findings.add(
                            Finding(
                                id = "battery",
                                topic = Topic.BATTERY,
                                title = "Batterie : données indisponibles",
                                severity = Severity.LOW,
                                confidence = Confidence.FAIBLE,
                                observed = "Le niveau de batterie n'est pas accessible.",
                                measures = measures,
                                causes = emptyList(),
                                checks = listOf("Vérifier les permissions système"),
                                solutions = emptyList(),
                                actionIds = listOf("battery_usage")
                            )
                        )
                    } else {
                        val lvlVal = lvl.value ?: 100
                        val lowSev = when {
                            lvlVal <= thresholds.batteryCritical -> Severity.CRITICAL
                            lvlVal <= thresholds.batteryLow -> Severity.HIGH
                            else -> Severity.OK
                        }
                        val fast = drain.isAvailable && (drain.value ?: 0f) >= thresholds.drainPctPerHourHigh
                        val sev = worstOf(listOf(lowSev, if (fast) Severity.MEDIUM else Severity.OK))
                        findings.add(
                            Finding(
                                id = "battery",
                                topic = Topic.BATTERY,
                                title = when {
                                    sev == Severity.OK -> "Batterie en bon état"
                                    lowSev != Severity.OK -> "Batterie faible"
                                    else -> "Décharge rapide"
                                },
                                severity = sev,
                                confidence = if (drain.isAvailable) Confidence.ELEVEE else Confidence.MOYENNE,
                                observed = "Niveau $lvlVal %" + if (drain.isAvailable) ", décharge ≈ %.1f %%/h".format(drain.value ?: 0f) else " (vitesse de décharge non mesurable)",
                                measures = measures,
                                causes = if (sev == Severity.OK) emptyList() else listOf("Luminosité élevée", "Applications actives en arrière-plan", "Localisation GPS permanente", "Réseau mobile faible"),
                                checks = if (sev == Severity.OK) emptyList() else listOf("Ouvrir l'utilisation batterie par application"),
                                solutions = if (sev == Severity.OK) listOf("Aucune action nécessaire") else listOf("Activer l'économie d'énergie", "Réduire la luminosité / passer en thème sombre", "Restreindre l'arrière-plan des apps"),
                                actionIds = if (sev == Severity.OK) emptyList() else listOf("battery_saver", "battery_usage", "brightness")
                            )
                        )
                    }
                }
                Topic.SLOW -> {
                    val pct = ramFreePct(snapshot)
                    val sp = storageFreePct(snapshot)
                    val measures = listOf(
                        measureLine("RAM totale", snapshot.memory.totalMb) { "%.1f Go".format(it.toFloat() / 1024f) },
                        measureLine("RAM disponible", snapshot.memory.availableMb) { "%.2f Go".format(it.toFloat() / 1024f) }
                    )
                    if (pct == null) {
                        findings.add(
                            Finding(
                                id = "slow",
                                topic = Topic.SLOW,
                                title = "Lenteur : mémoire disponible inconnue",
                                severity = Severity.LOW,
                                confidence = Confidence.FAIBLE,
                                observed = "La RAM disponible n'est pas lisible sur cette source.",
                                measures = measures,
                                causes = listOf("Trop d'applications ouvertes", "Stockage presque plein", "Mise à jour système en attente"),
                                checks = listOf("Vérifier l'espace de stockage", "Redémarrer le téléphone"),
                                solutions = listOf("Redémarrer", "Désinstaller les applications inutilisées"),
                                actionIds = listOf("restart", "uninstall_apps")
                            )
                        )
                    } else {
                        val sev = when {
                            pct < thresholds.ramFreePctLow / 2f -> Severity.HIGH
                            pct < thresholds.ramFreePctLow -> Severity.MEDIUM
                            sp != null && sp < thresholds.storageFreePctLow -> Severity.MEDIUM
                            else -> Severity.OK
                        }
                        findings.add(
                            Finding(
                                id = "slow",
                                topic = Topic.SLOW,
                                title = if (sev == Severity.OK) "Mémoire suffisante" else "Mémoire vive saturée",
                                severity = sev,
                                confidence = Confidence.MOYENNE,
                                observed = "%.0f %% de RAM disponible%s".format(pct, if (sp != null) ", %.0f %% de stockage libre".format(sp) else ""),
                                measures = measures,
                                causes = if (sev == Severity.OK) emptyList() else listOf("Nombreuses applications en mémoire", "Application gourmande ou défectueuse", "Stockage trop rempli ralentissant le système"),
                                checks = if (sev == Severity.OK) emptyList() else listOf("Identifier les apps les plus utilisées (accès aux statistiques d'usage requis)"),
                                solutions = if (sev == Severity.OK) listOf("Aucune action nécessaire") else listOf("Redémarrer le téléphone", "Désinstaller ou désactiver les apps inutilisées", "Libérer de l'espace de stockage"),
                                actionIds = if (sev == Severity.OK) emptyList() else listOf("restart", "uninstall_apps", "storage_settings")
                            )
                        )
                    }
                }
                Topic.STORAGE -> {
                    val pct = storageFreePct(snapshot)
                    val measures = listOf(
                        measureLine("Stockage total", snapshot.storage.totalGb) { "%.0f Go".format(it) },
                        measureLine("Stockage libre", snapshot.storage.freeGb) { "%.1f Go".format(it) }
                    )
                    if (pct == null) {
                        findings.add(
                            Finding(
                                id = "storage",
                                topic = Topic.STORAGE,
                                title = "Stockage : mesure indisponible",
                                severity = Severity.LOW,
                                confidence = Confidence.FAIBLE,
                                observed = "Impossible d'accéder aux statistiques de stockage StatFs.",
                                measures = measures,
                                causes = emptyList(),
                                checks = listOf("Consulter Paramètres › Stockage"),
                                solutions = emptyList(),
                                actionIds = listOf("storage_settings")
                            )
                        )
                    } else {
                        val sev = when {
                            pct <= thresholds.storageFreePctCritical -> Severity.CRITICAL
                            pct <= thresholds.storageFreePctLow -> Severity.HIGH
                            else -> Severity.OK
                        }
                        findings.add(
                            Finding(
                                id = "storage",
                                topic = Topic.STORAGE,
                                title = if (sev == Severity.OK) "Espace de stockage suffisant" else "Stockage presque saturé",
                                severity = sev,
                                confidence = Confidence.ELEVEE,
                                observed = "%.1f %% d'espace libre (seuil %.0f %%)".format(pct, thresholds.storageFreePctLow),
                                measures = measures,
                                causes = if (sev == Severity.OK) emptyList() else listOf("Photos et vidéos volumineuses", "Caches d'applications", "Fichiers téléchargés oubliés", "Médias de messagerie (WhatsApp…)"),
                                checks = if (sev == Severity.OK) emptyList() else listOf("Analyser les gros fichiers accessibles", "Trier les médias de messagerie"),
                                solutions = if (sev == Severity.OK) listOf("Aucune action nécessaire") else listOf("Supprimer vous-même les fichiers inutiles après vérification", "Vider le cache des apps depuis les Paramètres", "Sauvegarder les photos ailleurs"),
                                actionIds = if (sev == Severity.OK) emptyList() else listOf("large_files", "clear_own_cache", "storage_settings")
                            )
                        )
                    }
                }
                Topic.DATA -> {
                    val today = snapshot.data.mobileTodayMb
                    val avg = snapshot.data.avgDailyMb
                    val measures = listOf(
                        measureLine("Données mobiles aujourd'hui", today) { "$it Mo" },
                        measureLine("Moyenne quotidienne", avg) { "$it Mo" }
                    )
                    if (!today.isAvailable) {
                        findings.add(
                            Finding(
                                id = "data",
                                topic = Topic.DATA,
                                title = "Données mobiles : statistiques indisponibles",
                                severity = Severity.LOW,
                                confidence = Confidence.FAIBLE,
                                observed = "Les statistiques réseau exigent l'accès spécial aux données d'utilisation.",
                                measures = measures,
                                causes = emptyList(),
                                checks = listOf("Accorder l'accès aux données d'utilisation", "Consulter Paramètres › Réseau › Utilisation des données"),
                                solutions = listOf("Activer l'économiseur de données"),
                                actionIds = listOf("data_saver", "usage_access")
                            )
                        )
                    } else {
                        val todayVal = today.value ?: 0L
                        val avgVal = avg.value ?: 0L
                        val spike = avg.isAvailable && avgVal > 0 && todayVal > avgVal * 3
                        val sev = when {
                            todayVal >= thresholds.dataDailyMbHigh -> Severity.HIGH
                            spike -> Severity.MEDIUM
                            else -> Severity.OK
                        }
                        findings.add(
                            Finding(
                                id = "data",
                                topic = Topic.DATA,
                                title = if (sev == Severity.OK) "Consommation de données normale" else "Consommation de données élevée",
                                severity = sev,
                                confidence = if (avg.isAvailable) Confidence.ELEVEE else Confidence.MOYENNE,
                                observed = "$todayVal Mo aujourd'hui" + if (avg.isAvailable) " (moyenne $avgVal Mo)" else "",
                                measures = measures,
                                causes = if (sev == Severity.OK) emptyList() else listOf("Lecture vidéo en haute qualité", "Mises à jour automatiques sur réseau mobile", "Sauvegarde cloud des photos", "Application en arrière-plan"),
                                checks = if (sev == Severity.OK) emptyList() else listOf("Voir la consommation par application"),
                                solutions = if (sev == Severity.OK) listOf("Aucune action nécessaire") else listOf("Activer l'économiseur de données", "Limiter les mises à jour au Wi-Fi", "Réduire la qualité vidéo"),
                                actionIds = if (sev == Severity.OK) emptyList() else listOf("data_saver", "data_usage")
                            )
                        )
                    }
                }
                Topic.PERMISSIONS -> {
                    val p = snapshot.permissions
                    val measures = listOf(
                        measureLine("Notifications", p.notifications) { if (it) "Accordées" else "Refusées" },
                        measureLine("Accès statistiques d'usage", p.usageAccess) { if (it) "Accordé" else "Non accordé" },
                        measureLine("Microphone", p.microphone) { if (it) "Accordé" else "Refusé" }
                    )
                    val missing = mutableListOf<String>()
                    if (p.notifications.isAvailable && p.notifications.value == false) missing.add("notifications")
                    if (p.usageAccess.isAvailable && p.usageAccess.value == false) missing.add("statistiques d'usage")
                    val sev = if (missing.isNotEmpty()) Severity.MEDIUM else Severity.OK
                    findings.add(
                        Finding(
                            id = "permissions",
                            topic = Topic.PERMISSIONS,
                            title = if (missing.isNotEmpty()) "Autorisations importantes manquantes" else "Autorisations en ordre",
                            severity = sev,
                            confidence = Confidence.ELEVEE,
                            observed = if (missing.isNotEmpty()) "Non accordées : ${missing.joinToString(", ")}." else "Aucune autorisation essentielle manquante détectée.",
                            measures = measures,
                            causes = emptyList(),
                            checks = emptyList(),
                            solutions = if (missing.isNotEmpty()) listOf("Accorder les autorisations depuis les Paramètres en toute confiance") else emptyList(),
                            actionIds = if (missing.isNotEmpty()) listOf("notif_settings", "usage_access") else emptyList()
                        )
                    )
                }
                Topic.CROSS -> {}
            }
        }

        // Diagnostic croisé
        val abnormalMeasured = findings.filter { it.severity != Severity.OK && it.confidence != Confidence.FAIBLE }
        if (abnormalMeasured.size >= 2) {
            val conf = if (abnormalMeasured.size >= 3) Confidence.MOYENNE else Confidence.FAIBLE
            findings.add(
                0,
                Finding(
                    id = "cross",
                    topic = Topic.CROSS,
                    title = "Diagnostic croisé : plusieurs anomalies simultanées",
                    severity = worstOf(abnormalMeasured.map { it.severity }),
                    confidence = conf,
                    observed = "Indicateurs anormaux : " + abnormalMeasured.joinToString(" ; ") { it.title.lowercase() },
                    measures = abnormalMeasured.mapNotNull { it.measures.firstOrNull() },
                    causes = listOf("Hypothèse : une application ou un processus consomme des ressources de façon anormale — cause NON vérifiée"),
                    checks = listOf("Consulter l'utilisation de la batterie et des données par application", "Redémarrer puis comparer les mesures"),
                    solutions = listOf("Redémarrer le téléphone", "Mettre à jour les applications et le système"),
                    actionIds = listOf("battery_usage", "restart")
                )
            )
        }

        val missing = findings.flatMap { f ->
            f.measures.filter { it.source == DataSource.UNAVAILABLE }.map { it.label }
        }.distinct()

        val worst = worstOf(findings.map { it.severity })
        val problems = findings.filter { it.severity != Severity.OK && it.topic != Topic.CROSS }
        val summary = when {
            problems.isNotEmpty() -> "${problems.size} point(s) à surveiller : " + problems.joinToString(", ") { it.title.lowercase() } + "."
            missing.size == findings.sumOf { it.measures.size } -> "Je n'ai pas assez de mesures pour conclure."
            else -> "Aucun problème détecté sur les mesures disponibles."
        }

        return DiagnosticReport(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            query = query,
            topics = topics,
            findings = findings,
            missing = missing,
            summary = summary,
            worst = worst,
            adapter = snapshot.adapter
        )
    }
}
