import fs from "node:fs";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { ECHO_ROOT, LOCAL_DATA_DIR, toDisplayPath } from "./paths.js";
import type {
  EchoBridgeConfirmationRequest,
  EchoBridgeCreateSessionRequest,
  EchoBridgeDiagnostic,
  EchoBridgeExecutorConfig,
  EchoBridgeExecutorProbe,
  EchoBridgeJob,
  EchoBridgeJobStatus,
  EchoBridgeLogChunk,
  EchoBridgePromptRequest,
  EchoBridgeSafeActionRequest,
  EchoBridgeSession,
  EchoBridgeStreamCursors,
  EchoBridgeState
} from "../shared/types.js";

export interface EchoBridgeServiceOptions {
  statePath?: string;
  executorConfigPath?: string;
  rootDir?: string;
  now?: () => Date;
}

const DEFAULT_STATE_PATH = path.join(LOCAL_DATA_DIR, "bridge-state.json");
const DEFAULT_EXECUTOR_CONFIG_PATH = path.join(LOCAL_DATA_DIR, "bridge-executor.json");
const DEFAULT_ALLOWED_EXECUTABLE_NAMES = ["codex", "codex.cmd", "codex.exe", "codex.ps1"];
const EXECUTOR_ARGUMENT_MODE = "echo_bridge_sidecar_v1";
const ACTIVE_EXECUTOR_PROCESSES = new Map<string, ActiveBridgeProcess>();

interface ActiveBridgeProcess {
  child: ChildProcessWithoutNullStreams;
  canceled: boolean;
}

interface TrustedExecutorLaunchConfig {
  executablePath: string;
  workspaceRoot: string;
  sidecarEntrypoint: string;
}

export function loadBridgeState(options: EchoBridgeServiceOptions = {}): EchoBridgeState {
  const statePath = options.statePath ?? DEFAULT_STATE_PATH;
  const loaded = readState(statePath);
  return normalizeState(loaded ?? initialState(options), options);
}

export function probeBridgeExecutor(options: EchoBridgeServiceOptions = {}): EchoBridgeExecutorProbe {
  return executorProbeFor(executorConfigFor(options), options);
}

export function createBridgeSession(request: EchoBridgeCreateSessionRequest = {}, options: EchoBridgeServiceOptions = {}): EchoBridgeState {
  const state = loadBridgeState(options);
  const now = isoNow(options);
  const session: EchoBridgeSession = {
    id: `bridge-session-${randomUUID()}`,
    displayName: sanitizeText(request.displayName, "CyberDex local session"),
    workspace: sanitizePath(request.workspace ?? state.workspace),
    status: state.executorStatus === "configured" ? "idle" : "not_configured",
    activeJobIds: [],
    createdAt: now,
    updatedAt: now,
    connectedPcBridge: "local-command-center",
    protectedFiles: state.protectedFiles,
    safeEditZones: state.safeEditZones
  };
  const next = {
    ...state,
    generatedAt: now,
    sessions: [session, ...state.sessions.filter((candidate) => candidate.id !== session.id)].slice(0, 12)
  };
  writeState(next, options);
  return next;
}

