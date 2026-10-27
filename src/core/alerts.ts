import type { AlertCategory, AlertEvent, AlertPriority, DeviceSnapshot, Thresholds } from "./types";
import { ramFreePct, storageFreePct } from "./rules/engine";

export interface AlertSettings {
  enabled: Record<AlertCategory, boolean>;
  cooldownMinutes: number;
  silent: boolean;
  voice: boolean;
}

export const ALERT_LABELS: Record<AlertCategory, string> = {
  battery_low: "Batterie faible",
  battery_drain: "Décharge rapide",
  storage: "Stockage saturé",
  memory: "Mémoire faible",
  data: "Données mobiles",
  temperature: "Température élevée",
  permission: "Autorisation désactivée",
  anomaly: "Anomalie à vérifier",
};

export const DEFAULT_ALERT_SETTINGS: AlertSettings = {
  enabled: Object.fromEntries(Object.keys(ALERT_LABELS).map((k) => [k, true])) as Record<AlertCategory, boolean>,
  cooldownMinutes: 30,
  silent: false,
  voice: true,
};

type Candidate = { category: AlertCategory; priority: AlertPriority; message: string };

/** Règles d'alerte : ne se déclenchent que sur des mesures disponibles. */
export function detectAlerts(s: DeviceSnapshot, t: Thresholds): Candidate[] {
  const out: Candidate[] = [];
  const lvl = s.battery.levelPct.value;
  if (lvl !== null && !s.battery.charging.value) {
    if (lvl <= t.batteryCritical) out.push({ category: "battery_low", priority: "critique", message: `Attention, votre batterie est à ${lvl} %. Branchez votre chargeur.` });
    else if (lvl <= t.batteryLow) out.push({ category: "battery_low", priority: "élevée", message: `Attention, votre batterie est à ${lvl} %. Vous pouvez activer le mode économie d'énergie.` });
  }
  const drain = s.battery.drainPctPerHour.value;
  if (drain !== null && drain >= t.drainPctPerHourHigh) out.push({ category: "battery_drain", priority: "moyenne", message: `Décharge rapide estimée à ${drain} % par heure.` });
  const sp = storageFreePct(s);
  if (sp !== null && sp <= t.storageFreePctLow) out.push({ category: "storage", priority: sp <= t.storageFreePctCritical ? "critique" : "élevée", message: `Stockage presque saturé : ${sp.toFixed(1)} % libre.` });
  const rp = ramFreePct(s);
  if (rp !== null && rp <= t.ramFreePctLow) out.push({ category: "memory", priority: "moyenne", message: `Mémoire disponible faible : ${rp.toFixed(0)} %.` });
  const temp = s.battery.temperatureC.value;
  if (temp !== null && temp >= t.tempHigh) out.push({ category: "temperature", priority: temp >= t.tempCritical ? "critique" : "élevée", message: `Température de batterie élevée : ${temp.toFixed(1)} °C.` });
  const data = s.data.mobileTodayMb.value;
  if (data !== null && data >= t.dataDailyMbHigh) out.push({ category: "data", priority: "moyenne", message: `Forte consommation de données aujourd'hui : ${data} Mo.` });
  if (s.permissions.notifications.value === false) out.push({ category: "permission", priority: "faible", message: "Les notifications sont désactivées : vous ne recevrez pas les alertes." });
  return out;
}

/** Filtre catégories désactivées et anti-répétition (cooldown). */
export function evaluateAlerts(s: DeviceSnapshot, t: Thresholds, settings: AlertSettings, history: AlertEvent[], now = Date.now()): AlertEvent[] {
  const cooldown = settings.cooldownMinutes * 60_000;
  return detectAlerts(s, t)
    .filter((c) => settings.enabled[c.category])
    .filter((c) => !history.some((h) => h.category === c.category && now - h.createdAt < cooldown))
    .map((c, i) => ({ ...c, id: `${now.toString(36)}-${i}`, createdAt: now, adapter: s.adapter, read: false }));
}
