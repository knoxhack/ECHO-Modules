import {
  Activity,
  AlertTriangle,
  Archive,
  Boxes,
  Download,
  FileText,
  Gauge,
  Image,
  Layers,
  Map,
  Package,
  PackageCheck,
  Rocket,
  Settings as SettingsIcon,
  ShieldCheck,
  TerminalSquare
} from "lucide-react";
import type { CommandRun, FeatureStatus, JarTargetStatus, ModpackStatus, Project, QaFinding, ReadinessChecklistItem, ReleaseAction } from "../shared/types";
import type { ViewKey } from "./view-model";
import { commandStatusClass, commandStatusLabel, formatDate, runDuration } from "./view-model";

export const views: Array<{ key: ViewKey; label: string; icon: typeof Activity }> = [
  { key: "projects", label: "Projects", icon: Boxes },
  { key: "dashboard", label: "Dashboard", icon: Gauge },
  { key: "roadmap", label: "Roadmap", icon: Map },
  { key: "features", label: "Features", icon: FileText },
  { key: "qa", label: "QA Scanner", icon: ShieldCheck },
  { key: "prompts", label: "Codex Prompts", icon: TerminalSquare },
  { key: "bridge", label: "Bridge", icon: TerminalSquare },
  { key: "release", label: "Release Deck", icon: Rocket },
  { key: "distributions", label: "Distributions", icon: Archive },
  { key: "modpack", label: "Modpack", icon: PackageCheck },
  { key: "jars", label: "Jars", icon: Package },
  { key: "terminal", label: "Terminal Planner", icon: Layers },
  { key: "assets", label: "Asset Prompts", icon: Image },
  { key: "exports", label: "Exports", icon: Download },
  { key: "settings", label: "Settings", icon: SettingsIcon }
];

export function BrandPanel({ status }: { status: string }): JSX.Element {
  return (
    <section className="surface shell-brand p-4">
      <div className="flex items-center gap-3">
        <div className="brand-mark">
          <TerminalSquare className="h-5 w-5 text-signal-cyan" />
        </div>
        <div className="min-w-0">
          <h1 className="truncate text-sm font-semibold uppercase text-slate-100">Noxhack</h1>
          <p className="truncate text-xs text-slate-400">Command Center</p>
        </div>
      </div>
      <div className="mt-4 flex items-center gap-2 text-xs text-slate-400">
        <span className="status-dot" />
        <span className="truncate">{status}</span>
      </div>
    </section>
  );
}

