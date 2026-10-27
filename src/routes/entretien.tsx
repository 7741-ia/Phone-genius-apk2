import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useApp } from "@/store/app-store";
import { Btn, Card, PageTitle } from "@/components/pg/ui";
import { AVAILABILITY_LABELS, CARE_ACTIONS } from "@/core/actions";
import { getNativeBridge } from "@/core/adapters/native-bridge";
import { storageFreePct } from "@/core/rules/engine";
import type { ActionAvailability, CareAction } from "@/core/types";

export const Route = createFileRoute("/entretien")({
  head: () => ({
    meta: [
      { title: "Entretien et actions — Phone Genius" },
      { name: "description", content: "Actions sûres pour entretenir votre téléphone, classées selon ce qu'Android autorise." },
      { property: "og:title", content: "Entretien et actions — Phone Genius" },
      { property: "og:description", content: "Centre de maintenance sûr, sans suppression automatique." },
    ],
  }),
  component: CarePage,
});

const AV_STYLE: Record<ActionAvailability, string> = {
  auto: "bg-success/15 text-success", confirm: "bg-accent/15 text-accent", special: "bg-warning/15 text-warning",
  manual: "bg-primary/15 text-primary", impossible: "bg-muted text-muted-foreground",
};

function CarePage() {
  const { snapshot, operations, logOperation } = useApp();
  const [pending, setPending] = useState<CareAction | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const sp = snapshot ? storageFreePct(snapshot) : null;

  const execute = (a: CareAction) => {
    const bridge = getNativeBridge();
    let result: string;
    if (a.availability === "impossible") result = "Non réalisable par une application ordinaire.";
    else if (bridge && a.androidSettings?.startsWith("android.settings")) { bridge.openSettings(a.androidSettings); result = "Écran Paramètres ouvert."; }
    else if (a.id === "storage_scan") result = sp === null ? "Stockage du téléphone non lisible depuis le navigateur." : `Analyse : ${sp.toFixed(1)} % libre${snapshot?.adapter === "demo" ? " (fictif)" : ""}.`;
    else if (a.id === "clear_own_cache") result = "Simulation : dans la version Android, seul le cache de Phone Genius serait vidé.";
    else result = `Simulation web — dans l'app Android : ${a.androidSettings ?? "action guidée"}.`;
    logOperation({ actionId: a.id, label: a.title, result });
    setMsg(result);
    setPending(null);
  };

  const groups = (Object.keys(AVAILABILITY_LABELS) as ActionAvailability[]).map((k) => ({ k, items: CARE_ACTIONS.filter((a) => a.availability === k) }));

  return (
    <div className="space-y-5">
      <PageTitle title="Entretien & actions" subtitle="Phone Genius ne supprime jamais automatiquement vos photos, vidéos ou documents." />
      {msg && <Card className="border-primary/40 text-sm" ><span role="status">{msg}</span></Card>}

      {groups.map(({ k, items }) => items.length > 0 && (
        <section key={k}>
          <h2 className="mb-2 flex items-center gap-2 font-display text-sm font-semibold"><span className={`rounded px-2 py-0.5 text-xs ${AV_STYLE[k]}`}>{AVAILABILITY_LABELS[k]}</span></h2>
          <div className="grid gap-3 md:grid-cols-2">
            {items.map((a) => (
              <Card key={a.id} className={k === "impossible" ? "opacity-70" : ""}>
                <h3 className="font-semibold">{a.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{a.description}</p>
                {a.steps.length > 0 && <ol className="mt-2 list-decimal pl-5 text-xs text-muted-foreground">{a.steps.map((s) => <li key={s}>{s}</li>)}</ol>}
                {a.androidSettings && <p className="mt-2 font-mono text-[10px] text-muted-foreground">{a.androidSettings}</p>}
                {k !== "impossible" && (
                  <Btn className="mt-3" variant={a.destructive ? "danger" : "ghost"} onClick={() => (a.destructive || k === "confirm" ? setPending(a) : execute(a))}>
                    {k === "manual" ? "Ouvrir les Paramètres" : "Lancer"}
                  </Btn>
                )}
              </Card>
            ))}
          </div>
        </section>
      ))}

      <Card>
        <h2 className="font-display font-semibold">Historique des opérations</h2>
        {operations.length === 0 ? <p className="mt-2 text-sm text-muted-foreground">Aucune opération.</p> : (
          <ul className="mt-2 divide-y divide-border/50 text-sm">{operations.slice(0, 20).map((o) => (
            <li key={o.id} className="py-2"><b>{o.label}</b> <span className="text-xs text-muted-foreground">{new Date(o.at).toLocaleString("fr-FR")}</span><div className="text-muted-foreground">{o.result}</div></li>
          ))}</ul>
        )}
      </Card>

      {pending && (
        <div role="dialog" aria-modal="true" aria-labelledby="confirm-title" className="fixed inset-0 z-50 grid place-items-center bg-background/80 p-4 backdrop-blur">
          <Card className="max-w-sm">
            <h3 id="confirm-title" className="font-display font-semibold">Confirmer : {pending.title}</h3>
            <p className="mt-2 text-sm text-muted-foreground">{pending.description}</p>
            {pending.destructive && <p className="mt-2 text-sm text-warning">Cette action peut supprimer des données. Aucune photo, vidéo ou document personnel ne sera touché.</p>}
            <div className="mt-4 flex justify-end gap-2">
              <Btn variant="ghost" onClick={() => setPending(null)}>Annuler</Btn>
              <Btn variant={pending.destructive ? "danger" : "primary"} onClick={() => execute(pending)}>Confirmer</Btn>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
