import { createRequire } from "node:module";
import fs from "node:fs";
import path from "node:path";
import featureCatalog from "../shared/feature-catalog.json" with { type: "json" };
import seedData from "../shared/seed-data.json" with { type: "json" };
import type {
  AppSettings,
  CommandRun,
  CommandRunStatus,
  FeatureRecord,
  Project,
  ProjectDetail,
  PromptTemplate,
  QaFinding,
  QaTrack,
  ReleaseAction,
  RoadmapPhase,
  ScanMode,
  ScanReport,
  ScanStatus,
  TerminalPlannerGroup
} from "../shared/types.js";
import { DB_PATH, DEFAULT_PYTHON_EXECUTABLE, ECHO_ROOT, LOCAL_DATA_DIR } from "./paths.js";
import { projectWithLiveModuleVersions } from "./workspace.js";

type DatabaseLike = {
  exec(sql: string): void;
  prepare(sql: string): StatementLike;
  close(): void;
};

type StatementLike = {
  run(...params: unknown[]): { lastInsertRowid?: number | bigint; changes?: number | bigint };
  get(...params: unknown[]): Record<string, unknown> | undefined;
  all(...params: unknown[]): Record<string, unknown>[];
};

const require = createRequire(import.meta.url);
const { DatabaseSync } = require("node:sqlite") as { DatabaseSync: new (path: string) => DatabaseLike };

const DEFAULT_SETTINGS: AppSettings = {
  echoRoot: String(seedData.settings.echoRoot ?? ECHO_ROOT),
  modpackModsDir: String(seedData.settings.modpackModsDir ?? ""),
  pythonExecutable: String(seedData.settings.pythonExecutable ?? DEFAULT_PYTHON_EXECUTABLE),
  runtimeLogMaxAgeMinutes: Number(seedData.settings.runtimeLogMaxAgeMinutes ?? 180),
  defaultScanMode: seedData.settings.defaultScanMode === "deep" ? "deep" : "quick"
};

function parseJson<T>(value: unknown, fallback: T): T {
  if (typeof value !== "string" || !value.trim()) {
    return fallback;
  }
  try {
    return JSON.parse(value) as T;
  } catch {
    return fallback;
  }
}

function json(value: unknown): string {
  return JSON.stringify(value);
}

function nowIso(): string {
  return new Date().toISOString();
}

function valueAsString(value: unknown, fallback = ""): string {
  return typeof value === "string" && value.trim() ? value : fallback;
}

function liveSeedProjects(): Project[] {
  return (seedData.projects as Project[]).map((project) => projectWithLiveModuleVersions(project, DEFAULT_SETTINGS));
}

export class CommandCenterStore {
  private readonly db: DatabaseLike;

  constructor(dbPath = DB_PATH) {
    fs.mkdirSync(path.dirname(dbPath), { recursive: true });
    this.db = new DatabaseSync(dbPath);
    this.db.exec("PRAGMA journal_mode = WAL;");
    this.db.exec("PRAGMA foreign_keys = ON;");
    this.migrate();
    this.seed();
  }

  close(): void {
    this.db.close();
  }

  listProjects(): Project[] {
    return this.db.prepare("SELECT * FROM projects ORDER BY id ASC").all().map((row) => this.withLiveVersions(rowToProject(row)));
  }

  getProject(slug: string): Project | null {
    const row = this.db.prepare("SELECT * FROM projects WHERE slug = ?").get(slug);
    return row ? this.withLiveVersions(rowToProject(row)) : null;
  }

  getRoadmap(projectSlug: string): RoadmapPhase[] {
    return this.db
      .prepare("SELECT * FROM roadmap_phases WHERE project_slug = ? ORDER BY order_index ASC")
      .all(projectSlug)
      .map(rowToRoadmap);
  }

  getQaTracks(projectSlug: string): QaTrack[] {
    return this.db
      .prepare("SELECT * FROM qa_tracks WHERE project_slug = ? ORDER BY id ASC")
      .all(projectSlug)
      .map(rowToQaTrack);
  }

  getPrompts(projectSlug: string): PromptTemplate[] {
    return this.db
      .prepare("SELECT * FROM prompt_templates WHERE project_slug = ? ORDER BY id ASC")
      .all(projectSlug)
      .map(rowToPrompt);
  }

