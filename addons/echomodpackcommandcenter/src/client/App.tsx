import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  Archive,
  Boxes,
  CheckCircle2,
  Clipboard,
  Copy,
  Database,
  Download,
  FileJson,
  FileText,
  Gauge,
  Image,
  Layers,
  ListChecks,
  Map,
  Package,
  PackageCheck,
  Play,
  RefreshCw,
  Rocket,
  Settings,
  ShieldCheck,
  Sparkles,
  Square,
  TerminalSquare
} from "lucide-react";
import type {
  AppSettings,
  CommandRun,
  DistributionPack,
  DistributionSummary,
  EchoOperationalReports,
  EchoReportDrilldown,
  EchoReportJobDetail,
  EchoReportJobSummary,
  EchoReportJobDefinition,
  EchoReportPanel,
  EchoReportTopIssue,
  FeatureCatalogResponse,
  FeatureRecord,
  FeatureStatus,
  JarManifest,
  ModpackInventory,
  ModpackPipelineRun,
  Project,
  ProjectDetail,
  PromptTemplate,
  QaFinding,
  ReadinessChecklistItem,
  ReadinessReport,
  ReleaseAction,
  RoadmapPhase,
  ScanMode,
  ScanReport,
  TerminalPlannerGroup
} from "../shared/types";
import {
  getHealth,
  exportDistributionPack,
  getDistributionRuns,
  getDistributionSummary,
  getEchoReportDrilldown,
  getEchoReportJobDetail,
  getEchoReportJobs,
  getEchoOperationalReports,
  getFeatures,
  getJars,
  getModpackRuns,
  getModpackSummary,
  getProject,
  getProjects,
  getReadiness,
  getRelease,
  getRun,
  getSettings,
  listScans,
  renderPrompt,
  buildJars,
  promoteJars,
  rebuildModpack,
  runReleaseAction,
  runScan,
  saveSettings,
  stopDistributionRun,
  startEchoReportJob,
  stopEchoReportJob,
  stopModpackRun,
  stopRun
} from "./api";
import {
  ActionTile,
  Alert,
  BrandPanel,
  EmptyState,
  FeatureStatusBadge,
  InfoBanner,
  JarStatusBadge,
  LoadingPanel,
  Metric,
  ModpackStatusBadge,
  Nav,
  OutputPanel,
  PageHeader,
  Risk,
  RunHistoryPanel,
  SectionTitle,
  Severity,
  TopBar
} from "./ui";
import { ConfirmJarModal, ConfirmModal, ReadinessChecklistPanel, type JarActionKind } from "./workflow";
import { BridgeView } from "./BridgeView";
import {
  commandStatusClass,
  displayCommand,
  formatBytes,
  formatDate,
  mergeRun,
  operationalStatusTone,
  promoteDisabledReason,
  reportArtifactTone,
  selectedRunFrom,
  type ViewKey
} from "./view-model";

const emptySettings: AppSettings = {
  echoRoot: "",
  modpackModsDir: "",
  pythonExecutable: "",
  runtimeLogMaxAgeMinutes: 180,
  defaultScanMode: "quick"
};

