package com.example.phonegenius.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phonegenius.data.engine.AlertsEngine
import com.example.phonegenius.data.model.AlertPriority
import com.example.phonegenius.data.model.AlertSettings
import com.example.phonegenius.data.model.DemoScenario
import com.example.phonegenius.data.model.Thresholds
import com.example.phonegenius.data.repository.DeviceSnapshotRepository

class AlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = DeviceSnapshotRepository(appContext)
        val snapshot = repo.readNativeSnapshot()
        val alerts = AlertsEngine.evaluateAlerts(
            s = snapshot,
            t = Thresholds(),
            settings = AlertSettings(),
            history = emptyList()
        )

        val urgent = alerts.firstOrNull { it.priority == AlertPriority.CRITIQUE || it.priority == AlertPriority.ELEVEE }
        if (urgent != null) {
            postNotification(urgent.message, urgent.priority)
        }

        return Result.success()
    }

    private fun postNotification(message: String, priority: AlertPriority) {
        val channelId = "phone_genius_alerts"
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertes Phone Genius",
                if (priority == AlertPriority.CRITIQUE) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes de diagnostic matériel et batterie"
            }
            manager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Phone Genius")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (priority == AlertPriority.CRITIQUE) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notif)
    }
}
