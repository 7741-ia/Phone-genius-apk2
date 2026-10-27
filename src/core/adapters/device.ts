import type { DataSource, DeviceSnapshot, Metric } from "../types";
import { getNativeBridge } from "./native-bridge";

/** Contrat commun à toutes les sources de données de l'appareil. */
export interface DeviceAdapter {
  readonly kind: DataSource;
  readSnapshot(): Promise<DeviceSnapshot>;
}

const na = <T,>(note: string): Metric<T> => ({ value: null, source: "unavailable", note });
const demo = <T,>(value: T): Metric<T> => ({ value, source: "demo" });

export type DemoScenario = "normal" | "surchauffe" | "stockage" | "batterie" | "lent";

export const DEMO_SCENARIOS: Record<DemoScenario, string> = {
  normal: "Appareil en bon état",
  surchauffe: "Surchauffe + décharge rapide",
  stockage: "Stockage presque plein",
  batterie: "Batterie faible",
  lent: "Téléphone lent (RAM saturée)",
};

/** Données FICTIVES, toujours marquées source = "demo". */
export class DemoAdapter implements DeviceAdapter {
  readonly kind = "demo" as const;
  constructor(private scenario: DemoScenario = "normal") {}

  async readSnapshot(): Promise<DeviceSnapshot> {
    const s = this.scenario;
    return {
      takenAt: Date.now(),
      adapter: "demo",
      battery: {
        levelPct: demo(s === "batterie" ? 12 : s === "surchauffe" ? 41 : 78),
        charging: demo(false),
        temperatureC: demo(s === "surchauffe" ? 44.5 : 31.2),
        health: demo("Bonne"),
        drainPctPerHour: demo(s === "surchauffe" ? 19 : s === "batterie" ? 9 : 5),
      },
      memory: { totalMb: demo(8192), availableMb: demo(s === "lent" ? 480 : s === "surchauffe" ? 1100 : 3400) },
      storage: { totalGb: demo(128), freeGb: demo(s === "stockage" ? 4.2 : 46.8) },
      data: { mobileTodayMb: demo(s === "surchauffe" ? 1450 : 220), avgDailyMb: demo(310) },
      permissions: {
        notifications: demo(true),
        usageAccess: demo(s !== "normal"),
        microphone: demo(true),
      },
    };
  }
}

/** Ce que le NAVIGATEUR expose réellement — le reste est déclaré indisponible. */
export class WebAdapter implements DeviceAdapter {
  readonly kind = "web" as const;
  async readSnapshot(): Promise<DeviceSnapshot> {
    const nav = (typeof navigator !== "undefined" ? navigator : {}) as Navigator & {
      getBattery?: () => Promise<{ level: number; charging: boolean }>;
      deviceMemory?: number;
    };
    let levelPct: Metric<number> = na("API Batterie non disponible dans ce navigateur");
    let charging: Metric<boolean> = na("API Batterie non disponible");
    try {
      if (nav.getBattery) {
        const b = await nav.getBattery();
        levelPct = { value: Math.round(b.level * 100), source: "web" };
        charging = { value: b.charging, source: "web" };
      }
    } catch {
      /* refusé ou indisponible */
    }
    const totalMb: Metric<number> = nav.deviceMemory
      ? { value: nav.deviceMemory * 1024, source: "web", note: "Approximation arrondie par le navigateur" }
      : na("Non exposé par le navigateur");
    const notif =
      typeof Notification !== "undefined"
        ? { value: Notification.permission === "granted", source: "web" as const }
        : na<boolean>("Notifications web non supportées");
    return {
      takenAt: Date.now(),
      adapter: "web",
      battery: {
        levelPct,
        charging,
        temperatureC: na("Nécessite BatteryManager (Android natif)"),
        health: na("Nécessite BatteryManager (Android natif)"),
        drainPctPerHour: na("Estimation fiable impossible depuis le web"),
      },
      memory: { totalMb, availableMb: na("Nécessite ActivityManager (Android natif)") },
      storage: {
        totalGb: na("Le navigateur ne donne que le quota du site, pas le stockage du téléphone"),
        freeGb: na("Nécessite StatFs (Android natif)"),
      },
      data: {
        mobileTodayMb: na("Nécessite NetworkStatsManager + accès spécial"),
        avgDailyMb: na("Nécessite NetworkStatsManager + accès spécial"),
      },
      permissions: {
        notifications: notif,
        usageAccess: na("Concept propre à Android"),
        microphone: na("Vérifié au moment de l'utilisation"),
      },
    };
  }
}

/** Adaptateur relié au pont Kotlin (WebView / Capacitor). */
export class NativeAdapter implements DeviceAdapter {
  readonly kind = "native" as const;
  async readSnapshot(): Promise<DeviceSnapshot> {
    const bridge = getNativeBridge();
    if (!bridge) throw new Error("Pont natif Android absent");
    return JSON.parse(await bridge.getSnapshot()) as DeviceSnapshot;
  }
}

export function createAdapter(pref: "demo" | "web", scenario: DemoScenario): DeviceAdapter {
  if (getNativeBridge()) return new NativeAdapter();
  return pref === "web" ? new WebAdapter() : new DemoAdapter(scenario);
}
