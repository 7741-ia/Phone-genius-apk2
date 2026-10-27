/** Stockage local (localStorage). Côté Android : DataStore / Room. Rien ne quitte l'appareil. */
const PREFIX = "phone-genius:";

export function load<T>(key: string, fallback: T): T {
  if (typeof localStorage === "undefined") return fallback;
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw ? ({ ...(fallback as object), ...JSON.parse(raw) } as T) : fallback;
  } catch { return fallback; }
}

export function loadList<T>(key: string): T[] {
  if (typeof localStorage === "undefined") return [];
  try { return JSON.parse(localStorage.getItem(PREFIX + key) ?? "[]") as T[]; } catch { return []; }
}

export function save(key: string, value: unknown) {
  try { localStorage.setItem(PREFIX + key, JSON.stringify(value)); } catch { /* quota */ }
}

export function clearAll() {
  Object.keys(localStorage).filter((k) => k.startsWith(PREFIX)).forEach((k) => localStorage.removeItem(k));
}
