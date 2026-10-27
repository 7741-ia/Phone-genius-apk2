# Phone Genius — Application Android Native (Kotlin & Jetpack Compose)

Assistant intelligent **100 % local et privé** de diagnostic, maintenance et optimisation pour smartphones Android (cible : Android 12 → Android 16, `minSdk 31`, `targetSdk 36`).

Ce projet a été réécrit en **application Android native moderne** avec **Kotlin**, **Jetpack Compose** et **Material Design 3**.

> **Principe fondamental :** Aucune API distante payante, aucun compte, aucun tracking, aucun enregistrement vocal envoyé à un serveur. Tout s'exécute sur le processeur de l'appareil.

---

## 📱 Modules Principaux

| Module | Écran Compose | Fonctionnalités Natives |
|---|---|---|
| **1. Tableau de bord** | `DashboardScreen.kt` | Jauges circulaires animées (Batterie, Stockage, RAM, Température, Données), état de charge, score de santé général, recommandations en direct, modes Simple / Avancé / Expert. |
| **2. Diagnostic intelligent** | `DiagnosticScreen.kt` | Interprétation en langage naturel (`parseIntent`) → moteur de règles déterministe local (`RulesEngine.kt`) analysant température, batterie, RAM, stockage, données, autorisations et diagnostic croisé multi-anomalies. |
| **3. Voice Genius Hors Ligne** | `VoiceScreen.kt` | Bouton microphone animé avec pulsation, reconnaissance vocale locale et synthèse vocale française TextToSpeech (`VoiceAssistant.kt`), commandes rapides (`aide`, `silence`, `réactive`, `répète`). |
| **4. Centre d'alertes** | `AlertsScreen.kt` | Détection continue et cooldown anti-spam (`AlertsEngine.kt`), surveillance périodique en arrière-plan via `WorkManager` (`AlertWorker.kt`), seuils personnalisables. |
| **5. Entretien & Actions** | `CareScreen.kt` | Catalogue honnête d'actions classées par capacité Android réelle (`Auto`, `Confirmation`, `Spéciale`, `Paramètres manuels`, `Impossible pour une app standard`), déclenchement direct d'intents système Android et suppression sécurisée du cache de l'app. |
| **6. Réglages & Confidentialité** | `SettingsScreen.kt` | Bascule télémétrie réelle (capteurs natifs Android) vs simulation Démo (5 profils : normal, surchauffe, stockage, batterie, lent), thème M3 clair/sombre, effacement sécurisé des données locales. |

---

## 🛠️ Architecture Android

Le projet Android suit les recommandations officielles Google Android et Clean Architecture :

```
app/
├── src/main/
│   ├── AndroidManifest.xml          # Déclarations permissions (Audio, Notifications, UsageStats)
│   ├── java/com/example/phonegenius/
│   │   ├── MainActivity.kt          # Point d'entrée, edge-to-edge, runtime permissions
│   │   ├── data/
│   │   │   ├── model/Types.kt       # Modèles typés avec traçabilité de source (Native/Demo)
│   │   │   ├── engine/
│   │   │   │   ├── RulesEngine.kt   # Moteur déterministe de diagnostic local
│   │   │   │   ├── AlertsEngine.kt  # Moteur d'évaluation d'alertes avec cooldown
│   │   │   │   └── CareCatalog.kt   # Catalogue des 16 actions et Intents Android
│   │   │   └── repository/
│   │   │       └── DeviceSnapshotRepository.kt # Intégration BatteryManager, StatFs, ActivityManager
│   │   ├── service/
│   │   │   ├── AlertWorker.kt       # Tâche périodique WorkManager en arrière-plan
│   │   │   └── VoiceAssistant.kt    # Synthèse vocale française hors ligne
│   │   └── ui/
│   │       ├── PhoneGeniusApp.kt    # Scaffold principal, barre de navigation, backhandler
│   │       ├── components/          # Composants M3 réutilisables (Jauges, Badges, Cartes)
│   │       ├── screens/             # 6 écrans Compose avec TestTags complets
│   │       └── theme/               # Thème Material 3 (palette sombre, typographie, shapes)
│   └── res/
│       ├── drawable/                # Icônes adaptatives et arrière-plans vectoriels
│       └── values/                  # strings.xml, colors.xml, themes.xml
├── build.gradle.kts                 # Configuration AGP 8.8.0, Kotlin 2.1.0, Compose Compiler
settings.gradle.kts                  # Configuration des dépôts et inclusion :app
gradle/libs.versions.toml            # Version Catalog pour les dépendances
```

---

## 🔒 Confidentialité & Politiques Google Play

1. **Aucun envoi de télémétrie :** Les statistiques CPU, batterie et stockage sont lues localement.
2. **Aucune fausse promesse :** Phone Genius explique clairement qu'une application normale ne peut pas fermer arbitrairement les processus système ou effacer les données d'autres applications sans passer par les Paramètres système Android.
3. **Respect des permissions minimales :** Utilisation des intents Android standard (`Settings.ACTION_INTERNAL_STORAGE_SETTINGS`, `Settings.ACTION_BATTERY_SAVER_SETTINGS`, `Settings.ACTION_DISPLAY_SETTINGS`) plutôt que de requérir des autorisations sensibles dangereuses.
