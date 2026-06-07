export type CommandMode = "shell" | "quarantine" | "notes";
export type CommandRunStatus = "queued" | "running" | "succeeded" | "failed" | "rejected" | "stopped";
export type ExportFormat = "json" | "markdown";
export type ScanMode = "quick" | "deep";
export type ScanStatus = "running" | "passed" | "warning" | "failed";
export type JarArtifactStatus = "missing" | "built" | "current" | "stale";
export type JarTargetStatus = "missing" | "current" | "stale" | "duplicate" | "foreign";
export type ReadinessItemStatus = "done" | "missing" | "blocked" | "warning";
export type FeatureStatus = "implemented" | "partial" | "planned" | "deferred" | "blocked";
export type ModpackStatus = "ready" | "missing" | "blocked" | "running" | "succeeded" | "failed" | "stopped";
export type DistributionPackId = "echo-prime" | "ashfall" | "orbital" | "arcane-division";
export type DistributionStatus = "ready" | "missing" | "blocked" | "running" | "succeeded" | "failed" | "stopped";
export type EchoReportArtifactStatus = "loaded" | "missing" | "invalid";
export type EchoOperationalReportStatus = "ready" | "ready_with_warnings" | "degraded" | "blocked" | "unknown";
export type EchoReportCategoryKey =
  | "platform"
  | "workspace"
  | "graphs"
  | "packos"
  | "diagnostics"
  | "health"
  | "recovery"
  | "bridge"
  | "ai"
  | "assets"
  | "release"
  | "support"
  | "launcher";

export interface ProjectModule {
  modId: string;
  label: string;
  version: string;
  path: string;
}

export interface Project {
  slug: string;
  name: string;
  kind: string;
  status: string;
  currentMilestone: string;
  buildHealth: number;
  criticalIssues: number;
  polishTasks: number;
  lastScanLabel: string;
  nextRecommendedAction: string;
  accent: string;
  description: string;
  workspacePath: string;
  modules: ProjectModule[];
}

export interface RoadmapPhase {
  projectSlug: string;
  title: string;
  status: string;
  progress: number;
  summary: string;
}

export interface QaTrack {
  projectSlug: string;
  key: string;
  title: string;
  severity: "critical" | "high" | "medium" | "low";
  status: string;
  summary: string;
  checks: string[];
}

export interface QaFinding {
  id?: number;
  track: string;
  title: string;
  severity: QaTrack["severity"];
  status: string;
  detail: string;
  path?: string;
  line?: number;
  code?: string;
  source?: string;
  metadata?: Record<string, unknown>;
}

export interface ScanReport {
  id: number;
  projectSlug: string;
  createdAt: string;
  mode: ScanMode;
  status: ScanStatus;
  startedAt: string;
  finishedAt?: string;
  durationMs: number;
  source: Record<string, unknown>;
  rawOutput: string;
  summary: {
    status: string;
    buildHealth: number;
    criticalIssues: number;
    polishTasks: number;
    inventory: Record<string, number>;
    readinessScore?: number;
  };
  findings: QaFinding[];
}

export interface PromptTemplate {
  projectSlug: string;
  id: string;
  category: string;
  title: string;
  description: string;
  body: string;
}

export interface TerminalPlannerGroup {
  projectSlug: string;
  group: string;
  pages: string[];
}

export interface ReleaseAction {
  projectSlug: string;
  commandId: string;
  label: string;
  description: string;
  mode: CommandMode;
  risk: "low" | "medium" | "high";
  executable: string;
  args: string[];
}

export interface FeatureSource {
  label: string;
  path: string;
  section: string;
}

export interface FeatureEvidence {
  kind: string;
  label: string;
  path?: string;
  detail?: string;
}

export interface FeatureRecord {
  projectSlug: string;
  id: string;
  title: string;
  category: string;
  status: FeatureStatus;
  playerPromise: string;
  loreContext: string;
  implementationSummary: string;
  nextAction: string;
  sources: FeatureSource[];
  evidence: FeatureEvidence[];
  order: number;
}

