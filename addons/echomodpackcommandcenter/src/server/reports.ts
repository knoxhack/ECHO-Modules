import fs from "node:fs";
import path from "node:path";
import type {
  EchoOperationalReports,
  EchoOperationalReportStatus,
  EchoReportDataPreviewEntry,
  EchoReportDrilldown,
  EchoReportArtifact,
  EchoReportArtifactStatus,
  EchoReportCategory,
  EchoReportCategoryKey,
  EchoReportCounts,
  EchoReportPanel,
  EchoReportPanelMetric,
  EchoReportSafeCommand,
  EchoReportTopIssue
} from "../shared/types.js";
import { ECHO_ROOT } from "./paths.js";

const REPORT_ROOT = "reports/echo";

const CATEGORY_LABELS: Record<EchoReportCategoryKey, string> = {
  platform: "Platform",
  workspace: "Workspace",
  graphs: "Graphs",
  packos: "PackOS",
  diagnostics: "Diagnostics",
  health: "Health",
  recovery: "Recovery",
  bridge: "Bridge",
  ai: "AI Tasks",
  assets: "Assets",
  release: "Release",
  support: "Support",
  launcher: "Launcher / Command Center"
};

const REPORT_DEFINITIONS = [
  { key: "platformVerification", fileName: "platform-verification.json", category: "platform", label: "Platform Verification" },
  { key: "workspaceScan", fileName: "workspace-scan.json", category: "workspace", label: "Workspace Scan" },
  { key: "scannedModules", fileName: "scanned-modules.json", category: "workspace", label: "Scanned Modules" },
  { key: "moduleGraph", fileName: "module-graph.json", category: "graphs", label: "Module Graph" },
  { key: "dependencyGraph", fileName: "dependency-graph.json", category: "graphs", label: "Dependency Graph" },
  { key: "roleGraph", fileName: "role-graph.json", category: "graphs", label: "Role Graph" },
  { key: "featureGraph", fileName: "feature-graph.json", category: "graphs", label: "Feature Graph" },
  { key: "diagnostics", fileName: "diagnostics.json", category: "diagnostics", label: "Diagnostics" },
  { key: "packReadiness", fileName: "pack-readiness.json", category: "packos", label: "Pack Readiness" },
  { key: "packProfile", fileName: "pack-profile.json", category: "packos", label: "Pack Profile" },
  { key: "lockfile", fileName: "lockfile.json", category: "packos", label: "Lockfile" },
  { key: "installState", fileName: "install-state.json", category: "packos", label: "Install State" },
  { key: "lockfileStatus", fileName: "lockfile-status.json", category: "packos", label: "Lockfile Status" },
  { key: "repairPlan", fileName: "repair-plan.json", category: "packos", label: "Repair Plan" },
  { key: "packDoctor", fileName: "pack-doctor.json", category: "packos", label: "Pack Doctor" },
  { key: "health", fileName: "health.json", category: "health", label: "Health" },
  { key: "runtimeHealth", fileName: "runtime-health.json", category: "health", label: "Runtime Health" },
  { key: "degradedFeatures", fileName: "degraded-features.json", category: "health", label: "Degraded Features" },
  { key: "recoveryState", fileName: "recovery-state.json", category: "recovery", label: "Recovery State" },
  { key: "recoveryPlan", fileName: "recovery-plan.json", category: "recovery", label: "Recovery Plan" },
  { key: "bridgeSessions", fileName: "bridge-sessions.json", category: "bridge", label: "Bridge Sessions" },
  { key: "codexRunReport", fileName: "codex-run-report.json", category: "bridge", label: "Codex Run Report" },
  { key: "aiTasks", fileName: "ai-tasks.json", category: "ai", label: "AI Tasks" },
  { key: "releaseReadiness", fileName: "release-readiness.json", category: "release", label: "Release Readiness" },
  { key: "supportBundle", fileName: "support-bundle.json", category: "support", label: "Support Bundle" },
  { key: "missingAssets", fileName: "missing-assets.json", category: "assets", label: "Missing Assets" },
  { key: "officialPackAssetGates", fileName: "official-pack-asset-gates.json", category: "assets", label: "Official Pack Asset Gates" },
  { key: "launcherStatus", fileName: "launcher-status.json", category: "launcher", label: "Launcher Status" },
  { key: "commandCenterCatalog", fileName: "command-center-catalog.json", category: "launcher", label: "Command Center Catalog" }
] as const;

type ReportDefinition = (typeof REPORT_DEFINITIONS)[number];
type ReportKey = ReportDefinition["key"];

