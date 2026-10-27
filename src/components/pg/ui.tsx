import type { ReactNode } from "react";
import type { DataSource, Severity } from "@/core/types";
import { cn } from "@/lib/utils";

export const SEV_LABEL: Record<Severity, string> = { ok: "Satisfaisant", low: "Info", medium: "Moyen", high: "Élevé", critical: "Critique" };
export const SEV_TEXT: Record<Severity, string> = { ok: "text-success", low: "text-accent", medium: "text-warning", high: "text-warning", critical: "text-critical" };
export const SEV_BG: Record<Severity, string> = { ok: "bg-success/15 text-success", low: "bg-accent/15 text-accent", medium: "bg-warning/15 text-warning", high: "bg-warning/20 text-warning", critical: "bg-critical/20 text-critical" };

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn("pg-card p-4", className)}>{children}</div>;
}

export function SourceBadge({ source }: { source: DataSource }) {
  const map: Record<DataSource, [string, string]> = {
    demo: ["FICTIF", "bg-warning/15 text-warning"],
    web: ["RÉEL · navigateur", "bg-success/15 text-success"],
    native: ["RÉEL · Android", "bg-success/15 text-success"],
    unavailable: ["Indisponible", "bg-muted text-muted-foreground"],
  };
  const [l, c] = map[source];
  return <span className={cn("rounded px-1.5 py-0.5 font-mono text-[10px] font-semibold tracking-wide", c)}>{l}</span>;
}

export function SevBadge({ severity }: { severity: Severity }) {
  return <span className={cn("rounded-full px-2 py-0.5 text-xs font-semibold", SEV_BG[severity])}>{SEV_LABEL[severity]}</span>;
}

export function PageTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <header className="mb-5">
      <h1 className="font-display text-2xl font-bold tracking-tight md:text-3xl">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
    </header>
  );
}

export function Gauge({ value, max = 100, label, display, severity, unavailable }: {
  value: number | null; max?: number; label: string; display: string; severity: Severity; unavailable?: boolean;
}) {
  const r = 38, c = 2 * Math.PI * r;
  const pct = value === null ? 0 : Math.max(0, Math.min(1, value / max));
  return (
    <div className="flex flex-col items-center">
      <svg viewBox="0 0 100 100" className="h-24 w-24 -rotate-90" aria-label={`${label} : ${display}`}>
        <circle cx="50" cy="50" r={r} className="fill-none stroke-muted" strokeWidth="8" />
        {!unavailable && (
          <circle cx="50" cy="50" r={r} className={cn("pg-gauge fill-none stroke-current", SEV_TEXT[severity])} strokeWidth="8"
            strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - pct)} />
        )}
      </svg>
      <div className="-mt-[66px] mb-9 font-mono text-base font-bold">{display}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  );
}

export function Toggle({ checked, onChange, label }: { checked: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-3 py-2 text-sm">
      <span>{label}</span>
      <button type="button" role="switch" aria-checked={checked} aria-label={label} onClick={() => onChange(!checked)}
        className={cn("relative h-6 w-11 shrink-0 rounded-full transition-colors", checked ? "bg-primary" : "bg-muted")}>
        <span className={cn("absolute top-0.5 h-5 w-5 rounded-full bg-foreground transition-transform", checked ? "translate-x-5" : "translate-x-0.5")} />
      </button>
    </label>
  );
}

export function Btn({ children, onClick, variant = "primary", className, disabled, type = "button" }: {
  children: ReactNode; onClick?: () => void; variant?: "primary" | "ghost" | "danger"; className?: string; disabled?: boolean; type?: "button" | "submit";
}) {
  const v = { primary: "pg-btn-primary", ghost: "border border-border hover:bg-muted", danger: "bg-critical/15 text-critical hover:bg-critical/25" }[variant];
  return (
    <button type={type} disabled={disabled} onClick={onClick}
      className={cn("inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition disabled:opacity-50", v, className)}>
      {children}
    </button>
  );
}