export interface FeatureCatalogSummary {
  total: number;
  statusCounts: Record<FeatureStatus, number>;
  categoryCounts: Record<string, number>;
}

export interface FeatureCatalogResponse {
  projectSlug: string;
  generatedAt: string;
  features: FeatureRecord[];
  summary: FeatureCatalogSummary;
}

export interface CommandRun {
  id: string;
  projectSlug: string;
  commandId: string;
  status: CommandRunStatus;
  risk: ReleaseAction["risk"];
  command: string[];
  startedAt: string;
  finishedAt?: string;
  exitCode?: number;
  pid?: number;
  durationMs?: number;
  metadata?: Record<string, unknown>;
  output: string;
}

export interface JarArtifact {
  moduleId: string;
  label: string;
  version: string;
  expectedFileName: string;
  sourcePath: string;
  targetPath?: string;
  exists: boolean;
  current: boolean;
  status: JarArtifactStatus;
  size?: number;
  modifiedAt?: string;
  checksum?: string;
}

export interface JarTargetEntry {
  moduleId?: string;
  version?: string;
  expectedFileName?: string;
  fileName: string;
  path: string;
  status: JarTargetStatus;
  size: number;
  modifiedAt: string;
  checksum?: string;
}

export interface JarManifest {
  projectSlug: string;
  generatedAt: string;
  buildRoot: string;
  targetDir: string;
  targetConfigured: boolean;
  targetExists: boolean;
  quarantineDir: string;
  artifacts: JarArtifact[];
  targetEntries: JarTargetEntry[];
  blockers: string[];
  summary: {
    expected: number;
    built: number;
    missing: number;
    current: number;
    stale: number;
    duplicate: number;
    foreign: number;
  };
}

export interface JarPipelineRequest {
  confirmed?: boolean;
}

export interface JarPipelineResult {
  manifest: JarManifest;
  run: CommandRun;
  scanReport?: ScanReport;
  moved: string[];
  copied: string[];
  verified: string[];
}

export interface ModpackTarget {
  projectSlug: string;
  projectName: string;
  buildCommandId: string;
  status: ModpackStatus;
  manifest: JarManifest;
  blockers: string[];
}

export interface ModpackInventory {
  generatedAt: string;
  targetDir: string;
  targetConfigured: boolean;
  targetExists: boolean;
  status: ModpackStatus;
  blockers: string[];
  targets: ModpackTarget[];
  summary: {
    projects: number;
    expected: number;
    built: number;
    missing: number;
    current: number;
    stale: number;
    duplicate: number;
  };
  latestRun?: ModpackPipelineRun | null;
}

export interface ModpackPipelineStep {
  id: string;
  label: string;
  status: ModpackStatus;
  detail: string;
  command?: string[];
  startedAt?: string;
  finishedAt?: string;
  output?: string;
}

export interface ModpackPipelineRun {
  id: string;
  status: ModpackStatus;
  startedAt: string;
  finishedAt?: string;
  durationMs?: number;
  targetSlugs: string[];
  steps: ModpackPipelineStep[];
  output: string;
}

export interface ModpackPipelineRequest {
  confirmed?: boolean;
}

export interface ModpackPipelineResult {
  run: ModpackPipelineRun;
  summary: ModpackInventory;
  scanReports: ScanReport[];
  moved: string[];
  copied: string[];
  verified: string[];
}

export interface DistributionReleaseStatus {
  repo: string;
  repoUrl: string;
  status: DistributionStatus;
  latestVersion?: string;
  releaseUrl?: string;
  publishedAt?: string;
  assets: string[];
  blockers: string[];
}