const REPORT_REFRESH_COMMANDS: Partial<Record<ReportKey, string>> = {
  platformVerification: ".\\gradlew.bat verifyEchoNativePlatform -PechoAddonSet=beta",
  workspaceScan: ".\\gradlew.bat scanEchoWorkspace -PechoAddonSet=beta",
  scannedModules: ".\\gradlew.bat scanEchoWorkspace -PechoAddonSet=beta",
  moduleGraph: ".\\gradlew.bat generateEchoModuleGraph -PechoAddonSet=beta",
  dependencyGraph: ".\\gradlew.bat generateEchoModuleGraph -PechoAddonSet=beta",
  roleGraph: ".\\gradlew.bat generateEchoModuleGraph -PechoAddonSet=beta",
  featureGraph: ".\\gradlew.bat generateEchoFeatureGraph -PechoAddonSet=beta",
  diagnostics: ".\\gradlew.bat generateEchoDiagnostics -PechoAddonSet=beta",
  packReadiness: ".\\gradlew.bat generateEchoPackReadiness -PechoPack=ashfall -PechoAddonSet=beta",
  packProfile: ".\\gradlew.bat loadEchoPackProfile -PechoPack=ashfall",
  lockfile: ".\\gradlew.bat generateEchoLockfile -PechoPack=ashfall -PechoAddonSet=beta",
  installState: ".\\gradlew.bat scanEchoInstallState -PechoPack=ashfall -PechoAddonSet=beta",
  lockfileStatus: ".\\gradlew.bat verifyEchoLockfile -PechoPack=ashfall -PechoAddonSet=beta",
  repairPlan: ".\\gradlew.bat generateEchoRepairPlan -PechoPack=ashfall -PechoAddonSet=beta",
  packDoctor: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  health: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  runtimeHealth: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  degradedFeatures: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  recoveryState: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  recoveryPlan: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  bridgeSessions: ".\\gradlew.bat generateEchoBridgeReports -PechoAddonSet=beta",
  codexRunReport: ".\\gradlew.bat generateEchoBridgeReports -PechoAddonSet=beta",
  aiTasks: ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta",
  releaseReadiness: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
  supportBundle: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
  missingAssets: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
  officialPackAssetGates: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
  launcherStatus: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
  commandCenterCatalog: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta"
};

const REPORT_DOCS: Partial<Record<ReportKey, string[]>> = {
  platformVerification: ["docs/echo/platform/ECHO_PLATFORM_VERIFICATION_REPORT.md"],
  workspaceScan: ["docs/echo/tooling/ECHO_WORKSPACE_SCANNER.md"],
  scannedModules: ["docs/echo/tooling/ECHO_WORKSPACE_SCANNER.md"],
  moduleGraph: ["docs/echo/validation/ECHO_MODULE_GRAPH.md"],
  dependencyGraph: ["docs/echo/validation/ECHO_MODULE_GRAPH.md"],
  roleGraph: ["docs/echo/validation/ECHO_MODULE_GRAPH.md"],
  featureGraph: ["docs/echo/validation/ECHO_FEATURE_GRAPH.md"],
  diagnostics: ["docs/echo/validation/ECHO_DIAGNOSTICS_REPORT.md"],
  packReadiness: ["docs/echo/packos/ECHO_PACK_READINESS.md"],
  packProfile: ["docs/echo/packos/ECHO_PACK_PROFILES.md"],
  lockfile: ["docs/echo/packos/ECHO_LOCKFILE.md"],
  installState: ["docs/echo/packos/ECHO_INSTALL_STATE.md"],
  lockfileStatus: ["docs/echo/packos/ECHO_LOCKFILE_VERIFICATION.md"],
  repairPlan: ["docs/echo/packos/ECHO_REPAIR_PLANS.md"],
  packDoctor: ["docs/echo/packos/ECHO_PACK_DOCTOR.md"],
  health: ["docs/echo/runtime/ECHO_HEALTHCORE.md"],
  runtimeHealth: ["docs/echo/runtime/ECHO_HEALTHCORE.md"],
  degradedFeatures: ["docs/echo/runtime/ECHO_HEALTHCORE.md"],
  recoveryState: ["docs/echo/runtime/ECHO_RECOVERYCORE.md"],
  recoveryPlan: ["docs/echo/runtime/ECHO_RECOVERYCORE.md"],
  bridgeSessions: ["docs/echo/bridge/ECHO_BRIDGECORE_CODEX.md", "docs/echo/cyberdex/ECHO_CYBERDEX_AUTOMATION.md"],
  codexRunReport: ["docs/echo/bridge/ECHO_BRIDGECORE_CODEX.md", "docs/echo/cyberdex/ECHO_CYBERDEX_AUTOMATION.md"],
  aiTasks: ["docs/echo/ai/ECHO_AGENTCORE_TASKS.md"],
  releaseReadiness: ["docs/echo/release/ECHO_RELEASE_READINESS_REPORT.md"],
  supportBundle: ["docs/echo/support/ECHO_SUPPORT_BUNDLES.md"],
  missingAssets: ["docs/echo/assets/ECHO_MISSING_ASSET_REPORTS.md"],
  officialPackAssetGates: ["docs/echo/assets/ECHO_MISSING_ASSET_REPORTS.md"],
  launcherStatus: ["docs/echo/tooling/ECHO_LAUNCHER_PACKOS_REPORTS.md"],
  commandCenterCatalog: ["docs/echo/tooling/ECHO_COMMAND_CENTER_PACKOS_REPORTS.md"]
};

interface LoadedReport {
  artifact: EchoReportArtifact;
  payload: Record<string, unknown> | null;
}

export interface EchoOperationalReportLoaderOptions {
  now?: Date;
  rootDir?: string;
}

interface AggregatedCounts {
  warnings: number;
  errors: number;
  notices: number;
  fatals: number;
  blocking: number;
}

