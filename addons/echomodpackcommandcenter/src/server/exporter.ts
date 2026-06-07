import type {
  AppSettings,
  CommandRun,
  EchoOperationalReports,
  FeatureCatalogResponse,
  JarManifest,
  ModpackInventory,
  ModpackPipelineRun,
  ProjectDetail,
  ReadinessReport,
  ScanReport
} from "../shared/types.js";

export interface ExportContext {
  detail: ProjectDetail;
  settings: AppSettings;
  scans: ScanReport[];
  runs: CommandRun[];
  jarManifest?: JarManifest;
  readinessReport?: ReadinessReport;
  featureCatalog?: FeatureCatalogResponse;
  modpackSummary?: ModpackInventory;
  modpackRuns?: ModpackPipelineRun[];
  operationalReports?: EchoOperationalReports;
}

export function exportJson(context: ExportContext): string {
  const { detail, settings, scans, runs, jarManifest, readinessReport, featureCatalog, modpackSummary, modpackRuns, operationalReports } = context;
  const latestScan = detail.latestReport;
  const jarPromoteRuns = runs.filter((run) => run.commandId === "promote-jars");
  return JSON.stringify(
    {
      generatedAt: new Date().toISOString(),
      project: detail.project,
      settings,
      readinessScore: readinessReport?.score ?? latestScan?.summary.readinessScore ?? detail.project.buildHealth,
      readinessReport,
      latestScan,
      recentScans: scans,
      releaseRuns: runs,
      jarManifest,
      jarPromoteRuns,
      featureCatalog,
      modpackSummary,
      modpackRuns,
      operationalReports,
      roadmap: detail.roadmap,
      terminalPlanner: detail.terminalPlanner,
      promptTemplates: detail.prompts,
      releaseActions: detail.releaseActions
    },
    null,
    2
  );
}

export function exportMarkdown(context: ExportContext): string {
  const { detail, settings, scans, runs, jarManifest, readinessReport, featureCatalog, modpackSummary, modpackRuns, operationalReports } = context;
  const report = detail.latestReport;
  const jarPromoteRuns = runs.filter((run) => run.commandId === "promote-jars");
  const lines = [
    `# ${detail.project.name} Command Center Report`,
    "",
    `Status: ${detail.project.status}`,
    `Milestone: ${detail.project.currentMilestone}`,
    `Build Health: ${detail.project.buildHealth}%`,
    `Next Action: ${detail.project.nextRecommendedAction}`,
    `Readiness Score: ${readinessReport?.score ?? report?.summary.readinessScore ?? detail.project.buildHealth}%`,
    `Next Readiness Action: ${readinessReport?.nextAction?.label ?? "None"}`,
    "",
    "## Settings Snapshot",
    `- Project Workspace: ${detail.project.workspacePath}`,
    `- ECHO Root: ${settings.echoRoot}`,
    `- Mods Folder: ${settings.modpackModsDir || "not configured"}`,
    `- Python Executable: ${settings.pythonExecutable}`,
    `- Runtime Log Age: ${settings.runtimeLogMaxAgeMinutes} minute(s)`,
    `- Default Scan Mode: ${settings.defaultScanMode}`,
    "",
    "## Modules",
    ...detail.project.modules.map((module) => `- ${module.label} (${module.modId}) ${module.version}`),
    "",
    "## Latest QA",
    ...markdownReport(report),
    "",
    "## Recent Scans",
    ...(scans.length
      ? scans.map((scan) => `- #${scan.id} ${scan.mode} ${scan.status}: ${scan.summary.buildHealth}% health, ${scan.findings.length} finding(s)`)
      : ["No scan reports yet."]),
    "",
    "## Release Run History",
    ...(runs.length
      ? runs.map((run) => `- ${run.commandId}: ${run.status}${run.exitCode == null ? "" : ` (exit ${run.exitCode})`}${run.durationMs == null ? "" : `, ${run.durationMs}ms`}`)
      : ["No release runs yet."]),
    "",
    "## Jar Management",
    ...(jarManifest ? markdownJarManifest(jarManifest, jarPromoteRuns) : ["Jar manifest was not included in this export."]),
    "",
    "## Feature And Lore Implementation",
    ...(featureCatalog ? markdownFeatureCatalog(featureCatalog) : ["Feature catalog was not included in this export."]),
    "",
    "## Modpack Management",
    ...(modpackSummary ? markdownModpack(modpackSummary, modpackRuns ?? []) : ["Modpack summary was not included in this export."]),
    "",
    "## Operational Reports",
    ...(operationalReports ? markdownOperationalReports(operationalReports) : ["Operational reports were not included in this export."]),
    "",
    "## Roadmap",
    ...detail.roadmap.map((phase) => `- ${phase.title}: ${phase.status} (${phase.progress}%) - ${phase.summary}`),
    "",
    "## Codex Prompts",
    ...detail.prompts
      .filter((prompt) => prompt.category === "Codex QA")
      .map((prompt) => `- ${prompt.title}: ${prompt.description}`),
    "",
    "## Release Actions",
    ...detail.releaseActions.map((action) => `- ${action.label}: ${action.description}`)
  ];
  return `${lines.join("\n")}\n`;
}

