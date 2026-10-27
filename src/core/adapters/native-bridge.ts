/**
 * Contrat du pont JavaScript ⇄ Kotlin.
 * À implémenter côté Android via `webView.addJavascriptInterface(obj, "PhoneGeniusNative")`
 * (ou un plugin Capacitor exposant les mêmes méthodes). Voir ANDROID_IMPLEMENTATION.md.
 * Aucune de ces méthodes n'existe dans un navigateur ordinaire.
 */
export interface PhoneGeniusNativeBridge {
  /** JSON sérialisé conforme à DeviceSnapshot (source = "native"). */
  getSnapshot(): Promise<string> | string;
  /** Ouvre un écran Paramètres (ex. "android.settings.BATTERY_SAVER_SETTINGS"). */
  openSettings(action: string): void;
  /** Notification locale via NotificationManager. */
  notify(title: string, body: string, priority: string): void;
  /** Synthèse vocale via TextToSpeech. */
  speak(text: string, rate: number, volume: number): void;
  /** Reconnaissance vocale hors ligne ; renvoie "" si aucun moteur local. */
  listenOffline?(): Promise<string>;
  hasOfflineSpeechRecognizer?(): boolean;
  /** Programme les contrôles périodiques WorkManager. */
  scheduleChecks(intervalMinutes: number, thresholdsJson: string): void;
}

export function getNativeBridge(): PhoneGeniusNativeBridge | null {
  if (typeof window === "undefined") return null;
  return (window as unknown as { PhoneGeniusNative?: PhoneGeniusNativeBridge }).PhoneGeniusNative ?? null;
}
