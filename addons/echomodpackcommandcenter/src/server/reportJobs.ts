import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { randomUUID } from "node:crypto";
import path from "node:path";
import type {
  CommandRun,
  EchoReportCategoryKey,
  EchoReportJobDetail,
  EchoReportJobDefinition,
  EchoReportJobRequest,
  EchoReportJobSummary
} from "../shared/types.js";
import { CommandCenterStore } from "./db.js";
import { ECHO_ROOT } from "./paths.js";
import { runnerEnvironment } from "./runner.js";

const OUTPUT_LIMIT = 200_000;
const REPORT_JOB_PROJECT = "echo";
const REPORT_JOB_KIND = "echo-report-job";
const activeReportJobs = new Map<string, ChildProcessWithoutNullStreams>();
const stoppedReportJobs = new Set<string>();

const REPORT_JOB_OUTPUT_PREVIEW_LIMIT = 12_000;

const PANEL_REPORT_KEYS: Record<string, string[]> = {
  platform: ["platformVerification"],
  workspace: ["workspaceScan", "scannedModules"],
  graphs: ["moduleGraph", "dependencyGraph", "roleGraph", "featureGraph"],
  packos: ["packProfile", "packReadiness", "lockfile", "installState", "lockfileStatus", "repairPlan", "packDoctor"],
  diagnostics: ["diagnostics"],
  health: ["health", "runtimeHealth", "degradedFeatures"],
  recovery: ["recoveryState", "recoveryPlan"],
  bridge: ["bridgeSessions", "codexRunReport"],
  ai: ["aiTasks"],
  assets: ["missingAssets", "officialPackAssetGates"],
  release: ["releaseReadiness"],
  support: ["supportBundle"],
  launcher: ["launcherStatus", "commandCenterCatalog"]
};

interface ReportJobCommand {
  executable: string;
  args: string[];
  shell?: boolean;
}

export interface EchoReportJobServiceOptions {
  rootDir?: string;
  now?: () => Date;
  commandOverrides?: Partial<Record<string, ReportJobCommand>>;
}

export class EchoReportJobError extends Error {
  readonly statusCode: number;

  constructor(message: string, statusCode = 400) {
    super(message);
    this.name = "EchoReportJobError";
    this.statusCode = statusCode;
  }
}

const REPORT_JOB_DEFINITIONS: EchoReportJobDefinition[] = [
  definition(
    "verifyEchoNativePlatform",
    "Verify Platform",
    "Regenerates the platform verification report without launching Minecraft.",
    "platform",
    ["platformVerification"],
    ["verifyEchoNativePlatform", "-PechoAddonSet=beta"]
  ),
  definition(
    "scanEchoWorkspace",
    "Scan Workspace",
    "Refreshes workspace and scanned-module reports from local repo metadata.",
    "workspace",
    ["workspaceScan", "scannedModules"],
    ["scanEchoWorkspace", "-PechoAddonSet=beta"]
  ),
  definition(
    "generateEchoModuleGraph",
    "Generate Module Graph",
    "Refreshes module, dependency, and role graph reports.",
    "graphs",
    ["moduleGraph", "dependencyGraph", "roleGraph"],
    ["generateEchoModuleGraph", "-PechoAddonSet=beta"]
  ),
  definition(
    "generateEchoFeatureGraph",
    "Generate Feature Graph",
    "Refreshes the feature graph report.",
    "graphs",
    ["featureGraph"],
    ["generateEchoFeatureGraph", "-PechoAddonSet=beta"]
  ),
  definition(
    "generateEchoDiagnostics",
    "Generate Diagnostics",
    "Refreshes the aggregated diagnostics report.",
    "diagnostics",
    ["diagnostics"],
    ["generateEchoDiagnostics", "-PechoAddonSet=beta"]
  ),
  definition(
    "generateEchoPackReadiness",
    "Generate Pack Readiness",
    "Evaluates Ashfall pack readiness from existing report inputs.",
    "packos",
    ["packReadiness"],
    ["generateEchoPackReadiness", "-PechoPack=ashfall", "-PechoAddonSet=beta"]
  ),
  definition(
    "loadEchoPackProfile",
    "Load Pack Profile",
    "Loads the official Ashfall pack profile report.",
    "packos",
    ["packProfile"],
    ["loadEchoPackProfile", "-PechoPack=ashfall"]
  ),
  definition(
    "verifyEchoLockfile",
    "Verify Lockfile",
    "Verifies the current Ashfall lockfile and drift status.",
    "packos",
    ["lockfileStatus"],
    ["verifyEchoLockfile", "-PechoPack=ashfall", "-PechoAddonSet=beta"]
  ),
  definition(
    "generateEchoRepairPlan",
    "Generate Repair Plan",
    "Regenerates the planning-only repair plan report. It does not execute repairs.",
    "packos",
    ["repairPlan"],
    ["generateEchoRepairPlan", "-PechoPack=ashfall", "-PechoAddonSet=beta"]
  ),
  definition(
    "echoPackDoctor",
    "Run Pack Doctor",
    "Runs the safe PackOS report pipeline and updates report artifacts only.",
    "packos",
    [
      "workspaceScan",
      "scannedModules",
      "moduleGraph",
      "dependencyGraph",
      "roleGraph",
      "featureGraph",
      "diagnostics",
      "packReadiness",
      "lockfile",
      "installState",
      "lockfileStatus",
      "repairPlan",
      "packDoctor",
      "health",
      "runtimeHealth",
      "degradedFeatures",
      "recoveryState",
      "recoveryPlan"
    ],
    ["echoPackDoctor", "-PechoPack=ashfall", "-PechoAddonSet=beta"]
  ),
  definition(
    "validateEchoReports",
    "Validate Reports",
    "Validates current report contracts and launcher/catalog report shape.",
    "validation",
    ["releaseReadiness", "supportBundle", "missingAssets", "officialPackAssetGates", "launcherStatus", "commandCenterCatalog"],
    ["validateEchoReports", "-PechoAddonSet=beta"],
    "none",
    false
  )
];