export function loadEchoOperationalReports(options: Date | EchoOperationalReportLoaderOptions = {}): EchoOperationalReports {
  const resolved = resolveOptions(options);
  const loaded = REPORT_DEFINITIONS.map((definition) => loadReport(definition, resolved.rootDir));
  const byKey = Object.fromEntries(loaded.map((report) => [report.artifact.key, report])) as Record<ReportKey, LoadedReport>;
  const missingReports = loaded.filter((report) => report.artifact.status === "missing").map((report) => report.artifact.reportPath);
  const invalidReports = loaded.filter((report) => report.artifact.status === "invalid").map((report) => report.artifact.reportPath);

  const packDoctor = objectAt(byKey.packDoctor.payload, "packDoctor");
  const diagnosticsSummary = objectAt(byKey.diagnostics.payload, "summary");
  const health = objectAt(byKey.health.payload, "health");
  const recoveryState = objectAt(byKey.recoveryState.payload, "recoveryState");
  const recoveryPlan = objectAt(byKey.recoveryPlan.payload, "recoveryPlan");
  const repairPlan = objectAt(byKey.repairPlan.payload, "repairPlan");
  const lockfileStatus = objectAt(byKey.lockfileStatus.payload, "lockfileStatus");
  const packReadinessStatus = selectedPackReadinessStatus(byKey.packReadiness.payload, stringValue(packDoctor.packId, "ashfall"));
  const aggregatedCounts = aggregatePayloadCounts(loaded);
  const status = operationalStatus(
    stringValue(packDoctor.status, "unknown"),
    missingReports.length,
    invalidReports.length,
    aggregatedCounts.blocking
  );
  const categories = categoriesFor(loaded);
  const reportCounts = reportCountsFor(loaded, aggregatedCounts);
  const summary = {
    packId: stringValue(packDoctor.packId, "ashfall"),
    packOsStatus: stringValue(packDoctor.status, "unknown"),
    packReadinessStatus,
    lockfileStatus: stringValue(lockfileStatus.status, stringValue(packDoctor.lockfileStatus, "unknown")),
    installStateStatus: stringValue(packDoctor.installStateStatus, "unknown"),
    repairPlanStatus: stringValue(repairPlan.status, stringValue(packDoctor.repairPlanStatus, "unknown")),
    healthStatus: stringValue(health.overallStatus, "unknown"),
    recoveryMode: stringValue(recoveryState.mode, "unknown"),
    recoveryPlanStatus: stringValue(recoveryPlan.status, "unknown"),
    safeForLauncher: booleanValue(packDoctor.safeForLauncher, false),
    diagnosticsCount: totalDiagnostics(diagnosticsSummary, loaded),
    blockingDiagnostics: numberValue(diagnosticsSummary.blockingCount, 0),
    warningDiagnostics: numberValue(diagnosticsSummary.warningCount, aggregatedCounts.warnings),
    noticeDiagnostics: numberValue(diagnosticsSummary.noticeCount, aggregatedCounts.notices),
    recoveryTriggerCount: arrayAt(recoveryState, "triggers").length,
    recoveryActionCount: arrayAt(recoveryPlan, "actions").length,
    missingReportCount: missingReports.length,
    invalidReportCount: invalidReports.length
  };
  const topIssues = topIssuesFor(loaded, resolved.rootDir);
  const panels = panelsFor(byKey, summary, reportCounts);
  const recommendations = recommendationsFor(status, missingReports, invalidReports, recoveryState, recoveryPlan, repairPlan);

  return {
    generatedAt: resolved.now.toISOString(),
    reportRoot: REPORT_ROOT,
    status,
    artifacts: loaded.map((report) => report.artifact),
    summary,
    reportCounts,
    categories,
    panels,
    topIssues,
    recommendations,
    missingReports,
    invalidReports
  };
}

export function loadEchoReportDrilldown(reportKey: string, options: Date | EchoOperationalReportLoaderOptions = {}): EchoReportDrilldown | null {
  const definition = REPORT_DEFINITIONS.find((candidate) => candidate.key === reportKey);
  if (!definition) return null;

  const resolved = resolveOptions(options);
  const loaded = loadReport(definition, resolved.rootDir);
  const issues = drilldownIssuesFor(loaded.artifact, loaded.payload, resolved.rootDir);
  return {
    generatedAt: resolved.now.toISOString(),
    artifact: loaded.artifact,
    status: statusForArtifact(loaded.artifact),
    category: definition.category,
    summary: summaryPreviewFor(loaded.artifact, loaded.payload, resolved.rootDir),
    issueCounts: issueCountsFor(issues),
    issues,
    dataPreview: dataPreviewFor(loaded.payload, resolved.rootDir),
    safeCommands: safeCommandsFor(definition),
    relatedDocs: relatedDocsFor(definition)
  };
}

function resolveOptions(options: Date | EchoOperationalReportLoaderOptions): Required<EchoOperationalReportLoaderOptions> {
  if (options instanceof Date) {
    return { now: options, rootDir: ECHO_ROOT };
  }
  return {
    now: options.now ?? new Date(),
    rootDir: path.resolve(options.rootDir ?? ECHO_ROOT)
  };
}

