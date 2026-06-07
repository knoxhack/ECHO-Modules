import express from "express";
import cors from "cors";
import { pathToFileURL } from "node:url";
import { CommandCenterStore } from "./db.js";
import { runHybridScan } from "./scanner.js";
import { exportJson, exportMarkdown } from "./exporter.js";
import { startReleaseAction, stopReleaseAction } from "./runner.js";
import { buildJarManifest, jarBuildCommandId, JarPipelineError, runJarPromotion } from "./jars.js";
import { buildReadinessReport } from "./readiness.js";
import { buildFeatureCatalog } from "./features.js";
import { buildModpackSummary, listModpackRuns, ModpackPipelineError, startModpackRebuild, stopModpackRebuild, type ModpackServiceOptions } from "./modpack.js";
import { buildDistributionSummary, DistributionPipelineError, listDistributionRuns, startPackExport, stopDistributionRun, type DistributionServiceOptions } from "./distributions.js";
import { loadEchoOperationalReports, loadEchoReportDrilldown, type EchoOperationalReportLoaderOptions } from "./reports.js";
import {
  EchoReportJobError,
  listEchoReportJobs,
  loadEchoReportJobDetail,
  startEchoReportJob,
  stopEchoReportJob,
  type EchoReportJobServiceOptions
} from "./reportJobs.js";
import {
  bridgeJobLogs,
  cancelBridgeJob,
  confirmBridgeSafeAction,
  createBridgeSession,
  loadBridgeState,
  probeBridgeExecutor,
  submitBridgePrompt,
  type EchoBridgeServiceOptions
} from "./bridge.js";
import { DB_PATH, ECHO_ROOT, LOCAL_DATA_DIR } from "./paths.js";
import type {
  AppSettings,
  EchoBridgeConfirmationRequest,
  EchoBridgeCreateSessionRequest,
  EchoBridgePromptRequest,
  ExportFormat,
  JarPipelineRequest,
  ModpackPipelineRequest,
  PackExportRequest,
  ScanMode,
  EchoReportJobRequest
} from "../shared/types.js";

const HOST = "127.0.0.1";
const PORT = Number(process.env.NOXHACK_COMMAND_CENTER_PORT ?? 4177);

export interface AppOptions {
  modpack?: ModpackServiceOptions;
  distributions?: DistributionServiceOptions;
  reports?: EchoOperationalReportLoaderOptions;
  reportJobs?: EchoReportJobServiceOptions;
  bridge?: EchoBridgeServiceOptions;
}

export function createApp(store: CommandCenterStore, options: AppOptions = {}): express.Express {
const app = express();

app.use(cors({ origin: [/^http:\/\/127\.0\.0\.1:\d+$/, /^http:\/\/localhost:\d+$/] }));
app.use(express.json({ limit: "1mb" }));

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    service: "noxhack-command-center",
    echoRoot: ECHO_ROOT,
    localDataDir: LOCAL_DATA_DIR,
    database: DB_PATH
  });
});

app.get("/api/settings", (_req, res) => {
  res.json(store.getSettings());
});

app.put("/api/settings", (req, res) => {
  res.json(store.updateSettings(req.body as Partial<AppSettings>));
});

app.get("/api/projects", (_req, res) => {
  res.json({ projects: store.listProjects() });
});

app.get("/api/modpack/summary", (_req, res) => {
  res.json(buildModpackSummary(store, options.modpack));
});

app.get("/api/modpack/runs", (_req, res) => {
  res.json({ runs: listModpackRuns(store, 25) });
});

app.get("/api/distributions/summary", (_req, res) => {
  res.json(buildDistributionSummary(store, options.distributions));
});

app.get("/api/distributions/runs", (_req, res) => {
  res.json({ runs: listDistributionRuns(store, 25) });
});

app.get("/api/reports/echo", (_req, res) => {
  res.json(loadEchoOperationalReports(options.reports ?? {}));
});

app.get("/api/reports/echo/jobs", (_req, res) => {
  res.json(listEchoReportJobs(store, options.reportJobs ?? {}));
});

