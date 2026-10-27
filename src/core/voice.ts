/**
 * Couche vocale. Confidentialité : on n'utilise PAS la reconnaissance vocale du
 * navigateur (Chrome l'envoie à un serveur). La reconnaissance passe uniquement
 * par le pont natif (moteur hors ligne). La synthèse utilise seulement les voix
 * LOCALES (localService = true).
 */
import { getNativeBridge } from "./adapters/native-bridge";

export function offlineRecognitionAvailable(): boolean {
  return !!getNativeBridge()?.hasOfflineSpeechRecognizer?.();
}

export async function listenOffline(): Promise<string> {
  const b = getNativeBridge();
  if (!b?.listenOffline) throw new Error("Aucun moteur de reconnaissance hors ligne");
  return b.listenOffline();
}

export function localFrenchVoice(): SpeechSynthesisVoice | null {
  if (typeof speechSynthesis === "undefined") return null;
  const voices = speechSynthesis.getVoices().filter((v) => v.localService);
  return voices.find((v) => v.lang.startsWith("fr")) ?? null;
}

export function ttsAvailable(): boolean {
  return !!getNativeBridge() || localFrenchVoice() !== null;
}

export function speak(text: string, opts: { rate: number; volume: number }): boolean {
  const b = getNativeBridge();
  if (b) { b.speak(text, opts.rate, opts.volume); return true; }
  const voice = localFrenchVoice();
  if (!voice) return false;
  speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.voice = voice; u.lang = voice.lang; u.rate = opts.rate; u.volume = opts.volume;
  speechSynthesis.speak(u);
  return true;
}

export function stopSpeaking() {
  if (typeof speechSynthesis !== "undefined") speechSynthesis.cancel();
}

export type VoiceCommand = "help" | "silence" | "unsilence" | "repeat" | "diagnose";
export function parseCommand(text: string): VoiceCommand {
  const t = text.toLowerCase();
  if (/aide|que sais-tu|commandes/.test(t)) return "help";
  if (/réactive|reactive|parle à nouveau/.test(t)) return "unsilence";
  if (/silence|tais-toi|muet/.test(t)) return "silence";
  if (/répète|repete/.test(t)) return "repeat";
  return "diagnose";
}