export function submitBridgePrompt(sessionId: string, request: EchoBridgePromptRequest, options: EchoBridgeServiceOptions = {}): EchoBridgeState {
  const state = ensureSession(sessionId, loadBridgeState(options), options);
  const now = isoNow(options);
  const jobId = `codex-job-${randomUUID()}`;
  const taskId = sanitizeText(request.taskId, "phase6.cyberdex_codex_automation_loop");
  const actionId = `safe-action-${randomUUID()}`;
  const status: EchoBridgeJobStatus = request.startCodex === false ? "queued" : "needs_confirmation";
  const promptSummary = truncate(sanitizePromptLogText(request.promptText), 420);
  writeJobPromptFile(jobId, request, options);
  const safeAction: EchoBridgeSafeActionRequest = {
    id: actionId,
    sessionId,
    jobId,
    status: "pending_confirmation",
    kind: "start_codex_job",
    risk: "medium",
    requiresConfirmation: true,
    summary: "Start Codex job intent",
    developerDetails: "Command Center records the job request but will not launch Codex until a trusted local executor is configured and this action is confirmed.",
    relatedDiagnostics: [state.executorProbe.status === "configured" ? "ECHO-BRIDGE-EXECUTOR-CONFIRMATION-REQUIRED" : "ECHO-BRIDGE-CODEX-NOT-CONFIGURED"],
    createdAt: now
  };
  const log = logChunk(jobId, "state", `Prompt submitted: ${promptSummary}`, 0, now);
  const diagnostics = state.executorProbe.status === "configured"
    ? [executorReadyDiagnostic()]
    : [bridgeNotConfiguredDiagnostic()];
  const job: EchoBridgeJob = {
    id: jobId,
    sessionId,
    status,
    taskId,
    taskTitle: sanitizeText(request.taskTitle, "CyberDex Codex task"),
    moduleId: sanitizeText(request.moduleId, "echobridgecore"),
    agentLane: sanitizeText(request.agentLane, "architect_agent"),
    buildStatus: "not_run",
    validationStatus: "not_run",
    summary: "Prompt accepted into the local bridge queue. Codex execution requires confirmation and a configured local executor.",
    createdAt: now,
    updatedAt: now,
    diagnostics,
    safeActionRequestIds: [actionId],
    recentLogChunks: [log],
    streamCursors: streamCursorsFor(jobId, [log]),
    nextPrompt: state.nextGeneratedPrompt
  };
  const sessions = state.sessions.map((session) => session.id === sessionId
    ? { ...session, status: state.executorProbe.status === "configured" ? "idle" as const : "not_configured" as const, activeJobIds: [jobId, ...session.activeJobIds].slice(0, 5), updatedAt: now }
    : session);
  const next = {
    ...state,
    generatedAt: now,
    sessions,
    jobs: [job, ...state.jobs].slice(0, 25),
    safeActionRequests: [safeAction, ...state.safeActionRequests].slice(0, 25),
    diagnostics: uniqueDiagnostics([...diagnostics, ...state.diagnostics])
  };
  writeState(next, options);
  return next;
}

export function cancelBridgeJob(jobId: string, options: EchoBridgeServiceOptions = {}): EchoBridgeState {
  const state = loadBridgeState(options);
  const now = isoNow(options);
  const active = ACTIVE_EXECUTOR_PROCESSES.get(jobId);
  if (active) {
    active.canceled = true;
    active.child.kill();
  }
  const next = {
    ...state,
    generatedAt: now,
    jobs: state.jobs.map((job) => job.id === jobId && !["completed", "failed", "canceled", "blocked"].includes(job.status)
      ? withLog({
          ...job,
          status: "canceled" as const,
          summary: active ? "Job cancellation requested for the bridge-owned executor process." : "Job canceled by Command Center or CyberDex before execution.",
          updatedAt: now
        }, "state", active ? "Cancellation signal sent to bridge-owned executor process." : "Job canceled before execution.", now)
      : job)
  };
  writeState(next, options);
  return next;
}

export async function confirmBridgeSafeAction(requestId: string, request: EchoBridgeConfirmationRequest, options: EchoBridgeServiceOptions = {}): Promise<EchoBridgeState> {
  const state = loadBridgeState(options);
  const now = isoNow(options);
  const safeAction = state.safeActionRequests.find((candidate) => candidate.id === requestId);
  if (!safeAction) return state;
  const approved = request.confirmed === true;
  const updatedActions = state.safeActionRequests.map((candidate) => candidate.id === requestId
    ? { ...candidate, status: approved ? "approved" as const : "rejected" as const, developerDetails: appendNote(candidate.developerDetails, request.note) }
    : candidate);
  const probe = probeBridgeExecutor(options);
  const updatedJobs = state.jobs.map((job) => {
    if (job.id !== safeAction.jobId) return job;
    const executorConfigured = probe.status === "configured";
    const message = !approved
      ? "Start rejected. Job canceled before execution."
      : executorConfigured
        ? "Start confirmed and trusted executor probe passed. Command Center is starting the configured bridge-owned executor process."
        : "Start confirmed, but Codex executor is not configured. Job is blocked without launching a process.";
    const diagnostics = !approved
      ? job.diagnostics
      : executorConfigured
        ? uniqueDiagnostics([executorStartingDiagnostic(), ...job.diagnostics])
        : uniqueDiagnostics([bridgeNotConfiguredDiagnostic(), ...job.diagnostics]);
    return withLog({
      ...job,
      status: !approved ? "canceled" as const : executorConfigured ? "starting" as const : "blocked" as const,
      summary: message,
      updatedAt: now,
      diagnostics
    }, "state", message, now);
  });
  const historyEntry = {
    requestId,
    jobId: safeAction.jobId,
    action: safeAction.kind,
    status: approved ? (probe.status === "configured" ? "approved" as const : "blocked" as const) : "rejected" as const,
    confirmed: approved,
    note: sanitizeText(request.note, ""),
    createdAt: now
  };
  const next = {
    ...state,
    generatedAt: now,
    executorStatus: probe.status,
    executorReason: probe.reason,
    executorConfig: executorConfigFor(options),
    executorProbe: probe,
    safeActionRequests: updatedActions,
    jobs: updatedJobs,
    confirmationHistory: [historyEntry, ...state.confirmationHistory].slice(0, 50),
    diagnostics: approved ? uniqueDiagnostics([...probe.diagnostics, ...state.diagnostics]) : state.diagnostics
  };
  writeState(next, options);
  if (approved && probe.status === "configured") {
    return startTrustedExecutorProcess(safeAction.jobId, options);
  }
  return next;
}