export function Nav({ activeView, setActiveView }: { activeView: ViewKey; setActiveView: (view: ViewKey) => void }): JSX.Element {
  return (
    <nav className="surface p-2" aria-label="Primary navigation">
      {views.map((view) => {
        const Icon = view.icon;
        const selected = activeView === view.key;
        return (
          <button key={view.key} className={`nav-button ${selected ? "nav-button-active" : ""}`} title={view.label} onClick={() => setActiveView(view.key)}>
            <Icon className="h-4 w-4 shrink-0" />
            <span className="truncate">{view.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

export function TopBar({
  projects,
  selectedSlug,
  setSelectedSlug,
  activeView,
  setActiveView
}: {
  projects: Project[];
  selectedSlug: string;
  setSelectedSlug: (slug: string) => void;
  activeView: ViewKey;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const selectedProject = projects.find((project) => project.slug === selectedSlug);
  return (
    <header className="surface topbar p-3">
      <div className="min-w-0">
        <p className="eyebrow text-signal-cyan">Local Release Ops</p>
        <h2 className="truncate text-xl font-semibold text-white">{selectedProject?.name ?? "Noxhack Modpack Command Center"}</h2>
        <p className="mt-1 truncate text-xs text-slate-500">{selectedProject?.workspacePath ?? "Project data loading"}</p>
      </div>
      <div className="grid gap-2 sm:grid-cols-2 lg:flex lg:items-center">
        <select className="control min-w-52" value={selectedSlug} onChange={(event) => setSelectedSlug(event.target.value)} aria-label="Project">
          {projects.map((project) => (
            <option key={project.slug} value={project.slug}>
              {project.name}
            </option>
          ))}
        </select>
        <select className="control lg:hidden" value={activeView} onChange={(event) => setActiveView(event.target.value as ViewKey)} aria-label="View">
          {views.map((view) => (
            <option key={view.key} value={view.key}>
              {view.label}
            </option>
          ))}
        </select>
      </div>
    </header>
  );
}

export function PageHeader({
  icon: Icon,
  eyebrow,
  title,
  description,
  actions
}: {
  icon: typeof Activity;
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: JSX.Element;
}): JSX.Element {
  return (
    <section className="surface page-header p-5">
      <div className="min-w-0">
        {eyebrow ? <p className="eyebrow text-signal-cyan">{eyebrow}</p> : null}
        <div className="mt-1 flex min-w-0 items-center gap-3">
          <div className="section-icon">
            <Icon className="h-5 w-5 text-signal-cyan" />
          </div>
          <h2 className="min-w-0 break-words text-2xl font-semibold text-white">{title}</h2>
        </div>
        {description ? <p className="mt-3 max-w-4xl text-sm leading-6 text-slate-300">{description}</p> : null}
      </div>
      {actions ? <div className="flex shrink-0 flex-col gap-2 sm:flex-row">{actions}</div> : null}
    </section>
  );
}

export function SectionTitle({ icon: Icon, title }: { icon: typeof Activity; title: string }): JSX.Element {
  return (
    <div className="flex min-w-0 items-center gap-2">
      <Icon className="h-5 w-5 shrink-0 text-signal-cyan" />
      <h2 className="truncate text-lg font-semibold text-white">{title}</h2>
    </div>
  );
}

export function Metric({ label, value, tone, detail }: { label: string; value: string | number; tone?: "green" | "amber" | "red"; detail?: string }): JSX.Element {
  const toneClass = tone === "green" ? "text-signal-green" : tone === "amber" ? "text-signal-amber" : tone === "red" ? "text-signal-red" : "text-white";
  return (
    <div className="metric-card">
      <p className="truncate text-xs uppercase text-slate-500">{label}</p>
      <p className={`mt-2 break-words text-2xl font-semibold ${toneClass}`}>{value}</p>
      {detail ? <p className="mt-1 truncate text-xs text-slate-500">{detail}</p> : null}
    </div>
  );
}

export function ReadinessCount({ label, value, tone }: { label: string; value: number; tone?: "green" | "amber" | "red" }): JSX.Element {
  const toneClass = tone === "green" ? "text-signal-green" : tone === "amber" ? "text-signal-amber" : tone === "red" ? "text-signal-red" : "text-white";
  return (
    <div className="readiness-count">
      <p className="truncate text-xs uppercase text-slate-500">{label}</p>
      <p className={`mt-2 text-xl font-semibold ${toneClass}`}>{value}</p>
    </div>
  );
}

export function ActionTile({ label, detail, onClick, icon: Icon }: { label: string; detail: string; onClick: () => void; icon?: typeof Activity }): JSX.Element {
  return (
    <button className="action-tile" onClick={onClick}>
      <span className="flex min-w-0 items-center gap-2">
        {Icon ? <Icon className="h-4 w-4 shrink-0 text-signal-cyan" /> : null}
        <span className="truncate text-sm font-semibold text-white">{label}</span>
      </span>
      <span className="line-clamp-2 text-left text-sm text-slate-400">{detail}</span>
    </button>
  );
}

export function EmptyState({ icon: Icon = Activity, title, detail, action }: { icon?: typeof Activity; title: string; detail?: string; action?: JSX.Element }): JSX.Element {
  return (
    <div className="empty-state">
      <Icon className="h-5 w-5 text-signal-cyan" />
      <div className="min-w-0">
        <p className="text-sm font-semibold text-white">{title}</p>
        {detail ? <p className="mt-1 text-sm leading-6 text-slate-400">{detail}</p> : null}
        {action ? <div className="mt-3">{action}</div> : null}
      </div>
    </div>
  );
}

export function InfoBanner({ tone = "cyan", title, detail, icon: Icon = AlertTriangle }: { tone?: "cyan" | "amber" | "red" | "green"; title: string; detail: string; icon?: typeof Activity }): JSX.Element {
  const toneClass =
    tone === "red"
      ? "border-signal-red/45 bg-signal-red/10 text-signal-red"
      : tone === "amber"
        ? "border-signal-amber/45 bg-signal-amber/10 text-signal-amber"
        : tone === "green"
          ? "border-signal-green/45 bg-signal-green/10 text-signal-green"
          : "border-signal-cyan/45 bg-signal-cyan/10 text-signal-cyan";
  return (
    <section className={`info-banner ${toneClass}`}>
      <Icon className="mt-0.5 h-5 w-5 shrink-0" />
      <div className="min-w-0">
        <p className="text-sm font-semibold text-white">{title}</p>
        <p className="mt-1 break-words text-sm leading-6 text-slate-300">{detail}</p>
      </div>
    </section>
  );
}

export function Severity({ severity }: { severity: QaFinding["severity"] }): JSX.Element {
  const className =
    severity === "critical"
      ? "border-signal-red/50 text-signal-red"
      : severity === "high"
        ? "border-signal-amber/50 text-signal-amber"
        : severity === "medium"
          ? "border-signal-violet/50 text-signal-violet"
          : "border-signal-cyan/40 text-signal-cyan";
  return <span className={`status-pill ${className}`}>{severity}</span>;
}

export function Risk({ risk }: { risk: ReleaseAction["risk"] }): JSX.Element {
  const className =
    risk === "high" ? "border-signal-red/50 text-signal-red" : risk === "medium" ? "border-signal-amber/50 text-signal-amber" : "border-signal-green/50 text-signal-green";
  return <span className={`status-pill ${className}`}>{risk}</span>;
}

export function ReadinessStatusBadge({ status }: { status: ReadinessChecklistItem["status"] }): JSX.Element {
  const className =
    status === "done"
      ? "border-signal-green/50 text-signal-green"
      : status === "blocked"
        ? "border-signal-red/50 text-signal-red"
        : status === "warning"
          ? "border-signal-amber/50 text-signal-amber"
          : "border-slate-500/50 text-slate-300";
  return <span className={`status-pill ${className}`}>{status}</span>;
}

export function FeatureStatusBadge({ status }: { status: FeatureStatus }): JSX.Element {
  const className =
    status === "implemented"
      ? "border-signal-green/50 text-signal-green"
      : status === "partial"
        ? "border-signal-cyan/50 text-signal-cyan"
        : status === "blocked"
          ? "border-signal-red/50 text-signal-red"
          : status === "deferred"
            ? "border-slate-500/50 text-slate-400"
            : "border-signal-amber/50 text-signal-amber";
  return <span className={`status-pill ${className}`}>{status}</span>;
}

export function ModpackStatusBadge({ status }: { status: ModpackStatus }): JSX.Element {
  const className =
    status === "ready" || status === "succeeded"
      ? "border-signal-green/50 text-signal-green"
      : status === "running"
        ? "border-signal-cyan/50 text-signal-cyan"
        : status === "stopped"
          ? "border-slate-500/50 text-slate-400"
        : status === "blocked" || status === "failed"
          ? "border-signal-red/50 text-signal-red"
          : "border-signal-amber/50 text-signal-amber";
  return <span className={`status-pill ${className}`}>{status}</span>;
}

export function JarStatusBadge({ status, label }: { status: JarTargetStatus; label?: string }): JSX.Element {
  const className =
    status === "current"
      ? "border-signal-green/50 text-signal-green"
      : status === "missing" || status === "foreign"
        ? "border-slate-500/50 text-slate-400"
        : status === "duplicate"
          ? "border-signal-amber/50 text-signal-amber"
          : "border-signal-red/50 text-signal-red";
  return <span className={`status-pill ${className}`}>{label ?? status}</span>;
}

export function RunHistoryPanel({
  title,
  runs,
  selectedRun,
  empty,
  onSelectRun
}: {
  title: string;
  runs: CommandRun[];
  selectedRun: CommandRun | null;
  empty: string;
  onSelectRun: (run: CommandRun) => void;
}): JSX.Element {
  return (
    <div className="surface p-5">
      <SectionTitle icon={Activity} title={title} />
      <div className="mt-4 space-y-2">
        {runs.length === 0 ? (
          <EmptyState title={empty} detail="Runs will appear here after an allowlisted command starts." />
        ) : (
          runs.map((run) => (
            <button key={run.id} className={`history-row ${selectedRun?.id === run.id ? "history-row-active" : ""}`} onClick={() => onSelectRun(run)}>
              <span className="min-w-0">
                <span className="block truncate text-sm text-white">{run.commandId}</span>
                <span className="mt-1 block truncate font-mono text-xs text-slate-500">{formatDate(run.finishedAt ?? run.startedAt)} / {runDuration(run)}</span>
              </span>
              <span className={`font-mono text-xs ${commandStatusClass(run.status)}`}>{commandStatusLabel(run.status)}</span>
            </button>
          ))
        )}
      </div>
    </div>
  );
}

export function OutputPanel({ title, run, empty }: { title: string; run: CommandRun | null; empty: string }): JSX.Element {
  return (
    <div className="surface p-5">
      <SectionTitle icon={TerminalSquare} title={title} />
      {run ? (
        <div className="mt-4">
          <div className="run-summary">
            <span className="truncate">{run.commandId}</span>
            <span className={`font-mono text-xs ${commandStatusClass(run.status)}`}>{commandStatusLabel(run.status)}</span>
          </div>
          <pre className="output-block mt-3">{run.output || `${run.commandId} ${run.status}`}</pre>
        </div>
      ) : (
        <EmptyState title={empty} detail="Select a run from history after a command has been started." />
      )}
    </div>
  );
}

export function LoadingPanel(): JSX.Element {
  return (
    <section className="surface grid min-h-[360px] place-items-center p-8">
      <div className="flex items-center gap-3 text-slate-400">
        <Activity className="h-5 w-5 text-signal-cyan" />
        Loading command center data
      </div>
    </section>
  );
}

export function Alert({ message }: { message: string }): JSX.Element {
  return <div className="rounded-lg border border-signal-red/40 bg-signal-red/10 px-4 py-3 text-sm text-signal-red">{message}</div>;
}