export function echoReportJobDefinitions(options: EchoReportJobServiceOptions = {}): EchoReportJobDefinition[] {
  return REPORT_JOB_DEFINITIONS.map((job) => {
    const command = commandFor(job, options);
    return { ...job, command: [command.executable, ...command.args] };
  });
}

export function listEchoReportJobs(store: CommandCenterStore, options: EchoReportJobServiceOptions = {}): EchoReportJobSummary {
  const runs = store
    .listCommandRuns(REPORT_JOB_PROJECT, 50)
    .filter(isReportJobRun)
    .map((run) => refreshActiveRun(store, run));
  const runningRun = runs.find((run) => run.status === "running") ?? null;
  return {
    generatedAt: now(options).toISOString(),
    status: runningRun ? "running" : "idle",
    definitions: echoReportJobDefinitions(options),
    panelActions: panelActionsFor(echoReportJobDefinitions(options)),
    runs,
    runningRun
  };
}

export function loadEchoReportJobDetail(
  store: CommandCenterStore,
  runId: string,
  options: EchoReportJobServiceOptions = {}
): EchoReportJobDetail | null {
  const run = store.getCommandRun(runId);
  if (!run || !isReportJobRun(run)) {
    return null;
  }
  const refreshed = refreshActiveRun(store, run);
  const rootDir = path.resolve(options.rootDir ?? ECHO_ROOT);
  const definitionValue = definitionForRun(refreshed, options);
  const sanitizedCommand = refreshed.command.map((part) => sanitizeOutput(part, rootDir));
  const outputPreview = previewOutput(refreshed.output, rootDir);
  return {
    generatedAt: now(options).toISOString(),
    run: {
      id: refreshed.id,
      projectSlug: refreshed.projectSlug,
      commandId: refreshed.commandId,
      status: refreshed.status,
      risk: refreshed.risk,
      command: sanitizedCommand,
      startedAt: refreshed.startedAt,
      finishedAt: refreshed.finishedAt,
      exitCode: refreshed.exitCode,
      pid: refreshed.pid,
      durationMs: refreshed.durationMs,
      metadata: safeMetadata(refreshed.metadata ?? {}, rootDir),
      outputPreview,
      outputLineCount: refreshed.output.split(/\r?\n/).length,
      outputTruncated: sanitizeOutput(refreshed.output, rootDir).length > REPORT_JOB_OUTPUT_PREVIEW_LIMIT
    },
    definition: definitionValue,
    command: sanitizedCommand,
    reportKeys: reportKeysForRun(refreshed),
    failureSummary: failureSummaryFor(refreshed, rootDir),
    safety: {
      acceptedCommandIdOnly: true,
      rawCommandsAccepted: false,
      destructive: false,
      launchesMinecraft: false,
      modifiesGameFiles: false,
      downloadsRemoteModules: false,
      executesRepairs: false
    }
  };
}