export function bridgeJobLogs(jobId: string, options: EchoBridgeServiceOptions = {}, after?: string): EchoBridgeLogChunk[] {
  const logs = loadBridgeState(options).jobs.find((job) => job.id === jobId)?.recentLogChunks ?? [];
  if (!after) return logs;
  const parsed = parseCursor(after);
  if (!parsed) return logs;
  return logs.filter((chunk) => chunk.stream === parsed.stream && cursorIndex(chunk.cursor) > parsed.index);
}

function initialState(options: EchoBridgeServiceOptions): EchoBridgeState {
  const now = isoNow(options);
  const executorConfig = executorConfigFor(options);
  const executorProbe = executorProbeFor(executorConfig, options);
  return {
    generatedAt: now,
    localOnly: true,
    workspace: toDisplayPath(rootDir(options)),
    executorStatus: executorProbe.status,
    executorReason: executorProbe.reason,
    executorConfig,
    executorProbe,
    sessions: [],
    jobs: [],
    safeActionRequests: [],
    confirmationHistory: [],
    diagnostics: executorProbe.diagnostics,
    nextGeneratedPrompt: "Continue Phase 6 by configuring a trusted local Codex executor only after BridgeCore, Command Center, and CyberDex confirmation flows validate.",
    protectedFiles: [
      "packs/**",
      "saves/**",
      ".env",
      ".local/**/tokens/**"
    ],
    safeEditZones: [
      "addons/echobridgecore/**",
      "addons/echomodpackcommandcenter/**",
      "docs/echo/bridge/**",
      "docs/echo/cyberdex/**"
    ]
  };
}

function startTrustedExecutorProcess(jobId: string, options: EchoBridgeServiceOptions): EchoBridgeState {
  const launch = trustedExecutorLaunchConfig(options);
  if (!launch) {
    return blockExecutorStart(jobId, options, "Trusted executor launch config is unavailable.", bridgeNotConfiguredDiagnostic());
  }
  if (!launch.sidecarEntrypoint) {
    return updateBridgeJob(jobId, options, (job) => withLog({
      ...job,
      status: "failed",
      summary: "Trusted executor process failed because no bridge sidecar entrypoint is configured.",
      updatedAt: isoNow(options),
      diagnostics: uniqueDiagnostics([
        executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-SIDECAR-MISSING", "Executor launch requires a trusted bridge sidecar entrypoint."),
        ...job.diagnostics
      ])
    }, "state", "Executor start failed before launch because no trusted bridge sidecar entrypoint is configured.", isoNow(options)), { removeActiveJob: true });
  }
  const promptFile = jobPromptFile(jobId, options);
  if (!fs.existsSync(promptFile)) {
    return blockExecutorStart(
      jobId,
      options,
      "Trusted executor prompt file is missing; refusing to launch.",
      executorConfigInvalidDiagnostic("ECHO-BRIDGE-PROMPT-FILE-MISSING", "Bridge prompt file was not found in local bridge storage.")
    );
  }
  const statePath = options.statePath ?? DEFAULT_STATE_PATH;
  const args = executorArguments(jobId, launch, promptFile, statePath);
  const now = isoNow(options);

  try {
    const child = spawn(launch.executablePath, args, {
      cwd: launch.workspaceRoot,
      env: safeExecutorEnvironment(),
      shell: false,
      stdio: "pipe",
      windowsHide: true
    });
    ACTIVE_EXECUTOR_PROCESSES.set(jobId, { child, canceled: false });
    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (data) => appendExecutorLog(jobId, "stdout", String(data), options));
    child.stderr.on("data", (data) => appendExecutorLog(jobId, "stderr", String(data), options));
    child.on("error", (error) => {
      ACTIVE_EXECUTOR_PROCESSES.delete(jobId);
      blockExecutorStart(jobId, options, `Executor process failed to start: ${error.message}`, executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-SPAWN-FAILED", "Executor process failed to start."));
    });
    child.on("exit", (code, signal) => {
      const active = ACTIVE_EXECUTOR_PROCESSES.get(jobId);
      ACTIVE_EXECUTOR_PROCESSES.delete(jobId);
      finishExecutorProcess(jobId, code, signal, active?.canceled ?? false, options);
    });
    return updateBridgeJob(jobId, options, (job) => withLog({
      ...job,
      status: "running",
      summary: "Trusted executor process started with fixed ECHO bridge sidecar arguments.",
      updatedAt: now,
      executorPid: child.pid,
      executorStartedAt: now,
      diagnostics: uniqueDiagnostics([executorRunningDiagnostic(), ...job.diagnostics])
    }, "state", "Executor process started with fixed echo_bridge_sidecar_v1 arguments.", now), { sessionStatus: "connected" });
  } catch (error) {
    return blockExecutorStart(
      jobId,
      options,
      `Executor process failed to start: ${error instanceof Error ? error.message : String(error)}`,
      executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-SPAWN-FAILED", "Executor process failed to start.")
    );
  }
}