  getPrompt(projectSlug: string, promptId: string): PromptTemplate | null {
    const row = this.db
      .prepare("SELECT * FROM prompt_templates WHERE project_slug = ? AND template_id = ?")
      .get(projectSlug, promptId);
    return row ? rowToPrompt(row) : null;
  }

  getTerminalPlanner(projectSlug: string): TerminalPlannerGroup[] {
    return this.db
      .prepare("SELECT * FROM terminal_planner WHERE project_slug = ? ORDER BY order_index ASC")
      .all(projectSlug)
      .map(rowToTerminalPlanner);
  }

  getReleaseActions(projectSlug: string): ReleaseAction[] {
    return this.db
      .prepare("SELECT * FROM release_actions WHERE project_slug = ? ORDER BY id ASC")
      .all(projectSlug)
      .map(rowToReleaseAction);
  }

  getReleaseAction(projectSlug: string, commandId: string): ReleaseAction | null {
    const row = this.db
      .prepare("SELECT * FROM release_actions WHERE project_slug = ? AND command_id = ?")
      .get(projectSlug, commandId);
    return row ? rowToReleaseAction(row) : null;
  }

  getFeatures(projectSlug: string): FeatureRecord[] {
    return this.db
      .prepare("SELECT * FROM feature_records WHERE project_slug = ? ORDER BY order_index ASC, id ASC")
      .all(projectSlug)
      .map(rowToFeatureRecord);
  }

  getProjectDetail(slug: string): ProjectDetail | null {
    const project = this.getProject(slug);
    if (!project) {
      return null;
    }
    return {
      project,
      roadmap: this.getRoadmap(slug),
      qaTracks: this.getQaTracks(slug),
      prompts: this.getPrompts(slug),
      terminalPlanner: this.getTerminalPlanner(slug),
      releaseActions: this.getReleaseActions(slug),
      latestReport: this.getLatestScanReport(slug),
      recentRuns: this.listCommandRuns(slug, 8)
    };
  }

  createScanReport(input: {
    projectSlug: string;
    mode: ScanMode;
    status: ScanStatus;
    startedAt: string;
    finishedAt?: string;
    durationMs: number;
    source: Record<string, unknown>;
    rawOutput: string;
    summary: ScanReport["summary"];
    findings: QaFinding[];
  }): ScanReport {
    const createdAt = nowIso();
    this.db.exec("BEGIN");
    try {
      const result = this.db
        .prepare(
          `INSERT INTO scan_reports
          (project_slug, created_at, mode, status, started_at, finished_at, duration_ms, source_json, raw_output, summary_json)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
        )
        .run(
          input.projectSlug,
          createdAt,
          input.mode,
          input.status,
          input.startedAt,
          input.finishedAt ?? "",
          input.durationMs,
          json(input.source),
          input.rawOutput,
          json(input.summary)
        );
      const reportId = Number(result.lastInsertRowid);
      const insertFinding = this.db.prepare(
        `INSERT INTO qa_findings
        (report_id, track, title, severity, status, detail, path, line, code, source, metadata_json)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      );
      for (const finding of input.findings) {
        insertFinding.run(
          reportId,
          finding.track,
          finding.title,
          finding.severity,
          finding.status,
          finding.detail,
          finding.path ?? "",
          finding.line ?? null,
          finding.code ?? "",
          finding.source ?? "",
          json(finding.metadata ?? {})
        );
      }
      this.db
        .prepare(
          "UPDATE projects SET build_health = ?, critical_issues = ?, polish_tasks = ?, last_scan_label = ?, next_recommended_action = ? WHERE slug = ?"
        )
        .run(
          input.summary.buildHealth,
          input.summary.criticalIssues,
          input.summary.polishTasks,
          "Just now",
          nextActionFromFindings(input.findings),
          input.projectSlug
        );
      this.db.exec("COMMIT");
      return this.getScanReport(reportId) as ScanReport;
    } catch (error) {
      this.db.exec("ROLLBACK");
      throw error;
    }
  }

  listScanReports(projectSlug: string, limit = 20): ScanReport[] {
    return this.db
      .prepare("SELECT id FROM scan_reports WHERE project_slug = ? ORDER BY id DESC LIMIT ?")
      .all(projectSlug, limit)
      .map((row) => this.getScanReport(Number(row.id)))
      .filter((report): report is ScanReport => Boolean(report));
  }