app.post("/api/reports/echo/jobs", (req, res) => {
  try {
    res.status(202).json(startEchoReportJob(store, req.body as EchoReportJobRequest | undefined, options.reportJobs ?? {}));
  } catch (error) {
    if (error instanceof EchoReportJobError) {
      res.status(error.statusCode).json({ error: error.message });
      return;
    }
    res.status(500).json({ error: error instanceof Error ? error.message : String(error) });
  }
});

app.get("/api/reports/echo/jobs/:runId", (req, res) => {
  const result = loadEchoReportJobDetail(store, req.params.runId, options.reportJobs ?? {});
  if (!result) {
    res.status(404).json({ error: "Report job not found" });
    return;
  }
  res.json(result);
});

app.post("/api/reports/echo/jobs/:runId/stop", (req, res) => {
  const result = stopEchoReportJob(store, req.params.runId, options.reportJobs ?? {});
  if (!result) {
    res.status(404).json({ error: "Report job not found" });
    return;
  }
  res.json(result);
});

app.get("/api/reports/echo/:reportKey", (req, res) => {
  const drilldown = loadEchoReportDrilldown(req.params.reportKey, options.reports ?? {});
  if (!drilldown) {
    res.status(404).json({ error: `Unknown ECHO report key: ${req.params.reportKey}` });
    return;
  }
  res.json(drilldown);
});

app.get("/api/bridge", (_req, res) => {
  res.json(loadBridgeState(options.bridge ?? {}));
});

app.get("/api/bridge/executor/probe", (_req, res) => {
  res.json(probeBridgeExecutor(options.bridge ?? {}));
});

app.post("/api/bridge/sessions", (req, res) => {
  const body = req.body as EchoBridgeCreateSessionRequest | undefined;
  res.status(201).json(createBridgeSession(body ?? {}, options.bridge ?? {}));
});

app.post("/api/bridge/sessions/:sessionId/prompts", (req, res) => {
  const body = req.body as EchoBridgePromptRequest | undefined;
  if (!body?.promptText || typeof body.promptText !== "string") {
    res.status(400).json({ error: "promptText is required." });
    return;
  }
  res.status(202).json(submitBridgePrompt(req.params.sessionId, body, options.bridge ?? {}));
});

app.post("/api/bridge/jobs/:jobId/cancel", (req, res) => {
  res.json(cancelBridgeJob(req.params.jobId, options.bridge ?? {}));
});

app.post("/api/bridge/safe-actions/:requestId/confirm", async (req, res) => {
  const body = req.body as EchoBridgeConfirmationRequest | undefined;
  if (typeof body?.confirmed !== "boolean") {
    res.status(400).json({ error: "confirmed must be true or false." });
    return;
  }
  res.json(await confirmBridgeSafeAction(req.params.requestId, body, options.bridge ?? {}));
});

app.get("/api/bridge/jobs/:jobId/logs", (req, res) => {
  const after = typeof req.query.after === "string" ? req.query.after : undefined;
  const logs = bridgeJobLogs(req.params.jobId, options.bridge ?? {}, after);
  const job = loadBridgeState(options.bridge ?? {}).jobs.find((candidate) => candidate.id === req.params.jobId);
  res.json({ logs, streamCursors: job?.streamCursors ?? null });
});

app.post("/api/distributions/export", async (req, res) => {
  const body = req.body as PackExportRequest;
  try {
    const result = await startPackExport(store, body, options.distributions);
    res.json(result);
  } catch (error) {
    if (error instanceof DistributionPipelineError) {
      res.status(error.statusCode).json({
        error: error.message,
        summary: error.summary,
        run: error.run
      });
      return;
    }
    res.status(500).json({ error: error instanceof Error ? error.message : String(error) });
  }
});

app.post("/api/distributions/runs/:runId/stop", (req, res) => {
  const result = stopDistributionRun(store, req.params.runId, options.distributions);
  if (!result) {
    res.status(404).json({ error: "Distribution run not found" });
    return;
  }
  res.json(result);
});