export function startEchoReportJob(
  store: CommandCenterStore,
  request: EchoReportJobRequest | undefined,
  options: EchoReportJobServiceOptions = {}
): EchoReportJobSummary {
  const commandId = typeof request?.commandId === "string" ? request.commandId.trim() : "";
  if (!commandId) {
    throw new EchoReportJobError("commandId is required. Raw commands are not accepted.", 400);
  }
  const definitionValue = REPORT_JOB_DEFINITIONS.find((job) => job.id === commandId);
  if (!definitionValue) {
    throw new EchoReportJobError(`Report job is not allowlisted: ${commandId}`, 400);
  }
  const running = listEchoReportJobs(store, options).runningRun;
  if (running) {
    throw new EchoReportJobError(`Report job already running: ${running.commandId}`, 409);
  }

  const command = commandFor(definitionValue, options);
  const cwd = path.resolve(options.rootDir ?? ECHO_ROOT);
  const startedAt = now(options).toISOString();
  const run = store.createCommandRun({
    id: randomUUID(),
    projectSlug: REPORT_JOB_PROJECT,
    commandId: definitionValue.id,
    status: "running",
    risk: "low",
    command: [command.executable, ...command.args],
    startedAt,
    metadata: {
      kind: REPORT_JOB_KIND,
      cwd: ".",
      reportKeys: definitionValue.reportKeys,
      category: definitionValue.category,
      destructive: false,
      launchesMinecraft: false,
      modifiesGameFiles: false,
      writesReports: definitionValue.writesReports
    },
    output: `Running safe report job in .\n> ${[command.executable, ...command.args].join(" ")}\n\n`
  });

  const child = spawn(command.executable, command.args, {
    cwd,
    env: runnerEnvironment(),
    shell: command.shell ?? (process.platform === "win32"),
    windowsHide: true
  });
  activeReportJobs.set(run.id, child);
  store.updateCommandRun(run.id, {
    pid: child.pid,
    metadata: { ...(run.metadata ?? {}), pid: child.pid }
  });

  let output = run.output;
  const append = (chunk: Buffer): void => {
    output = `${output}${sanitizeOutput(chunk.toString(), cwd)}`;
    if (output.length > OUTPUT_LIMIT) {
      output = output.slice(output.length - OUTPUT_LIMIT);
    }
    store.updateCommandRun(run.id, { output });
  };

  child.stdout.on("data", append);
  child.stderr.on("data", append);
  child.on("error", (error) => {
    activeReportJobs.delete(run.id);
    if (stoppedReportJobs.has(run.id)) return;
    const current = store.getCommandRun(run.id);
    if (current?.status === "stopped") return;
    store.updateCommandRun(run.id, {
      status: "failed",
      finishedAt: now(options).toISOString(),
      exitCode: 1,
      durationMs: durationFrom(startedAt),
      metadata: { ...(current?.metadata ?? run.metadata ?? {}), failure: "spawn-error" },
      output: `${output}\n${sanitizeOutput(error.message, cwd)}`
    });
  });
  child.on("close", (code) => {
    activeReportJobs.delete(run.id);
    if (stoppedReportJobs.delete(run.id)) return;
    const current = store.getCommandRun(run.id);
    if (current?.status === "stopped") return;
    store.updateCommandRun(run.id, {
      status: code === 0 ? "succeeded" : "failed",
      finishedAt: now(options).toISOString(),
      exitCode: code ?? 1,
      durationMs: durationFrom(startedAt),
      metadata: { ...(current?.metadata ?? run.metadata ?? {}), completedBy: "report-job-runner" },
      output
    });
  });

  return listEchoReportJobs(store, options);
}

export function stopEchoReportJob(
  store: CommandCenterStore,
  runId: string,
  options: EchoReportJobServiceOptions = {}
): EchoReportJobSummary | null {
  const run = store.getCommandRun(runId);
  if (!run || !isReportJobRun(run)) {
    return null;
  }
  const child = activeReportJobs.get(runId);
  if (!child || run.status !== "running") {
    store.updateCommandRun(runId, {
      metadata: { ...(run.metadata ?? {}), stopRequestedAt: now(options).toISOString(), stopResult: "not-running" }
    });
    return listEchoReportJobs(store, options);
  }
  child.kill();
  activeReportJobs.delete(runId);
  stoppedReportJobs.add(runId);
  store.updateCommandRun(runId, {
    status: "stopped",
    finishedAt: now(options).toISOString(),
    exitCode: 130,
    durationMs: durationFrom(run.startedAt),
    metadata: { ...(run.metadata ?? {}), stopRequestedAt: now(options).toISOString(), stopResult: "signal-sent" },
    output: `${run.output}\n\nStopped by Command Center.`
  });
  return listEchoReportJobs(store, options);
}

function definition(
  id: string,
  label: string,
  description: string,
  category: EchoReportCategoryKey | "validation",
  reportKeys: string[],
  gradleArgs: string[],
  risk: "none" | "low" = "low",
  writesReports = true
): EchoReportJobDefinition {
  return {
    id,
    label,
    description,
    category,
    command: [gradleExecutable(), ...gradleArgs],
    reportKeys,
    risk,
    destructive: false,
    launchesMinecraft: false,
    modifiesGameFiles: false,
    writesReports
  };
}

function commandFor(definitionValue: EchoReportJobDefinition, options: EchoReportJobServiceOptions): ReportJobCommand {
  const override = options.commandOverrides?.[definitionValue.id];
  if (override) {
    return override;
  }
  const [executable = gradleExecutable(), ...args] = definitionValue.command;
  return { executable, args };
}