  getLatestScanReport(projectSlug: string): ScanReport | null {
    const row = this.db
      .prepare("SELECT id FROM scan_reports WHERE project_slug = ? ORDER BY id DESC LIMIT 1")
      .get(projectSlug);
    return row ? this.getScanReport(Number(row.id)) : null;
  }

  getScanReportById(projectSlug: string, reportId: number): ScanReport | null {
    const row = this.db.prepare("SELECT id FROM scan_reports WHERE project_slug = ? AND id = ?").get(projectSlug, reportId);
    return row ? this.getScanReport(Number(row.id)) : null;
  }

  getSettings(): AppSettings {
    return {
      echoRoot: this.getSetting("echoRoot") ?? DEFAULT_SETTINGS.echoRoot,
      modpackModsDir: this.getSetting("modpackModsDir") ?? DEFAULT_SETTINGS.modpackModsDir,
      pythonExecutable: this.getSetting("pythonExecutable") ?? DEFAULT_SETTINGS.pythonExecutable,
      runtimeLogMaxAgeMinutes: Number(this.getSetting("runtimeLogMaxAgeMinutes") ?? DEFAULT_SETTINGS.runtimeLogMaxAgeMinutes),
      defaultScanMode: ((this.getSetting("defaultScanMode") ?? DEFAULT_SETTINGS.defaultScanMode) === "deep" ? "deep" : "quick") as ScanMode
    };
  }

  updateSettings(settings: Partial<AppSettings>): AppSettings {
    const current = this.getSettings();
    const next: AppSettings = {
      echoRoot: normalizeText(settings.echoRoot, current.echoRoot),
      modpackModsDir: normalizeText(settings.modpackModsDir, current.modpackModsDir),
      pythonExecutable: normalizeText(settings.pythonExecutable, current.pythonExecutable),
      runtimeLogMaxAgeMinutes: Number.isFinite(Number(settings.runtimeLogMaxAgeMinutes))
        ? Math.max(1, Number(settings.runtimeLogMaxAgeMinutes))
        : current.runtimeLogMaxAgeMinutes,
      defaultScanMode: settings.defaultScanMode === "deep" ? "deep" : "quick"
    };
    this.setSetting("echoRoot", next.echoRoot);
    this.setSetting("modpackModsDir", next.modpackModsDir);
    this.setSetting("pythonExecutable", next.pythonExecutable);
    this.setSetting("runtimeLogMaxAgeMinutes", String(next.runtimeLogMaxAgeMinutes));
    this.setSetting("defaultScanMode", next.defaultScanMode);
    return next;
  }

  getSetting(key: string): string | null {
    const row = this.db.prepare("SELECT value FROM app_settings WHERE key = ?").get(key);
    return typeof row?.value === "string" ? row.value : null;
  }

  setSetting(key: string, value: string): void {
    this.db
      .prepare("INSERT INTO app_settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")
      .run(key, value);
  }

