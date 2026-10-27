import { Link } from "@tanstack/react-router";
import type { DiagnosticReport } from "@/core/types";
import { getAction, AVAILABILITY_LABELS } from "@/core/actions";
import { Card, SevBadge, SourceBadge } from "./ui";

export function ReportView({ report }: { report: DiagnosticReport }) {
  return (
    <div className="space-y-3">
      <Card className="border-primary/40">
        <div className="flex flex-wrap items-center gap-2"><SevBadge severity={report.worst} /><SourceBadge source={report.adapter} /></div>
        <p className="mt-2 font-medium">{report.summary}</p>
        {report.adapter === "demo" && <p className="mt-1 text-xs text-warning">Diagnostic calculé sur des données FICTIVES de démonstration.</p>}
        {report.missing.length > 0 && <p className="mt-1 text-xs text-muted-foreground">Mesures indisponibles : {report.missing.join(", ")}.</p>}
      </Card>
      {report.findings.map((f) => (
        <Card key={f.id}>
          <div className="flex items-start justify-between gap-2">
            <h3 className="font-display font-semibold">{f.title}</h3>
            <SevBadge severity={f.severity} />
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{f.observed}</p>
          <p className="mt-1 text-xs">Confiance : <span className="font-semibold">{f.confidence}</span></p>
          <ul className="mt-3 space-y-1 text-sm">
            {f.measures.map((m) => (
              <li key={m.label} className="flex flex-wrap items-center justify-between gap-2 border-b border-border/50 pb-1">
                <span className="text-muted-foreground">{m.label}</span>
                <span className="flex items-center gap-2 font-mono text-xs">{m.value} <SourceBadge source={m.source} /></span>
              </li>
            ))}
          </ul>
          <Section title="Causes possibles (non vérifiées)" items={f.causes} />
          <Section title="Vérifications complémentaires" items={f.checks} />
          <Section title="Solutions recommandées" items={f.solutions} />
          {f.actionIds.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {f.actionIds.map((id) => { const a = getAction(id); return a && (
                <Link key={id} to="/entretien" className="rounded-md border border-primary/40 px-2 py-1 text-xs text-primary hover:bg-primary/10" title={AVAILABILITY_LABELS[a.availability]}>{a.title}</Link>
              ); })}
            </div>
          )}
        </Card>
      ))}
    </div>
  );
}

function Section({ title, items }: { title: string; items: string[] }) {
  if (!items.length) return null;
  return (
    <div className="mt-3">
      <div className="text-xs font-semibold uppercase tracking-wider text-accent">{title}</div>
      <ul className="mt-1 list-disc space-y-0.5 pl-5 text-sm">{items.map((i) => <li key={i}>{i}</li>)}</ul>
    </div>
  );
}