function loadReport(definition: ReportDefinition, rootDir: string): LoadedReport {
  const reportPath = `${REPORT_ROOT}/${definition.fileName}`;
  const absolutePath = path.join(rootDir, "reports", "echo", definition.fileName);
  if (!fs.existsSync(absolutePath)) {
    return {
      artifact: artifact(definition, reportPath, "missing", 0, "Report artifact is missing."),
      payload: null
    };
  }
  try {
    const payload = JSON.parse(fs.readFileSync(absolutePath, "utf-8")) as unknown;
    if (!isRecord(payload)) {
      return {
        artifact: artifact(definition, reportPath, "invalid", 0, "Report root must be a JSON object."),
        payload: null
      };
    }
    return {
      artifact: {
        ...artifact(definition, reportPath, "loaded", issueCount(payload)),
        schema: stringValue(payload.schema, ""),
        reportKind: definition.key,
        reportStatus: stringValue(payload.status, stringValue(payload.artifactKind, "available")),
        generatedDate: stringValue(payload.generatedAt, stringValue(payload.generatedDate, "unspecified"))
      },
      payload
    };
  } catch (error) {
    return {
      artifact: artifact(
        definition,
        reportPath,
        "invalid",
        0,
        error instanceof Error ? error.message : String(error)
      ),
      payload: null
    };
  }
}

function artifact(
  definition: ReportDefinition,
  reportPath: string,
  status: EchoReportArtifactStatus,
  issueCountValue: number,
  error?: string
): EchoReportArtifact {
  return {
    key: definition.key,
    fileName: definition.fileName,
    reportPath,
    category: definition.category,
    label: definition.label,
    status,
    issueCount: issueCountValue,
    ...(error ? { error } : {})
  };
}

function operationalStatus(packOsStatus: string, missingCount: number, invalidCount: number, blockingDiagnostics: number): EchoOperationalReportStatus {
  if (invalidCount > 0 || blockingDiagnostics > 0 || packOsStatus === "blocked") return "blocked";
  if (missingCount > 0) return "degraded";
  if (packOsStatus === "ready_with_warnings") return "ready_with_warnings";
  if (packOsStatus === "ready") return "ready";
  return packOsStatus ? "degraded" : "unknown";
}

function recommendationsFor(
  status: EchoOperationalReportStatus,
  missingReports: string[],
  invalidReports: string[],
  recoveryState: Record<string, unknown>,
  recoveryPlan: Record<string, unknown>,
  repairPlan: Record<string, unknown>
): string[] {
  const recommendations: string[] = [];
  if (missingReports.length || invalidReports.length) {
    recommendations.push("Run .\\gradlew echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta to refresh operational reports.");
  }
  if (stringValue(recoveryState.mode, "unknown") === "normal" && booleanValue(recoveryState.diagnosticModeRecommended, false)) {
    recommendations.push("Review diagnostic-mode recommendation before treating offline runtime health as launch-proven.");
  }
  if (stringValue(recoveryPlan.status, "unknown") === "diagnostic_recommended") {
    recommendations.push("Recovery plan is diagnostic-only; no recovery action has been executed.");
  }
  if (stringValue(repairPlan.status, "unknown") === "no_repair_needed") {
    recommendations.push("PackOS repair plan reports no repair needed for the current source-only scan.");
  }
  if (status === "ready" || status === "ready_with_warnings") {
    recommendations.push("Keep repair and recovery actions confirmation-gated in future Command Center flows.");
  }
  return recommendations;
}

function selectedPackReadinessStatus(payload: Record<string, unknown> | null, packId: string): string {
  const reportData = dataObject(payload);
  const entries = Array.isArray(reportData.packReadiness) ? reportData.packReadiness : [];
  const selected = entries.find((entry) => isRecord(entry) && (entry.id === packId || entry.selectedForPackDoctor === true));
  return isRecord(selected) ? stringValue(selected.status, "unknown") : "unknown";
}

function totalDiagnostics(summary: Record<string, unknown>, loaded: LoadedReport[]): number {
  const fromSummary = numberValue(summary.blockingCount, 0) + numberValue(summary.warningCount, 0) + numberValue(summary.noticeCount, 0);
  if (fromSummary > 0) return fromSummary;
  return loaded.reduce((total, report) => total + report.artifact.issueCount, 0);
}

function aggregatePayloadCounts(loaded: LoadedReport[]): AggregatedCounts {
  return loaded.reduce<AggregatedCounts>(
    (counts, report) => {
      const summary = objectAt(report.payload, "summary");
      counts.warnings += numberValue(summary.warningCount, numberValue(summary.warnings, 0));
      counts.errors += numberValue(summary.errorCount, numberValue(summary.errors, 0));
      counts.notices += numberValue(summary.noticeCount, numberValue(summary.notices, 0));
      counts.fatals += numberValue(summary.fatalCount, numberValue(summary.fatals, 0));
      counts.blocking += numberValue(summary.blockingCount, 0);
      return counts;
    },
    { warnings: 0, errors: 0, notices: 0, fatals: 0, blocking: 0 }
  );
}

function reportCountsFor(loaded: LoadedReport[], aggregatedCounts: AggregatedCounts): EchoReportCounts {
  return {
    total: loaded.length,
    loaded: loaded.filter((report) => report.artifact.status === "loaded").length,
    missing: loaded.filter((report) => report.artifact.status === "missing").length,
    invalid: loaded.filter((report) => report.artifact.status === "invalid").length,
    issues: loaded.reduce((total, report) => total + report.artifact.issueCount, 0),
    warnings: aggregatedCounts.warnings,
    errors: aggregatedCounts.errors,
    notices: aggregatedCounts.notices,
    fatals: aggregatedCounts.fatals,
    blocking: aggregatedCounts.blocking
  };
}