  createCommandRun(run: CommandRun): CommandRun {
    this.db
      .prepare(
        `INSERT INTO command_runs
        (run_id, project_slug, command_id, status, risk, command_json, started_at, finished_at, exit_code, pid, duration_ms, metadata_json, output)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .run(
        run.id,
        run.projectSlug,
        run.commandId,
        run.status,
        run.risk,
        json(run.command),
        run.startedAt,
        run.finishedAt ?? "",
        run.exitCode ?? null,
        run.pid ?? null,
        run.durationMs ?? null,
        json(run.metadata ?? {}),
        run.output
      );
    return this.getCommandRun(run.id) as CommandRun;
  }

  updateCommandRun(
    runId: string,
    patch: Partial<Pick<CommandRun, "status" | "finishedAt" | "exitCode" | "output" | "pid" | "durationMs" | "metadata">>
  ): CommandRun | null {
    const current = this.getCommandRun(runId);
    if (!current) {
      return null;
    }
    const next = { ...current, ...patch };
    this.db
      .prepare(
        "UPDATE command_runs SET status = ?, finished_at = ?, exit_code = ?, pid = ?, duration_ms = ?, metadata_json = ?, output = ? WHERE run_id = ?"
      )
      .run(
        next.status,
        next.finishedAt ?? "",
        next.exitCode ?? null,
        next.pid ?? null,
        next.durationMs ?? null,
        json(next.metadata ?? {}),
        next.output,
        runId
      );
    return this.getCommandRun(runId);
  }

  getCommandRun(runId: string): CommandRun | null {
    const row = this.db.prepare("SELECT * FROM command_runs WHERE run_id = ?").get(runId);
    return row ? rowToCommandRun(row) : null;
  }

  listCommandRuns(projectSlug: string, limit = 25): CommandRun[] {
    return this.db
      .prepare("SELECT * FROM command_runs WHERE project_slug = ? ORDER BY id DESC LIMIT ?")
      .all(projectSlug, limit)
      .map(rowToCommandRun);
  }

  private getScanReport(reportId: number): ScanReport | null {
    const row = this.db.prepare("SELECT * FROM scan_reports WHERE id = ?").get(reportId);
    if (!row) {
      return null;
    }
    const findings = this.db
      .prepare("SELECT * FROM qa_findings WHERE report_id = ? ORDER BY id ASC")
      .all(reportId)
      .map(rowToFinding);
    return {
      id: Number(row.id),
      projectSlug: String(row.project_slug),
      createdAt: String(row.created_at),
      mode: valueAsString(row.mode, "quick") === "deep" ? "deep" : "quick",
      status: scanStatus(row.status),
      startedAt: valueAsString(row.started_at, String(row.created_at)),
      finishedAt: valueAsString(row.finished_at) || undefined,
      durationMs: Number(row.duration_ms ?? 0),
      source: parseJson<Record<string, unknown>>(row.source_json, {}),
      rawOutput: valueAsString(row.raw_output),
      summary: parseJson<ScanReport["summary"]>(row.summary_json, {
        status: "Unknown",
        buildHealth: 0,
        criticalIssues: 0,
        polishTasks: 0,
        inventory: {},
        readinessScore: 0
      }),
      findings
    };
  }

  private migrate(): void {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        slug TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        kind TEXT NOT NULL,
        status TEXT NOT NULL,
        current_milestone TEXT NOT NULL,
        build_health INTEGER NOT NULL,
        critical_issues INTEGER NOT NULL,
        polish_tasks INTEGER NOT NULL,
        last_scan_label TEXT NOT NULL,
        next_recommended_action TEXT NOT NULL,
        accent TEXT NOT NULL,
        description TEXT NOT NULL,
        workspace_path TEXT NOT NULL,
        modules_json TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS roadmap_phases (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        order_index INTEGER NOT NULL,
        title TEXT NOT NULL,
        status TEXT NOT NULL,
        progress INTEGER NOT NULL,
        summary TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS qa_tracks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        track_key TEXT NOT NULL,
        title TEXT NOT NULL,
        severity TEXT NOT NULL,
        status TEXT NOT NULL,
        summary TEXT NOT NULL,
        checks_json TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS scan_reports (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        created_at TEXT NOT NULL,
        summary_json TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS qa_findings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        report_id INTEGER NOT NULL,
        track TEXT NOT NULL,
        title TEXT NOT NULL,
        severity TEXT NOT NULL,
        status TEXT NOT NULL,
        detail TEXT NOT NULL,
        path TEXT NOT NULL,
        FOREIGN KEY(report_id) REFERENCES scan_reports(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS prompt_templates (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        template_id TEXT NOT NULL,
        category TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT NOT NULL,
        body TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS terminal_planner (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        order_index INTEGER NOT NULL,
        group_name TEXT NOT NULL,
        pages_json TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS release_actions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        command_id TEXT NOT NULL,
        label TEXT NOT NULL,
        description TEXT NOT NULL,
        mode TEXT NOT NULL,
        risk TEXT NOT NULL,
        executable TEXT NOT NULL,
        args_json TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS feature_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_slug TEXT NOT NULL,
        feature_id TEXT NOT NULL,
        order_index INTEGER NOT NULL,
        title TEXT NOT NULL,
        category TEXT NOT NULL,
        status TEXT NOT NULL,
        player_promise TEXT NOT NULL,
        lore_context TEXT NOT NULL,
        implementation_summary TEXT NOT NULL,
        next_action TEXT NOT NULL,
        sources_json TEXT NOT NULL,
        evidence_json TEXT NOT NULL,
        UNIQUE(project_slug, feature_id)
      );

      CREATE TABLE IF NOT EXISTS command_runs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        run_id TEXT UNIQUE NOT NULL,
        project_slug TEXT NOT NULL,
        command_id TEXT NOT NULL,
        status TEXT NOT NULL,
        command_json TEXT NOT NULL,
        started_at TEXT NOT NULL,
        finished_at TEXT NOT NULL,
        exit_code INTEGER,
        output TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );
    `);
    this.addColumn("scan_reports", "mode", "TEXT NOT NULL DEFAULT 'quick'");
    this.addColumn("scan_reports", "status", "TEXT NOT NULL DEFAULT 'warning'");
    this.addColumn("scan_reports", "started_at", "TEXT NOT NULL DEFAULT ''");
    this.addColumn("scan_reports", "finished_at", "TEXT NOT NULL DEFAULT ''");
    this.addColumn("scan_reports", "duration_ms", "INTEGER NOT NULL DEFAULT 0");
    this.addColumn("scan_reports", "source_json", "TEXT NOT NULL DEFAULT '{}'");
    this.addColumn("scan_reports", "raw_output", "TEXT NOT NULL DEFAULT ''");
    this.addColumn("qa_findings", "line", "INTEGER");
    this.addColumn("qa_findings", "code", "TEXT NOT NULL DEFAULT ''");
    this.addColumn("qa_findings", "source", "TEXT NOT NULL DEFAULT ''");
    this.addColumn("qa_findings", "metadata_json", "TEXT NOT NULL DEFAULT '{}'");
    this.addColumn("command_runs", "risk", "TEXT NOT NULL DEFAULT 'low'");
    this.addColumn("command_runs", "pid", "INTEGER");
    this.addColumn("command_runs", "duration_ms", "INTEGER");
    this.addColumn("command_runs", "metadata_json", "TEXT NOT NULL DEFAULT '{}'");
  }

