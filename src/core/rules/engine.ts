/**
 * Moteur de diagnostic LOCAL basé sur des règles. Aucun appel réseau, aucune IA externe.
 * Principe : une règle ne conclut que sur des mesures disponibles ; sinon elle
 * le dit et baisse la confiance.
 */
import type {
  Confidence, DeviceSnapshot, DiagnosticReport, Finding, MeasureLine, Metric, Severity, Thresholds, Topic,
} from "../types";

export const DEFAULT_THRESHOLDS: Thresholds = {
  batteryLow: 20,
  batteryCritical: 10,
  storageFreePctLow: 15,
  storageFreePctCritical: 5,
  ramFreePctLow: 12,
  tempHigh: 40,
  tempCritical: 45,
  dataDailyMbHigh: 1000,
  drainPctPerHourHigh: 15,
};

const ALL_TOPICS: Topic[] = ["heat", "battery", "slow", "storage", "data", "permissions"];

const KEYWORDS: Record<Topic, string[]> = {
  heat: ["chauf", "chaud", "températ", "brûl", "surchauf"],
  battery: ["batter", "décharg", "decharg", "autonomie", "se vide", "charge"],
  slow: ["lent", "rame", "bloqu", "freez", "lag", "ram", "mémoire vive", "memoire vive"],
  storage: ["espace", "stockage", "plein", "saturé", "sature", "mémoire pleine", "fichier"],
  data: ["donnée", "donnee", "forfait", "mobile", "internet", "data", "giga", "mo "],
  permissions: ["autorisation", "permission", "accès", "acces"],
};

const strip = (s: string) => s.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

/** Interprète une demande en langage naturel → sujets à contrôler. */
export function parseIntent(text: string): Topic[] {
  const t = strip(` ${text} `);
  if (/tous|tout|complet|global|analyse|general|scan/.test(t) && !ALL_TOPICS.some((k) => KEYWORDS[k].some((w) => t.includes(strip(w))))) {
    return [...ALL_TOPICS];
  }
  if (/tous les problemes|tout analyser|analyse complete|diagnostic complet/.test(t)) return [...ALL_TOPICS];
  const found = ALL_TOPICS.filter((k) => KEYWORDS[k].some((w) => t.includes(strip(w))));
  return found.length ? found : [...ALL_TOPICS];
}

const SEV_ORDER: Severity[] = ["ok", "low", "medium", "high", "critical"];
export const worstOf = (list: Severity[]): Severity =>
  list.reduce<Severity>((a, b) => (SEV_ORDER.indexOf(b) > SEV_ORDER.indexOf(a) ? b : a), "ok");

const has = <T,>(m: Metric<T>): m is Metric<T> & { value: T } => m.value !== null && m.source !== "unavailable";
const line = <T,>(label: string, m: Metric<T>, fmt: (v: T) => string): MeasureLine => ({
  label,
  value: has(m) ? fmt(m.value) : `Indisponible${m.note ? ` — ${m.note}` : ""}`,
  source: m.source,
});

export function storageFreePct(s: DeviceSnapshot): number | null {
  const { totalGb, freeGb } = s.storage;
  return has(totalGb) && has(freeGb) && totalGb.value > 0 ? (freeGb.value / totalGb.value) * 100 : null;
}
export function ramFreePct(s: DeviceSnapshot): number | null {
  const { totalMb, availableMb } = s.memory;
  return has(totalMb) && has(availableMb) && totalMb.value > 0 ? (availableMb.value / totalMb.value) * 100 : null;
}

type Rule = (s: DeviceSnapshot, t: Thresholds) => Finding;