function categoriesFor(loaded: LoadedReport[]): EchoReportCategory[] {
  return (Object.keys(CATEGORY_LABELS) as EchoReportCategoryKey[]).map((key) => {
    const reports = loaded.filter((report) => report.artifact.category === key);
    return {
      key,
      label: CATEGORY_LABELS[key],
      status: worstStatus(reports.map((report) => statusForArtifact(report.artifact))),
      reportKeys: reports.map((report) => report.artifact.key),
      loaded: reports.filter((report) => report.artifact.status === "loaded").length,
      missing: reports.filter((report) => report.artifact.status === "missing").length,
      invalid: reports.filter((report) => report.artifact.status === "invalid").length,
      issueCount: reports.reduce((total, report) => total + report.artifact.issueCount, 0)
    };
  });
}

function panelsFor(
  byKey: Record<ReportKey, LoadedReport>,
  summary: EchoOperationalReports["summary"],
  counts: EchoReportCounts
): EchoReportPanel[] {
  const workspaceScan = objectAt(byKey.workspaceScan.payload, "workspaceScan");
  const scannedModules = objectAt(byKey.scannedModules.payload, "scannedModules");
  const commandCenterCatalog = objectAt(byKey.commandCenterCatalog.payload, "catalogSummary");
  const supportBundle = objectAt(byKey.supportBundle.payload, "supportBundle");
  const assetSummary = objectAt(byKey.missingAssets.payload, "assetSummary");
  const bridgeSessions = objectAt(byKey.bridgeSessions.payload, "bridgeSessions");
  const codexRunReport = objectAt(byKey.codexRunReport.payload, "codexRunReport");

  return [
    panel("platform", "Platform Status", "platform", ["platformVerification"], byKey, "Platform", reportStatus(byKey.platformVerification), [
      metric("issues", byKey.platformVerification.artifact.issueCount, byKey.platformVerification.artifact.issueCount ? "amber" : "green"),
      metric("status", reportStatus(byKey.platformVerification))
    ]),
    panel("workspace", "Workspace Scan", "workspace", ["workspaceScan", "scannedModules"], byKey, "Workspace", `${numberValue(workspaceScan.selectedModules, numberValue(scannedModules.moduleCount, 0))} module(s)`, [
      metric("selected", numberValue(workspaceScan.selectedModules, 0), "green"),
      metric("configured", numberValue(workspaceScan.uniqueConfiguredAddons, 0)),
      metric("unlisted", arrayAt(workspaceScan, "unlistedAddonDirectories").length, arrayAt(workspaceScan, "unlistedAddonDirectories").length ? "amber" : "green")
    ]),
    panel("graphs", "Module + Feature Graphs", "graphs", ["moduleGraph", "dependencyGraph", "roleGraph", "featureGraph"], byKey, "Graphs", `${categoryIssueCount(byKey, ["moduleGraph", "dependencyGraph", "roleGraph", "featureGraph"])} issue(s)`, [
      metric("module", reportStatus(byKey.moduleGraph)),
      metric("dependency", reportStatus(byKey.dependencyGraph)),
      metric("feature", reportStatus(byKey.featureGraph))
    ]),
    panel("packos", "PackOS", "packos", ["packProfile", "packReadiness", "lockfile", "installState", "lockfileStatus", "repairPlan", "packDoctor"], byKey, summary.packOsStatus, `readiness ${summary.packReadinessStatus}`, [
      metric("lockfile", summary.lockfileStatus, summary.lockfileStatus.includes("valid") ? "green" : "amber"),
      metric("install", summary.installStateStatus, summary.installStateStatus === "not_configured" ? "amber" : "green"),
      metric("repair", summary.repairPlanStatus, summary.repairPlanStatus === "no_repair_needed" ? "green" : "amber")
    ]),
    panel("diagnostics", "Diagnostics", "diagnostics", ["diagnostics"], byKey, `${summary.diagnosticsCount} finding(s)`, `${summary.blockingDiagnostics} blocking`, [
      metric("blocking", summary.blockingDiagnostics, summary.blockingDiagnostics ? "red" : "green"),
      metric("warnings", summary.warningDiagnostics, summary.warningDiagnostics ? "amber" : "green"),
      metric("notices", summary.noticeDiagnostics)
    ]),
    panel("health", "Runtime Health", "health", ["health", "runtimeHealth", "degradedFeatures"], byKey, summary.healthStatus, summary.installStateStatus, [
      metric("health", reportStatus(byKey.health)),
      metric("runtime", reportStatus(byKey.runtimeHealth)),
      metric("degraded", byKey.degradedFeatures.artifact.issueCount, byKey.degradedFeatures.artifact.issueCount ? "amber" : "green")
    ]),
    panel("recovery", "Recovery", "recovery", ["recoveryState", "recoveryPlan"], byKey, summary.recoveryMode, summary.recoveryPlanStatus, [
      metric("triggers", summary.recoveryTriggerCount, summary.recoveryTriggerCount ? "amber" : "green"),
      metric("actions", summary.recoveryActionCount, summary.recoveryActionCount ? "amber" : "green")
    ]),
    panel("bridge", "Bridge Sessions", "bridge", ["bridgeSessions", "codexRunReport"], byKey, stringValue(bridgeSessions.executorStatus, "unknown"), stringValue(codexRunReport.status, reportStatus(byKey.codexRunReport)), [
      metric("sessions", numberValue(bridgeSessions.sessionCount, 0)),
      metric("jobs", numberValue(bridgeSessions.jobCount, 0)),
      metric("safe actions", numberValue(bridgeSessions.pendingConfirmationCount, 0), numberValue(bridgeSessions.pendingConfirmationCount, 0) ? "amber" : "green")
    ]),
    panel("ai", "AI Tasks", "ai", ["aiTasks"], byKey, `${arrayAt(dataObject(byKey.aiTasks.payload), "tasks").length} task(s)`, reportStatus(byKey.aiTasks), [
      metric("tasks", arrayAt(dataObject(byKey.aiTasks.payload), "tasks").length)
    ]),
    panel("assets", "Assets", "assets", ["missingAssets", "officialPackAssetGates"], byKey, `${arrayAt(dataObject(byKey.missingAssets.payload), "findings").length} finding(s)`, reportStatus(byKey.missingAssets), [
      metric("missing", arrayAt(dataObject(byKey.missingAssets.payload), "findings").length, arrayAt(dataObject(byKey.missingAssets.payload), "findings").length ? "amber" : "green"),
      metric("declared", numberValue(assetSummary.declaredAssetCount, numberValue(assetSummary.assetCount, 0)))
    ]),
    panel("release", "Release Readiness", "release", ["releaseReadiness"], byKey, reportStatus(byKey.releaseReadiness), `${counts.blocking} blocking`, [
      metric("warnings", counts.warnings, counts.warnings ? "amber" : "green"),
      metric("errors", counts.errors, counts.errors ? "red" : "green")
    ]),
    panel("support", "Support Bundles", "support", ["supportBundle"], byKey, reportStatus(byKey.supportBundle), booleanValue(supportBundle.secretsRedacted, true) ? "redacted" : "review", [
      metric("issues", byKey.supportBundle.artifact.issueCount, byKey.supportBundle.artifact.issueCount ? "amber" : "green")
    ]),
    panel("launcher", "Launcher / Command Center", "launcher", ["launcherStatus", "commandCenterCatalog"], byKey, reportStatus(byKey.launcherStatus), `${numberValue(commandCenterCatalog.moduleCount, 0)} catalog module(s)`, [
      metric("launcher", reportStatus(byKey.launcherStatus)),
      metric("catalog", reportStatus(byKey.commandCenterCatalog)),
      metric("modules", numberValue(commandCenterCatalog.moduleCount, 0))
    ])
  ];
}