export function App(): JSX.Element {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedSlug, setSelectedSlug] = useState("echo");
  const [detail, setDetail] = useState<ProjectDetail | null>(null);
  const [settings, setSettings] = useState<AppSettings>(emptySettings);
  const [scanHistory, setScanHistory] = useState<ScanReport[]>([]);
  const [runs, setRuns] = useState<CommandRun[]>([]);
  const [jarManifest, setJarManifest] = useState<JarManifest | null>(null);
  const [readiness, setReadiness] = useState<ReadinessReport | null>(null);
  const [featureCatalog, setFeatureCatalog] = useState<FeatureCatalogResponse | null>(null);
  const [modpackSummary, setModpackSummary] = useState<ModpackInventory | null>(null);
  const [modpackRuns, setModpackRuns] = useState<ModpackPipelineRun[]>([]);
  const [distributionSummary, setDistributionSummary] = useState<DistributionSummary | null>(null);
  const [distributionRuns, setDistributionRuns] = useState<CommandRun[]>([]);
  const [operationalReports, setOperationalReports] = useState<EchoOperationalReports | null>(null);
  const [reportJobs, setReportJobs] = useState<EchoReportJobSummary | null>(null);
  const [activeView, setActiveView] = useState<ViewKey>("projects");
  const [status, setStatus] = useState("Booting command center");
  const [error, setError] = useState<string | null>(null);
  const [activeRun, setActiveRun] = useState<CommandRun | null>(null);
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
  const [selectedModpackRunId, setSelectedModpackRunId] = useState<string | null>(null);
  const [selectedDistributionRunId, setSelectedDistributionRunId] = useState<string | null>(null);
  const [scanBusy, setScanBusy] = useState<ScanMode | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getHealth(), getProjects(), getSettings()])
      .then(([health, projectList, loadedSettings]) => {
        if (cancelled) return;
        setProjects(projectList);
        setSettings(loadedSettings);
        setStatus(`API online at ${health.service}`);
        if (!projectList.some((project) => project.slug === selectedSlug) && projectList[0]) {
          setSelectedSlug(projectList[0].slug);
        }
      })
      .catch((cause) => {
        if (!cancelled) setError(cause instanceof Error ? cause.message : String(cause));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    void refreshAll();
  }, [selectedSlug]);

  useEffect(() => {
    if (!activeRun || activeRun.status !== "running") {
      return;
    }
    const timer = window.setInterval(() => {
      getRun(activeRun.id)
        .then((run) => {
          setActiveRun(run);
          setRuns((current) => mergeRun(current, run));
          if (run.status !== "running") {
            void refreshAll();
          }
        })
        .catch((cause) => setError(cause instanceof Error ? cause.message : String(cause)));
    }, 1400);
    return () => window.clearInterval(timer);
  }, [activeRun]);

  useEffect(() => {
    if (!modpackRuns.some((run) => run.status === "running")) {
      return;
    }
    const timer = window.setInterval(() => {
      refreshModpack().catch((cause) => setError(cause instanceof Error ? cause.message : String(cause)));
    }, 1600);
    return () => window.clearInterval(timer);
  }, [modpackRuns]);

  useEffect(() => {
    if (!distributionRuns.some((run) => run.status === "running")) {
      return;
    }
    const timer = window.setInterval(() => {
      refreshDistributions().catch((cause) => setError(cause instanceof Error ? cause.message : String(cause)));
    }, 1600);
    return () => window.clearInterval(timer);
  }, [distributionRuns]);

  useEffect(() => {
    if (!reportJobs?.runs.some((run) => run.status === "running")) {
      return;
    }
    const timer = window.setInterval(() => {
      refreshReportJobs()
        .then((jobs) => {
          if (!jobs.runs.some((run) => run.status === "running")) {
            void refreshOperationalReports();
          }
        })
        .catch((cause) => setError(cause instanceof Error ? cause.message : String(cause)));
    }, 1600);
    return () => window.clearInterval(timer);
  }, [reportJobs]);

  const selectedProject = useMemo(
    () => projects.find((project) => project.slug === selectedSlug) ?? detail?.project ?? null,
    [projects, selectedSlug, detail]
  );
  const selectedRun = useMemo(
    () => selectedRunFrom(runs, selectedRunId, activeRun?.projectSlug === selectedSlug ? activeRun : null),
    [activeRun, runs, selectedRunId, selectedSlug]
  );

  async function refreshAll(): Promise<void> {
    try {
      const [
        projectDetail,
        features,
        scans,
        release,
        jars,
        loadedReadiness,
        loadedSettings,
        loadedModpackSummary,
        loadedModpackRuns,
        loadedDistributionSummary,
        loadedDistributionRuns,
        loadedOperationalReports,
        loadedReportJobs
      ] = await Promise.all([
        getProject(selectedSlug),
        getFeatures(selectedSlug),
        listScans(selectedSlug),
        getRelease(selectedSlug),
        getJars(selectedSlug),
        getReadiness(selectedSlug),
        getSettings(),
        getModpackSummary(),
        getModpackRuns(),
        getDistributionSummary(),
        getDistributionRuns(),
        getEchoOperationalReports(),
        getEchoReportJobs()
      ]);
      setDetail(projectDetail);
      setFeatureCatalog(features);
      setScanHistory(scans);
      setRuns(release.runs);
      setJarManifest(jars);
      setReadiness(loadedReadiness);
      setSettings(loadedSettings);
      setModpackSummary(loadedModpackSummary);
      setModpackRuns(loadedModpackRuns);
      setDistributionSummary(loadedDistributionSummary);
      setDistributionRuns(loadedDistributionRuns);
      setOperationalReports(loadedOperationalReports);
      setReportJobs(loadedReportJobs);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }

  async function handleRunScan(mode: ScanMode): Promise<void> {
    setScanBusy(mode);
    try {
      const report = await runScan(selectedSlug, mode);
      setDetail((current) => (current ? { ...current, latestReport: report, project: { ...current.project, buildHealth: report.summary.buildHealth, criticalIssues: report.summary.criticalIssues, polishTasks: report.summary.polishTasks } } : current));
      await refreshAll();
    } finally {
      setScanBusy(null);
    }
  }

  async function handleRunAction(action: ReleaseAction, confirmed = false): Promise<void> {
    try {
      const run = await runReleaseAction(selectedSlug, action.commandId, confirmed);
      setActiveRun(run);
      setSelectedRunId(run.id);
      setRuns((current) => mergeRun(current, run));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }

  async function refreshModpack(): Promise<void> {
    const [loadedSummary, loadedRuns] = await Promise.all([getModpackSummary(), getModpackRuns()]);
    setModpackSummary(loadedSummary);
    setModpackRuns(loadedRuns);
  }

  async function refreshDistributions(): Promise<void> {
    const [loadedSummary, loadedRuns] = await Promise.all([getDistributionSummary(), getDistributionRuns()]);
    setDistributionSummary(loadedSummary);
    setDistributionRuns(loadedRuns);
  }

  async function refreshOperationalReports(): Promise<void> {
    const [loadedReports, loadedReportJobs] = await Promise.all([getEchoOperationalReports(), getEchoReportJobs()]);
    setOperationalReports(loadedReports);
    setReportJobs(loadedReportJobs);
  }

  async function refreshReportJobs(): Promise<EchoReportJobSummary> {
    const loadedReportJobs = await getEchoReportJobs();
    setReportJobs(loadedReportJobs);
    return loadedReportJobs;
  }

  async function handleStartReportJob(commandId: string): Promise<void> {
    try {
      const loadedReportJobs = await startEchoReportJob(commandId);
      setReportJobs(loadedReportJobs);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshReportJobs().catch(() => undefined);
    }
  }

  async function handleStopReportJob(runId: string): Promise<void> {
    try {
      const loadedReportJobs = await stopEchoReportJob(runId);
      setReportJobs(loadedReportJobs);
      setError(null);
      await refreshOperationalReports();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshReportJobs().catch(() => undefined);
    }
  }

  async function handleStopRun(runId: string): Promise<void> {
    const run = await stopRun(runId);
      setActiveRun(run);
      setSelectedRunId(run.id);
      setRuns((current) => mergeRun(current, run));
  }

  async function handleSaveSettings(next: AppSettings): Promise<void> {
    const saved = await saveSettings(next);
    setSettings(saved);
    await refreshAll();
  }

  async function handleBuildJars(confirmed = false): Promise<void> {
    try {
      const result = await buildJars(selectedSlug, confirmed);
      setJarManifest(result.manifest);
      setActiveRun(result.run);
      setSelectedRunId(result.run.id);
      setRuns((current) => mergeRun(current, result.run));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }

  async function handlePromoteJars(confirmed = false): Promise<void> {
    try {
      const result = await promoteJars(selectedSlug, confirmed);
      setJarManifest(result.manifest);
      setActiveRun(result.run);
      setSelectedRunId(result.run.id);
      setRuns((current) => mergeRun(current, result.run));
      if (result.scanReport) {
        setDetail((current) =>
          current
            ? {
                ...current,
                latestReport: result.scanReport ?? current.latestReport,
                project: {
                  ...current.project,
                  buildHealth: result.scanReport?.summary.buildHealth ?? current.project.buildHealth,
                  criticalIssues: result.scanReport?.summary.criticalIssues ?? current.project.criticalIssues,
                  polishTasks: result.scanReport?.summary.polishTasks ?? current.project.polishTasks
                }
              }
            : current
        );
      }
      await refreshAll();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }

  async function handleRebuildModpack(confirmed = false): Promise<void> {
    try {
      const result = await rebuildModpack(confirmed);
      setModpackSummary(result.summary);
      setModpackRuns((current) => [result.run, ...current.filter((run) => run.id !== result.run.id)].slice(0, 25));
      setSelectedModpackRunId(result.run.id);
      setError(null);
      await refreshModpack();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshModpack().catch(() => undefined);
    }
  }

  async function handleStopModpackRun(runId: string): Promise<void> {
    try {
      const result = await stopModpackRun(runId);
      setModpackSummary(result.summary);
      setModpackRuns((current) => [result.run, ...current.filter((run) => run.id !== result.run.id)].slice(0, 25));
      setSelectedModpackRunId(result.run.id);
      setError(null);
      await refreshModpack();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshModpack().catch(() => undefined);
    }
  }

  async function handleExportDistribution(packId: DistributionPack["id"], targetModsDir: string, buildFirst: boolean, includeOptionalModules: boolean): Promise<void> {
    try {
      const result = await exportDistributionPack({
        packId,
        targetModsDir,
        buildFirst,
        includeOptionalModules,
        confirmed: true
      });
      setDistributionSummary(result.summary);
      setDistributionRuns((current) => [result.run, ...current.filter((run) => run.id !== result.run.id)].slice(0, 25));
      setSelectedDistributionRunId(result.run.id);
      setError(null);
      await refreshDistributions();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshDistributions().catch(() => undefined);
    }
  }

  async function handleStopDistributionRun(runId: string): Promise<void> {
    try {
      const result = await stopDistributionRun(runId);
      setDistributionSummary(result.summary);
      setDistributionRuns((current) => [result.run, ...current.filter((run) => run.id !== result.run.id)].slice(0, 25));
      setSelectedDistributionRunId(result.run.id);
      setError(null);
      await refreshDistributions();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
      await refreshDistributions().catch(() => undefined);
    }
  }

  return (
    <div className="min-h-screen bg-deck-950 text-slate-100">
      <div className="screen-grid" />
      <div className="relative mx-auto flex min-h-screen max-w-[1600px] gap-4 px-4 py-4">
        <aside className="hidden w-64 shrink-0 lg:block">
          <div className="sticky top-4 space-y-4">
            <BrandPanel status={status} />
            <Nav activeView={activeView} setActiveView={setActiveView} />
          </div>
        </aside>

        <main className="flex min-w-0 flex-1 flex-col gap-4">
          <TopBar
            projects={projects}
            selectedSlug={selectedSlug}
            setSelectedSlug={setSelectedSlug}
            activeView={activeView}
            setActiveView={setActiveView}
          />
          {error ? <Alert message={error} /> : null}
          {!detail || !selectedProject ? (
            <LoadingPanel />
          ) : (
            <View
              view={activeView}
              detail={detail}
              projects={projects}
              selectedSlug={selectedSlug}
              setSelectedSlug={setSelectedSlug}
              setActiveView={setActiveView}
              onRunScan={handleRunScan}
              onRunAction={handleRunAction}
              onStopRun={handleStopRun}
              activeRun={activeRun}
              selectedRun={selectedRun}
              onSelectRun={(run) => setSelectedRunId(run.id)}
              runs={runs}
              scans={scanHistory}
              scanBusy={scanBusy}
              settings={settings}
              onSaveSettings={handleSaveSettings}
              jarManifest={jarManifest}
              readiness={readiness}
              featureCatalog={featureCatalog}
              modpackSummary={modpackSummary}
              modpackRuns={modpackRuns}
              operationalReports={operationalReports}
              reportJobs={reportJobs}
              onRefreshReports={refreshOperationalReports}
              onStartReportJob={handleStartReportJob}
              onStopReportJob={handleStopReportJob}
              selectedModpackRunId={selectedModpackRunId}
              onSelectModpackRun={(run) => setSelectedModpackRunId(run.id)}
              onRebuildModpack={handleRebuildModpack}
              onStopModpackRun={handleStopModpackRun}
              onRefreshModpack={refreshModpack}
              distributionSummary={distributionSummary}
              distributionRuns={distributionRuns}
              selectedDistributionRunId={selectedDistributionRunId}
              onSelectDistributionRun={(run) => setSelectedDistributionRunId(run.id)}
              onExportDistribution={handleExportDistribution}
              onStopDistributionRun={handleStopDistributionRun}
              onRefreshDistributions={refreshDistributions}
              onRefreshJars={refreshAll}
              onBuildJars={handleBuildJars}
              onPromoteJars={handlePromoteJars}
            />
          )}
        </main>
      </div>
    </div>
  );
}

function View({
  view,
  detail,
  projects,
  selectedSlug,
  setSelectedSlug,
  setActiveView,
  onRunScan,
  onRunAction,
  onStopRun,
  activeRun,
  selectedRun,
  onSelectRun,
  runs,
  scans,
  scanBusy,
  settings,
  onSaveSettings,
  jarManifest,
  readiness,
  featureCatalog,
  modpackSummary,
  modpackRuns,
  operationalReports,
  reportJobs,
  onRefreshReports,
  onStartReportJob,
  onStopReportJob,
  selectedModpackRunId,
  onSelectModpackRun,
  onRebuildModpack,
  onStopModpackRun,
  onRefreshModpack,
  distributionSummary,
  distributionRuns,
  selectedDistributionRunId,
  onSelectDistributionRun,
  onExportDistribution,
  onStopDistributionRun,
  onRefreshDistributions,
  onRefreshJars,
  onBuildJars,
  onPromoteJars
}: {
  view: ViewKey;
  detail: ProjectDetail;
  projects: Project[];
  selectedSlug: string;
  setSelectedSlug: (slug: string) => void;
  setActiveView: (view: ViewKey) => void;
  onRunScan: (mode: ScanMode) => Promise<void>;
  onRunAction: (action: ReleaseAction, confirmed?: boolean) => Promise<void>;
  onStopRun: (runId: string) => Promise<void>;
  activeRun: CommandRun | null;
  selectedRun: CommandRun | null;
  onSelectRun: (run: CommandRun) => void;
  runs: CommandRun[];
  scans: ScanReport[];
  scanBusy: ScanMode | null;
  settings: AppSettings;
  onSaveSettings: (settings: AppSettings) => Promise<void>;
  jarManifest: JarManifest | null;
  readiness: ReadinessReport | null;
  featureCatalog: FeatureCatalogResponse | null;
  modpackSummary: ModpackInventory | null;
  modpackRuns: ModpackPipelineRun[];
  operationalReports: EchoOperationalReports | null;
  reportJobs: EchoReportJobSummary | null;
  onRefreshReports: () => Promise<void>;
  onStartReportJob: (commandId: string) => Promise<void>;
  onStopReportJob: (runId: string) => Promise<void>;
  selectedModpackRunId: string | null;
  onSelectModpackRun: (run: ModpackPipelineRun) => void;
  onRebuildModpack: (confirmed?: boolean) => Promise<void>;
  onStopModpackRun: (runId: string) => Promise<void>;
  onRefreshModpack: () => Promise<void>;
  distributionSummary: DistributionSummary | null;
  distributionRuns: CommandRun[];
  selectedDistributionRunId: string | null;
  onSelectDistributionRun: (run: CommandRun) => void;
  onExportDistribution: (packId: DistributionPack["id"], targetModsDir: string, buildFirst: boolean, includeOptionalModules: boolean) => Promise<void>;
  onStopDistributionRun: (runId: string) => Promise<void>;
  onRefreshDistributions: () => Promise<void>;
  onRefreshJars: () => Promise<void>;
  onBuildJars: (confirmed?: boolean) => Promise<void>;
  onPromoteJars: (confirmed?: boolean) => Promise<void>;
}): JSX.Element {
  if (view === "projects") {
    return <ProjectsView projects={projects} selectedSlug={selectedSlug} setSelectedSlug={setSelectedSlug} setActiveView={setActiveView} />;
  }
  if (view === "roadmap") return <RoadmapView phases={detail.roadmap} />;
  if (view === "features") return <FeaturesView detail={detail} catalog={featureCatalog} />;
  if (view === "qa") return <QaView detail={detail} scans={scans} readiness={readiness} onRunScan={onRunScan} scanBusy={scanBusy} setActiveView={setActiveView} />;
  if (view === "prompts") return <PromptsView detail={detail} category="Codex QA" />;
  if (view === "bridge") return <BridgeView />;
  if (view === "release") {
    return <ReleaseView detail={detail} onRunAction={onRunAction} onStopRun={onStopRun} activeRun={activeRun} selectedRun={selectedRun} onSelectRun={onSelectRun} runs={runs} settings={settings} />;
  }
  if (view === "distributions") {
    return (
      <DistributionsView
        summary={distributionSummary}
        runs={distributionRuns}
        selectedRunId={selectedDistributionRunId}
        defaultTargetModsDir={settings.modpackModsDir}
        onSelectRun={onSelectDistributionRun}
        onExport={onExportDistribution}
        onStopRun={onStopDistributionRun}
        onRefresh={onRefreshDistributions}
      />
    );
  }
  if (view === "modpack") {
    return (
      <ModpackView
        summary={modpackSummary}
        runs={modpackRuns}
        selectedRunId={selectedModpackRunId}
        onSelectRun={onSelectModpackRun}
        onRebuild={onRebuildModpack}
        onStopRun={onStopModpackRun}
        onRefresh={onRefreshModpack}
        setActiveView={setActiveView}
      />
    );
  }
  if (view === "jars") {
    return (
      <JarsView
        detail={detail}
        settings={settings}
        manifest={jarManifest}
        activeRun={activeRun}
        selectedRun={selectedRun}
        onSelectRun={onSelectRun}
        runs={runs}
        onRefresh={onRefreshJars}
        onBuild={onBuildJars}
        onPromote={onPromoteJars}
        onStopRun={onStopRun}
      />
    );
  }
  if (view === "terminal") return <TerminalPlannerView groups={detail.terminalPlanner} />;
  if (view === "assets") return <PromptsView detail={detail} category="Asset Prompt" />;
  if (view === "exports") return <ExportsView detail={detail} />;
  if (view === "settings") return <SettingsView settings={settings} onSave={onSaveSettings} />;
  return <DashboardView detail={detail} scans={scans} runs={runs} readiness={readiness} featureCatalog={featureCatalog} modpackSummary={modpackSummary} operationalReports={operationalReports} reportJobs={reportJobs} onRefreshReports={onRefreshReports} onStartReportJob={onStartReportJob} onStopReportJob={onStopReportJob} onRunScan={onRunScan} scanBusy={scanBusy} setActiveView={setActiveView} />;
}

function ProjectsView({
  projects,
  selectedSlug,
  setSelectedSlug,
  setActiveView
}: {
  projects: Project[];
  selectedSlug: string;
  setSelectedSlug: (slug: string) => void;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const groups = groupProjects(projects);
  return (
    <div className="space-y-6">
      <PageHeader icon={Boxes} title="Projects" description="Open a real workspace project, review its current readiness signals, and jump into the next operational action." />
      {groups.length === 0 ? <EmptyState title="No projects synced" detail="Seed data did not return any project cards." /> : null}
      {groups.map((group) => (
        <section key={group.label} className="space-y-3">
          <div className="flex items-center gap-2 px-1">
            <Boxes className="h-5 w-5 text-signal-cyan" />
            <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-300">{group.label}</h2>
          </div>
          <div className="grid gap-4 xl:grid-cols-2">
            {group.projects.map((project) => (
              <article key={project.slug} className={`surface project-card p-5 ${project.slug === selectedSlug ? "project-card-selected" : ""}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{project.kind}</p>
                    <h3 className="mt-1 truncate text-2xl font-semibold text-white">{project.name}</h3>
                  </div>
                  <span className="status-pill" style={{ borderColor: project.accent, color: project.accent }}>
                    {project.status}
                  </span>
                </div>
                <p className="mt-3 line-clamp-2 text-sm text-slate-300">{project.description}</p>
                <div className="mt-5 grid gap-3 sm:grid-cols-4">
                  <Metric label="Milestone" value={project.currentMilestone} />
                  <Metric label="Build Health" value={`${project.buildHealth}%`} />
                  <Metric label="Critical" value={project.criticalIssues} tone="red" />
                  <Metric label="Polish" value={project.polishTasks} tone="amber" />
                </div>
                <div className="mt-5 flex flex-col gap-3 border-t border-deck-line pt-4 sm:flex-row sm:items-center sm:justify-between">
                  <p className="min-w-0 text-sm text-slate-300">
                    <span className="text-slate-500">Next:</span> {project.nextRecommendedAction}
                  </p>
                  <button
                    className="primary-button"
                    onClick={() => {
                      setSelectedSlug(project.slug);
                      setActiveView("dashboard");
                    }}
                  >
                    <Gauge className="h-4 w-4" />
                    Open
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function groupProjects(projects: Project[]): Array<{ label: string; projects: Project[] }> {
  const laneOrder = ["Full Stack", "Core Module", "Root Module", "Beta/Dev Module", "Release Module", "Standalone Mod", "Tooling/Example"];
  return laneOrder
    .map((label) => ({
      label,
      projects: projects.filter((project) => project.kind === label)
    }))
    .filter((group) => group.projects.length > 0);
}

function uniqueSourceCount(features: FeatureRecord[]): number {
  return new Set(features.flatMap((feature) => feature.sources.map((source) => `${source.path}#${source.section}`))).size;
}

function DashboardView({
  detail,
  scans,
  runs,
  readiness,
  featureCatalog,
  modpackSummary,
  operationalReports,
  reportJobs,
  onRefreshReports,
  onStartReportJob,
  onStopReportJob,
  onRunScan,
  scanBusy,
  setActiveView
}: {
  detail: ProjectDetail;
  scans: ScanReport[];
  runs: CommandRun[];
  readiness: ReadinessReport | null;
  featureCatalog: FeatureCatalogResponse | null;
  modpackSummary: ModpackInventory | null;
  operationalReports: EchoOperationalReports | null;
  reportJobs: EchoReportJobSummary | null;
  onRefreshReports: () => Promise<void>;
  onStartReportJob: (commandId: string) => Promise<void>;
  onStopReportJob: (runId: string) => Promise<void>;
  onRunScan: (mode: ScanMode) => Promise<void>;
  scanBusy: ScanMode | null;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const latest = detail.latestReport;
  const failedRuns = runs.filter((run) => run.status === "failed").length;
  const readinessScore = readiness?.score ?? latest?.summary.readinessScore ?? detail.project.buildHealth;
  const featureSummary = featureCatalog?.summary;
  const implementedFeatures = featureSummary?.statusCounts.implemented ?? 0;
  const incompleteFeatures = featureSummary ? featureSummary.total - implementedFeatures : 0;
  return (
    <div className="space-y-4">
      <section className="surface page-header p-5">
        <div className="min-w-0">
          <p className="eyebrow text-signal-cyan">{detail.project.kind}</p>
          <h2 className="mt-1 break-words text-3xl font-semibold text-white">{detail.project.name}</h2>
          <p className="mt-3 max-w-4xl text-sm leading-6 text-slate-300">{detail.project.description}</p>
          <p className="mt-3 break-all font-mono text-xs text-slate-500">{detail.project.workspacePath}</p>
        </div>
          <button className="primary-button" disabled={Boolean(scanBusy)} onClick={() => onRunScan("quick")}>
            <RefreshCw className={`h-4 w-4 ${scanBusy ? "animate-spin" : ""}`} />
            Quick Scan
          </button>
      </section>
      <section className="surface p-4">
        <div className="grid gap-3 md:grid-cols-4">
          <Metric label="To 100%" value={`${readinessScore}%`} tone={readinessScore === 100 ? "green" : "amber"} />
          <Metric label="Scan Status" value={latest?.status ?? "No scan"} />
          <Metric label="Critical Issues" value={latest?.summary.criticalIssues ?? detail.project.criticalIssues} tone="red" />
          <Metric label="Failed Runs" value={failedRuns} tone={failedRuns ? "red" : "green"} />
        </div>
      </section>

      <OperationalReportsPanel reports={operationalReports} reportJobs={reportJobs} onRefreshReports={onRefreshReports} onStartReportJob={onStartReportJob} onStopReportJob={onStopReportJob} setActiveView={setActiveView} />

      <ReadinessChecklistPanel readiness={readiness} setActiveView={setActiveView} />

      <section className="surface p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <SectionTitle icon={FileText} title="Feature Implementation" />
            <p className="mt-2 max-w-4xl text-sm leading-6 text-slate-400">
              Curated from lore docs, mod plans, release notes, and concrete project evidence so the selected project can say what is built, what is partial, and what still needs proof.
            </p>
          </div>
          <button className="secondary-button justify-center" onClick={() => setActiveView("features")}>
            <FileText className="h-4 w-4" />
            Open Features
          </button>
        </div>
        <div className="mt-4 grid gap-3 md:grid-cols-4">
          <Metric label="Feature Rows" value={featureSummary?.total ?? 0} />
          <Metric label="Implemented" value={implementedFeatures} tone="green" />
          <Metric label="Still Needs Proof" value={incompleteFeatures} tone={incompleteFeatures ? "amber" : "green"} />
          <Metric label="Sources" value={featureCatalog ? uniqueSourceCount(featureCatalog.features) : 0} />
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="surface p-5">
          <SectionTitle icon={ListChecks} title="Recommended Actions" />
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <ActionTile icon={ShieldCheck} label="Open QA findings" detail={detail.project.nextRecommendedAction} onClick={() => setActiveView("qa")} />
            <ActionTile icon={FileText} label="Review features" detail={`${featureSummary?.total ?? 0} lore-backed feature row(s) for this project.`} onClick={() => setActiveView("features")} />
            <ActionTile icon={Rocket} label="Build release stack" detail="Confirm and run allowlisted release commands." onClick={() => setActiveView("release")} />
            <ActionTile icon={PackageCheck} label="Update modpack" detail={modpackSummary?.blockers[0] ?? `${modpackSummary?.summary.current ?? 0}/${modpackSummary?.summary.expected ?? 0} managed jar(s) current.`} onClick={() => setActiveView("modpack")} />
            <ActionTile icon={Package} label="Manage jars" detail="Build, compare, quarantine, promote, and verify expected mod jars." onClick={() => setActiveView("jars")} />
            <ActionTile icon={Settings} label="Review settings" detail="Check Python and mods folder paths before deep scans." onClick={() => setActiveView("settings")} />
            <ActionTile icon={Download} label="Export report" detail={`${scans.length} scan report(s), ${runs.length} release run(s).`} onClick={() => setActiveView("exports")} />
          </div>
        </div>
        <div className="surface p-5">
          <SectionTitle icon={Database} title="Workspace Modules" />
          <div className="mt-4 space-y-2">
            {detail.project.modules.map((module) => (
              <div key={module.modId} className="module-row">
                <span className="truncate">{module.label}</span>
                <span className="font-mono text-xs text-slate-400">{module.version}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

function OperationalReportsPanel({
  reports,
  reportJobs,
  onRefreshReports,
  onStartReportJob,
  onStopReportJob,
  setActiveView
}: {
  reports: EchoOperationalReports | null;
  reportJobs: EchoReportJobSummary | null;
  onRefreshReports: () => Promise<void>;
  onStartReportJob: (commandId: string) => Promise<void>;
  onStopReportJob: (runId: string) => Promise<void>;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const [selectedReportKey, setSelectedReportKey] = useState<string | null>(null);
  const [drilldown, setDrilldown] = useState<EchoReportDrilldown | null>(null);
  const [drilldownLoading, setDrilldownLoading] = useState(false);
  const [drilldownError, setDrilldownError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [copiedCommandId, setCopiedCommandId] = useState<string | null>(null);
  const [jobStartingId, setJobStartingId] = useState<string | null>(null);
  const [jobStoppingId, setJobStoppingId] = useState<string | null>(null);
  const [selectedReportJobId, setSelectedReportJobId] = useState<string | null>(null);
  const [reportJobDetail, setReportJobDetail] = useState<EchoReportJobDetail | null>(null);
  const [reportJobDetailLoading, setReportJobDetailLoading] = useState(false);
  const [reportJobDetailError, setReportJobDetailError] = useState<string | null>(null);

  useEffect(() => {
    if (!reports) {
      setSelectedReportKey(null);
      return;
    }
    const availableKeys = new Set(reports.artifacts.map((artifact) => artifact.key));
    if (!selectedReportKey || !availableKeys.has(selectedReportKey)) {
      setSelectedReportKey(reports.panels[0]?.reportKeys[0] ?? reports.artifacts[0]?.key ?? null);
    }
  }, [reports, selectedReportKey]);

  useEffect(() => {
    if (!selectedReportKey) {
      setDrilldown(null);
      setDrilldownError(null);
      return;
    }
    let cancelled = false;
    setDrilldownLoading(true);
    getEchoReportDrilldown(selectedReportKey)
      .then((loaded) => {
        if (cancelled) return;
        setDrilldown(loaded);
        setDrilldownError(null);
      })
      .catch((cause) => {
        if (cancelled) return;
        setDrilldown(null);
        setDrilldownError(cause instanceof Error ? cause.message : String(cause));
      })
      .finally(() => {
        if (!cancelled) setDrilldownLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedReportKey, reports?.generatedAt]);

  useEffect(() => {
    if (!reportJobs?.runs.length) {
      setSelectedReportJobId(null);
      return;
    }
    if (!selectedReportJobId || !reportJobs.runs.some((run) => run.id === selectedReportJobId)) {
      setSelectedReportJobId(reportJobs.runs[0].id);
    }
  }, [reportJobs, selectedReportJobId]);

  useEffect(() => {
    if (!selectedReportJobId) {
      setReportJobDetail(null);
      setReportJobDetailError(null);
      return;
    }
    let cancelled = false;
    setReportJobDetailLoading(true);
    getEchoReportJobDetail(selectedReportJobId)
      .then((loaded) => {
        if (cancelled) return;
        setReportJobDetail(loaded);
        setReportJobDetailError(null);
      })
      .catch((cause) => {
        if (cancelled) return;
        setReportJobDetail(null);
        setReportJobDetailError(cause instanceof Error ? cause.message : String(cause));
      })
      .finally(() => {
        if (!cancelled) setReportJobDetailLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedReportJobId, reportJobs?.generatedAt]);

  async function handleRefreshReports(): Promise<void> {
    setRefreshing(true);
    try {
      await onRefreshReports();
    } finally {
      setRefreshing(false);
    }
  }

  async function handleCopyCommand(commandId: string, command: string): Promise<void> {
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(command);
      }
    } catch {
      // Clipboard permissions vary by local browser context; the command remains visible for manual copy.
    }
    setCopiedCommandId(commandId);
    window.setTimeout(() => setCopiedCommandId((current) => (current === commandId ? null : current)), 1800);
  }

  async function handleStartJob(commandId: string): Promise<void> {
    setJobStartingId(commandId);
    try {
      await onStartReportJob(commandId);
    } finally {
      setJobStartingId(null);
    }
  }

  async function handleStopJob(runId: string): Promise<void> {
    setJobStoppingId(runId);
    try {
      await onStopReportJob(runId);
    } finally {
      setJobStoppingId(null);
    }
  }

  if (!reports) {
    return (
      <section className="surface p-5">
        <SectionTitle icon={Database} title="Operational Reports" />
        <EmptyState icon={Database} title="Reports loading" detail="Command Center is waiting for reports/echo ingestion." />
      </section>
    );
  }
  const summary = reports.summary;
  const statusTone = operationalStatusTone(reports.status);
  const statusClass = toneTextClass(statusTone);
  const issueDetail = reports.missingReports.length || reports.invalidReports.length
    ? `${reports.missingReports.length} missing, ${reports.invalidReports.length} invalid report artifact(s). Run .\\gradlew echoPackDoctor -PechoPack=ashfall -PechoAddonSet=beta.`
    : reports.recommendations[0] ?? "Operational reports are loaded.";
  const runningReportJob = reportJobs?.runningRun ?? null;
  const runningDefinition = runningReportJob
    ? reportJobs?.definitions.find((definition) => definition.id === runningReportJob.commandId) ?? null
    : null;
  const runningReportKeys = new Set(runningDefinition?.reportKeys ?? []);
  return (
    <section className="surface p-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <SectionTitle icon={Database} title="Operational Reports" />
          <p className="mt-2 text-sm leading-6 text-slate-400">
            <span className={`font-semibold ${statusClass}`}>{reports.status}</span>
            <span className="text-slate-500"> / {reports.reportRoot} / {reports.generatedAt}</span>
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <button className="secondary-button justify-center" disabled={refreshing} onClick={() => void handleRefreshReports()}>
            <RefreshCw className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
            Refresh
          </button>
          <button className="secondary-button justify-center" onClick={() => setActiveView("modpack")}>
            <PackageCheck className="h-4 w-4" />
            PackOS
          </button>
          <button className="secondary-button justify-center" onClick={() => setActiveView("qa")}>
            <ShieldCheck className="h-4 w-4" />
            Diagnostics
          </button>
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <Metric label="PackOS" value={summary.packOsStatus} tone={operationalStatusTone(reports.status)} detail={`lockfile ${summary.lockfileStatus}`} />
        <Metric label="Health" value={summary.healthStatus} tone={summary.healthStatus === "warning_without_runtime" ? "amber" : operationalStatusTone(summary.healthStatus)} detail={summary.installStateStatus} />
        <Metric label="Recovery" value={summary.recoveryMode} tone={summary.recoveryMode === "normal" ? "green" : "amber"} detail={summary.recoveryPlanStatus} />
        <Metric label="Diagnostics" value={summary.diagnosticsCount} tone={summary.blockingDiagnostics ? "red" : summary.warningDiagnostics ? "amber" : "green"} detail={`${summary.blockingDiagnostics} blocking`} />
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <Metric label="Reports" value={`${reports.reportCounts.loaded}/${reports.reportCounts.total}`} tone={reports.reportCounts.invalid ? "red" : reports.reportCounts.missing ? "amber" : "green"} detail={`${reports.reportCounts.missing} missing`} />
        <Metric label="Issues" value={reports.reportCounts.issues} tone={reports.reportCounts.blocking || reports.reportCounts.errors || reports.reportCounts.fatals ? "red" : reports.reportCounts.warnings ? "amber" : "green"} detail={`${reports.reportCounts.warnings} warnings`} />
        <Metric label="Categories" value={reports.categories.length} tone={reports.categories.some((category) => category.status === "blocked") ? "red" : reports.categories.some((category) => category.status === "degraded") ? "amber" : "green"} />
        <Metric label="Report Jobs" value={runningReportJob ? "running" : reportJobs?.runs.length ?? 0} tone={runningReportJob ? "amber" : "green"} detail={runningReportJob?.commandId ?? `${reportJobs?.definitions.length ?? 0} safe jobs`} />
      </div>

      <InfoBanner
        tone={reports.status === "blocked" ? "red" : reports.status === "ready" ? "green" : "amber"}
        title={summary.safeForLauncher ? "Launcher handoff is report-safe" : "Launcher handoff needs review"}
        detail={issueDetail}
      />

      <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {reports.panels.map((panel) => (
          <OperationalReportPanelCard
            key={panel.id}
            panel={panel}
            running={panel.reportKeys.some((key) => runningReportKeys.has(key))}
            safeJob={safeJobForPanel(panel, reportJobs)}
            jobDisabled={Boolean(runningReportJob || jobStartingId)}
            jobStarting={jobStartingId}
            selectedReportKey={selectedReportKey}
            onSelectReport={setSelectedReportKey}
            onStartJob={handleStartJob}
          />
        ))}
      </div>

      <div className="mt-4 grid gap-2 md:grid-cols-2 xl:grid-cols-4">
        {reports.categories.map((category) => {
          const tone = operationalStatusTone(category.status);
          return (
            <button key={category.key} className="module-row text-left" type="button" onClick={() => setSelectedReportKey(category.reportKeys[0] ?? null)}>
              <div className="min-w-0">
                <p className="truncate text-sm text-white">{category.label}</p>
                <p className="truncate font-mono text-xs text-slate-500">
                  {category.loaded} loaded / {category.missing} missing / {category.issueCount} issue(s)
                </p>
              </div>
              <span className={`status-pill ${toneBorderClass(tone)}`}>{category.status}</span>
            </button>
          );
        })}
      </div>

      <div className="mt-4 grid gap-2 md:grid-cols-2 xl:grid-cols-4">
        {reports.artifacts.map((artifact) => {
          const tone = reportArtifactTone(artifact.status);
          return (
            <button key={artifact.key} className={`module-row text-left ${artifact.key === selectedReportKey ? "report-artifact-row-active" : ""}`} type="button" onClick={() => setSelectedReportKey(artifact.key)}>
              <div className="min-w-0">
                <p className="truncate text-sm text-white">{artifact.label ?? artifact.fileName}</p>
                <p className="truncate font-mono text-xs text-slate-500">{artifact.error ?? artifact.reportStatus ?? artifact.reportPath}</p>
              </div>
              <span className={`status-pill ${toneBorderClass(tone)}`}>{artifact.status}</span>
            </button>
          );
        })}
      </div>

      <OperationalReportJobsPanel
        reportJobs={reportJobs}
        selectedReportKey={selectedReportKey}
        selectedReportJobId={selectedReportJobId}
        detail={reportJobDetail}
        detailLoading={reportJobDetailLoading}
        detailError={reportJobDetailError}
        jobStartingId={jobStartingId}
        jobStoppingId={jobStoppingId}
        onSelectJob={setSelectedReportJobId}
        onStartJob={handleStartJob}
        onStopJob={handleStopJob}
      />

      <OperationalReportDrilldownPanel
        drilldown={drilldown}
        loading={drilldownLoading}
        error={drilldownError}
        copiedCommandId={copiedCommandId}
        onCopyCommand={handleCopyCommand}
      />

      {reports.topIssues.length ? (
        <div className="mt-4 space-y-2">
          {reports.topIssues.slice(0, 6).map((issue, index) => {
            const severityTone = issue.severity === "FATAL" || issue.severity === "ERROR" ? "red" : issue.severity === "WARNING" ? "amber" : "green";
            return (
              <div key={`${issue.reportKey}-${issue.code}-${index}`} className="finding-row">
                <span className={`status-pill ${toneBorderClass(severityTone)}`}>{issue.severity}</span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                    <p className="truncate text-sm font-semibold text-white">{issue.title}</p>
                    <span className="font-mono text-xs text-slate-500">{issue.reportPath}</span>
                  </div>
                  <p className="mt-1 text-sm leading-6 text-slate-400">{issue.summary}</p>
                  {issue.relatedPaths.length ? <p className="mt-1 truncate font-mono text-xs text-slate-500">{issue.relatedPaths.join(", ")}</p> : null}
                </div>
              </div>
            );
          })}
        </div>
      ) : null}
    </section>
  );
}

function OperationalReportPanelCard({
  panel,
  running,
  safeJob,
  jobDisabled,
  jobStarting,
  selectedReportKey,
  onSelectReport,
  onStartJob
}: {
  panel: EchoReportPanel;
  running: boolean;
  safeJob: EchoReportJobDefinition | null;
  jobDisabled: boolean;
  jobStarting: string | null;
  selectedReportKey: string | null;
  onSelectReport: (reportKey: string | null) => void;
  onStartJob: (commandId: string) => Promise<void>;
}): JSX.Element {
  const Icon = reportPanelIcon(panel.id);
  const tone: Tone = running ? "blue" : operationalStatusTone(panel.status);
  const selected = selectedReportKey ? panel.reportKeys.includes(selectedReportKey) : false;
  return (
    <article className={`report-panel-row text-left ${selected ? "report-panel-row-active" : ""}`}>
      <div className="flex min-w-0 items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <Icon className="h-4 w-4 shrink-0 text-signal-cyan" />
            <h3 className="truncate text-sm font-semibold text-white">{panel.title}</h3>
          </div>
          <p className="mt-2 break-words text-lg font-semibold text-white">{panel.primary}</p>
          <p className="mt-1 truncate text-xs text-slate-500">{panel.secondary}</p>
        </div>
        <span className={`status-pill ${toneBorderClass(tone)}`}>{running ? "running" : panel.status}</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {panel.metrics.slice(0, 4).map((metric) => (
          <span key={`${panel.id}-${metric.label}`} className={`chip ${metric.tone ? toneTextClass(metric.tone) : ""}`}>
            {metric.label}: {metric.value}
          </span>
        ))}
      </div>
      <p className="mt-3 line-clamp-2 font-mono text-xs text-slate-500">{panel.detail}</p>
      <div className="mt-4 flex flex-col gap-2 sm:flex-row">
        <button className="secondary-button justify-center" type="button" onClick={() => onSelectReport(panel.reportKeys[0] ?? null)}>
          <FileJson className="h-4 w-4" />
          Inspect
        </button>
        {safeJob ? (
          <button className="secondary-button justify-center" type="button" disabled={jobDisabled} onClick={() => void onStartJob(safeJob.id)}>
            {jobStarting === safeJob.id || running ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
            {jobStarting === safeJob.id || running ? "Running" : safeJob.label}
          </button>
        ) : null}
      </div>
    </article>
  );
}

function OperationalReportJobsPanel({
  reportJobs,
  selectedReportKey,
  selectedReportJobId,
  detail,
  detailLoading,
  detailError,
  jobStartingId,
  jobStoppingId,
  onSelectJob,
  onStartJob,
  onStopJob
}: {
  reportJobs: EchoReportJobSummary | null;
  selectedReportKey: string | null;
  selectedReportJobId: string | null;
  detail: EchoReportJobDetail | null;
  detailLoading: boolean;
  detailError: string | null;
  jobStartingId: string | null;
  jobStoppingId: string | null;
  onSelectJob: (runId: string | null) => void;
  onStartJob: (commandId: string) => Promise<void>;
  onStopJob: (runId: string) => Promise<void>;
}): JSX.Element {
  if (!reportJobs) {
    return (
      <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/45 p-4">
        <div className="flex items-center gap-3 text-sm text-slate-400">
          <RefreshCw className="h-4 w-4 animate-spin text-signal-cyan" />
          Loading report job history
        </div>
      </section>
    );
  }

  const runningRun = reportJobs.runningRun ?? null;
  const matchingDefinitions = selectedReportKey
    ? reportJobs.definitions.filter((definition) => definition.reportKeys.includes(selectedReportKey))
    : [];
  const definitions = matchingDefinitions.length ? matchingDefinitions : reportJobs.definitions;
  const visibleRuns = reportJobs.runs.slice(0, 8);

  return (
    <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/45 p-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <TerminalSquare className="h-5 w-5 shrink-0 text-signal-cyan" />
            <h3 className="truncate text-lg font-semibold text-white">Safe Report Jobs</h3>
            {runningRun ? <RefreshCw className="h-4 w-4 animate-spin text-signal-cyan" /> : null}
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-400">
            Allowlisted report regeneration only. Raw commands, launch tasks, downloads, jar deletes, config resets, save changes, and repair execution are rejected by the API.
          </p>
        </div>
        <span className={`status-pill ${toneBorderClass(runningRun ? "blue" : "gray")}`}>{runningRun ? "running" : "idle"}</span>
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-[0.95fr_1.05fr]">
        <div className="rounded-lg border border-deck-line bg-black/20 p-3">
          <h4 className="text-sm font-semibold text-white">Allowlisted Actions</h4>
          <div className="mt-3 grid gap-3 md:grid-cols-2">
            {definitions.map((definition) => {
              const isRunning = runningRun?.commandId === definition.id;
              const disabled = Boolean(runningRun || jobStartingId);
              return (
                <div key={definition.id} className="rounded-lg border border-deck-line bg-deck-950/45 p-3">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-white">{definition.label}</p>
                      <p className="mt-1 line-clamp-2 text-xs text-slate-400">{definition.description}</p>
                      <p className="mt-2 font-mono text-xs text-slate-500">{definition.reportKeys.join(", ")}</p>
                    </div>
                    <button className="secondary-button shrink-0 justify-center" type="button" disabled={disabled} onClick={() => void onStartJob(definition.id)}>
                      {isRunning || jobStartingId === definition.id ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
                      {isRunning ? "Running" : "Run"}
                    </button>
                  </div>
                  <pre className="command-preview mt-3">{definition.command.join(" ")}</pre>
                </div>
              );
            })}
          </div>
        </div>

        <div className="rounded-lg border border-deck-line bg-black/20 p-3">
          <div className="flex items-center justify-between gap-3">
            <h4 className="text-sm font-semibold text-white">Job History</h4>
            <span className="font-mono text-xs text-slate-500">{reportJobs.generatedAt}</span>
          </div>
          <div className="mt-3 space-y-2">
            {visibleRuns.map((run) => {
              const tone = reportJobTone(run.status);
              return (
                <div key={run.id} className={`module-row items-start ${selectedReportJobId === run.id ? "report-artifact-row-active" : ""}`}>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                      <p className="truncate text-sm font-semibold text-white">{run.commandId}</p>
                      <span className={`status-pill ${toneBorderClass(tone)}`}>{run.status}</span>
                    </div>
                    <p className="mt-1 truncate font-mono text-xs text-slate-500">{run.command.join(" ")}</p>
                    <p className={`mt-1 text-xs ${commandStatusClass(run.status)}`}>
                      {formatDate(run.startedAt)} / {run.durationMs == null ? "running" : `${Math.round(run.durationMs / 1000)}s`}
                    </p>
                  </div>
                  <button className="secondary-button shrink-0 justify-center" type="button" onClick={() => onSelectJob(run.id)}>
                    <FileText className="h-4 w-4" />
                    Detail
                  </button>
                  {run.status === "running" ? (
                    <button className="danger-button shrink-0 justify-center" type="button" disabled={jobStoppingId === run.id} onClick={() => void onStopJob(run.id)}>
                      {jobStoppingId === run.id ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Square className="h-4 w-4" />}
                      Stop
                    </button>
                  ) : null}
                </div>
              );
            })}
            {!visibleRuns.length ? <EmptyState icon={TerminalSquare} title="No report jobs yet" detail="Start a safe report job to populate local job history." /> : null}
          </div>
        </div>
      </div>

      <ReportJobDetailPanel detail={detail} loading={detailLoading} error={detailError} />
    </section>
  );
}

function ReportJobDetailPanel({
  detail,
  loading,
  error
}: {
  detail: EchoReportJobDetail | null;
  loading: boolean;
  error: string | null;
}): JSX.Element {
  if (error) {
    return <InfoBanner tone="red" title="Report job detail unavailable" detail={error} />;
  }
  if (loading && !detail) {
    return (
      <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/50 p-4">
        <div className="flex items-center gap-3 text-sm text-slate-400">
          <RefreshCw className="h-4 w-4 animate-spin text-signal-cyan" />
          Loading report job detail
        </div>
      </section>
    );
  }
  if (!detail) {
    return <EmptyState icon={TerminalSquare} title="No report job selected" detail="Run or select a safe report job to inspect its sanitized command and output preview." />;
  }

  const tone = reportJobTone(detail.run.status);
  return (
    <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/45 p-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <TerminalSquare className="h-5 w-5 shrink-0 text-signal-cyan" />
            <h3 className="truncate text-lg font-semibold text-white">{detail.definition.label}</h3>
            {loading ? <RefreshCw className="h-4 w-4 animate-spin text-slate-500" /> : null}
          </div>
          <p className="mt-2 break-all font-mono text-xs text-slate-500">{detail.run.id}</p>
        </div>
        <span className={`status-pill ${toneBorderClass(tone)}`}>{detail.run.status}</span>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <Metric label="Command" value={detail.run.commandId} tone={detail.run.status === "failed" ? "red" : detail.run.status === "succeeded" ? "green" : "amber"} />
        <Metric label="Reports" value={detail.reportKeys.length} detail={detail.reportKeys.slice(0, 2).join(", ")} />
        <Metric label="Duration" value={detail.run.durationMs == null ? "running" : `${Math.round(detail.run.durationMs / 1000)}s`} />
        <Metric label="Exit" value={detail.run.exitCode ?? "pending"} tone={detail.run.exitCode === 0 ? "green" : detail.run.exitCode == null ? "amber" : "red"} />
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-[0.85fr_1.15fr]">
        <div className="rounded-lg border border-deck-line bg-black/20 p-3">
          <h4 className="text-sm font-semibold text-white">Sanitized Command</h4>
          <pre className="command-preview mt-3">{detail.command.join(" ")}</pre>
          <div className="mt-3 flex flex-wrap gap-2">
            {detail.reportKeys.map((key) => <span key={key} className="chip">{key}</span>)}
          </div>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            <span className="chip text-signal-green">raw commands rejected</span>
            <span className="chip text-signal-green">no Minecraft launch</span>
            <span className="chip text-signal-green">no repairs executed</span>
            <span className="chip text-signal-green">no game-file mutation</span>
          </div>
          {detail.failureSummary ? (
            <div className="mt-3 rounded-lg border border-signal-red/40 bg-signal-red/10 p-3">
              <p className="text-xs uppercase tracking-[0.14em] text-signal-red">Failure Summary</p>
              <pre className="mt-2 whitespace-pre-wrap break-words font-mono text-xs text-slate-200">{detail.failureSummary}</pre>
            </div>
          ) : null}
        </div>

        <div className="rounded-lg border border-deck-line bg-black/20 p-3">
          <div className="flex items-center justify-between gap-3">
            <h4 className="text-sm font-semibold text-white">Output Preview</h4>
            <span className="font-mono text-xs text-slate-500">
              {detail.run.outputLineCount} line(s){detail.run.outputTruncated ? " / tail preview" : ""}
            </span>
          </div>
          <pre className="prompt-output mt-3 max-h-80 text-xs">{detail.run.outputPreview || "No output captured yet."}</pre>
        </div>
      </div>
    </section>
  );
}

function OperationalReportDrilldownPanel({
  drilldown,
  loading,
  error,
  copiedCommandId,
  onCopyCommand
}: {
  drilldown: EchoReportDrilldown | null;
  loading: boolean;
  error: string | null;
  copiedCommandId: string | null;
  onCopyCommand: (commandId: string, command: string) => Promise<void>;
}): JSX.Element {
  if (error) {
    return <InfoBanner tone="red" title="Report drilldown unavailable" detail={error} />;
  }
  if (loading && !drilldown) {
    return (
      <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/50 p-4">
        <div className="flex items-center gap-3 text-sm text-slate-400">
          <RefreshCw className="h-4 w-4 animate-spin text-signal-cyan" />
          Loading report drilldown
        </div>
      </section>
    );
  }
  if (!drilldown) {
    return <EmptyState icon={FileJson} title="No report selected" detail="Select a panel, category, or report artifact to inspect sanitized report details." />;
  }

  const tone = operationalStatusTone(drilldown.status);
  const summaryEntries = Object.entries(drilldown.summary);
  return (
    <section className="mt-4 rounded-lg border border-deck-line bg-deck-950/45 p-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <FileJson className="h-5 w-5 shrink-0 text-signal-cyan" />
            <h3 className="truncate text-lg font-semibold text-white">{drilldown.artifact.label ?? drilldown.artifact.fileName}</h3>
            {loading ? <RefreshCw className="h-4 w-4 animate-spin text-slate-500" /> : null}
          </div>
          <p className="mt-2 break-all font-mono text-xs text-slate-500">{drilldown.artifact.reportPath}</p>
        </div>
        <span className={`status-pill ${toneBorderClass(tone)}`}>{drilldown.status}</span>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <Metric label="Report" value={drilldown.artifact.status} tone={reportArtifactTone(drilldown.artifact.status)} detail={drilldown.artifact.reportStatus ?? drilldown.category} />
        <Metric label="Issues" value={drilldown.issues.length} tone={drilldown.issueCounts.FATAL || drilldown.issueCounts.ERROR ? "red" : drilldown.issueCounts.WARNING ? "amber" : "green"} detail={`${drilldown.issueCounts.WARNING ?? 0} warnings`} />
        <Metric label="Preview Keys" value={drilldown.dataPreview.length} />
        <Metric label="Safe Commands" value={drilldown.safeCommands.length} tone="green" detail="copy-only" />
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="rounded-lg border border-deck-line bg-deck-950/40 p-3">
          <h4 className="text-sm font-semibold text-white">Summary</h4>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            {summaryEntries.slice(0, 16).map(([key, value]) => (
              <div key={key} className="min-w-0 rounded-lg border border-deck-line bg-black/20 p-2">
                <p className="truncate text-xs uppercase text-slate-500">{key}</p>
                <p className="mt-1 break-words font-mono text-xs text-slate-300">{String(value)}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-lg border border-deck-line bg-deck-950/40 p-3">
          <h4 className="text-sm font-semibold text-white">Key Data</h4>
          <div className="mt-3 overflow-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-deck-950 text-xs uppercase tracking-[0.14em] text-slate-500">
                <tr>
                  <th className="px-3 py-2">Key</th>
                  <th className="px-3 py-2">Kind</th>
                  <th className="px-3 py-2">Preview</th>
                </tr>
              </thead>
              <tbody>
                {drilldown.dataPreview.map((entry) => (
                  <tr key={entry.key} className="border-t border-deck-line">
                    <td className="px-3 py-2 font-mono text-xs text-slate-300">{entry.key}</td>
                    <td className="px-3 py-2 text-slate-400">{entry.kind}</td>
                    <td className="px-3 py-2 break-words text-slate-400">{previewValue(entry)}</td>
                  </tr>
                ))}
                {!drilldown.dataPreview.length ? (
                  <tr>
                    <td className="px-3 py-4 text-slate-400" colSpan={3}>No report payload preview is available.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <OperationalIssueTable issues={drilldown.issues} />

      <div className="mt-4 grid gap-3 xl:grid-cols-[1fr_0.7fr]">
        <div className="rounded-lg border border-deck-line bg-deck-950/40 p-3">
          <h4 className="text-sm font-semibold text-white">Safe Report Actions</h4>
          <div className="mt-3 grid gap-3 md:grid-cols-2">
            {drilldown.safeCommands.map((command) => (
              <div key={command.id} className="rounded-lg border border-deck-line bg-black/20 p-3">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-white">{command.label}</p>
                    <p className="mt-1 text-xs text-slate-500">{command.executesInUi ? "UI execution" : "copy-only"} / {command.risk} risk</p>
                  </div>
                  <button className="secondary-button justify-center" type="button" title={command.notes} onClick={() => void onCopyCommand(command.id, command.command)}>
                    <Copy className="h-4 w-4" />
                    {copiedCommandId === command.id ? "Copied" : "Copy"}
                  </button>
                </div>
                <pre className="command-preview mt-3">{command.command}</pre>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-lg border border-deck-line bg-deck-950/40 p-3">
          <h4 className="text-sm font-semibold text-white">Related Docs</h4>
          <div className="mt-3 space-y-2">
            {drilldown.relatedDocs.map((doc) => (
              <div key={doc} className="path-chip">{doc}</div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function OperationalIssueTable({ issues }: { issues: EchoReportTopIssue[] }): JSX.Element {
  return (
    <div className="mt-4 rounded-lg border border-deck-line bg-deck-950/40 p-3">
      <h4 className="text-sm font-semibold text-white">Issues</h4>
      <div className="mt-3 overflow-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-deck-950 text-xs uppercase tracking-[0.14em] text-slate-500">
            <tr>
              <th className="px-3 py-2">Severity</th>
              <th className="px-3 py-2">Code</th>
              <th className="px-3 py-2">Source</th>
              <th className="px-3 py-2">Scope</th>
              <th className="px-3 py-2">Path</th>
            </tr>
          </thead>
          <tbody>
            {issues.map((issue, index) => {
              const tone = severityTone(issue.severity);
              return (
                <tr key={`${issue.reportKey}-${issue.code}-${index}`} className="border-t border-deck-line">
                  <td className="px-3 py-3"><span className={`status-pill ${toneBorderClass(tone)}`}>{issue.severity}</span></td>
                  <td className="px-3 py-3">
                    <p className="font-mono text-xs text-white">{issue.code}</p>
                    <p className="mt-1 line-clamp-2 text-xs text-slate-400">{issue.title}</p>
                  </td>
                  <td className="px-3 py-3 text-xs text-slate-400">{issue.source ?? issue.category ?? "report"}</td>
                  <td className="px-3 py-3 text-xs text-slate-400">{issueScope(issue)}</td>
                  <td className="px-3 py-3 font-mono text-xs text-slate-500">{issue.relatedPaths[0] ?? issue.reportPath}</td>
                </tr>
              );
            })}
            {!issues.length ? (
              <tr>
                <td className="px-3 py-4 text-slate-400" colSpan={5}>No issues were reported by this artifact.</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function previewValue(entry: EchoReportDrilldown["dataPreview"][number]): string {
  if (entry.kind === "array") {
    return `${entry.count ?? 0} item(s)${entry.keys?.length ? ` / keys: ${entry.keys.join(", ")}` : ""}`;
  }
  if (entry.kind === "object") {
    return `${entry.count ?? 0} key(s)${entry.keys?.length ? ` / ${entry.keys.join(", ")}` : ""}`;
  }
  if (entry.value === null) return "null";
  if (entry.value === undefined) return "unavailable";
  return String(entry.value);
}

function severityTone(severity: string): "green" | "amber" | "red" {
  const normalized = severity.toUpperCase();
  if (normalized === "FATAL" || normalized === "ERROR") return "red";
  if (normalized === "WARNING") return "amber";
  return "green";
}

function issueScope(issue: EchoReportTopIssue): string {
  return [issue.moduleId, issue.featureId, issue.packId].filter(Boolean).join(" / ") || "workspace";
}

function reportPanelIcon(id: string): typeof Activity {
  if (id === "platform") return ShieldCheck;
  if (id === "workspace") return Boxes;
  if (id === "graphs") return Layers;
  if (id === "packos") return PackageCheck;
  if (id === "diagnostics") return AlertTriangle;
  if (id === "health") return Activity;
  if (id === "recovery") return RefreshCw;
  if (id === "ai") return Sparkles;
  if (id === "assets") return Image;
  if (id === "release") return Rocket;
  if (id === "support") return Archive;
  return Database;
}

function safeJobForPanel(panel: EchoReportPanel, reportJobs: EchoReportJobSummary | null): EchoReportJobDefinition | null {
  if (!reportJobs) return null;
  const preferred: Record<string, string> = {
    platform: "verifyEchoNativePlatform",
    workspace: "scanEchoWorkspace",
    graphs: "generateEchoModuleGraph",
    packos: "echoPackDoctor",
    diagnostics: "generateEchoDiagnostics",
    health: "echoPackDoctor",
    recovery: "echoPackDoctor",
    ai: "echoPackDoctor",
    assets: "validateEchoReports",
    release: "validateEchoReports",
    support: "validateEchoReports",
    launcher: "validateEchoReports"
  };
  const preferredJob = reportJobs.definitions.find((definition) => definition.id === preferred[panel.id]);
  if (preferredJob && intersects(preferredJob.reportKeys, panel.reportKeys)) {
    return preferredJob;
  }
  const reportKeys = new Set(panel.reportKeys);
  return reportJobs.definitions.find((definition) => definition.reportKeys.some((key) => reportKeys.has(key))) ?? null;
}

function intersects(left: string[], right: string[]): boolean {
  const rightSet = new Set(right);
  return left.some((value) => rightSet.has(value));
}

type Tone = "green" | "amber" | "yellow" | "orange" | "red" | "gray" | "blue";

function reportJobTone(status: CommandRun["status"]): Tone {
  if (status === "running" || status === "queued") return "blue";
  if (status === "succeeded") return "green";
  if (status === "failed" || status === "rejected") return "red";
  return "gray";
}

function toneTextClass(tone: Tone): string {
  if (tone === "green") return "text-signal-green";
  if (tone === "red") return "text-signal-red";
  if (tone === "blue") return "text-signal-cyan";
  if (tone === "gray") return "text-slate-400";
  if (tone === "orange") return "text-orange-300";
  return "text-signal-amber";
}

function toneBorderClass(tone: Tone): string {
  if (tone === "green") return "border-signal-green/50 text-signal-green";
  if (tone === "red") return "border-signal-red/50 text-signal-red";
  if (tone === "blue") return "border-signal-cyan/50 text-signal-cyan";
  if (tone === "gray") return "border-slate-500/50 text-slate-400";
  if (tone === "orange") return "border-orange-400/50 text-orange-300";
  return "border-signal-amber/50 text-signal-amber";
}

function RoadmapView({ phases }: { phases: RoadmapPhase[] }): JSX.Element {
  return (
    <section className="surface p-5">
      <SectionTitle icon={Map} title="Roadmap" />
      {phases.length === 0 ? <EmptyState icon={Map} title="No roadmap phases" detail="Roadmap items will appear when they are seeded for the selected project." /> : null}
      <div className="mt-5 space-y-3">
        {phases.map((phase, index) => (
          <div key={phase.title} className="roadmap-row">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-deck-line bg-deck-900 font-mono text-xs text-signal-cyan">
              {String(index + 1).padStart(2, "0")}
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                <h3 className="truncate text-sm font-semibold text-white">{phase.title}</h3>
                <span className="text-xs uppercase tracking-[0.14em] text-slate-400">{phase.status}</span>
              </div>
              <p className="mt-1 text-sm text-slate-400">{phase.summary}</p>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-deck-900">
                <div className="h-full bg-signal-cyan" style={{ width: `${phase.progress}%` }} />
              </div>
            </div>
            <span className="w-12 text-right font-mono text-sm text-slate-300">{phase.progress}%</span>
          </div>
        ))}
      </div>
    </section>
  );
}

const featureStatuses: Array<FeatureStatus | "all"> = ["all", "implemented", "partial", "planned", "deferred", "blocked"];

function FeaturesView({ detail, catalog }: { detail: ProjectDetail; catalog: FeatureCatalogResponse | null }): JSX.Element {
  const [statusFilter, setStatusFilter] = useState<FeatureStatus | "all">("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const features = catalog?.features ?? [];
  const categories = ["all", ...Array.from(new Set(features.map((feature) => feature.category)))];
  const filtered = features.filter((feature) => {
    const statusMatch = statusFilter === "all" || feature.status === statusFilter;
    const categoryMatch = categoryFilter === "all" || feature.category === categoryFilter;
    return statusMatch && categoryMatch;
  });
  const summary = catalog?.summary;

  return (
    <div className="space-y-4">
      <PageHeader
        icon={FileText}
        title="Features"
        description={`Feature-level implementation intelligence for ${detail.project.name}, curated from lore docs, mod plans, release notes, and concrete project evidence.`}
      />

      <section className="surface p-4">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
          <Metric label="Total" value={summary?.total ?? 0} />
          <Metric label="Implemented" value={summary?.statusCounts.implemented ?? 0} tone="green" />
          <Metric label="Partial" value={summary?.statusCounts.partial ?? 0} tone="amber" />
          <Metric label="Planned" value={summary?.statusCounts.planned ?? 0} />
          <Metric label="Deferred" value={summary?.statusCounts.deferred ?? 0} />
          <Metric label="Blocked" value={summary?.statusCounts.blocked ?? 0} tone="red" />
        </div>
      </section>

      <section className="surface p-5">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <SectionTitle icon={ListChecks} title="Implementation Catalog" />
          <div className="grid gap-2 sm:grid-cols-2">
            <select className="control" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as FeatureStatus | "all")} aria-label="Feature status filter">
              {featureStatuses.map((status) => (
                <option key={status} value={status}>
                  {status === "all" ? "all statuses" : status}
                </option>
              ))}
            </select>
            <select className="control" value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)} aria-label="Feature category filter">
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category === "all" ? "all categories" : category}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-5 space-y-3">
          {filtered.length === 0 ? (
            <EmptyState icon={FileText} title="No feature rows for this filter" detail="Clear the filters to see the curated feature catalog for this project." />
          ) : (
            filtered.map((feature) => <FeatureCard key={feature.id} feature={feature} />)
          )}
        </div>
      </section>
    </div>
  );
}

function FeatureCard({ feature }: { feature: FeatureRecord }): JSX.Element {
  return (
    <article className="feature-row">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <FeatureStatusBadge status={feature.status} />
            <span className="chip">{feature.category}</span>
          </div>
          <h3 className="mt-3 break-words text-lg font-semibold text-white">{feature.title}</h3>
          <p className="mt-2 text-sm leading-6 text-slate-300">{feature.playerPromise}</p>
        </div>
        <p className="path-chip shrink-0">#{String(feature.order).padStart(2, "0")}</p>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-3">
        <div className="feature-note">
          <p className="eyebrow text-signal-cyan">Lore Context</p>
          <p className="mt-2 text-sm leading-6 text-slate-300">{feature.loreContext}</p>
        </div>
        <div className="feature-note">
          <p className="eyebrow text-signal-green">Implemented</p>
          <p className="mt-2 text-sm leading-6 text-slate-300">{feature.implementationSummary}</p>
        </div>
        <div className="feature-note">
          <p className="eyebrow text-signal-amber">Next</p>
          <p className="mt-2 text-sm leading-6 text-slate-300">{feature.nextAction}</p>
        </div>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-2">
        <div className="feature-note">
          <p className="text-xs font-semibold uppercase text-slate-500">Sources</p>
          <div className="mt-3 space-y-2">
            {feature.sources.map((source) => (
              <div key={`${source.path}-${source.section}`} className="rounded-lg border border-deck-line bg-deck-950/70 p-3">
                <p className="text-sm font-semibold text-white">{source.label}</p>
                <p className="mt-1 text-xs text-slate-400">{source.section}</p>
                <p className="mt-2 break-all font-mono text-xs text-slate-500">{source.path}</p>
              </div>
            ))}
          </div>
        </div>
        <div className="feature-note">
          <p className="text-xs font-semibold uppercase text-slate-500">Evidence</p>
          <div className="mt-3 space-y-2">
            {feature.evidence.map((item) => (
              <div key={`${item.kind}-${item.label}`} className="rounded-lg border border-deck-line bg-deck-950/70 p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="chip">{item.kind}</span>
                  <p className="min-w-0 break-words text-sm font-semibold text-white">{item.label}</p>
                </div>
                {item.detail ? <p className="mt-2 text-sm leading-6 text-slate-400">{item.detail}</p> : null}
                {item.path ? <p className="mt-2 break-all font-mono text-xs text-slate-500">{item.path}</p> : null}
              </div>
            ))}
          </div>
        </div>
      </div>
    </article>
  );
}

function QaView({
  detail,
  scans,
  readiness,
  onRunScan,
  scanBusy,
  setActiveView
}: {
  detail: ProjectDetail;
  scans: ScanReport[];
  readiness: ReadinessReport | null;
  onRunScan: (mode: ScanMode) => Promise<void>;
  scanBusy: ScanMode | null;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const findings = detail.latestReport?.findings ?? [];
  const inventory = detail.latestReport?.summary.inventory ?? {};
  const [sourceFilter, setSourceFilter] = useState("all");
  const filteredFindings = sourceFilter === "all" ? findings : findings.filter((finding) => (finding.source ?? "quick") === sourceFilter);
  const sources = ["all", ...Array.from(new Set(findings.map((finding) => finding.source ?? "quick")))];
  return (
    <div className="space-y-4">
      <PageHeader
        icon={ShieldCheck}
        title="QA Scanner"
        description="Quick scans inspect local resources, references, runtime logs, and jar expectations. Deep scans run project validators where available."
        actions={
          <>
          <button className="primary-button" disabled={Boolean(scanBusy)} onClick={() => onRunScan("quick")}>
            <RefreshCw className={`h-4 w-4 ${scanBusy === "quick" ? "animate-spin" : ""}`} />
            Quick Scan
          </button>
          <button className="primary-button" disabled={Boolean(scanBusy)} onClick={() => onRunScan("deep")}>
            <ShieldCheck className={`h-4 w-4 ${scanBusy === "deep" ? "animate-spin" : ""}`} />
            Deep Scan
          </button>
          </>
        }
      />

      {detail.latestReport ? (
        <section className="grid gap-3 md:grid-cols-5">
          <Metric label="Scan Status" value={`${detail.latestReport.mode} ${detail.latestReport.status}`} />
          <Metric label="To 100%" value={`${readiness?.score ?? detail.latestReport.summary.readinessScore ?? detail.latestReport.summary.buildHealth}%`} tone={readiness?.score === 100 ? "green" : "amber"} />
          <Metric label="Critical" value={detail.latestReport.summary.criticalIssues} tone="red" />
          <Metric label="Findings" value={detail.latestReport.findings.length} tone="amber" />
          <Metric label="Duration" value={`${Math.round(detail.latestReport.durationMs / 1000)}s`} />
        </section>
      ) : (
        <EmptyState icon={ShieldCheck} title="No scan report yet" detail="Run a quick scan to seed readiness, inventory, and findings for this project." />
      )}

      <ReadinessChecklistPanel readiness={readiness} setActiveView={setActiveView} />

      <section className="surface p-5">
        <SectionTitle icon={Database} title="Inventory" />
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {Object.entries(inventory).length ? Object.entries(inventory).map(([key, value]) => (
            <Metric key={key} label={key} value={value} />
          )) : <EmptyState title="No inventory yet" detail="Inventory appears after the first scan." />}
        </div>
      </section>

      <section className="surface p-5">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <SectionTitle icon={Activity} title="Latest Findings" />
          <select className="control" value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
            {sources.map((source) => (
              <option key={source} value={source}>
                {source}
              </option>
            ))}
          </select>
        </div>
        <Findings findings={filteredFindings} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.7fr_1.3fr]">
        <div className="surface p-5">
          <SectionTitle icon={ListChecks} title="Scan History" />
          <div className="mt-4 space-y-2">
            {scans.length === 0 ? (
              <p className="text-sm text-slate-400">No scan reports yet.</p>
            ) : (
              scans.map((scan) => (
                <div key={scan.id} className="module-row">
                  <span className="truncate">#{scan.id} {scan.mode} {scan.status}</span>
                  <span className="font-mono text-xs text-slate-400">{scan.summary.buildHealth}%</span>
                </div>
              ))
            )}
          </div>
        </div>
        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Raw Validator Output" />
          <pre className="mt-4 max-h-[360px] overflow-auto rounded-lg border border-deck-line bg-black/40 p-4 text-xs leading-5 text-slate-300">
            {detail.latestReport?.rawOutput || "No deep validator output yet."}
          </pre>
        </div>
      </section>
    </div>
  );
}

function Findings({ findings }: { findings: QaFinding[] }): JSX.Element {
  return (
    <div className="mt-4 space-y-3">
      {findings.length === 0 ? (
        <p className="text-sm text-slate-400">No findings for this filter.</p>
      ) : (
        findings.map((finding, index) => (
          <div key={`${finding.code}-${finding.path}-${finding.line}-${index}`} className="finding-row">
            <Severity severity={finding.severity} />
            <div className="min-w-0 flex-1">
              <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                <h3 className="truncate text-sm font-semibold text-white">{finding.title}</h3>
                <span className="font-mono text-xs text-slate-500">{finding.source ?? "quick"} {finding.code ?? ""}</span>
              </div>
              <p className="mt-1 text-sm leading-6 text-slate-400">{finding.detail}</p>
              {finding.path ? <p className="mt-2 font-mono text-xs text-slate-500">{finding.path}{finding.line ? `:${finding.line}` : ""}</p> : null}
            </div>
          </div>
        ))
      )}
    </div>
  );
}

function PromptsView({ detail, category }: { detail: ProjectDetail; category: "Codex QA" | "Asset Prompt" }): JSX.Element {
  const prompts = detail.prompts.filter((prompt) => prompt.category === category);
  const [selectedId, setSelectedId] = useState(prompts[0]?.id ?? "");
  const [rendered, setRendered] = useState<PromptTemplate | null>(prompts[0] ?? null);
  const [copied, setCopied] = useState(false);
  const selected = prompts.find((prompt) => prompt.id === selectedId) ?? prompts[0] ?? null;

  useEffect(() => {
    if (selected && selected.id !== rendered?.id) {
      setRendered(selected);
      setSelectedId(selected.id);
    }
  }, [selected?.id]);

  async function handleRender(): Promise<void> {
    if (!selected) return;
    setRendered(await renderPrompt(detail.project.slug, selected.id));
  }

  async function handleCopy(): Promise<void> {
    if (rendered) {
      await navigator.clipboard.writeText(rendered.body);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1400);
    }
  }

  return (
    <section className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
      <div className="surface p-5">
        <SectionTitle icon={category === "Codex QA" ? TerminalSquare : Image} title={category} />
        <div className="mt-4 space-y-2">
          {prompts.length ? prompts.map((prompt) => (
            <button
              key={prompt.id}
              className={`prompt-row ${selectedId === prompt.id ? "prompt-row-active" : ""}`}
              onClick={() => {
                setSelectedId(prompt.id);
                setRendered(prompt);
              }}
            >
              <span className="truncate font-semibold">{prompt.title}</span>
              <span className="line-clamp-2 text-left text-xs text-slate-400">{prompt.description}</span>
            </button>
          )) : <EmptyState title="No prompts for this project" detail="Prompt templates will appear here when they are seeded for the selected category." />}
        </div>
      </div>

      <div className="surface p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <SectionTitle icon={Sparkles} title={rendered?.title ?? "Prompt"} />
          <div className="flex gap-2">
            <button className="icon-button" title="Render prompt" onClick={handleRender}>
              <RefreshCw className="h-4 w-4" />
            </button>
            <button className="icon-button" title="Copy prompt" onClick={handleCopy}>
              <Clipboard className="h-4 w-4" />
            </button>
          </div>
        </div>
        {copied ? <p className="mt-3 text-sm text-signal-green">Prompt copied.</p> : null}
        <pre className="prompt-output mt-4 whitespace-pre-wrap text-sm leading-6">{rendered?.body ?? "Select a prompt."}</pre>
      </div>
    </section>
  );
}

function ReleaseView({
  detail,
  onRunAction,
  onStopRun,
  activeRun,
  selectedRun,
  onSelectRun,
  runs,
  settings
}: {
  detail: ProjectDetail;
  onRunAction: (action: ReleaseAction, confirmed?: boolean) => Promise<void>;
  onStopRun: (runId: string) => Promise<void>;
  activeRun: CommandRun | null;
  selectedRun: CommandRun | null;
  onSelectRun: (run: CommandRun) => void;
  runs: CommandRun[];
  settings: AppSettings;
}): JSX.Element {
  const [pending, setPending] = useState<ReleaseAction | null>(null);
  const running = activeRun?.status === "running" && activeRun.projectSlug === detail.project.slug ? activeRun : runs.find((run) => run.status === "running") ?? null;
  return (
    <div className="space-y-4">
      <PageHeader
        icon={Rocket}
        title="Release Deck"
        description="Run allowlisted build, verification, GameTest, validator, notes, and local release operations for the selected project."
        actions={<span className="path-chip">Mods folder: {settings.modpackModsDir || "not configured"}</span>}
      />

      {running ? (
        <section className="surface flex flex-col gap-3 border-signal-cyan/50 p-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-sm font-semibold text-white">Running: {running.commandId}</p>
            <p className="font-mono text-xs text-slate-400">pid {running.pid ?? "pending"} / {running.command.join(" ")}</p>
          </div>
          <button className="danger-button" onClick={() => onStopRun(running.id)}>
            <Square className="h-4 w-4" />
            Stop
          </button>
        </section>
      ) : null}

      <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {detail.releaseActions.map((action) => (
          <article key={action.commandId} className="surface p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h3 className="truncate text-base font-semibold text-white">{action.label}</h3>
                <p className="mt-2 line-clamp-3 text-sm text-slate-400">{action.description}</p>
              </div>
              <Risk risk={action.risk} />
            </div>
            <code className="mt-4 block truncate rounded-lg border border-deck-line bg-deck-950 px-3 py-2 text-xs text-slate-400">
              {displayCommand(action, settings)}
            </code>
            <button className="mt-4 primary-button w-full justify-center" disabled={Boolean(running)} onClick={() => (action.risk === "low" ? onRunAction(action, false) : setPending(action))}>
              <Play className="h-4 w-4" />
              Run
            </button>
          </article>
        ))}
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <OutputPanel title="Command Output" run={selectedRun} empty="No command run selected." />
        <RunHistoryPanel title="Run History" runs={runs} selectedRun={selectedRun} empty="No release runs yet." onSelectRun={onSelectRun} />
      </section>

      {pending ? (
        <ConfirmModal
          action={pending}
          settings={settings}
          onCancel={() => setPending(null)}
          onConfirm={async () => {
            const action = pending;
            setPending(null);
            await onRunAction(action, true);
          }}
        />
      ) : null}
    </div>
  );
}

function ModpackView({
  summary,
  runs,
  selectedRunId,
  onSelectRun,
  onRebuild,
  onStopRun,
  onRefresh,
  setActiveView
}: {
  summary: ModpackInventory | null;
  runs: ModpackPipelineRun[];
  selectedRunId: string | null;
  onSelectRun: (run: ModpackPipelineRun) => void;
  onRebuild: (confirmed?: boolean) => Promise<void>;
  onStopRun: (runId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
  setActiveView: (view: ViewKey) => void;
}): JSX.Element {
  const [confirming, setConfirming] = useState(false);
  const running = runs.find((run) => run.status === "running") ?? null;
  const selectedRun = runs.find((run) => run.id === selectedRunId) ?? running ?? runs[0] ?? null;
  const disabledReason = modpackDisabledReason(summary, running);

  return (
    <div className="space-y-4">
      <PageHeader
        icon={PackageCheck}
        title="Modpack Management"
        description="Rebuild and update first-party managed jars across ECHO and ARCANA with one confirmed pipeline: build, quarantine stale jars, copy current jars, verify checksums, and rescan."
        actions={
          <>
            <button className="secondary-button justify-center" onClick={() => void onRefresh()}>
              <RefreshCw className="h-4 w-4" />
              Refresh
            </button>
            {running ? (
              <button className="danger-button justify-center" onClick={() => void onStopRun(running.id)}>
                <Square className="h-4 w-4" />
                Stop / Recover
              </button>
            ) : null}
            <button className="primary-button justify-center" disabled={Boolean(disabledReason)} onClick={() => setConfirming(true)}>
              <PackageCheck className="h-4 w-4" />
              Rebuild & Update All
            </button>
          </>
        }
      />

      {disabledReason ? <InfoBanner tone={running ? "cyan" : "amber"} title="Pipeline blocked" detail={disabledReason} /> : null}

      <section className="surface p-4">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-7">
          <Metric label="Status" value={summary?.status ?? "loading"} />
          <Metric label="Projects" value={summary?.summary.projects ?? 0} />
          <Metric label="Expected" value={summary?.summary.expected ?? 0} />
          <Metric label="Built" value={summary?.summary.built ?? 0} tone={(summary?.summary.missing ?? 0) ? "amber" : "green"} />
          <Metric label="Current" value={summary?.summary.current ?? 0} tone={summary && summary.summary.current === summary.summary.expected ? "green" : "amber"} />
          <Metric label="Stale" value={summary?.summary.stale ?? 0} tone={(summary?.summary.stale ?? 0) ? "red" : "green"} />
          <Metric label="Duplicate" value={summary?.summary.duplicate ?? 0} tone={(summary?.summary.duplicate ?? 0) ? "red" : "green"} />
        </div>
        <p className="mt-4 break-all font-mono text-xs text-slate-500">Target: {summary?.targetDir || "not configured"}</p>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="surface p-5">
          <SectionTitle icon={PackageCheck} title="Managed Targets" />
          <div className="mt-4 space-y-3">
            {summary?.targets.length ? summary.targets.map((target) => (
              <article key={target.projectSlug} className="finding-row">
                <ModpackStatusBadge status={target.status} />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0">
                      <h3 className="break-words text-sm font-semibold text-white">{target.projectName}</h3>
                      <p className="mt-1 font-mono text-xs text-slate-500">{target.buildCommandId}</p>
                    </div>
                    <span className="path-chip">{target.manifest.summary.current}/{target.manifest.summary.expected} current</span>
                  </div>
                  <div className="mt-3 grid gap-2 sm:grid-cols-4">
                    <Metric label="Built" value={target.manifest.summary.built} />
                    <Metric label="Missing" value={target.manifest.summary.missing} tone={target.manifest.summary.missing ? "amber" : "green"} />
                    <Metric label="Stale" value={target.manifest.summary.stale} tone={target.manifest.summary.stale ? "red" : "green"} />
                    <Metric label="Duplicate" value={target.manifest.summary.duplicate} tone={target.manifest.summary.duplicate ? "red" : "green"} />
                  </div>
                  {target.blockers.length ? <p className="mt-3 text-sm text-signal-amber">{target.blockers.join(" ")}</p> : null}
                </div>
              </article>
            )) : <EmptyState icon={PackageCheck} title="No managed targets" detail="The modpack module expects the ECHO full stack and ARCANA project to be seeded." />}
          </div>
        </div>

        <div className="surface p-5">
          <SectionTitle icon={ListChecks} title="Quick Routing" />
          <div className="mt-4 grid gap-3">
            <ActionTile icon={Package} label="Open Jars" detail="Inspect per-project build outputs and target comparison." onClick={() => setActiveView("jars")} />
            <ActionTile icon={Rocket} label="Open Release" detail="Run individual allowlisted build or verification actions." onClick={() => setActiveView("release")} />
            <ActionTile icon={Settings} label="Open Settings" detail="Configure the Modpack Mods Folder before promotion." onClick={() => setActiveView("settings")} />
            <ActionTile icon={Download} label="Open Exports" detail="Export latest modpack summary and pipeline evidence." onClick={() => setActiveView("exports")} />
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="surface p-5">
          <SectionTitle icon={Activity} title="Pipeline History" />
          <div className="mt-4 space-y-2">
            {runs.length ? runs.map((run) => (
              <button key={run.id} className={`history-row ${selectedRun?.id === run.id ? "history-row-active" : ""}`} onClick={() => onSelectRun(run)}>
                <span className="min-w-0">
                  <span className="block truncate text-sm text-white">Rebuild & Update All</span>
                  <span className="mt-1 block truncate font-mono text-xs text-slate-500">{formatDate(run.finishedAt ?? run.startedAt)}</span>
                </span>
                <ModpackStatusBadge status={run.status} />
              </button>
            )) : <EmptyState title="No modpack pipeline runs yet" detail="The first run will appear after Rebuild & Update All is confirmed." />}
          </div>
        </div>

        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Pipeline Output" />
          {selectedRun ? (
            <div className="mt-4 space-y-3">
              <div className="grid gap-2 md:grid-cols-2">
                {selectedRun.steps.map((step) => (
                  <div key={step.id} className="pipeline-step">
                    <div className="flex items-center justify-between gap-2">
                      <p className="truncate text-sm font-semibold text-white">{step.label}</p>
                      <ModpackStatusBadge status={step.status} />
                    </div>
                    <p className="mt-2 text-sm leading-6 text-slate-400">{step.detail}</p>
                    {step.command?.length ? <p className="mt-2 break-all font-mono text-xs text-slate-500">{step.command.join(" ")}</p> : null}
                  </div>
                ))}
              </div>
              <pre className="output-block">{selectedRun.output || "No output captured yet."}</pre>
            </div>
          ) : (
            <EmptyState title="No pipeline selected" detail="Select a run to inspect steps and captured output." />
          )}
        </div>
      </section>

      {confirming ? (
        <ConfirmModpackModal
          summary={summary}
          onCancel={() => setConfirming(false)}
          onConfirm={async () => {
            setConfirming(false);
            await onRebuild(true);
          }}
        />
      ) : null}
    </div>
  );
}

function ConfirmModpackModal({
  summary,
  onCancel,
  onConfirm
}: {
  summary: ModpackInventory | null;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}): JSX.Element {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <section className="surface max-w-3xl p-5 shadow-glow">
        <div className="flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-signal-amber" />
          <h2 className="text-lg font-semibold text-white">Confirm Rebuild & Update All</h2>
        </div>
        <p className="mt-3 text-sm leading-6 text-slate-300">
          This high-risk pipeline rebuilds the managed first-party projects, quarantines stale/conflicting managed jars, copies current jars into the configured mods folder, verifies checksums, and runs quick scans. No files are deleted.
        </p>
        <div className="mt-4 rounded-lg border border-deck-line bg-deck-950 p-3">
          <p className="break-all font-mono text-xs text-slate-300">Target: {summary?.targetDir || "not configured"}</p>
          <p className="mt-1 font-mono text-xs text-slate-500">
            Projects: {summary?.targets.map((target) => `${target.projectName} (${target.buildCommandId})`).join(", ") || "none"}
          </p>
          <p className="mt-1 font-mono text-xs text-slate-500">Expected jars: {summary?.summary.expected ?? 0}</p>
        </div>
        <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
          <button className="secondary-button" onClick={onCancel}>Cancel</button>
          <button className="primary-button justify-center" onClick={onConfirm}>
            <PackageCheck className="h-4 w-4" />
            Confirm Pipeline
          </button>
        </div>
      </section>
    </div>
  );
}

function DistributionsView({
  summary,
  runs,
  selectedRunId,
  defaultTargetModsDir,
  onSelectRun,
  onExport,
  onStopRun,
  onRefresh
}: {
  summary: DistributionSummary | null;
  runs: CommandRun[];
  selectedRunId: string | null;
  defaultTargetModsDir: string;
  onSelectRun: (run: CommandRun) => void;
  onExport: (packId: DistributionPack["id"], targetModsDir: string, buildFirst: boolean, includeOptionalModules: boolean) => Promise<void>;
  onStopRun: (runId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
}): JSX.Element {
  const [selectedPackId, setSelectedPackId] = useState<DistributionPack["id"]>("ashfall");
  const [targetModsDir, setTargetModsDir] = useState(defaultTargetModsDir);
  const [buildFirst, setBuildFirst] = useState(false);
  const [includeOptionalModules, setIncludeOptionalModules] = useState(true);
  const running = runs.find((run) => run.status === "running") ?? null;
  const selectedRun = runs.find((run) => run.id === selectedRunId) ?? running ?? runs[0] ?? null;
  const selectedPack = summary?.packs.find((pack) => pack.id === selectedPackId) ?? summary?.packs[0] ?? null;
  const disabledReason = distributionDisabledReason(summary, selectedPack, running, targetModsDir);

  return (
    <div className="space-y-4">
      <PageHeader
        icon={Archive}
        title="Distributions"
        description="Local publishing cockpit for central ECHO module releases, official pack release readiness, launcher platform release status, and dev-folder exports."
        actions={
          <>
            <button className="secondary-button justify-center" onClick={() => void onRefresh()}>
              <RefreshCw className="h-4 w-4" />
              Refresh
            </button>
            {running ? (
              <button className="danger-button justify-center" onClick={() => void onStopRun(running.id)}>
                <Square className="h-4 w-4" />
                Stop / Recover
              </button>
            ) : null}
          </>
        }
      />

      {disabledReason ? <InfoBanner tone={running ? "cyan" : "amber"} title="Local export blocked" detail={disabledReason} /> : null}

      <section className="grid gap-4 xl:grid-cols-3">
        <DistributionReleasePanel title="ECHO Modules" status={summary?.echoModuleRelease} />
        <DistributionReleasePanel title="Launcher Windows" status={summary?.launcherRelease.windows} />
        <DistributionReleasePanel title="Launcher Linux" status={summary?.launcherRelease.linux} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="surface p-5">
          <SectionTitle icon={PackageCheck} title="Official Packs" />
          <div className="mt-4 grid gap-3 lg:grid-cols-2">
            {summary?.packs.map((pack) => (
              <button key={pack.id} className={`action-tile text-left ${selectedPackId === pack.id ? "history-row-active" : ""}`} onClick={() => setSelectedPackId(pack.id)}>
                <span className="flex min-w-0 items-center justify-between gap-3">
                  <span className="truncate text-sm font-semibold text-white">{pack.name}</span>
                  <ModpackStatusBadge status={pack.blockers.length ? "blocked" : pack.latestRelease.status} />
                </span>
                <span className="break-all text-xs text-slate-500">{pack.repo}</span>
                <span className="text-sm text-slate-400">{pack.selectedModuleCount} module(s) / {pack.latestRelease.latestVersion ?? "no local release"}</span>
              </button>
            )) ?? <EmptyState icon={PackageCheck} title="Distribution catalog loading" />}
          </div>
        </div>

        <div className="surface p-5">
          <SectionTitle icon={Download} title="Local Export" />
          <div className="mt-4 space-y-3">
            <label className="grid gap-1">
              <span className="text-xs uppercase tracking-[0.14em] text-slate-500">Pack</span>
              <select className="control" value={selectedPackId} onChange={(event) => setSelectedPackId(event.target.value as DistributionPack["id"])}>
                {summary?.packs.map((pack) => (
                  <option key={pack.id} value={pack.id}>{pack.name}</option>
                ))}
              </select>
            </label>
            <label className="grid gap-1">
              <span className="text-xs uppercase tracking-[0.14em] text-slate-500">Target Dev Mods Folder</span>
              <input className="control" value={targetModsDir} onChange={(event) => setTargetModsDir(event.target.value)} placeholder="C:/Path/To/.minecraft/mods" />
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={buildFirst} onChange={(event) => setBuildFirst(event.target.checked)} />
              Build ECHO first
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={includeOptionalModules} onChange={(event) => setIncludeOptionalModules(event.target.checked)} />
              Include optional modules
            </label>
            <button className="primary-button w-full justify-center" disabled={Boolean(disabledReason)} onClick={() => selectedPack && void onExport(selectedPack.id, targetModsDir, buildFirst, includeOptionalModules)}>
              <Download className="h-4 w-4" />
              Export To Mods Folder
            </button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <RunHistoryPanel title="Distribution Export History" runs={runs} selectedRun={selectedRun} empty="No distribution exports yet" onSelectRun={onSelectRun} />
        <OutputPanel title="Distribution Output" run={selectedRun} empty="No distribution run selected" />
      </section>
    </div>
  );
}

function DistributionReleasePanel({ title, status }: { title: string; status?: DistributionSummary["echoModuleRelease"] }): JSX.Element {
  return (
    <section className="surface p-5">
      <div className="flex items-center justify-between gap-3">
        <SectionTitle icon={Archive} title={title} />
        <ModpackStatusBadge status={status?.status ?? "missing"} />
      </div>
      <div className="mt-4 grid gap-2">
        <Metric label="Latest" value={status?.latestVersion ?? "none"} />
        <p className="break-all font-mono text-xs text-slate-500">{status?.repo ?? "repo pending"}</p>
        {status?.blockers.length ? <p className="text-sm leading-6 text-signal-amber">{status.blockers.join(" ")}</p> : null}
      </div>
    </section>
  );
}

function distributionDisabledReason(summary: DistributionSummary | null, pack: DistributionPack | null, running: CommandRun | null, targetModsDir: string): string | null {
  if (running) return "A distribution export is already running.";
  if (!summary || !pack) return "Distribution status is still loading.";
  if (!targetModsDir.trim()) return "Choose a target dev mods folder.";
  return pack.blockers[0] ?? null;
}

function modpackDisabledReason(summary: ModpackInventory | null, running: ModpackPipelineRun | null): string | null {
  if (running) {
    return "A modpack rebuild/update pipeline is already running.";
  }
  if (!summary) {
    return "Modpack inventory is still loading.";
  }
  return summary.blockers[0] ?? null;
}

function JarsView({
  detail,
  settings,
  manifest,
  activeRun,
  selectedRun,
  onSelectRun,
  runs,
  onRefresh,
  onBuild,
  onPromote,
  onStopRun
}: {
  detail: ProjectDetail;
  settings: AppSettings;
  manifest: JarManifest | null;
  activeRun: CommandRun | null;
  selectedRun: CommandRun | null;
  onSelectRun: (run: CommandRun) => void;
  runs: CommandRun[];
  onRefresh: () => Promise<void>;
  onBuild: (confirmed?: boolean) => Promise<void>;
  onPromote: (confirmed?: boolean) => Promise<void>;
  onStopRun: (runId: string) => Promise<void>;
}): JSX.Element {
  const [pending, setPending] = useState<JarActionKind | null>(null);
  const running = activeRun?.status === "running" && activeRun.projectSlug === detail.project.slug ? activeRun : runs.find((run) => run.status === "running") ?? null;
  const missingSources = manifest?.artifacts.filter((artifact) => !artifact.exists) ?? [];
  const promoteReason = promoteDisabledReason(manifest, running);
  const promoteBlocked = Boolean(promoteReason);
  const jarRuns = runs.filter((run) => ["promote-jars", "build-full-stack", "build-module", "copy-jars", "check-jar-set"].includes(run.commandId));
  const selectedJarRun = jarRuns.find((run) => run.id === selectedRun?.id) ?? jarRuns[0] ?? null;
  const targetLabel = manifest
    ? manifest.targetConfigured
      ? manifest.targetExists
        ? "ready"
        : "missing"
      : "not configured"
    : "loading";
  const targetByFile = new globalThis.Map((manifest?.targetEntries ?? []).map((entry) => [entry.fileName, entry]));

  return (
    <div className="space-y-4">
      <section className="surface page-header p-5">
        <div className="min-w-0">
          <SectionTitle icon={Package} title="Jar Management" />
          <p className="mt-2 max-w-4xl text-sm leading-6 text-slate-400">
            {detail.project.slug === "echo"
              ? "Full-stack promotion manages every expected ECHO module jar."
              : "Scoped promotion manages only this module project's expected jar."}
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            <span className="path-chip">Mods folder: {settings.modpackModsDir || "not configured"}</span>
            {manifest ? <span className="path-chip">Build root: {manifest.buildRoot}</span> : null}
          </div>
          {promoteReason ? <p className="mt-3 text-sm text-signal-amber">Promote disabled: {promoteReason}</p> : null}
        </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <button className="secondary-button justify-center" onClick={() => void onRefresh()}>
              <RefreshCw className="h-4 w-4" />
              Refresh
            </button>
            <button className="primary-button justify-center" disabled={Boolean(running)} onClick={() => setPending("build")}>
              <Package className="h-4 w-4" />
              Build Jars
            </button>
            <button className="primary-button justify-center" disabled={promoteBlocked} onClick={() => setPending("promote")}>
              <Copy className="h-4 w-4" />
              Promote
            </button>
          </div>
      </section>

      {running ? (
        <section className="surface flex flex-col gap-3 border-signal-cyan/50 p-4 md:flex-row md:items-center md:justify-between">
          <div className="min-w-0">
            <p className="text-sm font-semibold text-white">Running: {running.commandId}</p>
            <p className="break-all font-mono text-xs text-slate-400">pid {running.pid ?? "pending"} / {running.command.join(" ")}</p>
          </div>
          <button className="danger-button" onClick={() => onStopRun(running.id)}>
            <Square className="h-4 w-4" />
            Stop
          </button>
        </section>
      ) : null}

      {promoteReason ? <InfoBanner tone="amber" title="Promote is blocked" detail={promoteReason} /> : null}

      <section className="grid gap-3 md:grid-cols-5">
        <Metric label="Target" value={targetLabel} tone={targetLabel === "ready" ? "green" : "amber"} />
        <Metric label="Expected" value={manifest?.summary.expected ?? detail.project.modules.length} />
        <Metric label="Built" value={manifest?.summary.built ?? 0} tone={missingSources.length ? "amber" : "green"} />
        <Metric label="Current" value={manifest?.summary.current ?? 0} tone={manifest?.summary.current === manifest?.summary.expected ? "green" : "amber"} />
        <Metric label="Stale" value={(manifest?.summary.stale ?? 0) + (manifest?.summary.duplicate ?? 0)} tone={manifest?.summary.stale || manifest?.summary.duplicate ? "red" : "green"} />
      </section>

      <section className="surface p-5">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <SectionTitle icon={CheckCircle2} title="Expected Artifacts" />
          <span className="font-mono text-xs text-slate-500">{manifest?.generatedAt ? `updated ${formatDate(manifest.generatedAt)}` : "loading"}</span>
        </div>
        <div className="mt-4 overflow-auto rounded-lg border border-deck-line">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-deck-950 text-xs uppercase tracking-[0.14em] text-slate-500">
              <tr>
                <th className="px-3 py-3">Module</th>
                <th className="px-3 py-3">Expected Jar</th>
                <th className="px-3 py-3">Source</th>
                <th className="px-3 py-3">Target</th>
                <th className="px-3 py-3">Checksum</th>
              </tr>
            </thead>
            <tbody>
              {(manifest?.artifacts ?? []).map((artifact) => {
                const target = targetByFile.get(artifact.expectedFileName);
                return (
                  <tr key={artifact.moduleId} className="border-t border-deck-line">
                    <td className="px-3 py-3">
                      <p className="font-semibold text-white">{artifact.label}</p>
                      <p className="font-mono text-xs text-slate-500">{artifact.moduleId}</p>
                    </td>
                    <td className="px-3 py-3">
                      <p className="font-mono text-xs text-slate-300">{artifact.expectedFileName}</p>
                      <p className="mt-1 break-all font-mono text-xs text-slate-600">{artifact.sourcePath}</p>
                    </td>
                    <td className="px-3 py-3">
                      <JarStatusBadge status={artifact.exists ? "current" : "missing"} label={artifact.exists ? "built" : "missing"} />
                      <p className="mt-2 font-mono text-xs text-slate-500">{artifact.size == null ? "" : formatBytes(artifact.size)}</p>
                    </td>
                    <td className="px-3 py-3">
                      <JarStatusBadge status={target?.status ?? "missing"} />
                      <p className="mt-2 font-mono text-xs text-slate-500">{target?.modifiedAt ? formatDate(target.modifiedAt) : ""}</p>
                    </td>
                    <td className="max-w-[280px] px-3 py-3">
                      <p className="truncate font-mono text-xs text-slate-400" title={artifact.checksum ?? ""}>{artifact.checksum ?? "not built"}</p>
                    </td>
                  </tr>
                );
              })}
              {!manifest?.artifacts.length ? (
                <tr>
                  <td className="px-3 py-5 text-sm text-slate-400" colSpan={5}>No jar manifest loaded.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <div className="surface p-5">
          <SectionTitle icon={Archive} title="Target Comparison" />
          <div className="mt-4 space-y-2">
            {manifest?.targetEntries.length ? (
              manifest.targetEntries.map((entry) => (
                <div key={entry.path} className="module-row">
                  <div className="min-w-0">
                    <p className="truncate text-sm text-white">{entry.fileName}</p>
                    <p className="truncate font-mono text-xs text-slate-500">{entry.path}</p>
                  </div>
                  <JarStatusBadge status={entry.status} />
                </div>
              ))
            ) : (
              <p className="text-sm text-slate-400">{manifest?.targetExists ? "No managed project jars found in the target folder." : "Target folder comparison is unavailable."}</p>
            )}
          </div>
        </div>

        <RunHistoryPanel title="Jar Run History" runs={jarRuns} selectedRun={selectedJarRun} empty="No jar pipeline runs yet." onSelectRun={onSelectRun} />
      </section>

      <OutputPanel title="Pipeline Output" run={selectedJarRun} empty="No jar pipeline output selected." />

      {pending ? (
        <ConfirmJarModal
          kind={pending}
          manifest={manifest}
          onCancel={() => setPending(null)}
          onConfirm={async () => {
            const action = pending;
            setPending(null);
            if (action === "build") {
              await onBuild(true);
            } else {
              await onPromote(true);
            }
          }}
        />
      ) : null}
    </div>
  );
}

function TerminalPlannerView({ groups }: { groups: TerminalPlannerGroup[] }): JSX.Element {
  return (
    <section className="surface p-5">
      <SectionTitle icon={Layers} title="Terminal Planner" />
      {groups.length === 0 ? (
        <EmptyState icon={Layers} title="No terminal planner for this project" detail="Standalone or tooling projects can still use Dashboard, QA, Release, Jars, Settings, and Exports without terminal page coverage." />
      ) : null}
      <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        {groups.map((group) => (
          <article key={group.group} className="planner-column">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-signal-cyan">{group.group}</h3>
            <div className="mt-4 space-y-2">
              {group.pages.map((page) => (
                <div key={page} className="module-row">
                  <span className="truncate">{page}</span>
                </div>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function ExportsView({ detail }: { detail: ProjectDetail }): JSX.Element {
  const base = `/api/projects/${detail.project.slug}/export`;
  return (
    <section className="grid gap-4 xl:grid-cols-[0.8fr_1.2fr]">
      <div className="surface p-5">
        <SectionTitle icon={Download} title="Exports" />
        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">
          Export the selected project with settings, readiness, latest QA report, feature/lore implementation catalog, modpack summary, scan history, release runs, jar manifest, roadmap, prompts, and release actions.
        </p>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row">
          <a className="primary-button" href={`${base}?format=markdown`} target="_blank" rel="noreferrer">
            <FileText className="h-4 w-4" />
            Markdown
          </a>
          <a className="primary-button" href={`${base}?format=json`} target="_blank" rel="noreferrer">
            <FileJson className="h-4 w-4" />
            JSON
          </a>
        </div>
      </div>
      <div className="surface p-5">
        <SectionTitle icon={ListChecks} title="Included Evidence" />
        <div className="mt-4 grid gap-2 sm:grid-cols-2">
          {["Project metadata", "Readiness report", "Jar manifest", "Feature/lore implementation catalog", "Modpack summary and pipeline history", "Operational reports", "Recent scan reports", "Release run history", "Roadmap", "Prompt templates", "Release actions"].map((item) => (
            <div key={item} className="module-row">
              <span className="truncate">{item}</span>
              <span className="status-pill border-signal-green/50 text-signal-green">included</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function SettingsView({ settings, onSave }: { settings: AppSettings; onSave: (settings: AppSettings) => Promise<void> }): JSX.Element {
  const [draft, setDraft] = useState<AppSettings>(settings);
  const [saving, setSaving] = useState(false);

  useEffect(() => setDraft(settings), [settings]);

  async function handleSave(): Promise<void> {
    setSaving(true);
    try {
      await onSave(draft);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="surface p-5">
      <SectionTitle icon={Settings} title="Settings" />
      <div className="mt-5 grid gap-4">
        <Field label="ECHO Root" help="Used for seeded ECHO projects and full-stack scans." value={draft.echoRoot} onChange={(value) => setDraft({ ...draft, echoRoot: value })} />
        <Field label="Modpack Mods Folder" help="Required before jar promotion can copy current jars into a local instance." value={draft.modpackModsDir} onChange={(value) => setDraft({ ...draft, modpackModsDir: value })} />
        <Field label="Python Executable" help="Used by deep validators and project release tasks that call Python." value={draft.pythonExecutable} onChange={(value) => setDraft({ ...draft, pythonExecutable: value })} />
        <label className="grid gap-2">
          <span className="text-xs uppercase tracking-[0.14em] text-slate-500">Runtime Log Max Age Minutes</span>
          <input className="control" type="number" min={1} value={draft.runtimeLogMaxAgeMinutes} onChange={(event) => setDraft({ ...draft, runtimeLogMaxAgeMinutes: Number(event.target.value) })} />
          <span className="text-xs text-slate-500">Only recent logs inside project run folders are considered during quick scans.</span>
        </label>
        <label className="grid gap-2">
          <span className="text-xs uppercase tracking-[0.14em] text-slate-500">Default Scan Mode</span>
          <select className="control" value={draft.defaultScanMode} onChange={(event) => setDraft({ ...draft, defaultScanMode: event.target.value as ScanMode })}>
            <option value="quick">quick</option>
            <option value="deep">deep</option>
          </select>
          <span className="text-xs text-slate-500">This is the fallback used by scan controls that do not specify a mode.</span>
        </label>
      </div>
      <button className="primary-button mt-5" disabled={saving} onClick={handleSave}>
        <Settings className="h-4 w-4" />
        {saving ? "Saving" : "Save Settings"}
      </button>
    </section>
  );
}

function Field({ label, value, onChange, help }: { label: string; value: string; onChange: (value: string) => void; help?: string }): JSX.Element {
  return (
    <label className="grid gap-2">
      <span className="text-xs uppercase tracking-[0.14em] text-slate-500">{label}</span>
      <input className="control" value={value} onChange={(event) => onChange(event.target.value)} />
      {help ? <span className="text-xs text-slate-500">{help}</span> : null}
    </label>
  );
}