const rules: Record<Topic, Rule> = {
  heat: (s, t) => {
    const temp = s.battery.temperatureC;
    const measures = [line("Température batterie", temp, (v) => `${v.toFixed(1)} °C`), line("En charge", s.battery.charging, (v) => (v ? "Oui" : "Non"))];
    if (!has(temp)) {
      return {
        id: "heat", topic: "heat", title: "Chauffe : mesure de température indisponible", severity: "low", confidence: "faible",
        observed: "Impossible de lire la température sur cette source de données.", measures,
        causes: ["Usage intensif (jeux, vidéo, GPS)", "Charge rapide", "Environnement chaud", "Application en boucle en arrière-plan"],
        checks: ["Installer l'application Android native pour lire BatteryManager", "Observer si la chauffe survient en charge ou pendant un usage précis"],
        solutions: ["Retirer la coque pendant la charge", "Éviter l'exposition au soleil", "Fermer les applications lourdes non utilisées"],
        actionIds: ["battery_usage", "battery_saver"],
      };
    }
    const sev: Severity = temp.value >= t.tempCritical ? "critical" : temp.value >= t.tempHigh ? "high" : "ok";
    return {
      id: "heat", topic: "heat", title: sev === "ok" ? "Température normale" : "Température batterie élevée", severity: sev,
      confidence: "élevée", observed: `Batterie à ${temp.value.toFixed(1)} °C (seuil ${t.tempHigh} °C).`, measures,
      causes: sev === "ok" ? [] : ["Usage intensif du processeur", "Charge en cours ou charge rapide", "Signal réseau faible (le modem force)", "Température ambiante"],
      checks: sev === "ok" ? [] : ["Consulter l'utilisation de la batterie par application", "Vérifier si la chauffe persiste au repos"],
      solutions: sev === "ok" ? ["Aucune action nécessaire"] : ["Arrêter jeux / vidéo quelques minutes", "Débrancher le chargeur si la température dépasse 45 °C", "Activer l'économie d'énergie"],
      actionIds: sev === "ok" ? [] : ["battery_usage", "battery_saver"],
    };
  },
  battery: (s, t) => {
    const lvl = s.battery.levelPct, drain = s.battery.drainPctPerHour;
    const measures = [line("Niveau", lvl, (v) => `${v} %`), line("Décharge estimée", drain, (v) => `${v} %/h`), line("État", s.battery.health, (v) => v)];
    if (!has(lvl)) {
      return { id: "battery", topic: "battery", title: "Batterie : données indisponibles", severity: "low", confidence: "faible",
        observed: "Le niveau de batterie n'est pas accessible.", measures, causes: [], checks: ["Utiliser l'application native"], solutions: [], actionIds: ["battery_usage"] };
    }
    const low = lvl.value <= t.batteryCritical ? "critical" : lvl.value <= t.batteryLow ? "high" : "ok";
    const fast = has(drain) && drain.value >= t.drainPctPerHourHigh;
    const sev = worstOf([low as Severity, fast ? "medium" : "ok"]);
    return {
      id: "battery", topic: "battery", title: sev === "ok" ? "Batterie en bon état" : low !== "ok" ? "Batterie faible" : "Décharge rapide",
      severity: sev, confidence: has(drain) ? "élevée" : "moyenne",
      observed: `Niveau ${lvl.value} %${has(drain) ? `, décharge ≈ ${drain.value} %/h` : " (vitesse de décharge non mesurable)"}.`,
      measures,
      causes: sev === "ok" ? [] : ["Luminosité élevée", "Applications actives en arrière-plan", "Localisation GPS permanente", "Réseau mobile faible", "Usure de la batterie (à vérifier)"],
      checks: sev === "ok" ? [] : ["Ouvrir l'utilisation batterie par application", ...(has(drain) ? [] : ["Mesurer la décharge sur 1 h (version native)"])],
      solutions: sev === "ok" ? ["Aucune action nécessaire"] : ["Activer l'économie d'énergie", "Réduire la luminosité / passer en thème sombre", "Restreindre l'arrière-plan des apps gourmandes"],
      actionIds: sev === "ok" ? [] : ["battery_saver", "battery_usage", "brightness"],
    };
  },
  slow: (s, t) => {
    const pct = ramFreePct(s);
    const sp = storageFreePct(s);
    const measures = [line("RAM totale", s.memory.totalMb, (v) => `${(v / 1024).toFixed(1)} Go`), line("RAM disponible", s.memory.availableMb, (v) => `${(v / 1024).toFixed(2)} Go`)];
    if (pct === null) {
      return { id: "slow", topic: "slow", title: "Lenteur : mémoire disponible inconnue", severity: "low", confidence: "faible",
        observed: "La RAM disponible n'est pas lisible sur cette source.", measures,
        causes: ["Trop d'applications ouvertes", "Stockage presque plein", "Mise à jour système en attente"],
        checks: ["Vérifier l'espace de stockage", "Redémarrer le téléphone"], solutions: ["Redémarrer", "Désinstaller les applications inutilisées"], actionIds: ["restart", "uninstall_apps"] };
    }
    const sev: Severity = pct < t.ramFreePctLow / 2 ? "high" : pct < t.ramFreePctLow ? "medium" : sp !== null && sp < t.storageFreePctLow ? "medium" : "ok";
    return {
      id: "slow", topic: "slow", title: sev === "ok" ? "Mémoire suffisante" : "Mémoire vive saturée", severity: sev, confidence: "moyenne",
      observed: `${pct.toFixed(0)} % de RAM disponible${sp !== null ? `, ${sp.toFixed(0)} % de stockage libre` : ""}.`, measures,
      causes: sev === "ok" ? [] : ["Nombreuses applications en mémoire", "Application gourmande ou défectueuse", "Stockage trop rempli ralentissant le système"],
      checks: sev === "ok" ? [] : ["Identifier les apps les plus utilisées (accès aux statistiques d'usage requis)"],
      solutions: sev === "ok" ? ["Aucune action nécessaire"] : ["Redémarrer le téléphone", "Désinstaller ou désactiver les apps inutilisées", "Libérer de l'espace de stockage"],
      actionIds: sev === "ok" ? [] : ["restart", "uninstall_apps", "storage_settings"],
    };
  },
  storage: (s, t) => {
    const pct = storageFreePct(s);
    const measures = [line("Stockage total", s.storage.totalGb, (v) => `${v} Go`), line("Stockage libre", s.storage.freeGb, (v) => `${v.toFixed(1)} Go`)];
    if (pct === null) {
      return { id: "storage", topic: "storage", title: "Stockage : mesure indisponible", severity: "low", confidence: "faible",
        observed: "Le navigateur ne peut pas lire l'espace du téléphone.", measures, causes: [],
        checks: ["Consulter Paramètres › Stockage", "Utiliser l'application native (StatFs)"], solutions: [], actionIds: ["storage_settings"] };
    }
    const sev: Severity = pct <= t.storageFreePctCritical ? "critical" : pct <= t.storageFreePctLow ? "high" : "ok";
    return {
      id: "storage", topic: "storage", title: sev === "ok" ? "Espace de stockage suffisant" : "Stockage presque saturé", severity: sev, confidence: "élevée",
      observed: `${pct.toFixed(1)} % d'espace libre (seuil ${t.storageFreePctLow} %).`, measures,
      causes: sev === "ok" ? [] : ["Photos et vidéos volumineuses", "Caches d'applications", "Fichiers téléchargés oubliés", "Médias de messagerie (WhatsApp…)"],
      checks: sev === "ok" ? [] : ["Analyser les gros fichiers accessibles", "Trier les médias de messagerie"],
      solutions: sev === "ok" ? ["Aucune action nécessaire"] : ["Supprimer vous-même les fichiers inutiles après vérification", "Vider le cache des apps depuis les Paramètres", "Sauvegarder les photos ailleurs"],
      actionIds: sev === "ok" ? [] : ["large_files", "clear_own_cache", "storage_settings"],
    };
  },
  data: (s, t) => {
    const today = s.data.mobileTodayMb, avg = s.data.avgDailyMb;
    const measures = [line("Données mobiles aujourd'hui", today, (v) => `${v} Mo`), line("Moyenne quotidienne", avg, (v) => `${v} Mo`)];
    if (!has(today)) {
      return { id: "data", topic: "data", title: "Données mobiles : statistiques indisponibles", severity: "low", confidence: "faible",
        observed: "Les statistiques réseau exigent l'accès spécial aux données d'utilisation.", measures, causes: [],
        checks: ["Accorder l'accès aux données d'utilisation (version native)", "Consulter Paramètres › Réseau › Utilisation des données"],
        solutions: ["Activer l'économiseur de données"], actionIds: ["data_saver", "usage_access"] };
    }
    const spike = has(avg) && today.value > avg.value * 3;
    const sev: Severity = today.value >= t.dataDailyMbHigh ? "high" : spike ? "medium" : "ok";
    return {
      id: "data", topic: "data", title: sev === "ok" ? "Consommation de données normale" : "Consommation de données élevée", severity: sev,
      confidence: has(avg) ? "élevée" : "moyenne", observed: `${today.value} Mo aujourd'hui${has(avg) ? ` (moyenne ${avg.value} Mo)` : ""}.`, measures,
      causes: sev === "ok" ? [] : ["Lecture vidéo en haute qualité", "Mises à jour automatiques sur réseau mobile", "Sauvegarde cloud des photos", "Application en arrière-plan"],
      checks: sev === "ok" ? [] : ["Voir la consommation par application"],
      solutions: sev === "ok" ? ["Aucune action nécessaire"] : ["Activer l'économiseur de données", "Limiter les mises à jour au Wi-Fi", "Réduire la qualité vidéo"],
      actionIds: sev === "ok" ? [] : ["data_saver", "data_usage"],
    };
  },
  permissions: (s) => {
    const p = s.permissions;
    const measures = [line("Notifications", p.notifications, (v) => (v ? "Accordées" : "Refusées")), line("Accès statistiques d'usage", p.usageAccess, (v) => (v ? "Accordé" : "Non accordé")), line("Microphone", p.microphone, (v) => (v ? "Accordé" : "Refusé"))];
    const missing = [has(p.notifications) && !p.notifications.value && "notifications", has(p.usageAccess) && !p.usageAccess.value && "statistiques d'usage"].filter(Boolean) as string[];
    const sev: Severity = missing.length ? "medium" : "ok";
    return {
      id: "permissions", topic: "permissions", title: missing.length ? "Autorisations importantes manquantes" : "Autorisations en ordre", severity: sev,
      confidence: "élevée", observed: missing.length ? `Non accordées : ${missing.join(", ")}.` : "Aucune autorisation essentielle manquante détectée.", measures,
      causes: [], checks: [], solutions: missing.length ? ["Accorder les autorisations depuis les Paramètres, en connaissance de cause (voir PERMISSIONS)"] : [],
      actionIds: missing.length ? ["notif_settings", "usage_access"] : [],
    };
  },
};

