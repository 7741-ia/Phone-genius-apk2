import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Mic, MicOff, Send, Volume2, VolumeX } from "lucide-react";
import { useApp } from "@/store/app-store";
import { Btn, Card, PageTitle } from "@/components/pg/ui";
import { listenOffline, offlineRecognitionAvailable, parseCommand, speak, stopSpeaking, ttsAvailable } from "@/core/voice";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/voix")({
  head: () => ({
    meta: [
      { title: "Voice Genius hors ligne — Phone Genius" },
      { name: "description", content: "Dialoguez avec Phone Genius à la voix, sans envoyer vos enregistrements à un serveur." },
      { property: "og:title", content: "Voice Genius hors ligne — Phone Genius" },
      { property: "og:description", content: "Assistant vocal local et privé." },
    ],
  }),
  component: VoicePage,
});

function VoicePage() {
  const { diagnose, settings, update, voiceLog, addVoice } = useApp();
  const [listening, setListening] = useState(false);
  const [text, setText] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [caps, setCaps] = useState({ stt: false, tts: false });

  useEffect(() => {
    const check = () => setCaps({ stt: offlineRecognitionAvailable(), tts: ttsAvailable() });
    check();
    if (typeof speechSynthesis !== "undefined") speechSynthesis.onvoiceschanged = check;
  }, []);

  const say = (reply: string) => {
    if (settings.voice.silent) return;
    if (!speak(reply, settings.voice)) setNotice("Aucune voix locale française détectée : réponse affichée en texte uniquement.");
  };

  const respond = (input: string) => {
    const cmd = parseCommand(input);
    let reply: string;
    if (cmd === "help") reply = "Vous pouvez me dire : pourquoi mon téléphone chauffe, ma batterie se vide, analyse tout, silence, réactive la voix, ou répète.";
    else if (cmd === "silence") { update({ voice: { ...settings.voice, silent: true } }); reply = "Mode silencieux activé. Je répondrai uniquement par écrit."; }
    else if (cmd === "unsilence") { update({ voice: { ...settings.voice, silent: false } }); reply = "Voix réactivée."; }
    else if (cmd === "repeat") reply = voiceLog[0]?.reply ?? "Je n'ai encore rien dit.";
    else {
      const r = diagnose(input);
      if (!r) reply = "Je n'ai pas encore lu les indicateurs du téléphone.";
      else {
        const checks = [...new Set(r.findings.flatMap((f) => f.measures.map((m) => m.label.toLowerCase())))].slice(0, 3).join(", ");
        const tip = r.findings.find((f) => f.severity !== "ok")?.solutions[0];
        reply = `Je vais examiner les informations disponibles, notamment ${checks}. ${r.summary}${tip ? ` Je vous propose : ${tip.toLowerCase()}.` : ""}${r.adapter === "demo" ? " Attention, ce sont des données fictives de démonstration." : ""}`;
      }
    }
    addVoice({ user: input, reply });
    if (cmd !== "silence") say(reply);
  };

  const startMic = async () => {
    setNotice(null);
    if (!caps.stt) {
      setNotice("Aucun moteur de reconnaissance vocale hors ligne n'est installé. Dans la version Android, installez ou téléchargez un modèle local (ex. paquet de langue française hors ligne ou Vosk). En attendant, écrivez votre question ci-dessous.");
      return;
    }
    setListening(true);
    try {
      const heard = await listenOffline();
      if (heard) { setText(heard); respond(heard); } else setNotice("Je n'ai rien entendu.");
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "Écoute impossible (micro refusé ?)");
    } finally { setListening(false); }
  };

  return (
    <div className="space-y-5">
      <PageTitle title="Voice Genius" subtitle="Aucun enregistrement n'est envoyé à un serveur." />
      <Card className="pg-hero flex flex-col items-center gap-4 py-8">
        <button onClick={listening ? () => setListening(false) : startMic} aria-label={listening ? "Arrêter l'écoute" : "Démarrer l'écoute"}
          className={cn("pg-btn-primary grid h-24 w-24 place-items-center rounded-full", listening && "pg-mic-listening")}>
          {listening ? <MicOff className="h-9 w-9" /> : <Mic className="h-9 w-9" />}
        </button>
        <div className="text-sm text-muted-foreground">{listening ? "J'écoute…" : "Touchez pour parler"}</div>
        <div className="flex flex-wrap justify-center gap-2 text-[11px] font-mono">
          <span className={cn("rounded px-2 py-0.5", caps.stt ? "bg-success/15 text-success" : "bg-warning/15 text-warning")}>Reconnaissance hors ligne : {caps.stt ? "disponible" : "absente"}</span>
          <span className={cn("rounded px-2 py-0.5", caps.tts ? "bg-success/15 text-success" : "bg-warning/15 text-warning")}>Voix locale : {caps.tts ? "disponible" : "absente"}</span>
        </div>
        {notice && <p role="status" className="max-w-md text-center text-xs text-warning">{notice}</p>}
      </Card>

      <Card>
        <form onSubmit={(e) => { e.preventDefault(); if (text.trim()) { respond(text.trim()); setText(""); } }} className="flex gap-2">
          <label htmlFor="vt" className="sr-only">Votre question</label>
          <input id="vt" value={text} onChange={(e) => setText(e.target.value)} placeholder="Phone Genius, pourquoi mon téléphone chauffe ?"
            className="flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          <Btn type="submit" disabled={!text.trim()}><Send className="h-4 w-4" /></Btn>
        </form>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <Btn variant="ghost" onClick={() => { stopSpeaking(); update({ voice: { ...settings.voice, silent: !settings.voice.silent } }); }}>
            {settings.voice.silent ? <><VolumeX className="h-4 w-4" />Silencieux</> : <><Volume2 className="h-4 w-4" />Voix active</>}
          </Btn>
          <label className="text-xs">Volume {Math.round(settings.voice.volume * 100)} %
            <input type="range" min={0} max={1} step={0.1} value={settings.voice.volume} onChange={(e) => update({ voice: { ...settings.voice, volume: +e.target.value } })} className="w-full accent-primary" />
          </label>
          <label className="text-xs">Vitesse ×{settings.voice.rate.toFixed(1)}
            <input type="range" min={0.5} max={2} step={0.1} value={settings.voice.rate} onChange={(e) => update({ voice: { ...settings.voice, rate: +e.target.value } })} className="w-full accent-primary" />
          </label>
        </div>
      </Card>

      <Card>
        <h2 className="font-display font-semibold">Échanges (stockés localement)</h2>
        {voiceLog.length === 0 ? <p className="mt-2 text-sm text-muted-foreground">Essayez « aide » pour connaître les commandes.</p> : (
          <ul className="mt-3 space-y-3">{voiceLog.slice(0, 20).map((v) => (
            <li key={v.id} className="space-y-1 text-sm">
              <div className="ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-sm bg-primary/20 px-3 py-2">{v.user}</div>
              <div className="w-fit max-w-[85%] rounded-2xl rounded-bl-sm bg-muted px-3 py-2">{v.reply}</div>
            </li>
          ))}</ul>
        )}
      </Card>
    </div>
  );
}