function trustedExecutorLaunchConfig(options: EchoBridgeServiceOptions): TrustedExecutorLaunchConfig | null {
  const probe = probeBridgeExecutor(options);
  if (probe.status !== "configured") return null;
  const raw = readJsonObject(options.executorConfigPath ?? DEFAULT_EXECUTOR_CONFIG_PATH);
  if (!raw || typeof raw.executablePath !== "string") return null;
  const root = rootDir(options);
  const workspaceRoot = typeof raw.workspaceRoot === "string" ? path.resolve(raw.workspaceRoot) : root;
  const sidecarEntrypoint = typeof raw.sidecarEntrypoint === "string" && raw.sidecarEntrypoint.trim()
    ? path.resolve(raw.sidecarEntrypoint)
    : "";
  return {
    executablePath: path.resolve(raw.executablePath),
    workspaceRoot,
    sidecarEntrypoint
  };
}

function executorArguments(jobId: string, launch: TrustedExecutorLaunchConfig, promptFile: string, statePath: string): string[] {
  return [
    ...(launch.sidecarEntrypoint ? [launch.sidecarEntrypoint] : []),
    "--echo-bridge-job",
    jobId,
    "--workspace",
    launch.workspaceRoot,
    "--prompt-file",
    promptFile,
    "--state-file",
    statePath,
    "--stream-protocol",
    EXECUTOR_ARGUMENT_MODE
  ];
}

function writeJobPromptFile(jobId: string, request: EchoBridgePromptRequest, options: EchoBridgeServiceOptions): void {
  const filePath = jobPromptFile(jobId, options);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const body = [
    "# ECHO Bridge Prompt",
    "",
    `jobId: ${jobId}`,
    `taskId: ${sanitizeText(request.taskId, "phase6.cyberdex_codex_automation_loop")}`,
    `moduleId: ${sanitizeText(request.moduleId, "echobridgecore")}`,
    `agentLane: ${sanitizeText(request.agentLane, "architect_agent")}`,
    "",
    "## Prompt",
    "",
    stripUnsafeControlCharacters(typeof request.promptText === "string" ? request.promptText : "")
  ].join("\n");
  fs.writeFileSync(filePath, `${body.trimEnd()}\n`, "utf8");
}

function jobPromptFile(jobId: string, options: EchoBridgeServiceOptions): string {
  return path.join(localBridgeDataDir(options), "bridge-jobs", jobId, "prompt.md");
}

function localBridgeDataDir(options: EchoBridgeServiceOptions): string {
  return path.dirname(options.statePath ?? DEFAULT_STATE_PATH);
}

function appendExecutorLog(jobId: string, stream: EchoBridgeLogChunk["stream"], text: string, options: EchoBridgeServiceOptions): void {
  const now = isoNow(options);
  updateBridgeJob(jobId, options, (job) => {
    const nextStatus = isTerminalJobStatus(job.status) ? job.status : "streaming";
    return withLog({
      ...job,
      status: nextStatus,
      updatedAt: now
    }, stream, redactLogText(text), now);
  });
}

