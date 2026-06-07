import { randomUUID } from "node:crypto";
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import type {
  AppSettings,
  CommandRun,
  ModpackInventory,
  ModpackPipelineResult,
  ModpackPipelineRun,
  ModpackPipelineStep,
  ModpackStatus,
  ModpackTarget,
  Project,
  ReleaseAction,
  ScanReport
} from "../shared/types.js";
import { CommandCenterStore } from "./db.js";
import { buildJarManifest, jarBuildCommandId, JarPipelineError, promoteJarArtifacts, type JarServiceOptions } from "./jars.js";
import { runHybridScan } from "./scanner.js";
import { projectWorkspaceRoot } from "./workspace.js";

const MODPACK_PROJECT_SLUG = "modpack";
const MODPACK_COMMAND_ID = "modpack-rebuild";
const OUTPUT_LIMIT = 240_000;
const activeBuilds = new Map<string, ActiveModpackBuild>();

interface ActiveModpackBuild {
  stop: () => void;
}

export interface ModpackServiceOptions extends JarServiceOptions {
  buildRoots?: Record<string, string>;
  runBuildCommand?: (context: {
    project: Project;
    action: ReleaseAction;
    settings: AppSettings;
    command: string[];
    cwd: string;
    signal: AbortSignal;
  }) => Promise<{ exitCode: number; output: string }>;
  runInBackground?: boolean;
  skipScans?: boolean;
}

export class ModpackPipelineError extends Error {
  readonly statusCode: number;
  readonly summary: ModpackInventory;
  readonly run?: ModpackPipelineRun;

  constructor(message: string, statusCode: number, summary: ModpackInventory, run?: ModpackPipelineRun) {
    super(message);
    this.name = "ModpackPipelineError";
    this.statusCode = statusCode;
    this.summary = summary;
    this.run = run;
  }
}

export function buildModpackSummary(store: CommandCenterStore, options: ModpackServiceOptions = {}): ModpackInventory {
  const settings = store.getSettings();
  const targets = managedTargets(store).map((project) => targetFromProject(project, settings, options));
  const blockers = targetBlockers(settings, targets);
  const latestRun = listModpackRuns(store, 1)[0] ?? null;
  const summary = {
    projects: targets.length,
    expected: sum(targets, (target) => target.manifest.summary.expected),
    built: sum(targets, (target) => target.manifest.summary.built),
    missing: sum(targets, (target) => target.manifest.summary.missing),
    current: sum(targets, (target) => target.manifest.summary.current),
    stale: sum(targets, (target) => target.manifest.summary.stale),
    duplicate: sum(targets, (target) => target.manifest.summary.duplicate)
  };

  return {
    generatedAt: (options.now?.() ?? new Date()).toISOString(),
    targetDir: settings.modpackModsDir.trim(),
    targetConfigured: settings.modpackModsDir.trim().length > 0,
    targetExists: settings.modpackModsDir.trim().length > 0 && safeIsDirectory(settings.modpackModsDir.trim()),
    status: statusFromSummary(blockers, summary),
    blockers,
    targets,
    summary,
    latestRun
  };
}

export function listModpackRuns(store: CommandCenterStore, limit = 25): ModpackPipelineRun[] {
  return store.listCommandRuns(MODPACK_PROJECT_SLUG, limit).map(commandRunToPipelineRun);
}

export async function startModpackRebuild(store: CommandCenterStore, confirmed: boolean, options: ModpackServiceOptions = {}): Promise<ModpackPipelineResult> {
  const summary = buildModpackSummary(store, options);
  if (!confirmed) {
    throw new ModpackPipelineError("Confirmation required", 409, summary);
  }
  if (summary.blockers.length > 0) {
    throw new ModpackPipelineError(summary.blockers.join("\n"), 400, summary);
  }
  if (hasAnyRunningRun(store)) {
    throw new ModpackPipelineError("A build, release, jar, or modpack pipeline is already active.", 409, summary);
  }

  const projects = managedTargets(store);
  const startedAt = new Date().toISOString();
  const steps = initialSteps(projects);
  const run = store.createCommandRun({
    id: randomUUID(),
    projectSlug: MODPACK_PROJECT_SLUG,
    commandId: MODPACK_COMMAND_ID,
    status: "running",
    risk: "high",
    command: ["modpack-rebuild", ...projects.map((project) => project.slug)],
    startedAt,
    metadata: { targetSlugs: projects.map((project) => project.slug), steps },
    output: "Modpack rebuild and update pipeline queued.\n"
  });

  const execute = async (): Promise<ModpackPipelineResult> => executePipeline(store, run.id, projects, options);
  if (options.runInBackground === false) {
    return execute();
  }
  void execute().catch(() => undefined);
  return {
    run: commandRunToPipelineRun(store.getCommandRun(run.id) ?? run),
    summary: buildModpackSummary(store, options),
    scanReports: [],
    moved: [],
    copied: [],
    verified: []
  };
}

