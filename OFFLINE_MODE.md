# Mode hors ligne

## Ce qui fonctionne sans Internet (une fois l'app chargée)
- Moteur de règles, alertes, actions, historique : 100 % JavaScript local, aucun appel réseau (vérifiable : `rg "fetch\(" src/core` ne renvoie rien).
- Stockage : `localStorage`.
- Voix : voix TTS locales seulement ; STT via moteur natif hors ligne.

## État actuel du Service Worker
**Pas encore activé.** Le projet expose un manifeste (installation sur l'écran d'accueil), mais pas de Service Worker : le rechargement d'une page sans réseau échoue tant que celui-ci n'est pas ajouté. Les polices Google se replient sur les polices système hors ligne.

## Activer le hors-ligne web (étape suivante)
1. `bun add -D vite-plugin-pwa`
2. Dans `vite.config.ts` : `VitePWA({ strategies: "generateSW", registerType: "autoUpdate", injectRegister: null, devOptions: { enabled: false }, filename: "sw.js", workbox: { navigateFallbackDenylist: [/^\/~oauth/], runtimeCaching: [{ urlPattern: ({ request }) => request.mode === "navigate", handler: "NetworkFirst" }] } })`
3. Enregistrer le SW depuis **un seul module**, uniquement en production, hors iframe et hors domaines de prévisualisation (`*.lovable.app` preview, `lovableproject.com`), avec un interrupteur `?sw=off`.
4. Dans l'APK hybride, les fichiers sont embarqués dans les assets : le hors-ligne est natif, le SW est inutile.

## Vérifier
Publier, ouvrir l'app, DevTools → Network → Offline, recharger.