function finishExecutorProcess(jobId: string, code: number | null, signal: NodeJS.Signals | null, canceled: boolean, options: EchoBridgeServiceOptions): EchoBridgeState {
  const now = isoNow(options);
  const success = code === 0 && !canceled;
  const status: EchoBridgeJobStatus = canceled ? "canceled" : success ? "completed" : "failed";
  const message = canceled
    ? "Executor process was canceled by Command Center or CyberDex."
    : success
      ? "Executor process completed successfully."
      : `Executor process failed with exit code ${code ?? "unknown"}${signal ? ` and signal ${signal}` : ""}.`;
  return updateBridgeJob(jobId, options, (job) => withLog({
    ...job,
    status,
    summary: message,
    updatedAt: now,
    executorFinishedAt: now,
    executorExitCode: code,
    diagnostics: success || canceled
      ? job.diagnostics
      : uniqueDiagnostics([executorProcessFailedDiagnostic(code, signal), ...job.diagnostics])
  }, "state", message, now), { removeActiveJob: true });
}

function blockExecutorStart(jobId: string, options: EchoBridgeServiceOptions, summary: string, diagnostic: EchoBridgeDiagnostic): EchoBridgeState {
  const now = isoNow(options);
  return updateBridgeJob(jobId, options, (job) => withLog({
    ...job,
    status: "blocked",
    summary,
    updatedAt: now,
    diagnostics: uniqueDiagnostics([diagnostic, ...job.diagnostics])
  }, "state", summary, now), { removeActiveJob: true });
}

function updateBridgeJob(
  jobId: string,
  options: EchoBridgeServiceOptions,
  transform: (job: EchoBridgeJob) => EchoBridgeJob,
  sessionUpdate: { sessionStatus?: EchoBridgeSession["status"]; removeActiveJob?: boolean } = {}
): EchoBridgeState {
  const state = loadBridgeState(options);
  const now = isoNow(options);
  const target = state.jobs.find((job) => job.id === jobId);
  const sessionId = target?.sessionId;
  const next = {
    ...state,
    generatedAt: now,
    sessions: state.sessions.map((session) => session.id === sessionId
      ? {
          ...session,
          status: sessionUpdate.sessionStatus ?? session.status,
          activeJobIds: sessionUpdate.removeActiveJob ? session.activeJobIds.filter((id) => id !== jobId) : session.activeJobIds,
          updatedAt: now
        }
      : session),
    jobs: state.jobs.map((job) => job.id === jobId ? transform(job) : job)
  };
  writeState(next, options);
  return next;
}

function normalizeState(state: EchoBridgeState, options: EchoBridgeServiceOptions): EchoBridgeState {
  const executorConfig = executorConfigFor(options);
  const executorProbe = executorProbeFor(executorConfig, options);
  const jobs = state.jobs.map((job) => {
    const recentLogChunks = job.recentLogChunks ?? [];
    return {
      ...job,
      recentLogChunks,
      streamCursors: job.streamCursors ?? streamCursorsFor(job.id, recentLogChunks)
    };
  });
  return {
    ...state,
    localOnly: true,
    workspace: state.workspace || toDisplayPath(rootDir(options)),
    executorStatus: executorProbe.status,
    executorReason: executorProbe.reason,
    executorConfig,
    executorProbe,
    jobs,
    confirmationHistory: state.confirmationHistory ?? [],
    diagnostics: uniqueDiagnostics([...executorProbe.diagnostics, ...(state.diagnostics ?? [])]),
    protectedFiles: state.protectedFiles ?? [],
    safeEditZones: state.safeEditZones ?? []
  };
}

function rootDir(options: EchoBridgeServiceOptions): string {
  return path.resolve(options.rootDir ?? ECHO_ROOT);
}