export function stopModpackRebuild(store: CommandCenterStore, runId: string, options: ModpackServiceOptions = {}): ModpackPipelineResult | null {
  const run = store.getCommandRun(runId);
  if (!run || run.projectSlug !== MODPACK_PROJECT_SLUG) {
    return null;
  }

  if (run.status !== "running") {
    const updated = store.updateCommandRun(runId, {
      metadata: {
        ...(run.metadata ?? {}),
        stopRequestedAt: new Date().toISOString(),
        stopResult: "not-running"
      }
    }) ?? run;
    return pipelineResult(store, updated, options, [], [], [], []);
  }

  const stoppedAt = new Date().toISOString();
  const activeBuild = activeBuilds.get(runId);
  const runningStep = currentRunningStep(run);
  const message = activeBuild ? "Stopped by Command Center." : "Stopped by stale-run recovery.";
  let stopResult = "orphaned-running-run";
  if (activeBuild) {
    activeBuild.stop();
    activeBuilds.delete(runId);
    stopResult = "signal-sent";
  }

  markCurrentRunningStepStopped(store, runId, message);
  const current = store.getCommandRun(runId) ?? run;
  const stopped = store.updateCommandRun(runId, {
    status: "stopped",
    finishedAt: stoppedAt,
    exitCode: 130,
    durationMs: durationFrom(run.startedAt),
    metadata: {
      ...(current.metadata ?? {}),
      stopRequestedAt: stoppedAt,
      stopResult,
      previousStep: runningStep?.id,
      ...(activeBuild
        ? {}
        : {
            staleRecoveredAt: stoppedAt,
            staleRecoveryReason: "No matching modpack pipeline child process was active; recovered persisted running run."
          })
    },
    output: `${current.output ?? ""}\n\n${message}\n`
  }) as CommandRun;
  return pipelineResult(store, stopped, options, [], [], [], []);
}