let counter = 0;
const uid = () => `${Date.now().toString(36)}-${(counter++).toString(36)}`;

export function runDiagnostic(snapshot: DeviceSnapshot, thresholds: Thresholds, query: string): DiagnosticReport {
  const topics = parseIntent(query);
  const findings = topics.map((tp) => rules[tp](snapshot, thresholds));

  // Diagnostic croisé : seulement si plusieurs indicateurs MESURÉS sont anormaux.
  const abnormalMeasured = findings.filter((f) => f.severity !== "ok" && f.confidence !== "faible");
  if (abnormalMeasured.length >= 2) {
    const conf: Confidence = abnormalMeasured.length >= 3 ? "moyenne" : "faible";
    findings.unshift({
      id: "cross", topic: "cross", title: "Diagnostic croisé : plusieurs anomalies simultanées", severity: worstOf(abnormalMeasured.map((f) => f.severity)),
      confidence: conf, observed: `Indicateurs anormaux : ${abnormalMeasured.map((f) => f.title.toLowerCase()).join(" ; ")}.`,
      measures: abnormalMeasured.flatMap((f) => f.measures.slice(0, 1)),
      causes: ["Hypothèse : une application ou un processus consomme des ressources de façon anormale — cause NON vérifiée"],
      checks: ["Consulter l'utilisation de la batterie et des données par application", "Redémarrer puis comparer les mesures"],
      solutions: ["Redémarrer le téléphone", "Mettre à jour les applications et le système"], actionIds: ["battery_usage", "restart"],
    });
  }

  const missing = findings.flatMap((f) => f.measures.filter((m) => m.source === "unavailable").map((m) => m.label));
  const worst = worstOf(findings.map((f) => f.severity));
  const problems = findings.filter((f) => f.severity !== "ok" && f.topic !== "cross");
  const summary = problems.length
    ? `${problems.length} point(s) à surveiller : ${problems.map((f) => f.title.toLowerCase()).join(", ")}.`
    : missing.length === findings.reduce((n, f) => n + f.measures.length, 0)
      ? "Je n'ai pas assez de mesures pour conclure."
      : "Aucun problème détecté sur les mesures disponibles.";

  return { id: uid(), createdAt: Date.now(), query, topics, findings, missing: [...new Set(missing)], summary, worst, adapter: snapshot.adapter };
}