function executorConfigFor(options: EchoBridgeServiceOptions): EchoBridgeExecutorConfig {
  const configPath = options.executorConfigPath ?? DEFAULT_EXECUTOR_CONFIG_PATH;
  const fallback: EchoBridgeExecutorConfig = {
    enabled: false,
    localOnly: true,
    argumentMode: EXECUTOR_ARGUMENT_MODE,
    executablePath: "",
    executableLabel: "",
    sidecarEntrypoint: "",
    workspaceRoot: toDisplayPath(rootDir(options)),
    allowlistedExecutableNames: DEFAULT_ALLOWED_EXECUTABLE_NAMES,
    configPath: safeLocalPath(configPath, rootDir(options)),
    redacted: true
  };
  const raw = readJsonObject(configPath);
  if (!raw) return fallback;
  const executablePath = typeof raw.executablePath === "string" ? raw.executablePath : "";
  const sidecarEntrypoint = typeof raw.sidecarEntrypoint === "string" ? raw.sidecarEntrypoint : "";
  const workspaceRoot = typeof raw.workspaceRoot === "string" ? raw.workspaceRoot : rootDir(options);
  const allowlistedExecutableNames = Array.isArray(raw.allowlistedExecutableNames)
    ? raw.allowlistedExecutableNames.filter((entry): entry is string => typeof entry === "string" && entry.trim().length > 0)
    : DEFAULT_ALLOWED_EXECUTABLE_NAMES;
  return {
    enabled: raw.enabled === true,
    localOnly: true,
    argumentMode: EXECUTOR_ARGUMENT_MODE,
    executablePath: safeLocalPath(executablePath, rootDir(options)),
    executableLabel: executablePath ? path.basename(executablePath) : "",
    sidecarEntrypoint: safeLocalPath(sidecarEntrypoint, rootDir(options)),
    workspaceRoot: safeLocalPath(workspaceRoot, rootDir(options)),
    allowlistedExecutableNames: allowlistedExecutableNames.length ? allowlistedExecutableNames.slice(0, 12).sort() : DEFAULT_ALLOWED_EXECUTABLE_NAMES,
    configPath: safeLocalPath(configPath, rootDir(options)),
    redacted: true
  };
}

function executorProbeFor(config: EchoBridgeExecutorConfig, options: EchoBridgeServiceOptions): EchoBridgeExecutorProbe {
  const now = isoNow(options);
  const configPath = options.executorConfigPath ?? DEFAULT_EXECUTOR_CONFIG_PATH;
  const raw = readJsonObject(configPath);
  const root = rootDir(options);
  const executablePath = typeof raw?.executablePath === "string" ? path.resolve(raw.executablePath) : "";
  const sidecarEntrypoint = typeof raw?.sidecarEntrypoint === "string" && raw.sidecarEntrypoint.trim()
    ? path.resolve(raw.sidecarEntrypoint)
    : "";
  const workspaceRoot = typeof raw?.workspaceRoot === "string" ? path.resolve(raw.workspaceRoot) : root;
  const allowed = config.allowlistedExecutableNames.map((entry) => entry.toLowerCase());

  if (!raw || raw.enabled !== true) {
    return probe("not_configured", config, now, "Trusted Codex executor config is absent or disabled.", [bridgeNotConfiguredDiagnostic()]);
  }
  if (!executablePath) {
    return probe("blocked", config, now, "Executor config is enabled but executablePath is missing.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-PATH-MISSING", "Executor config is enabled but executablePath is missing.")]);
  }
  if (!allowed.includes(path.basename(executablePath).toLowerCase())) {
    return probe("blocked", config, now, "Executor executable is not allowlisted.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-NOT-ALLOWLISTED", "Executor executable is not one of the allowlisted Codex binary names.")]);
  }
  if (!isInside(root, workspaceRoot)) {
    return probe("blocked", config, now, "Executor workspace root is outside the ECHO workspace.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-WORKSPACE-OUTSIDE-ROOT", "Executor workspace root must stay inside the ECHO workspace.")]);
  }
  if (sidecarEntrypoint && !isInside(root, sidecarEntrypoint)) {
    return probe("blocked", config, now, "Executor sidecar entrypoint is outside the ECHO workspace.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-SIDECAR-OUTSIDE-ROOT", "Executor sidecar entrypoint must stay inside the ECHO workspace.")]);
  }
  if (sidecarEntrypoint && !fs.existsSync(sidecarEntrypoint)) {
    return probe("offline", config, now, "Executor sidecar entrypoint is configured but not present on disk.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-SIDECAR-OFFLINE", "Executor sidecar entrypoint is configured but unavailable.")]);
  }
  if (!fs.existsSync(executablePath)) {
    return probe("offline", config, now, "Executor binary is configured but not present on disk.", [executorConfigInvalidDiagnostic("ECHO-BRIDGE-EXECUTOR-OFFLINE", "Executor binary is configured but unavailable.")]);
  }
  return probe("configured", config, now, "Trusted Codex executor probe passed. Process launch remains confirmation-gated.", [executorReadyDiagnostic()]);
}

function probe(status: EchoBridgeExecutorProbe["status"], config: EchoBridgeExecutorConfig, now: string, reason: string, diagnostics: EchoBridgeDiagnostic[]): EchoBridgeExecutorProbe {
  return {
    status,
    configured: status === "configured",
    enabled: config.enabled,
    available: status === "configured",
    localOnly: true,
    executableLabel: config.executableLabel,
    executablePath: config.executablePath,
    workspaceRoot: config.workspaceRoot,
    lastCheckedAt: now,
    reason,
    diagnostics
  };
}