async function executePipeline(
  store: CommandCenterStore,
  runId: string,
  projects: Project[],
  options: ModpackServiceOptions
): Promise<ModpackPipelineResult> {
  const settings = store.getSettings();
  const startedAt = store.getCommandRun(runId)?.startedAt ?? new Date().toISOString();
  let output = store.getCommandRun(runId)?.output ?? "";
  const scanReports: ScanReport[] = [];
  const moved: string[] = [];
  const copied: string[] = [];
  const verified: string[] = [];

  const appendOutput = (text: string): void => {
    output = `${output}${text}`;
    if (output.length > OUTPUT_LIMIT) {
      output = output.slice(output.length - OUTPUT_LIMIT);
    }
    store.updateCommandRun(runId, { output });
  };

  try {
    updateStep(store, runId, "preflight", { status: "running", startedAt: new Date().toISOString(), detail: "Checking target folder, active runs, and managed project set." });
    const preflight = buildModpackSummary(store, options);
    if (preflight.blockers.length > 0) {
      throw new ModpackPipelineError(preflight.blockers.join("\n"), 400, preflight);
    }
    updateStep(store, runId, "preflight", { status: "succeeded", finishedAt: new Date().toISOString(), detail: `Ready: ${preflight.summary.expected} expected jar(s) across ${preflight.summary.projects} project(s).` });
    appendOutput(`Preflight complete: ${preflight.summary.expected} expected jar(s).\n`);
    const stoppedAfterPreflight = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
    if (stoppedAfterPreflight) {
      return stoppedAfterPreflight;
    }

    for (const project of projects) {
      const stoppedBeforeBuild = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stoppedBeforeBuild) {
        return stoppedBeforeBuild;
      }
      const action = store.getReleaseAction(project.slug, jarBuildCommandId(project));
      if (!action) {
        throw new Error(`Build action is not configured for ${project.slug}.`);
      }
      const command = [action.executable, ...action.args];
      const cwd = projectWorkspaceRoot(project, settings);
      updateStep(store, runId, `build-${project.slug}`, {
        status: "running",
        startedAt: new Date().toISOString(),
        detail: `Running ${action.commandId} for ${project.name}.`,
        command
      });
      appendOutput(`\n[build:${project.slug}] ${command.join(" ")}\n`);
      const build = await runBuild(runId, project, action, settings, command, cwd, options);
      appendOutput(build.output ? `${build.output}\n` : `[build:${project.slug}] exited ${build.exitCode}\n`);
      const stopped = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stopped) {
        return stopped;
      }
      if (build.exitCode !== 0) {
        throw new Error(`${project.name} build failed with exit code ${build.exitCode}.`);
      }
      updateStep(store, runId, `build-${project.slug}`, {
        status: "succeeded",
        finishedAt: new Date().toISOString(),
        detail: `${project.name} build completed.`,
        output: trimStepOutput(build.output)
      });
    }

    for (const project of projects) {
      const stoppedBeforePromote = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stoppedBeforePromote) {
        return stoppedBeforePromote;
      }
      updateStep(store, runId, `promote-${project.slug}`, {
        status: "running",
        startedAt: new Date().toISOString(),
        detail: `Quarantining stale managed jars and promoting current ${project.name} jars.`
      });
      const promotion = promoteJarArtifacts(project, settings, jarOptionsFor(project, options));
      moved.push(...promotion.moved);
      copied.push(...promotion.copied);
      verified.push(...promotion.verified);
      appendOutput(`\n[promote:${project.slug}]\n${promotion.output}\n`);
      const stoppedAfterPromote = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stoppedAfterPromote) {
        return stoppedAfterPromote;
      }
      updateStep(store, runId, `promote-${project.slug}`, {
        status: "succeeded",
        finishedAt: new Date().toISOString(),
        detail: `Promoted ${promotion.copied.length} jar(s), verified ${promotion.verified.length}, quarantined ${promotion.moved.length}.`,
        output: trimStepOutput(promotion.output)
      });
    }

    for (const project of projects) {
      const stoppedBeforeScan = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stoppedBeforeScan) {
        return stoppedBeforeScan;
      }
      if (options.skipScans) {
        updateStep(store, runId, `scan-${project.slug}`, {
          status: "succeeded",
          finishedAt: new Date().toISOString(),
          detail: `${project.name} quick scan skipped by test harness.`
        });
        continue;
      }
      updateStep(store, runId, `scan-${project.slug}`, {
        status: "running",
        startedAt: new Date().toISOString(),
        detail: `Running quick scan for ${project.name}.`
      });
      const scan = runHybridScan(project, store.getQaTracks(project.slug), settings, "quick");
      const report = store.createScanReport(scan);
      scanReports.push(report);
      appendOutput(`\n[scan:${project.slug}] ${report.status} ${report.summary.buildHealth}% health, ${report.findings.length} finding(s).\n`);
      const stoppedAfterScan = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
      if (stoppedAfterScan) {
        return stoppedAfterScan;
      }
      const scanBlocksPipeline = scanFailureBlocksPipeline(report, project, settings, startedAt);
      if (report.status === "failed" && !scanBlocksPipeline) {
        appendOutput(`[scan:${project.slug}] non-blocking: only pre-existing runtime crash artifacts were found.\n`);
      }
      updateStep(store, runId, `scan-${project.slug}`, {
        status: scanBlocksPipeline ? "failed" : "succeeded",
        finishedAt: new Date().toISOString(),
        detail: scanBlocksPipeline
          ? `${project.name} quick scan ${report.status}: ${report.summary.buildHealth}% health, ${report.findings.length} finding(s).`
          : `${project.name} quick scan completed with no pipeline-blocking findings.`
      });
      if (scanBlocksPipeline) {
        throw new Error(`${project.name} quick scan failed after promotion.`);
      }
    }

    const stoppedBeforeFinish = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
    if (stoppedBeforeFinish) {
      return stoppedBeforeFinish;
    }
    const finalSummary = buildModpackSummary(store, options);
    const finished = store.updateCommandRun(runId, {
      status: "succeeded",
      finishedAt: new Date().toISOString(),
      exitCode: 0,
      durationMs: durationFrom(startedAt),
      metadata: {
        ...(store.getCommandRun(runId)?.metadata ?? {}),
        moved: moved.length,
        copied: copied.length,
        verified: verified.length,
        finalSummary: finalSummary.summary
      },
      output: `${output}\nModpack pipeline succeeded.\nCopied: ${copied.length}. Verified: ${verified.length}. Quarantined: ${moved.length}.\n`
    }) as CommandRun;
    return { run: commandRunToPipelineRun(finished), summary: finalSummary, scanReports, moved, copied, verified };
  } catch (error) {
    const stopped = stoppedResultIfNeeded(store, runId, options, scanReports, moved, copied, verified);
    if (stopped) {
      return stopped;
    }
    const message = error instanceof Error ? error.message : String(error);
    const failedSummary = error instanceof ModpackPipelineError ? error.summary : buildModpackSummary(store, options);
    markCurrentRunningStepFailed(store, runId, message);
    const failed = store.updateCommandRun(runId, {
      status: isRejectedPipelineError(error) ? "rejected" : "failed",
      finishedAt: new Date().toISOString(),
      exitCode: 1,
      durationMs: durationFrom(startedAt),
      metadata: {
        ...(store.getCommandRun(runId)?.metadata ?? {}),
        blockers: failedSummary.blockers,
        moved: moved.length,
        copied: copied.length,
        verified: verified.length
      },
      output: `${output}\nModpack pipeline failed: ${message}\n`
    }) as CommandRun;
    throw new ModpackPipelineError(message, pipelineErrorStatusCode(error), failedSummary, commandRunToPipelineRun(failed));
  }
}

