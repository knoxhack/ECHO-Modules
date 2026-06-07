import type {
  AppSettings,
  CommandRun,
  DistributionSummary,
  EchoBridgeConfirmationRequest,
  EchoBridgeCreateSessionRequest,
  EchoBridgeExecutorProbe,
  EchoBridgeLogChunk,
  EchoBridgePromptRequest,
  EchoBridgeState,
  EchoBridgeStreamCursors,
  EchoReportDrilldown,
  EchoReportJobDetail,
  EchoReportJobSummary,
  EchoOperationalReports,
  FeatureCatalogResponse,
  JarManifest,
  JarPipelineResult,
  ModpackInventory,
  ModpackPipelineResult,
  ModpackPipelineRun,
  PackExportRequest,
  PackExportResult,
  Project,
  ProjectDetail,
  PromptTemplate,
  ReadinessReport,
  ReleaseAction,
  ScanMode,
  ScanReport
} from "../shared/types";

const API_ROOT = "/api";

export async function getHealth(): Promise<Record<string, string | boolean>> {
  return request<Record<string, string | boolean>>("/health");
}

export async function getSettings(): Promise<AppSettings> {
  return request<AppSettings>("/settings");
}

export async function saveSettings(settings: Partial<AppSettings>): Promise<AppSettings> {
  return request<AppSettings>("/settings", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(settings)
  });
}

export async function getProjects(): Promise<Project[]> {
  const data = await request<{ projects: Project[] }>("/projects");
  return data.projects;
}

export async function getModpackSummary(): Promise<ModpackInventory> {
  return request<ModpackInventory>("/modpack/summary");
}

export async function getModpackRuns(): Promise<ModpackPipelineRun[]> {
  const data = await request<{ runs: ModpackPipelineRun[] }>("/modpack/runs");
  return data.runs;
}

export async function rebuildModpack(confirmed = false): Promise<ModpackPipelineResult> {
  return request<ModpackPipelineResult>("/modpack/rebuild", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ confirmed })
  });
}

export async function getDistributionSummary(): Promise<DistributionSummary> {
  return request<DistributionSummary>("/distributions/summary");
}

export async function getDistributionRuns(): Promise<CommandRun[]> {
  const data = await request<{ runs: CommandRun[] }>("/distributions/runs");
  return data.runs;
}

export async function getEchoOperationalReports(): Promise<EchoOperationalReports> {
  return request<EchoOperationalReports>("/reports/echo");
}

export async function getEchoReportDrilldown(reportKey: string): Promise<EchoReportDrilldown> {
  return request<EchoReportDrilldown>(`/reports/echo/${encodeURIComponent(reportKey)}`);
}

export async function getEchoReportJobs(): Promise<EchoReportJobSummary> {
  return request<EchoReportJobSummary>("/reports/echo/jobs");
}

export async function getEchoReportJobDetail(runId: string): Promise<EchoReportJobDetail> {
  return request<EchoReportJobDetail>(`/reports/echo/jobs/${encodeURIComponent(runId)}`);
}

export async function startEchoReportJob(commandId: string): Promise<EchoReportJobSummary> {
  return request<EchoReportJobSummary>("/reports/echo/jobs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ commandId })
  });
}

export async function stopEchoReportJob(runId: string): Promise<EchoReportJobSummary> {
  return request<EchoReportJobSummary>(`/reports/echo/jobs/${encodeURIComponent(runId)}/stop`, { method: "POST" });
}

export async function getBridgeState(): Promise<EchoBridgeState> {
  return request<EchoBridgeState>("/bridge");
}

export async function getBridgeExecutorProbe(): Promise<EchoBridgeExecutorProbe> {
  return request<EchoBridgeExecutorProbe>("/bridge/executor/probe");
}

