import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { createAdapter, type DemoScenario } from "@/core/adapters/device";
import { DEFAULT_ALERT_SETTINGS, evaluateAlerts, type AlertSettings } from "@/core/alerts";
import { DEFAULT_THRESHOLDS, runDiagnostic } from "@/core/rules/engine";
import { clearAll, load, loadList, save } from "@/core/storage";
import type { AlertEvent, DeviceSnapshot, DiagnosticReport, Thresholds } from "@/core/types";
import { speak } from "@/core/voice";
import { getNativeBridge } from "@/core/adapters/native-bridge";

export type ViewMode = "simple" | "avance" | "expert";

export interface Settings {
  mode: ViewMode;
  theme: "dark" | "light";
  adapter: "demo" | "web";
  scenario: DemoScenario;
  thresholds: Thresholds;
  alerts: AlertSettings;
  voice: { volume: number; rate: number; silent: boolean };
}

export const DEFAULT_SETTINGS: Settings = {
  mode: "simple", theme: "dark", adapter: "demo", scenario: "surchauffe",
  thresholds: DEFAULT_THRESHOLDS, alerts: DEFAULT_ALERT_SETTINGS,
  voice: { volume: 1, rate: 1, silent: false },
};

export interface VoiceExchange { id: string; at: number; user: string; reply: string }
export interface Operation { id: string; at: number; actionId: string; label: string; result: string }

interface Store {
  ready: boolean;
  settings: Settings;
  update: (patch: Partial<Settings>) => void;
  snapshot: DeviceSnapshot | null;
  snapshotError: string | null;
  history: DeviceSnapshot[];
  refresh: () => Promise<void>;
  reports: DiagnosticReport[];
  diagnose: (query: string) => DiagnosticReport | null;
  alerts: AlertEvent[];
  markAlertsRead: () => void;
  voiceLog: VoiceExchange[];
  addVoice: (e: Omit<VoiceExchange, "id" | "at">) => void;
  operations: Operation[];
  logOperation: (o: Omit<Operation, "id" | "at">) => void;
  clearHistory: () => void;
}

const Ctx = createContext<Store | null>(null);
const id = () => `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
const cap = <T,>(l: T[], n = 50) => l.slice(0, n);

export function AppStoreProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [snapshot, setSnapshot] = useState<DeviceSnapshot | null>(null);
  const [snapshotError, setSnapshotError] = useState<string | null>(null);
  const [history, setHistory] = useState<DeviceSnapshot[]>([]);
  const [reports, setReports] = useState<DiagnosticReport[]>([]);
  const [alerts, setAlerts] = useState<AlertEvent[]>([]);
  const [voiceLog, setVoiceLog] = useState<VoiceExchange[]>([]);
  const [operations, setOperations] = useState<Operation[]>([]);

  // Hydratation depuis le stockage local (après montage : compatible SSR).
  useEffect(() => {
    const s = load<Settings>("settings", DEFAULT_SETTINGS);
    s.thresholds = { ...DEFAULT_THRESHOLDS, ...s.thresholds };
    s.alerts = { ...DEFAULT_ALERT_SETTINGS, ...s.alerts, enabled: { ...DEFAULT_ALERT_SETTINGS.enabled, ...s.alerts?.enabled } };
    setSettings(s);
    setHistory(loadList("snapshots"));
    setReports(loadList("reports"));
    setAlerts(loadList("alerts"));
    setVoiceLog(loadList("voice"));
    setOperations(loadList("operations"));
    setReady(true);
  }, []);

  useEffect(() => { if (ready) save("settings", settings); }, [ready, settings]);
  useEffect(() => { if (ready) save("snapshots", history); }, [ready, history]);
  useEffect(() => { if (ready) save("reports", reports); }, [ready, reports]);
  useEffect(() => { if (ready) save("alerts", alerts); }, [ready, alerts]);
  useEffect(() => { if (ready) save("voice", voiceLog); }, [ready, voiceLog]);
  useEffect(() => { if (ready) save("operations", operations); }, [ready, operations]);

  useEffect(() => {
    document.documentElement.classList.toggle("light", settings.theme === "light");
  }, [settings.theme]);

  const update = useCallback((patch: Partial<Settings>) => setSettings((s) => ({ ...s, ...patch })), []);

  const refresh = useCallback(async () => {
    try {
      const snap = await createAdapter(settings.adapter, settings.scenario).readSnapshot();
      setSnapshot(snap);
      setSnapshotError(null);
      setHistory((h) => cap([snap, ...h], 30));
      setAlerts((prev) => {
        const fresh = evaluateAlerts(snap, settings.thresholds, settings.alerts, prev);
        if (fresh.length && !settings.alerts.silent) {
          const top = fresh[0]!;
          getNativeBridge()?.notify("Phone Genius", top.message, top.priority);
          if (settings.alerts.voice && !settings.voice.silent) speak(`Phone Genius : ${top.message}`, settings.voice);
        }
        return cap([...fresh, ...prev], 100);
      });
    } catch (e) {
      setSnapshotError(e instanceof Error ? e.message : "Lecture impossible");
    }
  }, [settings.adapter, settings.scenario, settings.thresholds, settings.alerts, settings.voice]);

  useEffect(() => { if (ready) void refresh(); }, [ready, settings.adapter, settings.scenario]); // eslint-disable-line react-hooks/exhaustive-deps

  const diagnose = useCallback((query: string) => {
    if (!snapshot) return null;
    const r = runDiagnostic(snapshot, settings.thresholds, query);
    setReports((l) => cap([r, ...l]));
    return r;
  }, [snapshot, settings.thresholds]);

  const value = useMemo<Store>(() => ({
    ready, settings, update, snapshot, snapshotError, history, refresh, reports, diagnose, alerts,
    markAlertsRead: () => setAlerts((l) => l.map((a) => ({ ...a, read: true }))),
    voiceLog, addVoice: (e) => setVoiceLog((l) => cap([{ ...e, id: id(), at: Date.now() }, ...l])),
    operations, logOperation: (o) => setOperations((l) => cap([{ ...o, id: id(), at: Date.now() }, ...l])),
    clearHistory: () => {
      clearAll();
      setHistory([]); setReports([]); setAlerts([]); setVoiceLog([]); setOperations([]);
      save("settings", settings);
    },
  }), [ready, settings, update, snapshot, snapshotError, history, refresh, reports, diagnose, alerts, voiceLog, operations]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useApp() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useApp hors du AppStoreProvider");
  return c;
}
