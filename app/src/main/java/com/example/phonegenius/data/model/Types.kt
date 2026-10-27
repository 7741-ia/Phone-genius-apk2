package com.example.phonegenius.data.model

enum class DataSource(val label: String) {
    DEMO("demo"),
    WEB("web"),
    NATIVE("native"),
    UNAVAILABLE("unavailable");

    companion object {
        fun from(value: String): DataSource = entries.find { it.label == value } ?: UNAVAILABLE
    }
}

data class Metric<T>(
    val value: T?,
    val source: DataSource,
    val note: String? = null
) {
    val isAvailable: Boolean get() = value != null && source != DataSource.UNAVAILABLE
}

data class BatteryInfo(
    val levelPct: Metric<Int>,
    val charging: Metric<Boolean>,
    val temperatureC: Metric<Float>,
    val health: Metric<String>,
    val drainPctPerHour: Metric<Float>
)

data class MemoryInfo(
    val totalMb: Metric<Long>,
    val availableMb: Metric<Long>
)

data class StorageInfo(
    val totalGb: Metric<Float>,
    val freeGb: Metric<Float>
)

data class DataUsageInfo(
    val mobileTodayMb: Metric<Long>,
    val avgDailyMb: Metric<Long>
)

data class PermissionsInfo(
    val notifications: Metric<Boolean>,
    val usageAccess: Metric<Boolean>,
    val microphone: Metric<Boolean>
)

data class DeviceSnapshot(
    val takenAt: Long = System.currentTimeMillis(),
    val adapter: DataSource = DataSource.NATIVE,
    val battery: BatteryInfo,
    val memory: MemoryInfo,
    val storage: StorageInfo,
    val data: DataUsageInfo,
    val permissions: PermissionsInfo
)

enum class Severity(val rank: Int, val labelFr: String) {
    OK(0, "Normal"),
    LOW(1, "Faible"),
    MEDIUM(2, "Moyen"),
    HIGH(3, "Élevé"),
    CRITICAL(4, "Critique")
}

enum class Confidence(val labelFr: String) {
    FAIBLE("faible"),
    MOYENNE("moyenne"),
    ELEVEE("élevée")
}

enum class Topic(val key: String, val labelFr: String) {
    HEAT("heat", "Chauffe & Température"),
    BATTERY("battery", "Batterie & Autonomie"),
    SLOW("slow", "Fluidité & RAM"),
    STORAGE("storage", "Espace de stockage"),
    DATA("data", "Consommation données"),
    PERMISSIONS("permissions", "Autorisations"),
    CROSS("cross", "Diagnostic croisé")
}

data class Thresholds(
    val batteryLow: Int = 20,
    val batteryCritical: Int = 10,
    val storageFreePctLow: Float = 15f,
    val storageFreePctCritical: Float = 5f,
    val ramFreePctLow: Float = 12f,
    val tempHigh: Float = 40f,
    val tempCritical: Float = 45f,
    val dataDailyMbHigh: Long = 1000L,
    val drainPctPerHourHigh: Float = 15f
)

data class MeasureLine(
    val label: String,
    val value: String,
    val source: DataSource
)

data class Finding(
    val id: String,
    val topic: Topic,
    val title: String,
    val severity: Severity,
    val confidence: Confidence,
    val observed: String,
    val measures: List<MeasureLine>,
    val causes: List<String>,
    val checks: List<String>,
    val solutions: List<String>,
    val actionIds: List<String>
)

data class DiagnosticReport(
    val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val query: String,
    val topics: List<Topic>,
    val findings: List<Finding>,
    val missing: List<String>,
    val summary: String,
    val worst: Severity,
    val adapter: DataSource
)

enum class AlertCategory(val key: String, val labelFr: String) {
    BATTERY_LOW("battery_low", "Batterie faible"),
    BATTERY_DRAIN("battery_drain", "Décharge rapide"),
    STORAGE("storage", "Stockage saturé"),
    MEMORY("memory", "Mémoire faible"),
    DATA("data", "Données mobiles"),
    TEMPERATURE("temperature", "Température élevée"),
    PERMISSION("permission", "Autorisation désactivée"),
    ANOMALY("anomaly", "Anomalie à vérifier")
}

enum class AlertPriority(val labelFr: String) {
    FAIBLE("faible"),
    MOYENNE("moyenne"),
    ELEVEE("élevée"),
    CRITIQUE("critique")
}

data class AlertEvent(
    val id: String,
    val category: AlertCategory,
    val priority: AlertPriority,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val adapter: DataSource,
    val read: Boolean = false
)

data class AlertSettings(
    val enabled: Map<AlertCategory, Boolean> = AlertCategory.entries.associateWith { true },
    val cooldownMinutes: Int = 30,
    val silent: Boolean = false,
    val voice: Boolean = true
)

enum class ActionAvailability(val labelFr: String) {
    AUTO("Disponible automatiquement"),
    CONFIRM("Disponible avec confirmation"),
    SPECIAL("Autorisation spéciale requise"),
    MANUAL("À faire dans les Paramètres Android"),
    IMPOSSIBLE("Impossible pour une application ordinaire")
}

data class CareAction(
    val id: String,
    val title: String,
    val description: String,
    val availability: ActionAvailability,
    val destructive: Boolean,
    val androidSettings: String? = null,
    val steps: List<String> = emptyList()
)

enum class DemoScenario(val labelFr: String) {
    NORMAL("Appareil en bon état"),
    SURCHAUFFE("Surchauffe + décharge rapide"),
    STOCKAGE("Stockage presque plein"),
    BATTERIE("Batterie faible"),
    LENT("Téléphone lent (RAM saturée)")
}

data class VoiceExchange(
    val id: String,
    val at: Long = System.currentTimeMillis(),
    val user: String,
    val reply: String
)

data class Operation(
    val id: String,
    val at: Long = System.currentTimeMillis(),
    val actionId: String,
    val label: String,
    val result: String
)

enum class ViewMode {
    SIMPLE, AVANCE, EXPERT
}