export async function createBridgeSession(body: EchoBridgeCreateSessionRequest = {}): Promise<EchoBridgeState> {
  return request<EchoBridgeState>("/bridge/sessions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function submitBridgePrompt(sessionId: string, body: EchoBridgePromptRequest): Promise<EchoBridgeState> {
  return request<EchoBridgeState>(`/bridge/sessions/${encodeURIComponent(sessionId)}/prompts`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function cancelBridgeJob(jobId: string): Promise<EchoBridgeState> {
  return request<EchoBridgeState>(`/bridge/jobs/${encodeURIComponent(jobId)}/cancel`, { method: "POST" });
}

export async function confirmBridgeSafeAction(requestId: string, body: EchoBridgeConfirmationRequest): Promise<EchoBridgeState> {
  return request<EchoBridgeState>(`/bridge/safe-actions/${encodeURIComponent(requestId)}/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function getBridgeJobLogs(jobId: string, after?: string): Promise<EchoBridgeLogChunk[]> {
  const query = after ? `?after=${encodeURIComponent(after)}` : "";
  const data = await request<{ logs: EchoBridgeLogChunk[]; streamCursors?: EchoBridgeStreamCursors | null }>(`/bridge/jobs/${encodeURIComponent(jobId)}/logs${query}`);
  return data.logs;
}

export async function exportDistributionPack(requestBody: PackExportRequest): Promise<PackExportResult> {
  return request<PackExportResult>("/distributions/export", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(requestBody)
  });
}

export async function stopDistributionRun(runId: string): Promise<PackExportResult> {
  return request<PackExportResult>(`/distributions/runs/${runId}/stop`, { method: "POST" });
}

export async function stopModpackRun(runId: string): Promise<ModpackPipelineResult> {
  return request<ModpackPipelineResult>(`/modpack/runs/${runId}/stop`, { method: "POST" });
}

export async function getProject(slug: string): Promise<ProjectDetail> {
  return request<ProjectDetail>(`/projects/${slug}`);
}

export async function getFeatures(slug: string): Promise<FeatureCatalogResponse> {
  return request<FeatureCatalogResponse>(`/projects/${slug}/features`);
}

export async function runScan(slug: string, mode: ScanMode): Promise<ScanReport> {
  return request<ScanReport>(`/projects/${slug}/scan`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ mode })
  });
}

export async function listScans(slug: string): Promise<ScanReport[]> {
  const data = await request<{ reports: ScanReport[] }>(`/projects/${slug}/scans`);
  return data.reports;
}

export async function getRelease(slug: string): Promise<{
  actions: ReleaseAction[];
  modpackModsDir: string | null;
  runs: CommandRun[];
}> {
  return request<{ actions: ReleaseAction[]; modpackModsDir: string | null; runs: CommandRun[] }>(`/projects/${slug}/release`);
}

export async function getJars(slug: string): Promise<JarManifest> {
  return request<JarManifest>(`/projects/${slug}/jars`);
}

export async function getReadiness(slug: string): Promise<ReadinessReport> {
  return request<ReadinessReport>(`/projects/${slug}/readiness`);
}

export async function buildJars(slug: string, confirmed = false): Promise<{ run: CommandRun; manifest: JarManifest }> {
  return request<{ run: CommandRun; manifest: JarManifest }>(`/projects/${slug}/jars/build`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ confirmed })
  });
}

export async function promoteJars(slug: string, confirmed = false): Promise<JarPipelineResult> {
  return request<JarPipelineResult>(`/projects/${slug}/jars/promote`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ confirmed })
  });
}

export async function listRuns(slug: string): Promise<CommandRun[]> {
  const data = await request<{ runs: CommandRun[] }>(`/projects/${slug}/runs`);
  return data.runs;
}

export async function runReleaseAction(slug: string, commandId: string, confirmed = false): Promise<CommandRun> {
  return request<CommandRun>(`/projects/${slug}/release/${commandId}/run`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ confirmed })
  });
}

export async function stopRun(runId: string): Promise<CommandRun> {
  return request<CommandRun>(`/runs/${runId}/stop`, { method: "POST" });
}

export async function getRun(runId: string): Promise<CommandRun> {
  return request<CommandRun>(`/runs/${runId}`);
}

export async function renderPrompt(slug: string, promptId: string): Promise<PromptTemplate> {
  const data = await request<{ prompt: PromptTemplate }>(`/projects/${slug}/prompts/render`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ promptId })
  });
  return data.prompt;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_ROOT}${path}`, init);
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `${response.status} ${response.statusText}`);
  }
  return (await response.json()) as T;
}
