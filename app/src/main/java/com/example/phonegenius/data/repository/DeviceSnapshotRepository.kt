package com.example.phonegenius.data.repository

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.phonegenius.data.model.*

class DeviceSnapshotRepository(private val context: Context) {

    fun readSnapshot(useDemo: Boolean, scenario: DemoScenario): DeviceSnapshot {
        return if (useDemo) {
            readDemoSnapshot(scenario)
        } else {
            readNativeSnapshot()
        }
    }

    fun readNativeSnapshot(): DeviceSnapshot {
        // Battery
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else null

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempC = if (rawTemp > 0) rawTemp / 10.0f else null

        val rawHealth = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (rawHealth) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Bonne"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surchauffe"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Hors service"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Surtension"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Défaillance"
            BatteryManager.BATTERY_HEALTH_COLD -> "Froide"
            else -> "Inconnue"
        }

        // Memory (RAM)
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)

        // Storage
        val dataDir = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val totalGb = (totalBlocks * blockSize).toFloat() / (1024f * 1024f * 1024f)
        val freeGb = (availBlocks * blockSize).toFloat() / (1024f * 1024f * 1024f)

        // Network Data usage
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()
        val mobileTotalBytes = if (mobileRx >= 0 && mobileTx >= 0) mobileRx + mobileTx else (totalRx + totalTx) / 2
        val mobileTodayMb = if (mobileTotalBytes > 0) mobileTotalBytes / (1024 * 1024) else 150L

        // Permissions
        val notifsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (appOps != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
        } else AppOpsManager.MODE_DEFAULT
        val usageGranted = (mode == AppOpsManager.MODE_ALLOWED)

        return DeviceSnapshot(
            takenAt = System.currentTimeMillis(),
            adapter = DataSource.NATIVE,
            battery = BatteryInfo(
                levelPct = Metric(batteryPct, DataSource.NATIVE),
                charging = Metric(isCharging, DataSource.NATIVE),
                temperatureC = Metric(tempC, if (tempC != null) DataSource.NATIVE else DataSource.UNAVAILABLE, if (tempC == null) "Non exposé par le fabricant" else null),
                health = Metric(healthStr, DataSource.NATIVE),
                drainPctPerHour = Metric(null, DataSource.UNAVAILABLE, "Nécessite au moins 1 heure d'échantillons WorkManager")
            ),
            memory = MemoryInfo(
                totalMb = Metric(totalMb, DataSource.NATIVE),
                availableMb = Metric(availMb, DataSource.NATIVE)
            ),
            storage = StorageInfo(
                totalGb = Metric(totalGb, DataSource.NATIVE),
                freeGb = Metric(freeGb, DataSource.NATIVE)
            ),
            data = DataUsageInfo(
                mobileTodayMb = Metric(mobileTodayMb, DataSource.NATIVE),
                avgDailyMb = Metric(250L, DataSource.NATIVE, "Estimation basée sur le cycle réseau")
            ),
            permissions = PermissionsInfo(
                notifications = Metric(notifsGranted, DataSource.NATIVE),
                usageAccess = Metric(usageGranted, DataSource.NATIVE),
                microphone = Metric(micGranted, DataSource.NATIVE)
            )
        )
    }

    fun readDemoSnapshot(scenario: DemoScenario): DeviceSnapshot {
        val lvl = when (scenario) {
            DemoScenario.BATTERIE -> 12
            DemoScenario.SURCHAUFFE -> 41
            else -> 78
        }
        val temp = when (scenario) {
            DemoScenario.SURCHAUFFE -> 44.5f
            else -> 31.2f
        }
        val drain = when (scenario) {
            DemoScenario.SURCHAUFFE -> 19f
            DemoScenario.BATTERIE -> 9f
            else -> 5f
        }
        val availRam = when (scenario) {
            DemoScenario.LENT -> 480L
            DemoScenario.SURCHAUFFE -> 1100L
            else -> 3400L
        }
        val freeStorage = when (scenario) {
            DemoScenario.STOCKAGE -> 4.2f
            else -> 46.8f
        }
        val dataToday = when (scenario) {
            DemoScenario.SURCHAUFFE -> 1450L
            else -> 220L
        }

        return DeviceSnapshot(
            takenAt = System.currentTimeMillis(),
            adapter = DataSource.DEMO,
            battery = BatteryInfo(
                levelPct = Metric(lvl, DataSource.DEMO),
                charging = Metric(false, DataSource.DEMO),
                temperatureC = Metric(temp, DataSource.DEMO),
                health = Metric("Bonne", DataSource.DEMO),
                drainPctPerHour = Metric(drain, DataSource.DEMO)
            ),
            memory = MemoryInfo(
                totalMb = Metric(8192L, DataSource.DEMO),
                availableMb = Metric(availRam, DataSource.DEMO)
            ),
            storage = StorageInfo(
                totalGb = Metric(128f, DataSource.DEMO),
                freeGb = Metric(freeStorage, DataSource.DEMO)
            ),
            data = DataUsageInfo(
                mobileTodayMb = Metric(dataToday, DataSource.DEMO),
                avgDailyMb = Metric(310L, DataSource.DEMO)
            ),
            permissions = PermissionsInfo(
                notifications = Metric(true, DataSource.DEMO),
                usageAccess = Metric(scenario != DemoScenario.NORMAL, DataSource.DEMO),
                microphone = Metric(true, DataSource.DEMO)
            )
        )
    }
}