function panel(
  id: string,
  title: string,
  category: EchoReportCategoryKey,
  reportKeys: ReportKey[],
  byKey: Record<ReportKey, LoadedReport>,
  primary: string,
  secondary: string,
  metrics: EchoReportPanelMetric[]
): EchoReportPanel {
  return {
    id,
    title,
    category,
    status: worstStatus(reportKeys.map((key) => statusForArtifact(byKey[key].artifact))),
    primary,
    secondary,
    detail: reportKeys.map((key) => byKey[key].artifact.fileName).join(", "),
    reportKeys,
    metrics
  };
}

function metric(label: string, value: string | number, tone?: "green" | "amber" | "red", detail?: string): EchoReportPanelMetric {
  return { label, value, ...(tone ? { tone } : {}), ...(detail ? { detail } : {}) };
}

function categoryIssueCount(byKey: Record<ReportKey, LoadedReport>, reportKeys: ReportKey[]): number {
  return reportKeys.reduce((total, key) => total + byKey[key].artifact.issueCount, 0);
}

function statusForArtifact(artifactValue: EchoReportArtifact): EchoOperationalReportStatus {
  if (artifactValue.status === "invalid") return "blocked";
  if (artifactValue.status === "missing") return "degraded";
  const normalized = (artifactValue.reportStatus ?? "").toUpperCase();
  if (normalized === "FAILED" || normalized === "BLOCKED") return "blocked";
  if (normalized === "DEGRADED" || normalized === "NOT_RUN" || normalized === "UNKNOWN") return "degraded";
  if (normalized === "PASS_WITH_WARNINGS") return "ready_with_warnings";
  return "ready";
}

function reportStatus(report: LoadedReport): string {
  return report.artifact.reportStatus ?? report.artifact.status;
}

function worstStatus(statuses: EchoOperationalReportStatus[]): EchoOperationalReportStatus {
  const rank: Record<EchoOperationalReportStatus, number> = {
    blocked: 4,
    degraded: 3,
    ready_with_warnings: 2,
    unknown: 1,
    ready: 0
  };
  return statuses.reduce<EchoOperationalReportStatus>((worst, status) => (rank[status] > rank[worst] ? status : worst), "ready");
}

function topIssuesFor(loaded: LoadedReport[], rootDir: string): EchoReportTopIssue[] {
  const issues = loaded.flatMap((report) => {
    const collected: Record<string, unknown>[] = [];
    collectIssueRecords(report.payload, collected, 24);
    return collected.map((issue) => issueFor(report.artifact, issue, rootDir));
  });
  return issues
    .sort((left, right) => severityRank(left.severity) - severityRank(right.severity) || left.reportPath.localeCompare(right.reportPath) || left.code.localeCompare(right.code))
    .slice(0, 12);
}

