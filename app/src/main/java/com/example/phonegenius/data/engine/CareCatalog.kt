package com.example.phonegenius.data.engine

import android.provider.Settings
import com.example.phonegenius.data.model.ActionAvailability
import com.example.phonegenius.data.model.CareAction

object CareCatalog {
    val CARE_ACTIONS = listOf(
        CareAction(
            id = "storage_scan",
            title = "Analyser l'espace de stockage",
            description = "Calcule l'espace total et libre directement depuis le système de fichiers.",
            availability = ActionAvailability.AUTO,
            destructive = false,
            steps = listOf("Lecture via StatFs (environnement interne)")
        ),
        CareAction(
            id = "large_files",
            title = "Identifier les gros fichiers accessibles",
            description = "Liste les fichiers volumineux partagés via le sélecteur Android. Rien n'est supprimé automatiquement.",
            availability = ActionAvailability.SPECIAL,
            destructive = false,
            androidSettings = "android.intent.action.OPEN_DOCUMENT_TREE",
            steps = listOf("Choisir un dossier", "Consulter la liste triée par taille", "Supprimer vous-même ce qui est inutile")
        ),
        CareAction(
            id = "clear_own_cache",
            title = "Vider le cache de Phone Genius",
            description = "Supprime uniquement les fichiers temporaires et caches de cette application.",
            availability = ActionAvailability.CONFIRM,
            destructive = true,
            steps = listOf("Confirmer", "Suppression des fichiers temporaires dans context.cacheDir")
        ),
        CareAction(
            id = "storage_settings",
            title = "Ouvrir Paramètres › Stockage",
            description = "Gérer les caches et fichiers volumineux des autres applications dans le gestionnaire système.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            steps = listOf("Ouvrir Stockage", "Choisir une catégorie", "Nettoyer manuellement")
        ),
        CareAction(
            id = "uninstall_apps",
            title = "Désinstaller les applications inutilisées",
            description = "Guide : repérer les applications non ouvertes depuis longtemps puis lancer leur désinstallation.",
            availability = ActionAvailability.CONFIRM,
            destructive = true,
            androidSettings = "android.intent.action.DELETE",
            steps = listOf("Accorder l'accès aux statistiques d'usage", "Consulter la liste des apps peu utilisées", "Android demande confirmation pour chaque désinstallation")
        ),
        CareAction(
            id = "battery_saver",
            title = "Activer l'économie d'énergie",
            description = "Une application ne peut pas l'activer seule : ouverture de l'écran dédié dans les paramètres Android.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            steps = listOf("Ouvrir l'écran", "Activer l'économiseur")
        ),
        CareAction(
            id = "battery_usage",
            title = "Voir l'utilisation de la batterie",
            description = "Identifier les applications les plus gourmandes dans le rapport Android.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = "android.intent.action.POWER_USAGE_SUMMARY",
            steps = listOf("Ouvrir l'écran", "Restreindre les apps gourmandes")
        ),
        CareAction(
            id = "brightness",
            title = "Réduire la luminosité",
            description = "Modifier la luminosité ou activer l'ajustement adaptatif dans les paramètres d'affichage.",
            availability = ActionAvailability.SPECIAL,
            destructive = false,
            androidSettings = Settings.ACTION_DISPLAY_SETTINGS,
            steps = listOf("Ouvrir Affichage", "Activer la luminosité adaptative")
        ),
        CareAction(
            id = "notif_settings",
            title = "Vérifier les notifications",
            description = "Autoriser Phone Genius à vous alerter en cas de surchauffe ou de batterie critique.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            steps = listOf("Ouvrir", "Activer les notifications")
        ),
        CareAction(
            id = "usage_access",
            title = "Accès aux statistiques d'usage",
            description = "Nécessaire pour mesurer la consommation par application (PACKAGE_USAGE_STATS).",
            availability = ActionAvailability.SPECIAL,
            destructive = false,
            androidSettings = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            steps = listOf("Ouvrir", "Autoriser Phone Genius")
        ),
        CareAction(
            id = "permission_review",
            title = "Revoir les autorisations des apps",
            description = "Gestionnaire d'autorisations Android pour protéger votre vie privée.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = Settings.ACTION_APPLICATION_SETTINGS,
            steps = listOf("Ouvrir Applications", "Vérifier micro, position, caméra")
        ),
        CareAction(
            id = "data_saver",
            title = "Activer l'économiseur de données",
            description = "Réduit les données consommées en arrière-plan par les applications.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = "android.settings.DATA_SAVER_SETTINGS",
            steps = listOf("Ouvrir", "Activer")
        ),
        CareAction(
            id = "data_usage",
            title = "Voir la consommation de données",
            description = "Consommation détaillée réseau et Wi-Fi par application.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            androidSettings = Settings.ACTION_DATA_USAGE_SETTINGS,
            steps = listOf("Ouvrir", "Repérer les apps gourmandes")
        ),
        CareAction(
            id = "restart",
            title = "Redémarrer le téléphone",
            description = "Seul l'utilisateur peut redémarrer l'appareil pour libérer la mémoire et réinitialiser les processus.",
            availability = ActionAvailability.MANUAL,
            destructive = false,
            steps = listOf("Maintenir le bouton marche/veille", "Choisir Redémarrer")
        ),
        CareAction(
            id = "kill_apps",
            title = "Fermer toutes les applications",
            description = "Android interdit à une application tierce de fermer arbitrairement les processus des autres apps.",
            availability = ActionAvailability.IMPOSSIBLE,
            destructive = false,
            steps = emptyList()
        ),
        CareAction(
            id = "clear_other_data",
            title = "Effacer les données d'autres apps",
            description = "Réservé au système Android et à l'utilisateur depuis les Paramètres pour des raisons de sécurité.",
            availability = ActionAvailability.IMPOSSIBLE,
            destructive = true,
            steps = emptyList()
        )
    )

    fun getAction(id: String): CareAction? = CARE_ACTIONS.find { it.id == id }
}