app.post("/api/modpack/rebuild", async (req, res) => {
  const body = req.body as ModpackPipelineRequest | undefined;
  try {
    const result = await startModpackRebuild(store, body?.confirmed === true, options.modpack);
    res.status(result.run.status === "running" ? 202 : 200).json(result);
  } catch (error) {
    if (error instanceof ModpackPipelineError) {
      res.status(error.statusCode).json({
        error: error.message,
        summary: error.summary,
        run: error.run
      });
      return;
    }
    res.status(500).json({ error: error instanceof Error ? error.message : String(error) });
  }
});

app.post("/api/modpack/runs/:runId/stop", (req, res) => {
  const result = stopModpackRebuild(store, req.params.runId, options.modpack);
  if (!result) {
    res.status(404).json({ error: "Modpack run not found" });
    return;
  }
  res.json(result);
});

app.get("/api/projects/:slug", (req, res) => {
  const detail = store.getProjectDetail(req.params.slug);
  if (!detail) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json(detail);
});

app.post("/api/projects/:slug/scan", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  const mode = scanMode(req.body?.mode, store.getSettings().defaultScanMode);
  const scan = runHybridScan(project, store.getQaTracks(project.slug), store.getSettings(), mode);
  const report = store.createScanReport(scan);
  res.json(report);
});

app.get("/api/projects/:slug/scans", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({ reports: store.listScanReports(project.slug, 25) });
});

app.get("/api/projects/:slug/scans/:reportId", (req, res) => {
  const report = store.getScanReportById(req.params.slug, Number(req.params.reportId));
  if (!report) {
    res.status(404).json({ error: "Scan report not found" });
    return;
  }
  res.json(report);
});

app.get("/api/projects/:slug/qa/latest", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({ report: store.getLatestScanReport(project.slug) });
});

app.get("/api/projects/:slug/roadmap", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({ roadmap: store.getRoadmap(project.slug) });
});

app.get("/api/projects/:slug/features", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json(buildFeatureCatalog(project.slug, store.getFeatures(project.slug)));
});

app.get("/api/projects/:slug/prompts", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({ prompts: store.getPrompts(project.slug) });
});

app.post("/api/projects/:slug/prompts/render", (req, res) => {
  const promptId = typeof req.body?.promptId === "string" ? req.body.promptId : "";
  const prompt = store.getPrompt(req.params.slug, promptId);
  if (!prompt) {
    res.status(404).json({ error: "Prompt not found" });
    return;
  }
  res.json({ prompt });
});

app.get("/api/projects/:slug/release", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({
    actions: store.getReleaseActions(project.slug),
    modpackModsDir: store.getSettings().modpackModsDir,
    runs: store.listCommandRuns(project.slug, 25)
  });
});

app.get("/api/projects/:slug/jars", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json(buildJarManifest(project, store.getSettings()));
});

app.get("/api/projects/:slug/readiness", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  const settings = store.getSettings();
  const jarManifest = buildJarManifest(project, settings);
  res.json(buildReadinessReport(project, settings, store.listScanReports(project.slug, 50), jarManifest));
});

app.post("/api/projects/:slug/jars/build", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  if (hasRunningRun(store, project.slug)) {
    res.status(409).json({ error: "A command run is already active for this project." });
    return;
  }
  const commandId = jarBuildCommandId(project);
  const action = store.getReleaseAction(project.slug, commandId);
  if (!action) {
    res.status(404).json({ error: `Build action is not configured for ${project.slug}` });
    return;
  }
  const body = req.body as JarPipelineRequest | undefined;
  if (action.risk !== "low" && body?.confirmed !== true) {
    res.status(409).json({
      error: "Confirmation required",
      commandId: action.commandId,
      risk: action.risk
    });
    return;
  }
  const run = startReleaseAction(store, project.slug, commandId);
  res.status(run.status === "rejected" ? 400 : 202).json({
    run,
    manifest: buildJarManifest(project, store.getSettings())
  });
});

