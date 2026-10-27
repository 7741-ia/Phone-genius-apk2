import type { ActionAvailability, CareAction } from "./types";

export const AVAILABILITY_LABELS: Record<ActionAvailability, string> = {
  auto: "Disponible automatiquement",
  confirm: "Disponible avec confirmation",
  special: "Autorisation spéciale requise",
  manual: "À faire dans les Paramètres Android",
  impossible: "Impossible pour une application ordinaire",
};

/** Catalogue des actions d'entretien, classées honnêtement. */
export const CARE_ACTIONS: CareAction[] = [
  { id: "storage_scan", title: "Analyser l'espace de stockage", description: "Calcule l'espace total et libre.", availability: "auto", destructive: false, steps: ["Lecture via StatFs (native)"] },
  { id: "large_files", title: "Identifier les gros fichiers accessibles", description: "Liste les fichiers volumineux que vous avez choisis de partager (sélecteur de documents). Rien n'est supprimé automatiquement.", availability: "special", destructive: false, androidSettings: "Intent.ACTION_OPEN_DOCUMENT_TREE", steps: ["Choisir un dossier", "Consulter la liste triée par taille", "Supprimer vous-même ce qui est inutile"] },
  { id: "clear_own_cache", title: "Vider le cache de Phone Genius", description: "Supprime uniquement les fichiers temporaires de cette application.", availability: "confirm", destructive: true, steps: ["Confirmer", "Suppression de context.cacheDir"] },
  { id: "storage_settings", title: "Ouvrir Paramètres › Stockage", description: "Gérer les caches et fichiers des autres applications.", availability: "manual", destructive: false, androidSettings: "android.settings.INTERNAL_STORAGE_SETTINGS", steps: ["Ouvrir Stockage", "Choisir une catégorie", "Nettoyer manuellement"] },
  { id: "uninstall_apps", title: "Désinstaller les applications inutilisées", description: "Guide : repérer les apps non ouvertes depuis longtemps (statistiques d'usage) puis les désinstaller.", availability: "confirm", destructive: true, androidSettings: "Intent.ACTION_DELETE (confirmation système)", steps: ["Accorder l'accès aux statistiques d'usage", "Consulter la liste des apps peu utilisées", "Android demande confirmation pour chaque désinstallation"] },
  { id: "battery_saver", title: "Activer l'économie d'énergie", description: "Une application ne peut pas l'activer seule : ouverture de l'écran dédié.", availability: "manual", destructive: false, androidSettings: "android.settings.BATTERY_SAVER_SETTINGS", steps: ["Ouvrir l'écran", "Activer l'économiseur"] },
  { id: "battery_usage", title: "Voir l'utilisation de la batterie", description: "Identifier les apps les plus gourmandes.", availability: "manual", destructive: false, androidSettings: "Intent.ACTION_POWER_USAGE_SUMMARY", steps: ["Ouvrir l'écran", "Restreindre les apps gourmandes"] },
  { id: "brightness", title: "Réduire la luminosité", description: "Modifier la luminosité système exige WRITE_SETTINGS.", availability: "special", destructive: false, androidSettings: "android.settings.DISPLAY_SETTINGS", steps: ["Ouvrir Affichage", "Activer la luminosité adaptative"] },
  { id: "notif_settings", title: "Vérifier les notifications", description: "Autoriser Phone Genius à vous alerter.", availability: "manual", destructive: false, androidSettings: "android.settings.APP_NOTIFICATION_SETTINGS", steps: ["Ouvrir", "Activer les notifications"] },
  { id: "usage_access", title: "Accès aux statistiques d'usage", description: "Nécessaire pour les données par application (PACKAGE_USAGE_STATS).", availability: "special", destructive: false, androidSettings: "android.settings.USAGE_ACCESS_SETTINGS", steps: ["Ouvrir", "Autoriser Phone Genius"] },
  { id: "permission_review", title: "Revoir les autorisations des apps", description: "Gestionnaire d'autorisations Android.", availability: "manual", destructive: false, androidSettings: "android.settings.APPLICATION_SETTINGS", steps: ["Ouvrir Applications", "Vérifier micro, position, caméra"] },
  { id: "data_saver", title: "Activer l'économiseur de données", description: "Réduit les données en arrière-plan.", availability: "manual", destructive: false, androidSettings: "android.settings.DATA_SAVER_SETTINGS", steps: ["Ouvrir", "Activer"] },
  { id: "data_usage", title: "Voir la consommation de données", description: "Consommation par application.", availability: "manual", destructive: false, androidSettings: "android.settings.DATA_USAGE_SETTINGS", steps: ["Ouvrir", "Repérer les apps gourmandes"] },
  { id: "restart", title: "Redémarrer le téléphone", description: "Seul l'utilisateur peut redémarrer l'appareil.", availability: "manual", destructive: false, steps: ["Maintenir le bouton marche", "Choisir Redémarrer"] },
  { id: "kill_apps", title: "Fermer toutes les applications", description: "Android interdit de fermer arbitrairement les autres apps.", availability: "impossible", destructive: false, steps: [] },
  { id: "clear_other_data", title: "Effacer les données d'autres apps", description: "Réservé au système ou à l'utilisateur.", availability: "impossible", destructive: true, steps: [] },
];

export const getAction = (id: string) => CARE_ACTIONS.find((a) => a.id === id);
