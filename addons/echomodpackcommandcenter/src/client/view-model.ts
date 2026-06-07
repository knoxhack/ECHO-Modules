import type {
  AppSettings,
  CommandRun,
  EchoOperationalReportStatus,
  EchoReportArtifactStatus,
  JarManifest,
  ReadinessChecklistItem,
  ReadinessReport,
  ReleaseAction
} from "../shared/types";

export type ViewKey =
  | "projects"
  | "dashboard"
  | "roadmap"
  | "features"
  | "qa"
  | "prompts"
  | "release"
  | "distributions"
  | "modpack"
  | "jars"
  | "bridge"
  | "terminal"
  | "assets"
  | "exports"
  | "settings";

export function displayCommand(action: ReleaseAction, settings: AppSettings): string {
  if (action.mode !== "shell") {
    return action.mode;
  }
  const args = [...action.args];
  if (settings.modpackModsDir && ["verify-release", "check-jar-set", "copy-jars"].includes(action.commandId)) {
    args.push(`-PechoModpackModsDir=${settings.modpackModsDir}`);
  }
  return [action.executable, ...args].join(" ");
}

export function mergeRun(runs: CommandRun[], run: CommandRun): CommandRun[] {
  return [run, ...runs.filter((candidate) => candidate.id !== run.id)].slice(0, 25);
}

export function readinessGroups(readiness: ReadinessReport | null): Array<{ category: ReadinessChecklistItem["category"]; items: ReadinessChecklistItem[] }> {
  const categories: ReadinessChecklistItem["category"][] = ["Scan", "Jars", "Settings"];
  return categories
    .map((category) => ({
      category,
      items: readiness?.items.filter((item) => item.category === category) ?? []
    }))
    .filter((group) => group.items.length > 0);
}

export function viewFromReadiness(value: string | undefined): ViewKey | null {
  return value === "dashboard" ||
    value === "qa" ||
    value === "jars" ||
    value === "bridge" ||
    value === "settings" ||
    value === "release" ||
    value === "distributions" ||
    value === "modpack" ||
    value === "projects" ||
    value === "roadmap" ||
    value === "features" ||
    value === "prompts" ||
    value === "terminal" ||
    value === "assets" ||
    value === "exports"
    ? value
    : null;
}

export function commandStatusClass(status: CommandRun["status"]): string {
  return status === "failed" || status === "rejected"
    ? "text-signal-red"
    : status === "succeeded"
      ? "text-signal-green"
      : status === "running"
        ? "text-signal-cyan"
        : "text-slate-400";
}

export function commandStatusLabel(status: CommandRun["status"]): string {
  return status === "succeeded" ? "passed" : status;
}

export function formatBytes(size: number): string {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

export function runDuration(run: CommandRun): string {
  if (run.durationMs == null) {
    return run.status === "running" ? "running" : "not recorded";
  }
  if (run.durationMs < 1000) {
    return `${run.durationMs}ms`;
  }
  return `${Math.round(run.durationMs / 1000)}s`;
}

export function selectedRunFrom(runs: CommandRun[], selectedRunId: string | null, fallback: CommandRun | null): CommandRun | null {
  if (selectedRunId) {
    const selected = runs.find((run) => run.id === selectedRunId);
    if (selected) {
      return selected;
    }
    if (fallback?.id === selectedRunId) {
      return fallback;
    }
  }
  return fallback ?? runs[0] ?? null;
}

export function promoteDisabledReason(manifest: JarManifest | null, running: CommandRun | null): string | null {
  if (running) {
    return `A ${running.commandId} run is active. Stop or wait for it before promoting jars.`;
  }
  if (!manifest) {
    return "Jar inventory is still loading.";
  }
  if (manifest.blockers.length > 0) {
    return manifest.blockers[0];
  }
  const missing = manifest.artifacts.filter((artifact) => !artifact.exists);
  if (missing.length > 0) {
    return `Build is required before promote: ${missing.map((artifact) => artifact.expectedFileName).join(", ")}.`;
  }
  return null;
}

export function operationalStatusTone(status: EchoOperationalReportStatus | string): "green" | "amber" | "red" {
  if (status === "ready") return "green";
  if (status === "blocked" || status === "failed" || status === "critical") return "red";
  return "amber";
}

export function reportArtifactTone(status: EchoReportArtifactStatus): "green" | "amber" | "red" {
  if (status === "loaded") return "green";
  if (status === "invalid") return "red";
  return "amber";
}