  private addColumn(table: string, column: string, definition: string): void {
    const rows = this.db.prepare(`PRAGMA table_info(${table})`).all();
    if (rows.some((row) => row.name === column)) {
      return;
    }
    this.db.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${definition}`);
  }

  private withLiveVersions(project: Project): Project {
    return projectWithLiveModuleVersions(project, this.getSettings());
  }

  private seed(): void {
    fs.mkdirSync(LOCAL_DATA_DIR, { recursive: true });
    this.db.exec("BEGIN");
    try {
      this.syncProjects();
      this.replaceRoadmap();
      this.replaceQaTracks();
      this.replacePrompts();
      this.replaceTerminalPlanner();
      this.replaceReleaseActions();
      this.replaceFeatures();
      this.ensureSettings();
      this.db.exec("COMMIT");
    } catch (error) {
      this.db.exec("ROLLBACK");
      throw error;
    }
  }

  private syncProjects(): void {
    const projects = liveSeedProjects();
    const slugs = projects.map((project) => project.slug);
    if (slugs.length > 0) {
      this.db.prepare(`DELETE FROM projects WHERE slug NOT IN (${slugs.map(() => "?").join(", ")})`).run(...slugs);
    }

    const insertProject = this.db.prepare(`
      INSERT INTO projects
      (slug, name, kind, status, current_milestone, build_health, critical_issues, polish_tasks, last_scan_label, next_recommended_action, accent, description, workspace_path, modules_json)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const updateProject = this.db.prepare(`
      UPDATE projects SET
        name = ?,
        kind = ?,
        status = ?,
        current_milestone = ?,
        accent = ?,
        description = ?,
        workspace_path = ?,
        modules_json = ?
      WHERE slug = ?
    `);
    for (const project of projects) {
      const existing = this.db.prepare("SELECT slug FROM projects WHERE slug = ?").get(project.slug);
      if (existing) {
        updateProject.run(
          project.name,
          project.kind,
          project.status,
          project.currentMilestone,
          project.accent,
          project.description,
          project.workspacePath,
          json(project.modules),
          project.slug
        );
      } else {
        insertProject.run(
          project.slug,
          project.name,
          project.kind,
          project.status,
          project.currentMilestone,
          project.buildHealth,
          project.criticalIssues,
          project.polishTasks,
          project.lastScanLabel,
          project.nextRecommendedAction,
          project.accent,
          project.description,
          project.workspacePath,
          json(project.modules)
        );
      }
    }
  }

