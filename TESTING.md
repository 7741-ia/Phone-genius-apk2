# Tests

## Automatisés
```bash
bunx vitest run
```
- `src/core/rules/engine.test.ts` : interprétation des demandes, stockage sous le seuil → critique, absence de mesure → confiance faible, diagnostic croisé, alerte batterie + anti-répétition, catégorie désactivée.
- `src/test/app-routing.test.tsx` : la page d'accueil est routée.

## Vérifications effectuées
| Critère | Résultat |
|---|---|
| Démarrage sans erreur | ✅ aucune erreur JS (navigateur automatisé) |
| Cinq modules + navigation | ✅ toutes les pages s'ouvrent (mobile 390 px) |
| Valeurs fictives identifiées | ✅ badge « FICTIF » sur chaque mesure, bandeau « DÉMO » |
| Moteur sans API externe | ✅ aucun `fetch` dans `src/core` |
| Historique local | ✅ persiste après rechargement |
| Autorisations manquantes / données absentes | ✅ adaptateur Navigateur → « Indisponible » + confiance faible |
| Hors ligne après chargement | ⚠️ logique OK ; rechargement hors ligne nécessite le Service Worker (voir OFFLINE_MODE.md) |

## Manuels recommandés
1. Réglages → scénario « Batterie faible » → une alerte apparaît (et est lue si une voix locale existe).
2. Réglages → source « Navigateur » → les champs non exposés affichent « Indisponible ».
3. Voix → micro sans moteur natif → message d'explication.
4. Entretien → action destructive → fenêtre de confirmation.
5. Réglages → Effacer l'historique.

## Android (à faire)
Matrice Android 12/13/14/15/16 : notifications (13+), reconnaissance sur l'appareil (13+), WorkManager sous Doze, température absente selon fabricant.
