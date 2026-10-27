package com.example.phonegenius.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.phonegenius.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class PhoneGeniusStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phone_genius_prefs", Context.MODE_PRIVATE)

    // Keys
    private val KEY_USE_DEMO = "use_demo"
    private val KEY_DEMO_SCENARIO = "demo_scenario"
    private val KEY_DARK_THEME = "dark_theme"
    private val KEY_VIEW_MODE = "view_mode"
    private val KEY_THRESHOLDS = "thresholds"
    private val KEY_ALERT_SETTINGS = "alert_settings"
    private val KEY_ALERTS = "alerts_history"
    private val KEY_REPORTS = "reports_history"
    private val KEY_VOICE_LOG = "voice_log"
    private val KEY_OPERATIONS = "operations_log"

    var useDemo: Boolean
        get() = prefs.getBoolean(KEY_USE_DEMO, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_DEMO, value).apply()

    var demoScenario: DemoScenario
        get() {
            val name = prefs.getString(KEY_DEMO_SCENARIO, DemoScenario.NORMAL.name)
            return try { DemoScenario.valueOf(name ?: DemoScenario.NORMAL.name) } catch (e: Exception) { DemoScenario.NORMAL }
        }
        set(value) = prefs.edit().putString(KEY_DEMO_SCENARIO, value.name).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var viewMode: ViewMode
        get() {
            val name = prefs.getString(KEY_VIEW_MODE, ViewMode.SIMPLE.name)
            return try { ViewMode.valueOf(name ?: ViewMode.SIMPLE.name) } catch (e: Exception) { ViewMode.SIMPLE }
        }
        set(value) = prefs.edit().putString(KEY_VIEW_MODE, value.name).apply()

    fun loadThresholds(): Thresholds {
        val str = prefs.getString(KEY_THRESHOLDS, null) ?: return Thresholds()
        return try {
            val json = JSONObject(str)
            Thresholds(
                batteryLow = json.optInt("batteryLow", 20),
                batteryCritical = json.optInt("batteryCritical", 10),
                storageFreePctLow = json.optDouble("storageFreePctLow", 15.0).toFloat(),
                storageFreePctCritical = json.optDouble("storageFreePctCritical", 5.0).toFloat(),
                ramFreePctLow = json.optDouble("ramFreePctLow", 12.0).toFloat(),
                tempHigh = json.optDouble("tempHigh", 40.0).toFloat(),
                tempCritical = json.optDouble("tempCritical", 45.0).toFloat(),
                dataDailyMbHigh = json.optLong("dataDailyMbHigh", 1000L),
                drainPctPerHourHigh = json.optDouble("drainPctPerHourHigh", 15.0).toFloat()
            )
        } catch (e: Exception) {
            Thresholds()
        }
    }

    fun saveThresholds(t: Thresholds) {
        val json = JSONObject().apply {
            put("batteryLow", t.batteryLow)
            put("batteryCritical", t.batteryCritical)
            put("storageFreePctLow", t.storageFreePctLow.toDouble())
            put("storageFreePctCritical", t.storageFreePctCritical.toDouble())
            put("ramFreePctLow", t.ramFreePctLow.toDouble())
            put("tempHigh", t.tempHigh.toDouble())
            put("tempCritical", t.tempCritical.toDouble())
            put("dataDailyMbHigh", t.dataDailyMbHigh)
            put("drainPctPerHourHigh", t.drainPctPerHourHigh.toDouble())
        }
        prefs.edit().putString(KEY_THRESHOLDS, json.toString()).apply()
    }

    fun loadAlerts(): List<AlertEvent> {
        val str = prefs.getString(KEY_ALERTS, null) ?: return emptyList()
        val list = mutableListOf<AlertEvent>()
        try {
            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AlertEvent(
                        id = o.getString("id"),
                        category = AlertCategory.valueOf(o.getString("category")),
                        priority = AlertPriority.valueOf(o.getString("priority")),
                        message = o.getString("message"),
                        createdAt = o.getLong("createdAt"),
                        adapter = DataSource.valueOf(o.getString("adapter")),
                        read = o.optBoolean("read", false)
                    )
                )
            }
        } catch (e: Exception) {
            /* ignore corrupted entries */
        }
        return list
    }

    fun saveAlerts(alerts: List<AlertEvent>) {
        val arr = JSONArray()
        alerts.take(50).forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("category", a.category.name)
                put("priority", a.priority.name)
                put("message", a.message)
                put("createdAt", a.createdAt)
                put("adapter", a.adapter.name)
                put("read", a.read)
            })
        }
        prefs.edit().putString(KEY_ALERTS, arr.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