const PRE_EXISTING_RUNTIME_FINDING_CODES = new Set(["FRESH_CRASH_REPORT", "RUNTIME_CRASH_SIGNATURE"]);

export function scanFailureBlocksPipeline(report: ScanReport, project: Project, settings: AppSettings, pipelineStartedAt: string): boolean {
  if (report.status !== "failed") {
    return false;
  }
  return report.findings.some((finding) => !isPreExistingRuntimeFinding(finding, project, settings, pipelineStartedAt));
}

function isPreExistingRuntimeFinding(finding: ScanReport["findings"][number], project: Project, settings: AppSettings, pipelineStartedAt: string): boolean {
  if (!finding.code || !PRE_EXISTING_RUNTIME_FINDING_CODES.has(finding.code) || !finding.path) {
    return false;
  }
  const runtimeRoot = projectRuntimeRoot(project, settings);
  const runtimePath = path.resolve(runtimeRoot, finding.path);
  if (!isPathInside(runtimeRoot, runtimePath) || !fs.existsSync(runtimePath)) {
    return false;
  }
  return fs.statSync(runtimePath).mtimeMs < Date.parse(pipelineStartedAt);
}

function isPathInside(root: string, candidate: string): boolean {
  const relative = path.relative(path.resolve(root), path.resolve(candidate));
  return relative === "" || (!relative.startsWith("..") && !path.isAbsolute(relative));
}

function projectRuntimeRoot(project: Project, settings: AppSettings): string {
  const workspaceRoot = projectWorkspaceRoot(project, settings);
  if (project.slug === "echo") {
    return workspaceRoot;
  }
  const modulePath = project.modules[0]?.path ?? project.workspacePath;
  if (!modulePath || modulePath === ".") {
    return workspaceRoot;
  }
  return path.resolve(workspaceRoot, modulePath);
}

function isRejectedPipelineError(error: unknown): boolean {
  return (
    (error instanceof ModpackPipelineError || error instanceof JarPipelineError) &&
    error.statusCode < 500
  );
}

function pipelineErrorStatusCode(error: unknown): number {
  if (error instanceof ModpackPipelineError || error instanceof JarPipelineError) {
    return error.statusCode;
  }
  return 500;
}

function managedTargets(store: CommandCenterStore): Project[] {
  return ["echo", "arcana"].map((slug) => store.getProject(slug)).filter((project): project is Project => Boolean(project));
}

function targetFromProject(project: Project, settings: AppSettings, options: ModpackServiceOptions): ModpackTarget {
  const manifest = buildJarManifest(project, settings, jarOptionsFor(project, options));
  const blockers = [...manifest.blockers];
  let status: ModpackStatus = "ready";
  if (blockers.length > 0) {
    status = "blocked";
  } else if (manifest.summary.missing > 0 || manifest.summary.current < manifest.summary.expected || manifest.summary.stale > 0 || manifest.summary.duplicate > 0) {
    status = "missing";
  }
  return {
    projectSlug: project.slug,
    projectName: project.name,
    buildCommandId: jarBuildCommandId(project),
    status,
    manifest,
    blockers
  };
}