app.post("/api/projects/:slug/jars/promote", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  const body = req.body as JarPipelineRequest | undefined;
  if (body?.confirmed !== true) {
    res.status(409).json({
      error: "Confirmation required",
      commandId: "promote-jars",
      risk: "high"
    });
    return;
  }
  if (hasRunningRun(store, project.slug)) {
    res.status(409).json({ error: "A build or command run is already active for this project." });
    return;
  }
  try {
    const result = runJarPromotion(store, project, store.getSettings());
    const scan = runHybridScan(project, store.getQaTracks(project.slug), store.getSettings(), "quick");
    const scanReport = store.createScanReport(scan);
    res.json({ ...result, scanReport });
  } catch (error) {
    if (error instanceof JarPipelineError) {
      res.status(error.statusCode).json({
        error: error.message,
        manifest: error.manifest,
        run: error.run
      });
      return;
    }
    res.status(500).json({ error: error instanceof Error ? error.message : String(error) });
  }
});

app.get("/api/projects/:slug/runs", (req, res) => {
  const project = store.getProject(req.params.slug);
  if (!project) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  res.json({ runs: store.listCommandRuns(project.slug, 50) });
});

app.post("/api/projects/:slug/release/:commandId/run", (req, res) => {
  const action = store.getReleaseAction(req.params.slug, req.params.commandId);
  if (action && action.risk !== "low" && req.body?.confirmed !== true) {
    res.status(409).json({
      error: "Confirmation required",
      commandId: action.commandId,
      risk: action.risk
    });
    return;
  }
  const run = startReleaseAction(store, req.params.slug, req.params.commandId);
  res.status(run.status === "rejected" ? 400 : 202).json(run);
});

app.get("/api/runs/:runId", (req, res) => {
  const run = store.getCommandRun(req.params.runId);
  if (!run) {
    res.status(404).json({ error: "Run not found" });
    return;
  }
  res.json(run);
});

app.post("/api/runs/:runId/stop", (req, res) => {
  const run = stopReleaseAction(store, req.params.runId);
  if (!run) {
    res.status(404).json({ error: "Run not found" });
    return;
  }
  res.json(run);
});

app.get("/api/projects/:slug/export", (req, res) => {
  const detail = store.getProjectDetail(req.params.slug);
  if (!detail) {
    res.status(404).json({ error: "Project not found" });
    return;
  }
  const settings = store.getSettings();
  const jarManifest = buildJarManifest(detail.project, settings);
  const context = {
    detail,
    settings,
    scans: store.listScanReports(req.params.slug, 10),
    runs: store.listCommandRuns(req.params.slug, 25),
    jarManifest,
    readinessReport: buildReadinessReport(detail.project, settings, store.listScanReports(req.params.slug, 50), jarManifest),
    featureCatalog: buildFeatureCatalog(detail.project.slug, store.getFeatures(detail.project.slug)),
    modpackSummary: buildModpackSummary(store, options.modpack),
    modpackRuns: listModpackRuns(store, 5),
    operationalReports: loadEchoOperationalReports()
  };
  const format = (req.query.format === "markdown" ? "markdown" : "json") satisfies ExportFormat;
  if (format === "markdown") {
    res.type("text/markdown").send(exportMarkdown(context));
    return;
  }
  res.type("application/json").send(exportJson(context));
});

app.use((req, res) => {
  res.status(404).json({ error: `No route for ${req.method} ${req.path}` });
});

return app;
}

if (isDirectRun()) {
  const store = new CommandCenterStore();
  const app = createApp(store);
  const server = app.listen(PORT, HOST, () => {
    console.log(`Noxhack Modpack Command Center API listening on http://${HOST}:${PORT}`);
  });

  function shutdown(): void {
    server.close(() => {
      store.close();
      process.exit(0);
    });
  }

  process.on("SIGINT", () => shutdown());
  process.on("SIGTERM", () => shutdown());
}

function scanMode(value: unknown, fallback: ScanMode): ScanMode {
  return value === "deep" || value === "quick" ? value : fallback;
}

function hasRunningRun(store: CommandCenterStore, projectSlug: string): boolean {
  return store.listCommandRuns(projectSlug, 50).some((run) => run.status === "running");
}

function isDirectRun(): boolean {
  return Boolean(process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url);
}