function ensureSession(sessionId: string, state: EchoBridgeState, options: EchoBridgeServiceOptions): EchoBridgeState {
  if (state.sessions.some((session) => session.id === sessionId)) return state;
  const next = createBridgeSession({ displayName: "CyberDex recovered session", workspace: state.workspace }, options);
  const created = next.sessions[0];
  if (!created) return state;
  return {
    ...next,
    sessions: next.sessions.map((session, index) => index === 0 ? { ...session, id: sessionId } : session)
  };
}

function bridgeNotConfiguredDiagnostic(): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-CODEX-NOT-CONFIGURED",
    severity: "WARNING",
    summary: "Codex executor is not configured, so bridge jobs are recorded but not launched.",
    source: "bridge",
    blocking: false
  };
}

function executorReadyDiagnostic(): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-EXECUTOR-CONFIGURED",
    severity: "INFO",
    summary: "Trusted Codex executor configuration passed local probe; launches still require confirmation.",
    source: "bridge",
    blocking: false
  };
}

function executorReadyNoLaunchDiagnostic(): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-EXECUTOR-READY-NO-LAUNCH",
    severity: "NOTICE",
    summary: "Start was confirmed and executor probe passed, but Command Center did not launch a process in this hardening slice.",
    source: "bridge",
    blocking: false
  };
}

function executorStartingDiagnostic(): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-EXECUTOR-STARTING",
    severity: "INFO",
    summary: "Start was confirmed and Command Center is starting the trusted local executor with fixed sidecar arguments.",
    source: "bridge",
    blocking: false
  };
}

function executorRunningDiagnostic(): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-EXECUTOR-RUNNING",
    severity: "INFO",
    summary: "Trusted local executor process is running under Command Center bridge ownership.",
    source: "bridge",
    blocking: false
  };
}

function executorProcessFailedDiagnostic(code: number | null, signal: NodeJS.Signals | null): EchoBridgeDiagnostic {
  return {
    code: "ECHO-BRIDGE-EXECUTOR-FAILED",
    severity: "ERROR",
    summary: `Trusted executor process failed with exit code ${code ?? "unknown"}${signal ? ` and signal ${signal}` : ""}.`,
    source: "bridge",
    blocking: true
  };
}

function executorConfigInvalidDiagnostic(code: string, summary: string): EchoBridgeDiagnostic {
  return {
    code,
    severity: "ERROR",
    summary,
    source: "bridge",
    blocking: true
  };
}

function withLog(job: EchoBridgeJob, stream: EchoBridgeLogChunk["stream"], text: string, now: string): EchoBridgeJob {
  const streamIndex = job.recentLogChunks.filter((chunk) => chunk.stream === stream).length;
  const recentLogChunks = [logChunk(job.id, stream, text, streamIndex, now), ...job.recentLogChunks].slice(0, 40);
  return {
    ...job,
    recentLogChunks,
    streamCursors: streamCursorsFor(job.id, recentLogChunks)
  };
}

function streamCursorsFor(jobId: string, chunks: EchoBridgeLogChunk[]): EchoBridgeStreamCursors {
  return {
    stdout: streamCursor(jobId, "stdout", chunks),
    stderr: streamCursor(jobId, "stderr", chunks),
    diagnostics: streamCursor(jobId, "diagnostics", chunks),
    state: streamCursor(jobId, "state", chunks)
  };
}

function streamCursor(jobId: string, stream: EchoBridgeLogChunk["stream"], chunks: EchoBridgeLogChunk[]): string {
  return `${jobId}.${stream}:${chunks.filter((chunk) => chunk.stream === stream).length}`;
}

function logChunk(jobId: string, stream: EchoBridgeLogChunk["stream"], text: string, index: number, now: string): EchoBridgeLogChunk {
  return {
    id: `log-${jobId}-${stream}-${index}`,
    jobId,
    stream,
    text,
    cursor: `${jobId}.${stream}:${index}`,
    createdAt: now,
    redacted: true
  };
}

function parseCursor(cursor: string): { stream: EchoBridgeLogChunk["stream"]; index: number } | null {
  const match = /\.(stdout|stderr|diagnostics|state):(\d+)$/.exec(cursor);
  if (!match) return null;
  return {
    stream: match[1] as EchoBridgeLogChunk["stream"],
    index: Number(match[2])
  };
}

function cursorIndex(cursor: string): number {
  return Number(cursor.split(":").at(-1) ?? -1);
}

