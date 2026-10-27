/**
 * Contrats de données typés de Phone Genius.
 * Chaque mesure porte sa SOURCE : on ne confond jamais une valeur fictive
 * (demo) avec une valeur réelle (web / native).
 */
export type DataSource = "demo" | "web" | "native" | "unavailable";

export interface Metric<T> {
  value: T | null;
  source: DataSource;
  /** Explication (ex. « non exposé par le navigateur »). */
  note?: string;
}

export interface BatteryInfo {
  levelPct: Metric<number>;
  charging: Metric<boolean>;
  temperatureC: Metric<number>;
  health: Metric<string>;
  /** Vitesse de décharge estimée (%/h). Null si non fiable. */
  drainPctPerHour: Metric<number>;
}

export interface MemoryInfo {
  totalMb: Metric<number>;
  availableMb: Metric<number>;
}

export interface StorageInfo {
  totalGb: Metric<number>;
  freeGb: Metric<number>;
}

export interface DataUsageInfo {
  mobileTodayMb: Metric<number>;
  avgDailyMb: Metric<number>;
}

export interface PermissionsInfo {
  notifications: Metric<boolean>;
  usageAccess: Metric<boolean>;
  microphone: Metric<boolean>;
}

export interface DeviceSnapshot {
  takenAt: number;
  adapter: DataSource;
  battery: BatteryInfo;
  memory: MemoryInfo;
  storage: StorageInfo;
  data: DataUsageInfo;
  permissions: PermissionsInfo;
}

export type Severity = "ok" | "low" | "medium" | "high" | "critical";
export type Confidence = "faible" | "moyenne" | "élevée";
export type Topic = "heat" | "battery" | "slow" | "storage" | "data" | "permissions";

export interface Thresholds {
  batteryLow: number;
  batteryCritical: number;
  storageFreePctLow: number;
  storageFreePctCritical: number;
  ramFreePctLow: number;
  tempHigh: number;
  tempCritical: number;
  dataDailyMbHigh: number;
  drainPctPerHourHigh: number;
}

export interface MeasureLine {
  label: string;
  value: string;
  source: DataSource;
}

export interface Finding {
  id: string;
  topic: Topic | "cross";
  title: string;
  severity: Severity;
  confidence: Confidence;
  observed: string;
  measures: MeasureLine[];
  causes: string[];
  checks: string[];
  solutions: string[];
  actionIds: string[];
}

export interface DiagnosticReport {
  id: string;
  createdAt: number;
  query: string;
  topics: Topic[];
  findings: Finding[];
  missing: string[];
  summary: string;
  worst: Severity;
  adapter: DataSource;
}

export type AlertCategory =
  | "battery_low"
  | "battery_drain"
  | "storage"
  | "memory"
  | "data"
  | "temperature"
  | "permission"
  | "anomaly";

export type AlertPriority = "faible" | "moyenne" | "élevée" | "critique";

export interface AlertEvent {
  id: string;
  category: AlertCategory;
  priority: AlertPriority;
  message: string;
  createdAt: number;
  adapter: DataSource;
  read: boolean;
}

export type ActionAvailability = "auto" | "confirm" | "special" | "manual" | "impossible";

export interface CareAction {
  id: string;
  title: string;
  description: string;
  availability: ActionAvailability;
  destructive: boolean;
  /** Intent Android de référence (implémentation native future). */
  androidSettings?: string;
  steps: string[];
}