export interface DistributionPack {
  id: DistributionPackId;
  name: string;
  repo: string;
  repoUrl: string;
  localPath: string;
  channel: string;
  includeAllModules: boolean;
  requiredModuleIds: string[];
  optionalModuleIds: string[];
  selectedModuleCount: number;
  latestRelease: DistributionReleaseStatus;
  blockers: string[];
}

export interface DistributionSummary {
  generatedAt: string;
  echoModuleRelease: DistributionReleaseStatus;
  launcherRelease: {
    repo: string;
    repoUrl: string;
    windows: DistributionReleaseStatus;
    linux: DistributionReleaseStatus;
  };
  packs: DistributionPack[];
  latestRun?: CommandRun | null;
}

export interface PackExportRequest {
  packId: DistributionPackId;
  targetModsDir: string;
  confirmed?: boolean;
  buildFirst?: boolean;
  includeOptionalModules?: boolean;
}

export interface PackExportResult {
  run: CommandRun;
  summary: DistributionSummary;
  pack: DistributionPack;
  moved: string[];
  copied: string[];
  verified: string[];
}

export interface EchoReportArtifact {
  key: string;
  fileName: string;
  reportPath: string;
  category?: EchoReportCategoryKey;
  label?: string;
  status: EchoReportArtifactStatus;
  schema?: string;
  reportKind?: string;
  reportStatus?: string;
  generatedDate?: string;
  issueCount: number;
  error?: string;
}

export interface EchoReportCounts {
  total: number;
  loaded: number;
  missing: number;
  invalid: number;
  issues: number;
  warnings: number;
  errors: number;
  notices: number;
  fatals: number;
  blocking: number;
}

export interface EchoReportCategory {
  key: EchoReportCategoryKey;
  label: string;
  status: EchoOperationalReportStatus;
  reportKeys: string[];
  loaded: number;
  missing: number;
  invalid: number;
  issueCount: number;
}

export interface EchoReportPanelMetric {
  label: string;
  value: string | number;
  tone?: "green" | "amber" | "red";
  detail?: string;
}

export interface EchoReportPanel {
  id: string;
  title: string;
  category: EchoReportCategoryKey;
  status: EchoOperationalReportStatus;
  primary: string;
  secondary: string;
  detail: string;
  reportKeys: string[];
  metrics: EchoReportPanelMetric[];
}

export interface EchoReportTopIssue {
  reportKey: string;
  reportPath: string;
  code: string;
  severity: string;
  title: string;
  summary: string;
  category?: string;
  source?: string;
  moduleId?: string;
  packId?: string;
  featureId?: string;
  blocking: boolean;
  relatedPaths: string[];
}

export interface EchoReportSafeCommand {
  id: string;
  label: string;
  command: string;
  risk: "none" | "low";
  confirmationRequired: boolean;
  executesInUi: boolean;
  notes: string;
}

export interface EchoReportDataPreviewEntry {
  key: string;
  kind: "array" | "object" | "string" | "number" | "boolean" | "null" | "unknown";
  value?: string | number | boolean | null;
  count?: number;
  keys?: string[];
}

export interface EchoReportDrilldown {
  generatedAt: string;
  artifact: EchoReportArtifact;
  status: EchoOperationalReportStatus;
  category: EchoReportCategoryKey;
  summary: Record<string, string | number | boolean | null>;
  issueCounts: Record<string, number>;
  issues: EchoReportTopIssue[];
  dataPreview: EchoReportDataPreviewEntry[];
  safeCommands: EchoReportSafeCommand[];
  relatedDocs: string[];
}

export type EchoReportJobStatus = "idle" | "running" | "blocked";

export interface EchoReportJobDefinition {
  id: string;
  label: string;
  description: string;
  category: EchoReportCategoryKey | "validation";
  command: string[];
  reportKeys: string[];
  risk: "none" | "low";
  destructive: false;
  launchesMinecraft: false;
  modifiesGameFiles: false;
  writesReports: boolean;
}

export interface EchoReportJobRequest {
  commandId?: string;
}