function markdownOperationalReports(reports: EchoOperationalReports): string[] {
  return [
    `Status: ${reports.status}`,
    `PackOS: ${reports.summary.packOsStatus}`,
    `Health: ${reports.summary.healthStatus}`,
    `Recovery: ${reports.summary.recoveryMode}`,
    `Diagnostics: ${reports.summary.diagnosticsCount} total, ${reports.summary.blockingDiagnostics} blocking`,
    `Repair Plan: ${reports.summary.repairPlanStatus}`,
    `Recovery Plan: ${reports.summary.recoveryPlanStatus}`,
    ...(reports.recommendations.length ? reports.recommendations.map((item) => `- ${item}`) : ["- No operational recommendations."]),
    ...reports.artifacts.map((artifact) => `- ${artifact.fileName}: ${artifact.status}${artifact.error ? ` (${artifact.error})` : ""}`)
  ];
}

function markdownModpack(summary: ModpackInventory, runs: ModpackPipelineRun[]): string[] {
  return [
    `Generated: ${summary.generatedAt}`,
    `Target Folder: ${summary.targetDir || "not configured"}`,
    `Target Status: ${summary.targetConfigured ? (summary.targetExists ? summary.status : "missing") : "not configured"}`,
    ...(summary.blockers.length ? summary.blockers.map((blocker) => `- Blocked: ${blocker}`) : ["- Rebuild & Update All is available."]),
    `Expected: ${summary.summary.expected}, built: ${summary.summary.built}, current: ${summary.summary.current}, stale: ${summary.summary.stale}, duplicate: ${summary.summary.duplicate}`,
    ...summary.targets.map((target) => `- ${target.projectName}: ${target.status}, command ${target.buildCommandId}, ${target.manifest.summary.current}/${target.manifest.summary.expected} current jar(s)`),
    "",
    "### Modpack Pipeline History",
    ...(runs.length
      ? runs.map((run) => `- ${run.status} at ${run.finishedAt ?? run.startedAt}: ${run.steps.filter((step) => step.status === "succeeded").length}/${run.steps.length} step(s) complete`)
      : ["No modpack pipeline runs yet."])
  ];
}

function markdownFeatureCatalog(catalog: FeatureCatalogResponse): string[] {
  const statusSummary = Object.entries(catalog.summary.statusCounts)
    .filter(([, count]) => count > 0)
    .map(([status, count]) => `${status}: ${count}`)
    .join(", ");
  return [
    `Generated: ${catalog.generatedAt}`,
    `Total Features: ${catalog.summary.total}${statusSummary ? ` (${statusSummary})` : ""}`,
    ...catalog.features.map((feature) => {
      const sources = feature.sources.map((source) => `${source.label} / ${source.section}`).join("; ");
      const evidence = feature.evidence.map((item) => item.label).join("; ");
      return `- [${feature.status}] ${feature.title}: ${feature.implementationSummary} Next: ${feature.nextAction}${sources ? ` Sources: ${sources}.` : ""}${evidence ? ` Evidence: ${evidence}.` : ""}`;
    })
  ];
}

function markdownJarManifest(manifest: JarManifest, jarPromoteRuns: CommandRun[]): string[] {
  return [
    `Generated: ${manifest.generatedAt}`,
    `Build Root: ${manifest.buildRoot}`,
    `Target Folder: ${manifest.targetDir || "not configured"}`,
    `Target Status: ${manifest.targetConfigured ? (manifest.targetExists ? "ready" : "missing") : "not configured"}`,
    ...(manifest.blockers.length ? manifest.blockers.map((blocker) => `- Blocked: ${blocker}`) : ["- Promote path is available."]),
    `Expected: ${manifest.summary.expected}, built: ${manifest.summary.built}, current in target: ${manifest.summary.current}, stale: ${manifest.summary.stale}, duplicate: ${manifest.summary.duplicate}`,
    ...manifest.artifacts.map((artifact) => {
      const sourceState = artifact.exists ? "built" : "missing";
      const targetState = artifact.current ? "current" : "not current";
      return `- ${artifact.expectedFileName}: ${sourceState}, target ${targetState}${artifact.checksum ? `, sha256 ${artifact.checksum}` : ""}`;
    }),
    "",
    "### Jar Promote History",
    ...(jarPromoteRuns.length
      ? jarPromoteRuns.map((run) => `- ${run.status} at ${run.finishedAt ?? run.startedAt}: ${run.output.split("\n")[0] ?? run.commandId}`)
      : ["No jar promote runs yet."])
  ];
}

function markdownReport(report: ScanReport | null): string[] {
  if (!report) {
    return ["No scan report yet."];
  }
  return [
    `Scan: #${report.id} ${report.mode} ${report.status} at ${report.finishedAt ?? report.createdAt}`,
    `Summary: ${report.summary.status}, ${report.summary.buildHealth}% health, ${report.summary.criticalIssues} critical issue(s), ${report.summary.polishTasks} polish task(s).`,
    ...report.findings.map((finding) => {
      const location = finding.path ? ` (${finding.path}${finding.line ? `:${finding.line}` : ""})` : "";
      return `- [${finding.severity}] ${finding.code ?? finding.title}: ${finding.detail}${location}`;
    }),
    report.rawOutput ? "\n### Raw Validator Output\n\n```text\n" + report.rawOutput.slice(0, 12000) + "\n```" : ""
  ].filter(Boolean);
}