function readState(statePath: string): EchoBridgeState | null {
  try {
    if (!fs.existsSync(statePath)) return null;
    return JSON.parse(fs.readFileSync(statePath, "utf8")) as EchoBridgeState;
  } catch {
    return null;
  }
}

function readJsonObject(filePath: string): Record<string, unknown> | null {
  try {
    if (!fs.existsSync(filePath)) return null;
    const payload = JSON.parse(fs.readFileSync(filePath, "utf8")) as unknown;
    return typeof payload === "object" && payload !== null && !Array.isArray(payload) ? payload as Record<string, unknown> : null;
  } catch {
    return null;
  }
}

function writeState(state: EchoBridgeState, options: EchoBridgeServiceOptions): void {
  const statePath = options.statePath ?? DEFAULT_STATE_PATH;
  fs.mkdirSync(path.dirname(statePath), { recursive: true });
  fs.writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`, "utf8");
}

function isoNow(options: EchoBridgeServiceOptions): string {
  return (options.now?.() ?? new Date()).toISOString();
}

function sanitizeText(value: unknown, fallback: string): string {
  if (typeof value !== "string") return fallback;
  const trimmed = value.replace(/[A-Za-z]:[\\/][^\s]+/g, "[localOnly:redacted-path]").trim();
  return trimmed.length ? trimmed : fallback;
}

function sanitizePromptLogText(value: unknown): string {
  const text = sanitizeText(value, "No prompt text supplied.");
  return text
    .replace(/[;&|`<>$]+/g, " ")
    .replace(/\b(?:whoami|curl|wget|powershell|pwsh|cmd|del|erase|rd|rmdir|rm|sudo|chmod|chown)\b/gi, "[redacted-command]")
    .replace(/\s+/g, " ")
    .trim();
}

function redactLogText(value: string): string {
  return truncate(stripUnsafeControlCharacters(value)
    .replace(/[A-Za-z]:[\\/][^\s"'`]+/g, "[localOnly:redacted-path]")
    .replace(/\b(?:api[_-]?key|token|secret|password)\s*[:=]\s*[^\s"'`]+/gi, "[redacted-secret]")
    .replace(/\bsk-[A-Za-z0-9_-]{16,}\b/g, "[redacted-token]")
    .trim(), 4000);
}

function stripUnsafeControlCharacters(value: string): string {
  return value.replace(/\0/g, "").replace(/[\u0001-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, "");
}

function sanitizePath(value: string): string {
  return toDisplayPath(value).replace(/[A-Za-z]:\/Users\/[^/]+/g, "[localOnly:user]");
}

function safeLocalPath(value: string, root: string): string {
  if (!value) return "";
  const normalized = toDisplayPath(value);
  try {
    const resolved = path.resolve(value);
    if (isInside(root, resolved)) {
      return path.relative(root, resolved).replaceAll("\\", "/") || ".";
    }
    return `[localOnly:${path.basename(normalized)}]`;
  } catch {
    return "[localOnly:invalid-path]";
  }
}

function isInside(root: string, candidate: string): boolean {
  const relative = path.relative(path.resolve(root), path.resolve(candidate));
  return relative === "" || (!relative.startsWith("..") && !path.isAbsolute(relative));
}

function truncate(value: string, length: number): string {
  return value.length > length ? `${value.slice(0, length - 3)}...` : value;
}

function appendNote(details: string, note?: string): string {
  const safeNote = sanitizeText(note, "");
  return safeNote ? `${details}\nConfirmation note: ${safeNote}` : details;
}

function uniqueDiagnostics(items: EchoBridgeDiagnostic[]): EchoBridgeDiagnostic[] {
  const byCode = new Map<string, EchoBridgeDiagnostic>();
  for (const item of items) byCode.set(item.code, item);
  return Array.from(byCode.values());
}

function safeExecutorEnvironment(): NodeJS.ProcessEnv {
  const env: NodeJS.ProcessEnv = {};
  for (const key of ["PATH", "Path", "SystemRoot", "WINDIR", "TEMP", "TMP", "HOME", "USERPROFILE"]) {
    const value = process.env[key];
    if (value) env[key] = value;
  }
  env.ECHO_BRIDGE_LOCAL_ONLY = "true";
  env.ECHO_BRIDGE_ARGUMENT_MODE = EXECUTOR_ARGUMENT_MODE;
  return env;
}

function isTerminalJobStatus(status: EchoBridgeJobStatus): boolean {
  return ["completed", "failed", "canceled", "blocked"].includes(status);
}
