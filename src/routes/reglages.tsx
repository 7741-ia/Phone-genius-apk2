import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useApp } from "@/store/app-store";
import { Btn, Card, PageTitle, Toggle } from "@/components/pg/ui";
import { DEMO_SCENARIOS, type DemoScenario } from "@/core/adapters/device";
import { getNativeBridge } from "@/core/adapters/native-bridge";

export const Route = createFileRoute("/reglages")({
  head: () => ({
    meta: [
      { title: "Réglages et confidentialité — Phone Genius" },
      { name: "description", content: "Source des données, thème, scénarios de démonstration et effacement de l'historique local." },
      { property: "og:title", content: "Réglages et confidentialité — Phone Genius" },
      { property: "og:description", content: "Vos données restent sur votre appareil." },
    ],
  }),
  component: SettingsPage,
});

function SettingsPage() {
  const { settings, update, clearHistory } = useApp();
  const [confirm, setConfirm] = useState(false);
  const [done, setDone] = useState(false);
  const native = typeof window !== "undefined" && !!getNativeBridge();
  const [notif, setNotif] = useState<string | null>(null);

  return (
    <div className="space-y-5">
      <PageTitle title="Réglages" subtitle="Tout est enregistré uniquement sur cet appareil." />
      <Card>
        <h2 className="font-display font-semibold">Source des données</h2>
        {native ? <p className="mt-2 text-sm text-success">Pont Android natif détecté : données réelles.</p> : (
          <div className="mt-2 space-y-2 text-sm">
            <label className="flex items-start gap-2"><input type="radio" checked={settings.adapter === "demo"} onChange={() => update({ adapter: "demo" })} className="mt-1 accent-primary" />
              <span><b>Démonstration</b> — valeurs FICTIVES, signalées partout par le badge « FICTIF ».</span></label>
            <label className="flex items-start gap-2"><input type="radio" checked={settings.adapter === "web"} onChange={() => update({ adapter: "web" })} className="mt-1 accent-primary" />
              <span><b>Navigateur (réel)</b> — seulement ce que le navigateur expose (batterie sur certains navigateurs, RAM approximative). Le reste s'affiche « Indisponible ».</span></label>
            {settings.adapter === "demo" && (
              <label className="block pt-2">Scénario de démonstration
                <select value={settings.scenario} onChange={(e) => update({ scenario: e.target.value as DemoScenario })} className="ml-2 rounded-md border border-input bg-background px-2 py-1">
                  {Object.entries(DEMO_SCENARIOS).map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                </select>
              </label>
            )}
          </div>
        )}
      </Card>

      <Card>
        <h2 className="font-display font-semibold">Affichage</h2>
        <Toggle label="Thème clair" checked={settings.theme === "light"} onChange={(v) => update({ theme: v ? "light" : "dark" })} />
        <label className="block py-2 text-sm">Mode
          <select value={settings.mode} onChange={(e) => update({ mode: e.target.value as typeof settings.mode })} className="ml-2 rounded-md border border-input bg-background px-2 py-1">
            <option value="simple">Simple</option><option value="avance">Avancé</option><option value="expert">Expert</option>
          </select>
        </label>
      </Card>

      <Card>
        <h2 className="font-display font-semibold">Autorisations</h2>
        <p className="mt-1 text-sm text-muted-foreground">Notifications : permettent d'afficher les alertes. Aucune donnée n'est transmise.</p>
        <Btn variant="ghost" className="mt-2" onClick={async () => {
          if (typeof Notification === "undefined") return setNotif("Notifications non supportées par ce navigateur.");
          const r = await Notification.requestPermission();
          setNotif(r === "granted" ? "Notifications autorisées." : "Autorisation refusée : les alertes resteront visibles dans l'application.");
        }}>Autoriser les notifications</Btn>
        {notif && <p role="status" className="mt-2 text-xs text-muted-foreground">{notif}</p>}
      </Card>

      <Card>
        <h2 className="font-display font-semibold">Confidentialité</h2>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-muted-foreground">
          <li>Aucun compte, aucune publicité, aucun suivi.</li>
          <li>Aucune donnée du téléphone n'est envoyée à un serveur.</li>
          <li>La reconnaissance vocale du navigateur (envoyée à un serveur par certains navigateurs) n'est volontairement pas utilisée.</li>
        </ul>
        {!confirm ? <Btn variant="danger" className="mt-3" onClick={() => setConfirm(true)}>Effacer l'historique local</Btn> : (
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
            <span>Effacer diagnostics, alertes, échanges vocaux et opérations ?</span>
            <Btn variant="danger" onClick={() => { clearHistory(); setConfirm(false); setDone(true); }}>Oui, effacer</Btn>
            <Btn variant="ghost" onClick={() => setConfirm(false)}>Annuler</Btn>
          </div>
        )}
        {done && <p role="status" className="mt-2 text-xs text-success">Historique effacé.</p>}
      </Card>
    </div>
  );
}