function targetBlockers(settings: AppSettings, targets: ModpackTarget[]): string[] {
  if (!settings.modpackModsDir.trim()) {
    return ["Settings > Modpack Mods Folder is not configured. Rebuild & Update All is blocked."];
  }
  if (!safeIsDirectory(settings.modpackModsDir.trim())) {
    return [`Configured Modpack Mods Folder does not exist: ${settings.modpackModsDir.trim()}`];
  }
  if (targets.length === 0) {
    return ["No managed first-party projects are configured."];
  }
  return Array.from(new Set(targets.flatMap((target) => target.blockers)));
}

function statusFromSummary(blockers: string[], summary: ModpackInventory["summary"]): ModpackStatus {
  if (blockers.length > 0) return "blocked";
  if (summary.missing > 0 || summary.current < summary.expected || summary.stale > 0 || summary.duplicate > 0) return "missing";
  return "ready";
}

function initialSteps(projects: Project[]): ModpackPipelineStep[] {
  return [
    { id: "preflight", label: "Preflight", status: "ready", detail: "Check settings, target folder, and managed project list." },
    ...projects.flatMap((project) => [
      { id: `build-${project.slug}`, label: `Build ${project.name}`, status: "ready" as const, detail: `Run ${jarBuildCommandId(project)}.` },
      { id: `promote-${project.slug}`, label: `Promote ${project.name}`, status: "ready" as const, detail: "Quarantine stale managed jars, copy current jars, and verify checksums." },
      { id: `scan-${project.slug}`, label: `Scan ${project.name}`, status: "ready" as const, detail: "Run a quick scan after promotion." }
    ])
  ];
}

async function runBuild(
  runId: string,
  project: Project,
  action: ReleaseAction,
  settings: AppSettings,
  command: string[],
  cwd: string,
  options: ModpackServiceOptions
): Promise<{ exitCode: number; output: string }> {
  if (options.runBuildCommand) {
    const controller = new AbortController();
    activeBuilds.set(runId, { stop: () => controller.abort() });
    try {
      return await options.runBuildCommand({ project, action, settings, command, cwd, signal: controller.signal });
    } finally {
      activeBuilds.delete(runId);
    }
  }
  return new Promise((resolve) => {
    const child = spawn(action.executable, action.args, {
      cwd,
      env: runnerEnvironment(),
      shell: process.platform === "win32",
      windowsHide: true
    });
    activeBuilds.set(runId, { stop: () => killChild(child) });
    let output = "";
    const append = (chunk: Buffer): void => {
      output = `${output}${chunk.toString()}`;
      if (output.length > OUTPUT_LIMIT) {
        output = output.slice(output.length - OUTPUT_LIMIT);
      }
    };
    child.stdout.on("data", append);
    child.stderr.on("data", append);
    child.on("error", (error) => {
      activeBuilds.delete(runId);
      resolve({ exitCode: 1, output: `${output}\n${error.message}` });
    });
    child.on("close", (code) => {
      activeBuilds.delete(runId);
      resolve({ exitCode: code ?? 1, output });
    });
  });
}

function updateStep(store: CommandCenterStore, runId: string, stepId: string, patch: Partial<ModpackPipelineStep>): void {
  const run = store.getCommandRun(runId);
  if (!run) return;
  const steps = stepsFromMetadata(run).map((step) => (step.id === stepId ? { ...step, ...patch } : step));
  store.updateCommandRun(runId, { metadata: { ...(run.metadata ?? {}), steps } });
}

function markCurrentRunningStepStopped(store: CommandCenterStore, runId: string, message: string): void {
  const run = store.getCommandRun(runId);
  if (!run) return;
  const steps = stepsFromMetadata(run).map((step) =>
    step.status === "running" ? { ...step, status: "stopped" as const, finishedAt: new Date().toISOString(), detail: message } : step
  );
  store.updateCommandRun(runId, { metadata: { ...(run.metadata ?? {}), steps } });
}

function markCurrentRunningStepFailed(store: CommandCenterStore, runId: string, message: string): void {
  const run = store.getCommandRun(runId);
  if (!run) return;
  const steps = stepsFromMetadata(run).map((step) =>
    step.status === "running" ? { ...step, status: "failed" as const, finishedAt: new Date().toISOString(), detail: message } : step
  );
  store.updateCommandRun(runId, { metadata: { ...(run.metadata ?? {}), steps } });
}