function drilldownIssuesFor(artifactValue: EchoReportArtifact, payload: Record<string, unknown> | null, rootDir: string): EchoReportTopIssue[] {
  if (artifactValue.status === "missing") {
    return [
      syntheticIssueFor(artifactValue, "ECHO-REPORT-MISSING", "WARNING", "Report artifact is missing.", "Run the safe refresh command or inspect the report generator.")
    ];
  }
  if (artifactValue.status === "invalid") {
    return [
      syntheticIssueFor(artifactValue, "ECHO-REPORT-INVALID", "ERROR", "Report artifact is invalid JSON.", artifactValue.error ?? "The report could not be parsed.")
    ];
  }

  const collected: Record<string, unknown>[] = [];
  collectIssueRecords(payload, collected, 100);
  return collected
    .map((issue) => issueFor(artifactValue, issue, rootDir))
    .sort((left, right) => severityRank(left.severity) - severityRank(right.severity) || left.code.localeCompare(right.code) || left.title.localeCompare(right.title));
}

function syntheticIssueFor(
  artifactValue: EchoReportArtifact,
  code: string,
  severity: string,
  title: string,
  summary: string
): EchoReportTopIssue {
  return {
    reportKey: artifactValue.key,
    reportPath: artifactValue.reportPath,
    code,
    severity,
    title,
    summary,
    blocking: severity === "ERROR" || severity === "FATAL",
    relatedPaths: [artifactValue.reportPath]
  };
}

function issueCountsFor(issues: EchoReportTopIssue[]): Record<string, number> {
  return issues.reduce<Record<string, number>>((counts, issue) => {
    const severity = normalizeSeverity(issue.severity);
    counts[severity] = (counts[severity] ?? 0) + 1;
    return counts;
  }, {});
}

function summaryPreviewFor(
  artifactValue: EchoReportArtifact,
  payload: Record<string, unknown> | null,
  rootDir: string
): Record<string, string | number | boolean | null> {
  const preview: Record<string, string | number | boolean | null> = {
    report: artifactValue.label ?? artifactValue.fileName,
    status: artifactValue.status,
    reportPath: artifactValue.reportPath,
    issues: artifactValue.issueCount
  };
  if (artifactValue.error) {
    preview.error = sanitizeString(artifactValue.error, rootDir);
  }
  if (!payload) return preview;

  for (const key of ["schema", "status", "generatedAt", "generator", "workspace", "addonSet", "packId"]) {
    putPrimitivePreview(preview, key, payload[key], rootDir);
  }

  const summary = objectAt(payload, "summary");
  for (const [key, value] of Object.entries(summary).sort(([left], [right]) => left.localeCompare(right)).slice(0, 18)) {
    putPrimitivePreview(preview, `summary.${key}`, value, rootDir);
  }
  return preview;
}

function putPrimitivePreview(
  target: Record<string, string | number | boolean | null>,
  key: string,
  value: unknown,
  rootDir: string
): void {
  const previewValue = primitivePreviewValue(value, rootDir);
  if (previewValue !== undefined) {
    target[key] = previewValue;
  }
}

function primitivePreviewValue(value: unknown, rootDir: string): string | number | boolean | null | undefined {
  if (typeof value === "string") return truncate(sanitizeString(value, rootDir), 220);
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "boolean") return value;
  if (value === null) return null;
  return undefined;
}

function dataPreviewFor(payload: Record<string, unknown> | null, rootDir: string): EchoReportDataPreviewEntry[] {
  if (!payload) return [];
  const reportData = dataObject(payload);
  const source = Object.keys(reportData).length ? reportData : payload;
  return Object.entries(source)
    .sort(([left], [right]) => left.localeCompare(right))
    .slice(0, 18)
    .map(([key, value]) => previewEntryFor(key, value, rootDir));
}

function previewEntryFor(key: string, value: unknown, rootDir: string): EchoReportDataPreviewEntry {
  if (Array.isArray(value)) {
    return { key, kind: "array", count: value.length, keys: previewArrayKeys(value) };
  }
  if (isRecord(value)) {
    return { key, kind: "object", count: Object.keys(value).length, keys: Object.keys(value).sort().slice(0, 8) };
  }
  const primitive = primitivePreviewValue(value, rootDir);
  if (primitive === null) return { key, kind: "null", value: null };
  if (typeof primitive === "string") return { key, kind: "string", value: primitive };
  if (typeof primitive === "number") return { key, kind: "number", value: primitive };
  if (typeof primitive === "boolean") return { key, kind: "boolean", value: primitive };
  return { key, kind: "unknown" };
}

function previewArrayKeys(value: unknown[]): string[] {
  const firstRecord = value.find(isRecord);
  return firstRecord ? Object.keys(firstRecord).sort().slice(0, 8) : [];
}

function safeCommandsFor(definition: ReportDefinition): EchoReportSafeCommand[] {
  const refreshCommand = REPORT_REFRESH_COMMANDS[definition.key] ?? ".\\gradlew.bat echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta";
  const commands: EchoReportSafeCommand[] = [
    {
      id: `${definition.key}-refresh`,
      label: `Refresh ${definition.label}`,
      command: refreshCommand,
      risk: "low",
      confirmationRequired: false,
      executesInUi: false,
      notes: "Copy-only helper. Command Center does not execute report refresh commands from drilldowns."
    },
    {
      id: "validate-echo-reports",
      label: "Validate report contracts",
      command: ".\\gradlew.bat validateEchoReports -PechoAddonSet=beta",
      risk: "none",
      confirmationRequired: false,
      executesInUi: false,
      notes: "Read-only validation command; copy it to a terminal when you want to rerun validation."
    }
  ];
  return commands.filter((command, index) => commands.findIndex((candidate) => candidate.command === command.command) === index);
}