  private replaceRoadmap(): void {
    this.db.exec("DELETE FROM roadmap_phases");
    const insertRoadmap = this.db.prepare(`
      INSERT INTO roadmap_phases (project_slug, order_index, title, status, progress, summary)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    seedData.roadmapPhases.forEach((phase, index) => {
      insertRoadmap.run(phase.projectSlug, index, phase.title, phase.status, phase.progress, phase.summary);
    });
  }

  private replaceQaTracks(): void {
    this.db.exec("DELETE FROM qa_tracks");
    const insertTrack = this.db.prepare(`
      INSERT INTO qa_tracks (project_slug, track_key, title, severity, status, summary, checks_json)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `);
    for (const track of seedData.qaTracks) {
      insertTrack.run(track.projectSlug, track.key, track.title, track.severity, track.status, track.summary, json(track.checks));
    }
  }

  private replacePrompts(): void {
    this.db.exec("DELETE FROM prompt_templates");
    const insertPrompt = this.db.prepare(`
      INSERT INTO prompt_templates (project_slug, template_id, category, title, description, body)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    for (const prompt of seedData.promptTemplates) {
      insertPrompt.run(prompt.projectSlug, prompt.id, prompt.category, prompt.title, prompt.description, prompt.body);
    }
  }

  private replaceTerminalPlanner(): void {
    this.db.exec("DELETE FROM terminal_planner");
    const insertPlanner = this.db.prepare(`
      INSERT INTO terminal_planner (project_slug, order_index, group_name, pages_json)
      VALUES (?, ?, ?, ?)
    `);
    seedData.terminalPlanner.forEach((group, index) => {
      insertPlanner.run(group.projectSlug, index, group.group, json(group.pages));
    });
  }

  private replaceReleaseActions(): void {
    this.db.exec("DELETE FROM release_actions");
    const insertAction = this.db.prepare(`
      INSERT INTO release_actions (project_slug, command_id, label, description, mode, risk, executable, args_json)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);
    for (const action of seedData.releaseActions) {
      insertAction.run(
        action.projectSlug,
        action.commandId,
        action.label,
        action.description,
        action.mode,
        action.risk,
        action.executable,
        json(action.args)
      );
    }
  }

  private replaceFeatures(): void {
    this.db.exec("DELETE FROM feature_records");
    const insertFeature = this.db.prepare(`
      INSERT INTO feature_records
      (project_slug, feature_id, order_index, title, category, status, player_promise, lore_context, implementation_summary, next_action, sources_json, evidence_json)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    for (const feature of featureCatalog.features) {
      insertFeature.run(
        feature.projectSlug,
        feature.id,
        Number(feature.order ?? 0),
        feature.title,
        feature.category,
        feature.status,
        feature.playerPromise,
        feature.loreContext,
        feature.implementationSummary,
        feature.nextAction,
        json(feature.sources ?? []),
        json(feature.evidence ?? [])
      );
    }
  }

  private ensureSettings(): void {
    const staleDefaults: Partial<Record<keyof AppSettings, string[]>> = {
      echoRoot: ["C:/New folder/Echo"],
      modpackModsDir: ["C:/Users/Ivan/curseforge/minecraft/Instances/Axes of Tomorrow/mods"],
      pythonExecutable: ["C:/Users/hacko/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe"]
    };
    for (const [key, value] of Object.entries(DEFAULT_SETTINGS)) {
      const existing = this.getSetting(key);
      const staleValues = staleDefaults[key as keyof AppSettings] ?? [];
      const normalizedExisting = existing?.replaceAll("\\", "/");
      if (existing == null || staleValues.includes(normalizedExisting ?? "")) {
        this.setSetting(key, String(value));
      }
    }
  }
}

function normalizeText(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function nextActionFromFindings(findings: QaFinding[]): string {
  const firstCritical = findings.find((finding) => finding.severity === "critical");
  if (firstCritical) {
    return `Fix ${firstCritical.title}`;
  }
  const firstHigh = findings.find((finding) => finding.severity === "high");
  if (firstHigh) {
    return `Review ${firstHigh.title}`;
  }
  return "Run full release verification";
}

function scanStatus(value: unknown): ScanStatus {
  return value === "passed" || value === "failed" || value === "running" || value === "warning" ? value : "warning";
}

function rowToProject(row: Record<string, unknown>): Project {
  return {
    slug: String(row.slug),
    name: String(row.name),
    kind: String(row.kind),
    status: String(row.status),
    currentMilestone: String(row.current_milestone),
    buildHealth: Number(row.build_health),
    criticalIssues: Number(row.critical_issues),
    polishTasks: Number(row.polish_tasks),
    lastScanLabel: String(row.last_scan_label),
    nextRecommendedAction: String(row.next_recommended_action),
    accent: String(row.accent),
    description: String(row.description),
    workspacePath: String(row.workspace_path),
    modules: parseJson<Project["modules"]>(row.modules_json, [])
  };
}

function rowToRoadmap(row: Record<string, unknown>): RoadmapPhase {
  return {
    projectSlug: String(row.project_slug),
    title: String(row.title),
    status: String(row.status),
    progress: Number(row.progress),
    summary: String(row.summary)
  };
}

function rowToQaTrack(row: Record<string, unknown>): QaTrack {
  return {
    projectSlug: String(row.project_slug),
    key: String(row.track_key),
    title: String(row.title),
    severity: String(row.severity) as QaTrack["severity"],
    status: String(row.status),
    summary: String(row.summary),
    checks: parseJson<string[]>(row.checks_json, [])
  };
}

function rowToPrompt(row: Record<string, unknown>): PromptTemplate {
  return {
    projectSlug: String(row.project_slug),
    id: String(row.template_id),
    category: String(row.category),
    title: String(row.title),
    description: String(row.description),
    body: String(row.body)
  };
}

function rowToTerminalPlanner(row: Record<string, unknown>): TerminalPlannerGroup {
  return {
    projectSlug: String(row.project_slug),
    group: String(row.group_name),
    pages: parseJson<string[]>(row.pages_json, [])
  };
}

function rowToReleaseAction(row: Record<string, unknown>): ReleaseAction {
  return {
    projectSlug: String(row.project_slug),
    commandId: String(row.command_id),
    label: String(row.label),
    description: String(row.description),
    mode: String(row.mode) as ReleaseAction["mode"],
    risk: String(row.risk) as ReleaseAction["risk"],
    executable: String(row.executable),
    args: parseJson<string[]>(row.args_json, [])
  };
}

function rowToFeatureRecord(row: Record<string, unknown>): FeatureRecord {
  return {
    projectSlug: String(row.project_slug),
    id: String(row.feature_id),
    title: String(row.title),
    category: String(row.category),
    status: String(row.status) as FeatureRecord["status"],
    playerPromise: String(row.player_promise),
    loreContext: String(row.lore_context),
    implementationSummary: String(row.implementation_summary),
    nextAction: String(row.next_action),
    sources: parseJson<FeatureRecord["sources"]>(row.sources_json, []),
    evidence: parseJson<FeatureRecord["evidence"]>(row.evidence_json, []),
    order: Number(row.order_index)
  };
}

function rowToFinding(row: Record<string, unknown>): QaFinding {
  return {
    id: Number(row.id),
    track: String(row.track),
    title: String(row.title),
    severity: String(row.severity) as QaFinding["severity"],
    status: String(row.status),
    detail: String(row.detail),
    path: String(row.path || ""),
    line: row.line === null || row.line === undefined ? undefined : Number(row.line),
    code: valueAsString(row.code) || undefined,
    source: valueAsString(row.source) || undefined,
    metadata: parseJson<Record<string, unknown>>(row.metadata_json, {})
  };
}

function rowToCommandRun(row: Record<string, unknown>): CommandRun {
  return {
    id: String(row.run_id),
    projectSlug: String(row.project_slug),
    commandId: String(row.command_id),
    status: String(row.status) as CommandRunStatus,
    risk: String(row.risk ?? "low") as ReleaseAction["risk"],
    command: parseJson<string[]>(row.command_json, []),
    startedAt: String(row.started_at),
    finishedAt: String(row.finished_at || "") || undefined,
    exitCode: row.exit_code === null || row.exit_code === undefined ? undefined : Number(row.exit_code),
    pid: row.pid === null || row.pid === undefined ? undefined : Number(row.pid),
    durationMs: row.duration_ms === null || row.duration_ms === undefined ? undefined : Number(row.duration_ms),
    metadata: parseJson<Record<string, unknown>>(row.metadata_json, {}),
    output: String(row.output)
  };
}
