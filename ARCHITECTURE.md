# Architecture

```text
┌──────────────────────────── UI (src/routes, src/components/pg) ────────────────────────────┐
│ Dashboard · Diagnostic · Voix · Alertes · Entretien · Réglages                              │
└──────────────────────────────┬──────────────────────────────────────────────────────────────┘
                               │ useApp()  (src/store/app-store.tsx — état simple via Context)
┌──────────────┬───────────────┼───────────────┬──────────────┬────────────────────────────┐
│ Règles       │ Alertes       │ Actions       │ Voix         │ Stockage local             │
│ core/rules   │ core/alerts   │ core/actions  │ core/voice   │ core/storage (localStorage)│
└──────────────┴───────────────┴───────┬───────┴──────┬───────┴────────────────────────────┘
                                       │              │
                         ┌─────────────┴──────────────┴─────────────┐
                         │ Adaptateurs appareil (core/adapters)      │
                         │  DemoAdapter  → source "demo"  (FICTIF)    │
                         │  WebAdapter   → source "web"   (réel)      │
                         │  NativeAdapter→ source "native" via pont   │
                         └──────────────────────┬────────────────────┘
                                                │ window.PhoneGeniusNative (Kotlin)
```

## Les six couches séparées
1. **Interface** — `src/routes/*`, `src/components/pg/*`. Aucune logique métier.
2. **Moteur de règles** — `src/core/rules/engine.ts`. Fonctions pures : `parseIntent`, `runDiagnostic`. Aucun réseau.
3. **Stockage local** — `src/core/storage.ts` (préfixe `phone-genius:`). Équivalent Android : DataStore / Room.
4. **Notifications** — `src/core/alerts.ts` (détection + cooldown) ; l'émission passe par `bridge.notify`.
5. **Voix** — `src/core/voice.ts` : STT uniquement via pont natif hors ligne ; TTS via voix locales ou `bridge.speak`.
6. **Natif Android** — contrat `src/core/adapters/native-bridge.ts`.

## Contrats de données (`src/core/types.ts`)
- `Metric<T> = { value, source, note }` — **chaque** mesure déclare sa source (`demo | web | native | unavailable`).
- `DeviceSnapshot` : `battery`, `memory`, `storage`, `data`, `permissions`.
- `DiagnosticReport` / `Finding` : sévérité, confiance, mesures, causes, vérifications, solutions, actions.
- `AlertEvent`, `CareAction` (avec `availability`).

## Règle d'or
Une règle ne produit un diagnostic de confiance « élevée » que si ses mesures sont disponibles. Sinon : confiance « faible », mesure listée comme indisponible, aucune cause présentée comme certaine.

## Extensibilité
- Nouvelle règle : ajouter un `Topic`, ses mots-clés et une fonction dans `rules`.
- Nouvelle plateforme (ex. Windows plus tard) : implémenter `DeviceAdapter` + un pont équivalent ; aucune modification de l'interface.