function relatedDocsFor(definition: ReportDefinition): string[] {
  return Array.from(new Set([...(REPORT_DOCS[definition.key] ?? []), "docs/echo/tooling/ECHO_COMMAND_CENTER_PACKOS_REPORTS.md"]));
}

function collectIssueRecords(value: unknown, collected: Record<string, unknown>[], limit: number): void {
  if (!isRecord(value) || collected.length >= limit) return;
  for (const [key, child] of Object.entries(value)) {
    if (collected.length >= limit) return;
    if (["issues", "findings", "diagnostics", "blockingIssues", "nonBlockingFindings"].includes(key) && Array.isArray(child)) {
      for (const item of child) {
        if (collected.length >= limit) return;
        if (isRecord(item)) collected.push(item);
      }
      continue;
    }
    if (isRecord(child)) {
      collectIssueRecords(child, collected, limit);
    }
  }
}

function issueFor(artifactValue: EchoReportArtifact, issue: Record<string, unknown>, rootDir: string): EchoReportTopIssue {
  const code = stringValue(issue.code, stringValue(issue.id, "UNKNOWN"));
  const summary = sanitizeString(stringValue(issue.summary, stringValue(issue.detail, "")), rootDir);
  return {
    reportKey: artifactValue.key,
    reportPath: artifactValue.reportPath,
    code,
    severity: normalizeSeverity(stringValue(issue.severity, "NOTICE")),
    title: sanitizeString(stringValue(issue.title, code), rootDir),
    summary,
    ...(stringValue(issue.category, "") ? { category: stringValue(issue.category, "") } : {}),
    ...(stringValue(issue.source, "") ? { source: stringValue(issue.source, "") } : {}),
    ...(stringValue(issue.moduleId, "") ? { moduleId: stringValue(issue.moduleId, "") } : {}),
    ...(stringValue(issue.packId, "") ? { packId: stringValue(issue.packId, "") } : {}),
    ...(stringValue(issue.featureId, "") ? { featureId: stringValue(issue.featureId, "") } : {}),
    blocking: booleanValue(issue.blocking, normalizeSeverity(stringValue(issue.severity, "")) === "ERROR" || normalizeSeverity(stringValue(issue.severity, "")) === "FATAL"),
    relatedPaths: arrayAt(issue, "likelyFiles")
      .filter((entry): entry is string => typeof entry === "string")
      .map((entry) => safeReportPath(entry, rootDir))
  };
}

function safeReportPath(value: string, rootDir: string): string {
  const normalized = value.replaceAll("\\", "/");
  if (!path.isAbsolute(value) && !/^[A-Za-z]:\//.test(normalized)) {
    return normalized;
  }
  const relative = path.relative(rootDir, value).replaceAll("\\", "/");
  if (relative && !relative.startsWith("../") && relative !== ".." && !path.isAbsolute(relative)) {
    return relative;
  }
  return `[localOnly:${path.basename(value)}]`;
}

function sanitizeString(value: string, rootDir: string): string {
  const normalized = value.replaceAll("\\", "/");
  if (path.isAbsolute(value) || /^[A-Za-z]:\//.test(normalized)) {
    return safeReportPath(value, rootDir);
  }
  if (/[A-Za-z]:[\\/]/.test(value)) {
    return "[localOnly:redacted-path]";
  }
  return value;
}

function truncate(value: string, maxLength: number): string {
  return value.length > maxLength ? `${value.slice(0, maxLength - 3)}...` : value;
}

function normalizeSeverity(value: string): string {
  const normalized = value.trim().toUpperCase();
  if (normalized === "WARN") return "WARNING";
  if (normalized === "CRITICAL") return "ERROR";
  return normalized || "NOTICE";
}

function severityRank(value: string): number {
  switch (normalizeSeverity(value)) {
    case "FATAL":
      return 0;
    case "ERROR":
      return 1;
    case "WARNING":
      return 2;
    case "NOTICE":
      return 3;
    case "INFO":
      return 4;
    default:
      return 5;
  }
}

function issueCount(payload: Record<string, unknown>): number {
  const reportData = dataObject(payload);
  const diagnostics = Array.isArray(reportData.diagnostics) ? reportData.diagnostics.length : 0;
  const issues = countNestedIssues(payload);
  return Math.max(diagnostics, issues);
}

function countNestedIssues(payload: Record<string, unknown>): number {
  const keys = ["issues", "findings", "diagnostics", "blockingIssues", "nonBlockingFindings"];
  let count = 0;
  for (const key of keys) {
    const value = payload[key];
    if (Array.isArray(value)) count += value.length;
  }
  for (const value of Object.values(payload)) {
    if (isRecord(value)) count += countNestedIssues(value);
  }
  return count;
}

function objectAt(payload: Record<string, unknown> | null, key: string): Record<string, unknown> {
  const reportData = dataObject(payload);
  const value = reportData[key] ?? payload?.[key];
  return isRecord(value) ? value : {};
}

function dataObject(payload: Record<string, unknown> | null): Record<string, unknown> {
  const data = payload?.data;
  return isRecord(data) ? data : {};
}

function arrayAt(payload: Record<string, unknown>, key: string): unknown[] {
  const value = payload[key];
  return Array.isArray(value) ? value : [];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" && value.length > 0 ? value : fallback;
}

function numberValue(value: unknown, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function booleanValue(value: unknown, fallback: boolean): boolean {
  return typeof value === "boolean" ? value : fallback;
}
