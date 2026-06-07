import { AlertTriangle, Copy, Package, Play } from "lucide-react";
import type { AppSettings, JarManifest, ReadinessChecklistItem, ReadinessReport, ReleaseAction } from "../shared/types";
import { displayCommand, readinessGroups, type ViewKey, viewFromReadiness } from "./view-model";
import { ReadinessCount, ReadinessStatusBadge, SectionTitle } from "./ui";
import { ListChecks } from "lucide-react";

export function ReadinessChecklistPanel({
  readiness,
  setActiveView
}: {
  readiness: ReadinessReport | null;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const groups = readinessGroups(readiness);
  const nextView = viewFromReadiness(readiness?.nextAction?.targetView);
  const complete = readiness && !readiness.nextAction;
  return (
    <section className={`surface readiness-panel p-5 ${complete ? "readiness-panel-complete" : ""}`}>
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <SectionTitle icon={ListChecks} title="To 100%" />
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">
            {readiness?.nextAction
              ? readiness.nextAction.detail
              : readiness
                ? "Every checklist item is done."
                : "Loading the current readiness checklist."}
          </p>
        </div>
        <div className="grid min-w-[280px] grid-cols-3 gap-2">
          <ReadinessCount label="Done" value={readiness?.counts.done ?? 0} tone="green" />
          <ReadinessCount label="Missing" value={readiness?.counts.missing ?? 0} tone={readiness?.counts.missing ? "amber" : "green"} />
          <ReadinessCount label="Blocked" value={readiness?.counts.blocked ?? 0} tone={readiness?.counts.blocked ? "red" : "green"} />
        </div>
      </div>

      {readiness?.nextAction ? (
        <button className="primary-button mt-4" onClick={() => nextView && setActiveView(nextView)}>
          <Play className="h-4 w-4" />
          {readiness.nextAction.actionLabel}
        </button>
      ) : null}

      <div className="mt-5 grid gap-4 xl:grid-cols-3">
        {groups.map((group) => (
          <div key={group.category} className="planner-column min-h-0">
            <h3 className="text-sm font-semibold uppercase text-signal-cyan">{group.category}</h3>
            <div className="mt-4 space-y-2">
              {group.items.map((item) => (
                <ReadinessRow key={item.id} item={item} setActiveView={setActiveView} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function ReadinessRow({
  item,
  setActiveView
}: {
  item: ReadinessChecklistItem;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const targetView = viewFromReadiness(item.targetView);
  return (
    <div className="finding-row readiness-row">
      <ReadinessStatusBadge status={item.status} />
      <div className="min-w-0 flex-1">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <h4 className="text-sm font-semibold text-white">{item.label}</h4>
          {targetView ? (
            <button className="secondary-button min-h-8 px-3 text-xs" onClick={() => setActiveView(targetView)}>
              {item.actionLabel}
            </button>
          ) : null}
        </div>
        <p className="mt-2 text-sm leading-6 text-slate-400">{item.detail}</p>
        {item.relatedFindingCodes.length ? (
          <p className="mt-2 font-mono text-xs text-slate-500">{item.relatedFindingCodes.join(", ")}</p>
        ) : null}
      </div>
    </div>
  );
}

export function ConfirmModal({
  action,
  settings,
  onCancel,
  onConfirm
}: {
  action: ReleaseAction;
  settings: AppSettings;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}): JSX.Element {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <section className="surface max-w-2xl p-5 shadow-glow">
        <div className="flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-signal-amber" />
          <h2 className="text-lg font-semibold text-white">Confirm {action.label}</h2>
        </div>
        <p className="mt-3 text-sm leading-6 text-slate-300">
          This {action.risk}-risk allowlisted action will run for this selected project only. It may build, verify, copy, or modify local project release artifacts.
        </p>
        <code className="command-preview mt-4">{displayCommand(action, settings)}</code>
        <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <button className="secondary-button" onClick={onCancel}>Cancel</button>
          <button className="primary-button justify-center" onClick={onConfirm}>
            <Play className="h-4 w-4" />
            Confirm Run
          </button>
        </div>
      </section>
    </div>
  );
}

export type JarActionKind = "build" | "promote";

export function ConfirmJarModal({
  kind,
  manifest,
  onCancel,
  onConfirm
}: {
  kind: JarActionKind;
  manifest: JarManifest | null;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}): JSX.Element {
  const isPromote = kind === "promote";
  const label = isPromote ? "Promote Jars" : "Build Jars";
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <section className="surface max-w-2xl p-5 shadow-glow">
        <div className="flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-signal-amber" />
          <h2 className="text-lg font-semibold text-white">Confirm {label}</h2>
        </div>
        <p className="mt-3 text-sm leading-6 text-slate-300">
          {isPromote
            ? "This high-risk local file action quarantines managed stale project jars, copies current expected jars into the configured mods folder, verifies checksums, and runs a quick scan."
            : "This medium-risk build action uses the existing allowlisted Gradle release command for the selected project."}
        </p>
        <div className="mt-4 rounded-lg border border-deck-line bg-deck-950 p-3">
          <p className="break-all font-mono text-xs text-slate-300">Target: {manifest?.targetDir || "not configured"}</p>
          <p className="mt-1 font-mono text-xs text-slate-500">Expected: {manifest?.summary.expected ?? 0} / built: {manifest?.summary.built ?? 0}</p>
        </div>
        <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <button className="secondary-button" onClick={onCancel}>Cancel</button>
          <button className="primary-button justify-center" onClick={onConfirm}>
            {isPromote ? <Copy className="h-4 w-4" /> : <Package className="h-4 w-4" />}
            Confirm
          </button>
        </div>
      </section>
    </div>
  );
}
