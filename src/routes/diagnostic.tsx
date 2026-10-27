import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { Sparkles } from "lucide-react";
import { useApp } from "@/store/app-store";
import { Btn, Card, PageTitle, SevBadge } from "@/components/pg/ui";
import { ReportView } from "@/components/pg/ReportView";

export const Route = createFileRoute("/diagnostic")({
  head: () => ({
    meta: [
      { title: "Diagnostic intelligent — Phone Genius" },
      { name: "description", content: "Décrivez votre problème : le moteur de règles local croise les mesures disponibles." },
      { property: "og:title", content: "Diagnostic intelligent — Phone Genius" },
      { property: "og:description", content: "Moteur de diagnostic local, sans IA externe." },
    ],
  }),
  component: DiagnosticPage,
});

const EXAMPLES = [
  "Mon téléphone chauffe beaucoup.",
  "Ma batterie se vide rapidement.",
  "Mon téléphone est devenu lent.",
  "Je n'ai presque plus d'espace.",
  "Mes données mobiles se consomment trop vite.",
  "Analyse tous les problèmes que tu peux détecter.",
];

function DiagnosticPage() {
  const { diagnose, reports, snapshot } = useApp();
  const [q, setQ] = useState("");
  const [currentId, setCurrentId] = useState<string | null>(null);
  const current = reports.find((r) => r.id === currentId) ?? null;

  const run = (text: string) => {
    if (!text.trim()) return;
    const r = diagnose(text.trim());
    if (r) setCurrentId(r.id);
  };

  return (
    <div className="space-y-5">
      <PageTitle title="Diagnostic intelligent" subtitle="Moteur de règles 100 % local. Il ne conclut que sur des mesures disponibles." />
      <Card>
        <form onSubmit={(e) => { e.preventDefault(); run(q); }} className="flex flex-col gap-2 sm:flex-row">
          <label htmlFor="q" className="sr-only">Décrivez le problème</label>
          <input id="q" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Ex. : Mon téléphone chauffe beaucoup"
            className="flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          <Btn type="submit" disabled={!snapshot || !q.trim()}><Sparkles className="h-4 w-4" />Analyser</Btn>
        </form>
        <div className="mt-3 flex flex-wrap gap-2">
          {EXAMPLES.map((e) => (
            <button key={e} onClick={() => { setQ(e); run(e); }} className="rounded-full border border-border px-3 py-1 text-xs text-muted-foreground hover:border-primary hover:text-foreground">{e}</button>
          ))}
        </div>
      </Card>

      {current && <ReportView report={current} />}

      {reports.length > 0 && (
        <Card>
          <h2 className="font-display font-semibold">Historique des diagnostics</h2>
          <ul className="mt-2 divide-y divide-border/50">
            {reports.slice(0, 10).map((r) => (
              <li key={r.id}>
                <button onClick={() => setCurrentId(r.id)} className="flex w-full items-center justify-between gap-2 py-2 text-left text-sm hover:text-primary">
                  <span className="truncate">« {r.query} » <span className="text-xs text-muted-foreground">{new Date(r.createdAt).toLocaleString("fr-FR")}</span></span>
                  <SevBadge severity={r.worst} />
                </button>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