function definitionForRun(run: CommandRun, options: EchoReportJobServiceOptions): EchoReportJobDefinition {
  const definitionValue = echoReportJobDefinitions(options).find((job) => job.id === run.commandId);
  if (definitionValue) {
    return definitionValue;
  }
  return {
    id: run.commandId,
    label: run.commandId,
    description: "Historical report job definition is no longer present in the active allowlist.",
    category: reportJobCategory(run),
    command: run.command,
    reportKeys: reportKeysForRun(run),
    risk: "low",
    destructive: false,
    launchesMinecraft: false,
    modifiesGameFiles: false,
    writesReports: true
  };
}

function panelActionsFor(definitions: EchoReportJobDefinition[]): Record<string, string[]> {
  return Object.fromEntries(
    Object.entries(PANEL_REPORT_KEYS).map(([panelId, reportKeys]) => {
      const reportKeySet = new Set(reportKeys);
      const commandIds = definitions
        .filter((definitionValue) => definitionValue.reportKeys.some((key) => reportKeySet.has(key)))
        .map((definitionValue) => definitionValue.id);
      return [panelId, Array.from(new Set(commandIds))];
    })
  );
}

function gradleExecutable(): string {
  return process.platform === "win32" ? ".\\gradlew.bat" : "./gradlew";
}

function isReportJobRun(run: CommandRun): boolean {
  return run.projectSlug === REPORT_JOB_PROJECT && run.metadata?.kind === REPORT_JOB_KIND;
}

function reportKeysForRun(run: CommandRun): string[] {
  const value = run.metadata?.reportKeys;
  return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}

function reportJobCategory(run: CommandRun): EchoReportCategoryKey | "validation" {
  const value = run.metadata?.category;
  if (
    value === "platform" ||
    value === "workspace" ||
    value === "graphs" ||
    value === "packos" ||
    value === "diagnostics" ||
    value === "health" ||
    value === "recovery" ||
    value === "bridge" ||
    value === "ai" ||
    value === "assets" ||
    value === "release" ||
    value === "support" ||
    value === "launcher" ||
    value === "validation"
  ) {
    return value;
  }
  return "validation";
}

function safeMetadata(metadata: Record<string, unknown>, rootDir: string): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(metadata)
      .filter(([key]) => !["cwd", "pid"].includes(key))
      .map(([key, value]) => [key, sanitizeMetadataValue(value, rootDir)])
  );
}

function sanitizeMetadataValue(value: unknown, rootDir: string): unknown {
  if (typeof value === "string") {
    return sanitizeOutput(value, rootDir);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => sanitizeMetadataValue(entry, rootDir));
  }
  if (value && typeof value === "object") {
    return safeMetadata(value as Record<string, unknown>, rootDir);
  }
  return value;
}

function refreshActiveRun(store: CommandCenterStore, run: CommandRun): CommandRun {
  if (run.status !== "running" || activeReportJobs.has(run.id)) {
    return run;
  }
  return store.updateCommandRun(run.id, {
    status: "failed",
    finishedAt: new Date().toISOString(),
    exitCode: 1,
    durationMs: durationFrom(run.startedAt),
    metadata: { ...(run.metadata ?? {}), failure: "runner-restarted" },
    output: `${run.output}\n\nReport job was marked failed because the Command Center process restarted before it finished.`
  }) ?? run;
}

function now(options: EchoReportJobServiceOptions): Date {
  return options.now?.() ?? new Date();
}

function durationFrom(startedAt: string): number {
  return Math.max(0, Date.now() - new Date(startedAt).getTime());
}

function previewOutput(value: string, rootDir: string): string {
  const sanitized = sanitizeOutput(value, rootDir);
  if (sanitized.length <= REPORT_JOB_OUTPUT_PREVIEW_LIMIT) {
    return sanitized;
  }
  return sanitized.slice(sanitized.length - REPORT_JOB_OUTPUT_PREVIEW_LIMIT);
}

function failureSummaryFor(run: CommandRun, rootDir: string): string {
  if (run.status !== "failed" && run.status !== "rejected" && run.status !== "stopped") {
    return "";
  }
  const sanitized = sanitizeOutput(run.output, rootDir);
  const lines = sanitized
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  const tail = lines.slice(-6).join("\n");
  return tail || `Report job ended with status ${run.status}${run.exitCode == null ? "." : ` and exit code ${run.exitCode}.`}`;
}

function sanitizeOutput(value: string, cwd: string): string {
  const forward = cwd.replaceAll("\\", "/");
  const escapedForward = escapeRegExp(forward);
  const escapedNative = escapeRegExp(cwd);
  return value
    .replace(new RegExp(escapedForward, "gi"), ".")
    .replace(new RegExp(escapedNative, "gi"), ".");
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