export interface EchoReportJobSummary {
  generatedAt: string;
  status: EchoReportJobStatus;
  definitions: EchoReportJobDefinition[];
  panelActions: Record<string, string[]>;
  runs: CommandRun[];
  runningRun?: CommandRun | null;
}

export interface EchoReportJobRunSnapshot extends Omit<CommandRun, "output"> {
  outputPreview: string;
  outputLineCount: number;
  outputTruncated: boolean;
}

export interface EchoReportJobSafety {
  acceptedCommandIdOnly: true;
  rawCommandsAccepted: false;
  destructive: false;
  launchesMinecraft: false;
  modifiesGameFiles: false;
  downloadsRemoteModules: false;
  executesRepairs: false;
}

export interface EchoReportJobDetail {
  generatedAt: string;
  run: EchoReportJobRunSnapshot;
  definition: EchoReportJobDefinition;
  command: string[];
  reportKeys: string[];
  failureSummary: string;
  safety: EchoReportJobSafety;
}

export interface EchoOperationalReportSummary {
  packId: string;
  packOsStatus: string;
  packReadinessStatus: string;
  lockfileStatus: string;
  installStateStatus: string;
  repairPlanStatus: string;
  healthStatus: string;
  recoveryMode: string;
  recoveryPlanStatus: string;
  safeForLauncher: boolean;
  diagnosticsCount: number;
  blockingDiagnostics: number;
  warningDiagnostics: number;
  noticeDiagnostics: number;
  recoveryTriggerCount: number;
  recoveryActionCount: number;
  missingReportCount: number;
  invalidReportCount: number;
}

export interface EchoOperationalReports {
  generatedAt: string;
  reportRoot: string;
  status: EchoOperationalReportStatus;
  artifacts: EchoReportArtifact[];
  summary: EchoOperationalReportSummary;
  reportCounts: EchoReportCounts;
  categories: EchoReportCategory[];
  panels: EchoReportPanel[];
  topIssues: EchoReportTopIssue[];
  recommendations: string[];
  missingReports: string[];
  invalidReports: string[];
}

export type EchoBridgeJobStatus =
  | "queued"
  | "executor_ready"
  | "starting"
  | "running"
  | "streaming"
  | "needs_confirmation"
  | "completed"
  | "failed"
  | "canceled"
  | "blocked"
  | "not_configured";

export type EchoBridgeExecutorStatus = "configured" | "not_configured" | "blocked" | "offline";

export interface EchoBridgeExecutorConfig {
  enabled: boolean;
  localOnly: boolean;
  argumentMode: "echo_bridge_sidecar_v1";
  executablePath: string;
  executableLabel: string;
  sidecarEntrypoint: string;
  workspaceRoot: string;
  allowlistedExecutableNames: string[];
  configPath: string;
  redacted: boolean;
}

export interface EchoBridgeExecutorProbe {
  status: EchoBridgeExecutorStatus;
  configured: boolean;
  enabled: boolean;
  available: boolean;
  localOnly: boolean;
  executableLabel: string;
  executablePath: string;
  workspaceRoot: string;
  lastCheckedAt: string;
  reason: string;
  diagnostics: EchoBridgeDiagnostic[];
}

export interface EchoBridgeStreamCursors {
  stdout: string;
  stderr: string;
  diagnostics: string;
  state: string;
}

export interface EchoBridgeConfirmationHistoryEntry {
  requestId: string;
  jobId: string;
  action: string;
  status: "approved" | "rejected" | "blocked";
  confirmed: boolean;
  note?: string;
  createdAt: string;
}

export interface EchoBridgeDiagnostic {
  code: string;
  severity: string;
  summary: string;
  source?: string;
  blocking: boolean;
}

export interface EchoBridgeLogChunk {
  id: string;
  jobId: string;
  stream: "stdout" | "stderr" | "diagnostics" | "state";
  text: string;
  cursor: string;
  createdAt: string;
  redacted: boolean;
}

