# Implémentation Android native

Cible : `minSdk 31` (Android 12), `targetSdk 36` (Android 16). Tester sur 12, 13, 14, 15, 16.

## Option A — Hybride (WebView + pont Kotlin)
```kotlin
webView.settings.javaScriptEnabled = true
webView.addJavascriptInterface(PhoneGeniusBridge(context), "PhoneGeniusNative")
webView.loadUrl("https://appassets.androidplatform.net/assets/index.html") // WebViewAssetLoader
```
Méthodes à exposer (contrat `src/core/adapters/native-bridge.ts`) :

| Méthode JS | Implémentation Kotlin |
|---|---|
| `getSnapshot()` → JSON `DeviceSnapshot` (`source:"native"`) | voir tableau ci-dessous |
| `openSettings(action)` | `startActivity(Intent(action))` avec repli sur `Settings.ACTION_SETTINGS` |
| `notify(title, body, priority)` | `NotificationCompat` + canal par priorité ; Android 13+ : `POST_NOTIFICATIONS` |
| `speak(text, rate, volume)` | `TextToSpeech` (`setSpeechRate`, `KEY_PARAM_VOLUME`) |
| `hasOfflineSpeechRecognizer()` / `listenOffline()` | Android 13+ : `SpeechRecognizer.createOnDeviceSpeechRecognizer` ; sinon `EXTRA_PREFER_OFFLINE` ou Vosk (modèle FR embarqué ~40 Mo) |
| `scheduleChecks(min, thresholdsJson)` | `PeriodicWorkRequest` (minimum 15 min) |

> `@JavascriptInterface` est synchrone : pour `listenOffline`, renvoyer le résultat via `webView.evaluateJavascript("window.__pgResolve(...)")` ou utiliser un plugin Capacitor.

## Sources des mesures
| Champ | API | Remarques |
|---|---|---|
| `battery.levelPct`, `charging`, `temperatureC`, `health` | `ACTION_BATTERY_CHANGED` (EXTRA_LEVEL, EXTRA_TEMPERATURE/10, EXTRA_HEALTH), `BatteryManager` | Température parfois absente selon fabricant → `unavailable` |
| `battery.drainPctPerHour` | Échantillons WorkManager sur ≥ 1 h hors charge | `null` si échantillons insuffisants |
| `memory.totalMb`, `availableMb` | `ActivityManager.getMemoryInfo()` | |
| `storage.totalGb`, `freeGb` | `StatFs(Environment.getDataDirectory())` | |
| `data.*` | `NetworkStatsManager.querySummaryForDevice(TYPE_MOBILE…)` | Requiert accès d'usage |
| `permissions.usageAccess` | `AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS…)` | |

## Portage natif complet (Option B — Jetpack Compose)
`src/core/*` est sans React ni réseau : le porter tel quel en Kotlin (`data class` pour `types.ts`, `object RulesEngine`). Les tests `src/core/rules/engine.test.ts` servent de spécification.

## Surveillance en arrière-plan
`WorkManager` exécute `evaluateAlerts` toutes les 15–60 min ; Doze peut retarder l'exécution. Ne jamais promettre une surveillance en temps réel.

## Ce qu'une app ordinaire ne peut PAS faire
Fermer les autres applications, effacer leurs données, activer seule l'économiseur de batterie, modifier des paramètres protégés, redémarrer l'appareil.
