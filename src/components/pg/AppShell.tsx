import { Link } from "@tanstack/react-router";
import { Activity, Bell, Mic, Settings, Stethoscope, Wrench } from "lucide-react";
import type { ReactNode } from "react";
import { useApp } from "@/store/app-store";

const NAV = [
  { to: "/", label: "Tableau", icon: Activity },
  { to: "/diagnostic", label: "Diagnostic", icon: Stethoscope },
  { to: "/voix", label: "Voix", icon: Mic },
  { to: "/alertes", label: "Alertes", icon: Bell },
  { to: "/entretien", label: "Entretien", icon: Wrench },
  { to: "/reglages", label: "Réglages", icon: Settings },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const { snapshot, alerts, settings } = useApp();
  const unread = alerts.filter((a) => !a.read).length;
  const isDemo = (snapshot?.adapter ?? settings.adapter) === "demo";
  return (
    <div className="min-h-screen md:flex">
      <aside className="hidden w-60 shrink-0 border-r border-border p-5 md:block">
        <Brand />
        <nav className="mt-8 space-y-1">
          {NAV.map(({ to, label, icon: Icon }) => (
            <Link key={to} to={to} activeOptions={{ exact: to === "/" }} className="pg-nav-item flex items-center gap-3 rounded-lg px-3 py-2 text-sm"
              activeProps={{ className: "pg-nav-active" }}>
              <Icon className="h-4 w-4" />{label}
              {to === "/alertes" && unread > 0 && <span className="ml-auto rounded-full bg-critical px-1.5 text-[10px] font-bold text-critical-foreground">{unread}</span>}
            </Link>
          ))}
        </nav>
        <p className="mt-8 text-xs text-muted-foreground">100 % local · aucune donnée envoyée</p>
      </aside>
      <div className="flex-1 pb-24 md:pb-8">
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-border bg-background/85 px-4 py-3 backdrop-blur md:px-8">
          <div className="md:hidden"><Brand /></div>
          {isDemo ? (
            <Link to="/reglages" className="rounded-full bg-warning/15 px-3 py-1 font-mono text-[11px] font-semibold text-warning">DÉMO · fictif</Link>
          ) : (
            <span className="rounded-full bg-success/15 px-3 py-1 font-mono text-[11px] font-semibold text-success">Données réelles ({snapshot?.adapter})</span>
          )}
        </header>
        <main className="mx-auto max-w-5xl px-4 py-6 md:px-8">{children}</main>
      </div>
      <nav className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-6 border-t border-border bg-background/95 backdrop-blur md:hidden">
        {NAV.map(({ to, label, icon: Icon }) => (
          <Link key={to} to={to} activeOptions={{ exact: to === "/" }} className="relative flex flex-col items-center gap-0.5 py-2 text-[10px] text-muted-foreground"
            activeProps={{ className: "text-primary" }}>
            <Icon className="h-5 w-5" />{label}
            {to === "/alertes" && unread > 0 && <span className="absolute right-3 top-1 h-2 w-2 rounded-full bg-critical" />}
          </Link>
        ))}
      </nav>
    </div>
  );
}

function Brand() {
  return (
    <Link to="/" className="flex items-center gap-2">
      <span className="pg-logo grid h-8 w-8 place-items-center rounded-lg font-display text-sm font-bold">PG</span>
      <span className="font-display text-lg font-bold tracking-tight">Phone<span className="text-accent">Genius</span></span>
    </Link>
  );
}
