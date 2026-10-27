# Autorisations

| Autorisation | Utilité | Type | Versions / limites | Si refusée |
|---|---|---|---|---|
| `POST_NOTIFICATIONS` | Afficher les alertes | Exécution | Android 13+ (auto-accordée en 12) | Alertes visibles uniquement dans l'app |
| `RECORD_AUDIO` | Commandes vocales | Exécution | Toutes | Saisie texte uniquement |
| `PACKAGE_USAGE_STATS` | Apps peu utilisées, données par app | Accès spécial (Paramètres) | Toutes ; écran `USAGE_ACCESS_SETTINGS` | Diagnostics données/lenteur en confiance faible |
| `READ_PHONE_STATE` | Non requise (éviter) | — | — | — |
| `WRITE_SETTINGS` | Luminosité | Accès spécial | Déconseillé ; préférer ouvrir l'écran Affichage | Action manuelle guidée |
| `REQUEST_DELETE_PACKAGES` | Lancer la désinstallation (confirmée par le système) | Normale | Android 9+ | Ouvrir la fiche de l'app |
| Accès fichiers | Gros fichiers | **Aucune permission large** : sélecteur `ACTION_OPEN_DOCUMENT_TREE` (SAF) | `MANAGE_EXTERNAL_STORAGE` refusé par Play pour ce cas | Analyse impossible, guide manuel |
| `FOREGROUND_SERVICE` | Non utilisé (WorkManager suffit) | — | Android 14 restreint les types | — |

Principes : demander chaque autorisation **au moment** où elle sert, expliquer pourquoi, fonctionner en mode dégradé si elle est refusée (l'interface affiche alors « Indisponible »).

Version web : seule la permission Notifications du navigateur est demandée (Réglages). La reconnaissance vocale du navigateur n'est pas utilisée car certains navigateurs envoient l'audio à des serveurs.
