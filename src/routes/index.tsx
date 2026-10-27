import { createFileRoute, Link } from "@tanstack/react-router";
import { RefreshCw, Stethoscope } from "lucide-react";
import { useApp } from "@/store/app-store";
import { Btn, Card, Gauge, PageTitle, SevBadge, SourceBadge } from "@/components/pg/ui";
import { ramFreePct, runDiagnostic, storageFreePct } from "@/core/rules/engine";
import type { Severity } from "@/core/types";
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Tableau de bord — Phone Genius" },
      { name: "description", content: "État de la batterie, mémoire, stockage et données de votre téléphone Android." },
      { property: "og:title", content: "Tableau de bord — Phone Genius" },
      { property: "og:description", content: "État général du téléphone, analysé localement." },
    ],
  }),
  component: Dashboard,
});

const MODES = [["simple", "Simple"], ["avance", "Avancé"], ["expert", "Expert"]] as const;

function Dashboard() {
  const { snapshot, snapshotError, settings, update, refresh, reports, alerts, history } = useApp();
  const t = settings.thresholds;

  if (snapshotError) return <Card className="border-critical/50"><p className="text-critical">Lecture impossible : {snapshotError}</p></Card>;
  if (!snapshot) return <p className="text-muted-foreground">Lecture des indicateurs…</p>;

  const b = snapshot.battery;
  const lvl = b.levelPct.value;
  const sp = storageFreePct(snapshot);
  const rp = ramFreePct(snapshot);
  const temp = b.temperatureC.value;
  const data = snapshot.data.mobileTodayMb.value;
  const live = runDiagnostic(snapshot, t, "analyse complète");
  const last = reports[0];
  const activeAlerts = alerts.filter((a) => !a.read).length;
  const recos = live.findings.filter((f) => f.severity !== "ok" && f.topic !== "cross").slice(0, 3);

  const sevBat: Severity = lvl === null ? "low" : lvl <= t.batteryCritical ? "critical" : lvl <= t.batteryLow ? "high" : "ok";
  const sevSto: Severity = sp === null ? "low" : sp <= t.storageFreePctCritical ? "critical" : sp <= t.storageFreePctLow ? "high" : "ok";
  const sevRam: Severity = rp === null ? "low" : rp <= t.ramFreePctLow ? "medium" : "ok";
  const sevTmp: Severity = temp === null ? "low" : temp >= t.tempCritical ? "critical" : temp >= t.tempHigh ? "high" : "ok";
  const sevDat: Severity = data === null ? "low" : data >= t.dataDailyMbHigh ? "high" : "ok";

  const gauges = [
    { label: "Batterie", v: lvl, max: 100, d: lvl === null ? "—" : `${lvl}%`, s: sevBat, src: b.levelPct.source },
    { label: "Stockage libre", v: sp, max: 100, d: sp === null ? "—" : `${sp.toFixed(0)}%`, s: sevSto, src: snapshot.storage.freeGb.source },
    { label: "RAM libre", v: rp, max: 100, d: rp === null ? "—" : `${rp.toFixed(0)}%`, s: sevRam, src: snapshot.memory.availableMb.source },
    { label: "Température", v: temp, max: 60, d: temp === null ? "—" : `${temp.toFixed(0)}°`, s: sevTmp, src: b.temperatureC.source },
    { label: "Données (jour)", v: data, max: t.dataDailyMbHigh * 1.5, d: data === null ? "—" : `${data}Mo`, s: sevDat, src: snapshot.data.mobileTodayMb.source },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <PageTitle title="Tableau de bord" subtitle={`Mesuré le ${new Date(snapshot.takenAt).toLocaleString("fr-FR")}`} />
        <div className="flex rounded-lg border border-border p-1 text-xs">
          {MODES.map(([k, l]) => (
            <button key={k} onClick={() => update({ mode: k })} className={`rounded-md px-3 py-1.5 font-semibold ${settings.mode === k ? "pg-btn-primary" : "text-muted-foreground"}`}>{l}</button>
          ))}
        </div>
      </div>

      <Card className="pg-hero">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="text-xs uppercase tracking-widest text-muted-foreground">État général</div>
            <div className="mt-1 flex items-center gap-2"><SevBadge severity={live.worst} /><SourceBadge source={snapshot.adapter} /></div>
            <p className="mt-2 max-w-md text-sm">{live.summary}</p>
          </div>
          <div className="flex gap-2">
            <Btn variant="ghost" onClick={() => void refresh()}><RefreshCw className="h-4 w-4" />Actualiser</Btn>
            <Link to="/diagnostic"><Btn><Stethoscope className="h-4 w-4" />Diagnostiquer</Btn></Link>
          </div>
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {gauges.map((g) => (
          <Card key={g.label} className="flex flex-col items-center gap-2">
            <Gauge value={g.v} max={g.max} label={g.label} display={g.d} severity={g.s} unavailable={g.v === null} />
            <SourceBadge source={g.src} />
          </Card>
        ))}
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <Card><div className="text-xs text-muted-foreground">Alertes non lues</div><div className="mt-1 font-mono text-2xl font-bold">{activeAlerts}</div><Link to="/alertes" className="text-xs text-primary">Voir les alertes →</Link></Card>
        <Card><div className="text-xs text-muted-foreground">Dernier diagnostic</div><div className="mt-1 text-sm">{last ? new Date(last.createdAt).toLocaleString("fr-FR") : "Aucun"}</div>{last && <div className="mt-1"><SevBadge severity={last.worst} /></div>}</Card>
        <Card><div className="text-xs text-muted-foreground">Batterie</div><div className="mt-1 text-sm">{b.charging.value === null ? "État de charge indisponible" : b.charging.value ? "En charge" : "Sur batterie"}{b.health.value && ` · état ${b.health.value}`}</div></Card>
      </div>

      <Card>
        <h2 className="font-display font-semibold">Recommandations prioritaires</h2>
        {recos.length === 0 ? <p className="mt-2 text-sm text-muted-foreground">Rien d'urgent sur les mesures disponibles.</p> : (
          <ul className="mt-2 space-y-2">{recos.map((f) => (
            <li key={f.id} className="flex items-start justify-between gap-2 text-sm"><span><b>{f.title}</b> — {f.solutions[0]}</span><SevBadge severity={f.severity} /></li>
          ))}</ul>
        )}
      </Card>

      {settings.mode !== "simple" && (
        <Card>
          <h2 className="font-display font-semibold">Mesures techniques</h2>
          <dl className="mt-2 grid gap-x-6 gap-y-1 font-mono text-xs sm:grid-cols-2">
            {[
              ["RAM totale", snapshot.memory.totalMb], ["RAM disponible", snapshot.memory.availableMb], ["Stockage total (Go)", snapshot.storage.totalGb],
              ["Stockage libre (Go)", snapshot.storage.freeGb], ["Décharge (%/h)", b.drainPctPerHour], ["Données moy./jour (Mo)", snapshot.data.avgDailyMb],
            ].map(([l, m]) => { const mm = m as { value: number | null; source: never; note?: string }; return (
              <div key={l as string} className="flex justify-between gap-2 border-b border-border/40 py-1">
                <dt className="text-muted-foreground">{l as string}</dt>
                <dd className="flex items-center gap-2" title={mm.note}>{mm.value ?? "—"} <SourceBadge source={mm.source} /></dd>
              </div>
            ); })}
          </dl>
        </Card>
      )}

      {settings.mode === "expert" && (
        <Card>
          <h2 className="font-display font-semibold">Tendances (historique local)</h2>
          {history.length < 2 ? <p className="mt-2 text-sm text-muted-foreground">Actualisez plusieurs fois pour construire un historique.</p> : (
            <div className="mt-3 h-48">
              <ResponsiveContainer>
                <LineChart data={[...history].reverse().map((h) => ({ t: new Date(h.takenAt).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }), bat: h.battery.levelPct.value, ram: ramFreePct(h), sto: storageFreePct(h) }))}>
                  <XAxis dataKey="t" stroke="var(--muted-foreground)" fontSize={10} />
                  <YAxis stroke="var(--muted-foreground)" fontSize={10} domain={[0, 100]} />
                  <Tooltip contentStyle={{ background: "var(--card)", border: "1px solid var(--border)" }} />
                  <Line dataKey="bat" name="Batterie %" stroke="var(--primary)" dot={false} strokeWidth={2} />
                  <Line dataKey="ram" name="RAM libre %" stroke="var(--accent)" dot={false} strokeWidth={2} />
                  <Line dataKey="sto" name="Stockage libre %" stroke="var(--success)" dot={false} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
          <p className="mt-2 text-xs text-muted-foreground">{reports.length} diagnostic(s) et {history.length} mesure(s) conservés sur cet appareil.</p>
        </Card>
      )}
    </div>
  );
}
