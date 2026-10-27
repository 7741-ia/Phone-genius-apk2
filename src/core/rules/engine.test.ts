import { describe, expect, it } from "vitest";
import { DemoAdapter, WebAdapter } from "../adapters/device";
import { DEFAULT_THRESHOLDS, parseIntent, runDiagnostic } from "./engine";
import { DEFAULT_ALERT_SETTINGS, evaluateAlerts } from "../alerts";

describe("parseIntent", () => {
  it("chauffe → heat", () => expect(parseIntent("Mon téléphone chauffe beaucoup.")).toEqual(["heat"]));
  it("analyse complète → tous les sujets", () => expect(parseIntent("Analyse tous les problèmes que tu peux détecter.")).toHaveLength(6));
  it("espace → storage", () => expect(parseIntent("Je n'ai presque plus d'espace.")).toContain("storage"));
});

describe("runDiagnostic", () => {
  it("stockage sous le seuil → alerte élevée/critique", async () => {
    const s = await new DemoAdapter("stockage").readSnapshot();
    const r = runDiagnostic(s, DEFAULT_THRESHOLDS, "espace");
    expect(r.findings[0]!.severity).toBe("critical"); // 4.2/128 = 3.3 % < 5 %
    expect(r.adapter).toBe("demo");
  });
  it("ne conclut pas sans mesure : confiance faible", async () => {
    const s = await new WebAdapter().readSnapshot();
    const r = runDiagnostic(s, DEFAULT_THRESHOLDS, "chauffe");
    expect(r.findings[0]!.confidence).toBe("faible");
    expect(r.findings[0]!.measures[0]!.source).toBe("unavailable");
  });
  it("plusieurs anomalies → diagnostic croisé", async () => {
    const s = await new DemoAdapter("surchauffe").readSnapshot();
    const r = runDiagnostic(s, DEFAULT_THRESHOLDS, "analyse complète");
    expect(r.findings[0]!.id).toBe("cross");
  });
});

describe("alertes", () => {
  it("batterie à 12 % (seuil 20) déclenche une alerte, puis cooldown", async () => {
    const s = await new DemoAdapter("batterie").readSnapshot();
    const first = evaluateAlerts(s, DEFAULT_THRESHOLDS, DEFAULT_ALERT_SETTINGS, []);
    expect(first.some((a) => a.category === "battery_low")).toBe(true);
    const again = evaluateAlerts(s, DEFAULT_THRESHOLDS, DEFAULT_ALERT_SETTINGS, first);
    expect(again.some((a) => a.category === "battery_low")).toBe(false);
  });
  it("catégorie désactivée → aucune alerte", async () => {
    const s = await new DemoAdapter("batterie").readSnapshot();
    const off = { ...DEFAULT_ALERT_SETTINGS, enabled: { ...DEFAULT_ALERT_SETTINGS.enabled, battery_low: false } };
    expect(evaluateAlerts(s, DEFAULT_THRESHOLDS, off, []).some((a) => a.category === "battery_low")).toBe(false);
  });
});