function stoppedResultIfNeeded(
  store: CommandCenterStore,
  runId: string,
  options: ModpackServiceOptions,
  scanReports: ScanReport[],
  moved: string[],
  copied: string[],
  verified: string[]
): ModpackPipelineResult | null {
  const run = store.getCommandRun(runId);
  if (run?.status !== "stopped") {
    return null;
  }
  return pipelineResult(store, run, options, scanReports, moved, copied, verified);
}

function pipelineResult(
  store: CommandCenterStore,
  run: CommandRun,
  options: ModpackServiceOptions,
  scanReports: ScanReport[],
  moved: string[],
  copied: string[],
  verified: string[]
): ModpackPipelineResult {
  return {
    run: commandRunToPipelineRun(run),
    summary: buildModpackSummary(store, options),
    scanReports,
    moved,
    copied,
    verified
  };
}

function commandRunToPipelineRun(run: CommandRun): ModpackPipelineRun {
  return {
    id: run.id,
    status: pipelineStatusFromCommandRun(run),
    startedAt: run.startedAt,
    finishedAt: run.finishedAt,
    durationMs: run.durationMs,
    targetSlugs: Array.isArray(run.metadata?.targetSlugs) ? run.metadata.targetSlugs.map(String) : [],
    steps: stepsFromMetadata(run),
    output: run.output
  };
}

function pipelineStatusFromCommandRun(run: CommandRun): ModpackStatus {
  if (run.status === "running" || run.status === "queued") return "running";
  if (run.status === "succeeded") return "succeeded";
  if (run.status === "stopped") return "stopped";
  if (run.status === "rejected") return "blocked";
  return "failed";
}

function stepsFromMetadata(run: CommandRun): ModpackPipelineStep[] {
  const steps = run.metadata?.steps;
  return Array.isArray(steps) ? (steps as ModpackPipelineStep[]) : [];
}

function currentRunningStep(run: CommandRun): ModpackPipelineStep | undefined {
  return stepsFromMetadata(run).find((step) => step.status === "running");
}

function hasAnyRunningRun(store: CommandCenterStore): boolean {
  const projectSlugs = [...store.listProjects().map((project) => project.slug), MODPACK_PROJECT_SLUG];
  return projectSlugs.some((slug) => store.listCommandRuns(slug, 10).some((run) => run.status === "running"));
}

function jarOptionsFor(project: Project, options: ModpackServiceOptions): JarServiceOptions {
  return {
    buildRoot: options.buildRoots?.[project.slug] ?? options.buildRoot,
    quarantineDir: options.quarantineDir,
    now: options.now
  };
}

function runnerEnvironment(): NodeJS.ProcessEnv {
  const env = { ...process.env };
  if (env.JAVA_HOME || hasJavaOnPath(env.PATH)) {
    return env;
  }
  const javaHome = discoverLocalJdk();
  if (!javaHome) {
    return env;
  }
  env.JAVA_HOME = javaHome;
  env.PATH = `${path.join(javaHome, "bin")}${path.delimiter}${env.PATH ?? ""}`;
  return env;
}

function hasJavaOnPath(pathValue: string | undefined): boolean {
  if (!pathValue) return false;
  return pathValue
    .split(path.delimiter)
    .some((entry) => fs.existsSync(path.join(entry, process.platform === "win32" ? "java.exe" : "java")));
}

function discoverLocalJdk(): string | null {
  const jdkRoot = path.join(os.homedir(), ".jdks");
  if (!fs.existsSync(jdkRoot)) return null;
  const candidates = fs
    .readdirSync(jdkRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join(jdkRoot, entry.name))
    .filter((candidate) => fs.existsSync(path.join(candidate, "bin", process.platform === "win32" ? "java.exe" : "java")))
    .sort((left, right) => right.localeCompare(left));
  return candidates[0] ?? null;
}

function safeIsDirectory(value: string): boolean {
  try {
    return fs.statSync(value).isDirectory();
  } catch {
    return false;
  }
}

function sum<T>(items: T[], read: (item: T) => number): number {
  return items.reduce((total, item) => total + read(item), 0);
}

function trimStepOutput(output: string): string {
  return output.length > 20_000 ? output.slice(output.length - 20_000) : output;
}

function durationFrom(startedAt: string): number {
  return Math.max(0, Date.now() - new Date(startedAt).getTime());
}

function killChild(child: ChildProcessWithoutNullStreams): void {
  if (!child.killed) {
    child.kill();
  }
}
