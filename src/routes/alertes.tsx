import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/store/app-store";
import { Btn, Card, PageTitle, SourceBadge, Toggle } from "@/components/pg/ui";
import { ALERT_LABELS } from "@/core/alerts";
import type { AlertCategory, AlertPriority, Thresholds } from "@/core/types";

export const Route = createFileRoute("/alertes")({
  head: () => ({
    meta: [
      { title: "Centre d'alertes — Phone Genius" },
      { name: "description", content: "Alertes batterie, stockage, mémoire, température et données avec seuils personnalisables." },
      { property: "og:title", content: "Centre d'alertes — Phone Genius" },
      { property: "og:description", content: "Alertes locales configurables." },
    ],
  }),
  component: AlertsPage,
});

const PRIO: Record<AlertPriority, string> = { faible: "bg-accent/15 text-accent", moyenne: "bg-warning/15 text-warning", "élevée": "bg-warning/25 text-warning", critique: "bg-critical/20 text-critical" };

const FIELDS: [keyof Thresholds, string, string][] = [
  ["batteryLow", "Batterie faible", "%"], ["batteryCritical", "Batterie critique", "%"], ["storageFreePctLow", "Stockage libre bas", "%"],
  ["storageFreePctCritical", "Stockage critique", "%"], ["ramFreePctLow", "RAM libre basse", "%"], ["tempHigh", "Température élevée", "°C"],
  ["tempCritical", "Température critique", "°C"], ["dataDailyMbHigh", "Données / jour", "Mo"], ["drainPctPerHourHigh", "Décharge rapide", "%/h"],
];

function AlertsPage() {
  const { alerts, settings, update, markAlertsRead, refresh } = useApp();
  const a = settings.alerts;

  return (
    <div className="space-y-5">
      <PageTitle title="Centre d'alertes" subtitle="Vérifié à chaque actualisation. La surveillance permanente en arrière-plan nécessitera WorkManager (Android)." />
      <div className="flex flex-wrap gap-2">
        <Btn onClick={() => void refresh()}>Vérifier maintenant</Btn>
        <Btn variant="ghost" onClick={markAlertsRead}>Tout marquer comme lu</Btn>
      </div>

      <Card>
        <h2 className="font-display font-semibold">Historique</h2>
        {alerts.length === 0 ? <p className="mt-2 text-sm text-muted-foreground">Aucune alerte.</p> : (
          <ul className="mt-2 divide-y divide-border/50">{alerts.slice(0, 30).map((al) => (
            <li key={al.id} className="flex flex-wrap items-start justify-between gap-2 py-2 text-sm">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  {!al.read && <span className="h-2 w-2 rounded-full bg-primary" />}
                  <b>{ALERT_LABELS[al.category]}</b><span className={`rounded px-1.5 text-[10px] font-semibold ${PRIO[al.priority]}`}>{al.priority}</span><SourceBadge source={al.adapter} />
                </div>
                <p className="mt-0.5 text-muted-foreground">{al.message}</p>
                <span className="text-xs text-muted-foreground">{new Date(al.createdAt).toLocaleString("fr-FR")}</span>
              </div>
              <Link to="/diagnostic" className="text-xs text-primary">Recommandations →</Link>
            </li>
          ))}</ul>
        )}
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <h2 className="font-display font-semibold">Catégories et options</h2>
          {(Object.keys(ALERT_LABELS) as AlertCategory[]).map((c) => (
            <Toggle key={c} label={ALERT_LABELS[c]} checked={a.enabled[c]} onChange={(v) => update({ alerts: { ...a, enabled: { ...a.enabled, [c]: v } } })} />
          ))}
          <div className="my-2 border-t border-border" />
          <Toggle label="Mode silencieux" checked={a.silent} onChange={(v) => update({ alerts: { ...a, silent: v } })} />
          <Toggle label="Lecture vocale des alertes" checked={a.voice} onChange={(v) => update({ alerts: { ...a, voice: v } })} />
          <label className="mt-2 block text-sm">Ne pas répéter une même alerte avant
            <select value={a.cooldownMinutes} onChange={(e) => update({ alerts: { ...a, cooldownMinutes: +e.target.value } })} className="ml-2 rounded-md border border-input bg-background px-2 py-1">
              {[0, 5, 15, 30, 60, 180, 720].map((m) => <option key={m} value={m}>{m === 0 ? "aucun délai" : `${m} min`}</option>)}
            </select>
          </label>
        </Card>
        <Card>
          <h2 className="font-display font-semibold">Seuils personnalisables</h2>
          <div className="mt-2 grid grid-cols-2 gap-3">
            {FIELDS.map(([k, l, u]) => (
              <label key={k} className="text-xs text-muted-foreground">{l} ({u})
                <input type="number" value={settings.thresholds[k]} onChange={(e) => update({ thresholds: { ...settings.thresholds, [k]: Number(e.target.value) } })}
                  className="mt-1 w-full rounded-md border border-input bg-background px-2 py-1 font-mono text-sm text-foreground" />
              </label>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