export interface EchoBridgeSafeActionRequest {
  id: string;
  sessionId: string;
  jobId: string;
  status: "pending_confirmation" | "approved" | "rejected" | "expired" | "blocked";
  kind: string;
  risk: "informational" | "low" | "medium" | "high" | "destructive" | "privileged";
  requiresConfirmation: boolean;
  summary: string;
  developerDetails: string;
  relatedDiagnostics: string[];
  createdAt: string;
  expiresAt?: string;
}

export interface EchoBridgeJob {
  id: string;
  sessionId: string;
  status: EchoBridgeJobStatus;
  taskId: string;
  taskTitle: string;
  moduleId: string;
  agentLane: string;
  buildStatus: string;
  validationStatus: string;
  summary: string;
  createdAt: string;
  updatedAt: string;
  diagnostics: EchoBridgeDiagnostic[];
  safeActionRequestIds: string[];
  recentLogChunks: EchoBridgeLogChunk[];
  streamCursors: EchoBridgeStreamCursors;
  executorPid?: number;
  executorStartedAt?: string;
  executorFinishedAt?: string;
  executorExitCode?: number | null;
  nextPrompt?: string;
}

export interface EchoBridgeSession {
  id: string;
  displayName: string;
  workspace: string;
  status: "connected" | "idle" | "not_configured" | "offline";
  activeJobIds: string[];
  createdAt: string;
  updatedAt: string;
  connectedPcBridge: string;
  protectedFiles: string[];
  safeEditZones: string[];
}

export interface EchoBridgeState {
  generatedAt: string;
  localOnly: boolean;
  workspace: string;
  executorStatus: EchoBridgeExecutorStatus;
  executorReason: string;
  executorConfig: EchoBridgeExecutorConfig;
  executorProbe: EchoBridgeExecutorProbe;
  sessions: EchoBridgeSession[];
  jobs: EchoBridgeJob[];
  safeActionRequests: EchoBridgeSafeActionRequest[];
  confirmationHistory: EchoBridgeConfirmationHistoryEntry[];
  diagnostics: EchoBridgeDiagnostic[];
  nextGeneratedPrompt: string;
  protectedFiles: string[];
  safeEditZones: string[];
}

export interface EchoBridgeCreateSessionRequest {
  displayName?: string;
  workspace?: string;
}

export interface EchoBridgePromptRequest {
  promptText: string;
  taskId?: string;
  taskTitle?: string;
  moduleId?: string;
  agentLane?: string;
  startCodex?: boolean;
}

export interface EchoBridgeConfirmationRequest {
  confirmed: boolean;
  note?: string;
}

export interface ReadinessChecklistItem {
  id: string;
  category: "Scan" | "Jars" | "Settings";
  label: string;
  status: ReadinessItemStatus;
  detail: string;
  actionLabel: string;
  targetView: string;
  relatedFindingCodes: string[];
  commandId?: string;
}

export interface ReadinessReport {
  projectSlug: string;
  generatedAt: string;
  score: number;
  latestQuickScanId?: number;
  latestDeepScanId?: number;
  nextAction: ReadinessChecklistItem | null;
  counts: {
    done: number;
    missing: number;
    blocked: number;
    warning: number;
    total: number;
  };
  items: ReadinessChecklistItem[];
}

export interface AppSettings {
  echoRoot: string;
  modpackModsDir: string;
  pythonExecutable: string;
  runtimeLogMaxAgeMinutes: number;
  defaultScanMode: ScanMode;
}

export interface ProjectDetail {
  project: Project;
  roadmap: RoadmapPhase[];
  qaTracks: QaTrack[];
  prompts: PromptTemplate[];
  terminalPlanner: TerminalPlannerGroup[];
  releaseActions: ReleaseAction[];
  latestReport: ScanReport | null;
  recentRuns?: CommandRun[];
}
