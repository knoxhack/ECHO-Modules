import { randomUUID } from "node:crypto";
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import type {
  AppSettings,
  CommandRun,
  DistributionPack,
  DistributionPackId,
  DistributionReleaseStatus,
  DistributionStatus,
  DistributionSummary,
  PackExportRequest,
  PackExportResult,
  Project,
  ReleaseAction
} from "../shared/types.js";
import { CommandCenterStore } from "./db.js";
import { promoteJarArtifacts, type JarServiceOptions } from "./jars.js";
import { ECHO_ROOT, QUARANTINE_DIR, toDisplayPath } from "./paths.js";
import { projectWorkspaceRoot } from "./workspace.js";

const DISTRIBUTION_PROJECT_SLUG = "distributions";
const EXPORT_COMMAND_ID = "pack-local-export";
const OUTPUT_LIMIT = 240_000;
const activeExports = new Map<string, { stop: () => void }>();

export interface DistributionServiceOptions extends JarServiceOptions {
  runBuildCommand?: (context: {
    pack: DistributionPack;
    project: Project;
    action: ReleaseAction;
    settings: AppSettings;
    command: string[];
    cwd: string;
    signal: AbortSignal;
  }) => Promise<{ exitCode: number; output: string }>;
}

interface DistributionPackDefinition {
  id: DistributionPackId;
  name: string;
  repo: string;
  folderName: string;
  channel: string;
  includeAllModules: boolean;
  requiredModuleIds: string[];
  optionalModuleIds: string[];
}

const PACK_DEFINITIONS: DistributionPackDefinition[] = [
  {
    id: "echo-prime",
    name: "ECHO Prime",
    repo: "knoxhack/ECHO-Prime",
    folderName: "ECHO-Prime",
    channel: "stable",
    includeAllModules: true,
    requiredModuleIds: [],
    optionalModuleIds: []
  },
  {
    id: "ashfall",
    name: "Ashfall",
    repo: "knoxhack/Ashfall",
    folderName: "Ashfall",
    channel: "stable",
    includeAllModules: false,
    requiredModuleIds: [
      "echocore",
      "echonetcore",
      "echoashfallprotocol",
      "echoworldcore",
      "echoweathercore",
      "echoterminal",
      "echomissioncore",
      "echodatacore",
      "echoruntimeguard",
      "echorendercore",
      "echothemecore",
      "echoscreencore",
      "echowiki",
      "echolens",
      "echoholomap"
    ],
    optionalModuleIds: ["echoarmory", "echoblockworks", "echoplayercore", "echopowergrid", "echosoundcore", "echorelictech", "echorecovery", "echonpcore", "signalos", "signalosexample"]
  },
  {
    id: "orbital",
    name: "Orbital",
    repo: "knoxhack/Orbital",
    folderName: "Orbital",
    channel: "stable",
    includeAllModules: false,
    requiredModuleIds: ["echocore", "echonetcore", "echoorbitalremnants", "echostationfall", "echoterminal", "echomissioncore", "echodatacore", "echoruntimeguard", "echorendercore", "echothemecore", "echoscreencore", "echolens", "echoholomap"],
    optionalModuleIds: ["echonexusprotocol", "echoblockworks", "echoarmory", "echopowergrid", "echosoundcore", "echologisticsnetwork"]
  },
  {
    id: "arcane-division",
    name: "Arcana Division",
    repo: "knoxhack/Arcane-Division",
    folderName: "Arcane-Division",
    channel: "stable",
    includeAllModules: false,
    requiredModuleIds: ["echocore", "echonetcore", "echoarcanacore", "echoarcaneindex", "echogrimoire", "echoritualcore", "echospellcore", "echocursecore", "echodatacore", "echoruntimeguard", "echorendercore", "echothemecore", "echoscreencore", "echowiki"],
    optionalModuleIds: ["echorelictech", "echosoundcore", "echoindex", "echolens"]
  }
];

export class DistributionPipelineError extends Error {
  readonly statusCode: number;
  readonly summary: DistributionSummary;
  readonly run?: CommandRun;

  constructor(message: string, statusCode: number, summary: DistributionSummary, run?: CommandRun) {
    super(message);
    this.name = "DistributionPipelineError";
    this.statusCode = statusCode;
    this.summary = summary;
    this.run = run;
  }
}

export function buildDistributionSummary(store: CommandCenterStore, options: DistributionServiceOptions = {}): DistributionSummary {
  const settings = store.getSettings();
  const generatedAt = (options.now?.() ?? new Date()).toISOString();
  const repoRoot = path.dirname(path.resolve(settings.echoRoot || ECHO_ROOT));
  const echoReleaseDir = path.join(settings.echoRoot || ECHO_ROOT, "dist", "echo-module-release");
  const launcherRoot = path.join(repoRoot, "ECHOLauncher");
  const packs = PACK_DEFINITIONS.map((definition) => packFromDefinition(definition, repoRoot, store));
  return {
    generatedAt,
    echoModuleRelease: localReleaseStatus("knoxhack/ECHO", echoReleaseDir, ["echo-modules-index.json", "release-manifest.tsv", "checksums.sha256"]),
    launcherRelease: {
      repo: "knoxhack/ECHOLauncher",
      repoUrl: repoUrl("knoxhack/ECHOLauncher"),
      windows: localReleaseStatus("knoxhack/ECHOLauncher", path.join(launcherRoot, "installer-artifacts"), ["latest.yml"], [".exe"]),
      linux: localReleaseStatus("knoxhack/ECHOLauncher", path.join(launcherRoot, "installer-artifacts"), ["latest-linux.yml"], [".AppImage"])
    },
    packs,
    latestRun: listDistributionRuns(store, 1)[0] ?? null
  };
}

export function listDistributionRuns(store: CommandCenterStore, limit = 25): CommandRun[] {
  return store.listCommandRuns(DISTRIBUTION_PROJECT_SLUG, limit);
}

export async function startPackExport(store: CommandCenterStore, request: PackExportRequest, options: DistributionServiceOptions = {}): Promise<PackExportResult> {
  const summary = buildDistributionSummary(store, options);
  const pack = summary.packs.find((candidate) => candidate.id === request.packId);
  if (!pack) {
    throw new DistributionPipelineError(`Unknown distribution pack: ${request.packId}`, 404, summary);
  }
  if (request.confirmed !== true) {
    throw new DistributionPipelineError("Confirmation required", 409, summary);
  }
  if (hasRunningDistributionRun(store)) {
    throw new DistributionPipelineError("A distribution export is already running.", 409, summary);
  }
  const targetModsDir = request.targetModsDir.trim();
  if (!targetModsDir) {
    throw new DistributionPipelineError("Target dev mods folder is required.", 400, summary);
  }
  if (!isDirectory(targetModsDir)) {
    throw new DistributionPipelineError(`Target dev mods folder does not exist: ${targetModsDir}`, 400, summary);
  }

  const settings = { ...store.getSettings(), modpackModsDir: targetModsDir };
  const project = projectForPack(store, pack, request.includeOptionalModules === true);
  const startedAt = new Date().toISOString();
  const run = store.createCommandRun({
    id: randomUUID(),
    projectSlug: DISTRIBUTION_PROJECT_SLUG,
    commandId: EXPORT_COMMAND_ID,
    status: "running",
    risk: "high",
    command: ["pack-export", pack.id, targetModsDir],
    startedAt,
    metadata: {
      packId: pack.id,
      targetModsDir,
      buildFirst: request.buildFirst === true,
      includeOptionalModules: request.includeOptionalModules === true
    },
    output: `Distribution export queued for ${pack.name}.\n`
  });

  try {
    let output = run.output;
    if (request.buildFirst) {
      const action = buildActionForProject(project);
      const command = [action.executable, ...action.args];
      const cwd = projectWorkspaceRoot(project, settings);
      store.updateCommandRun(run.id, { output: appendOutput(output, `Running build: ${command.join(" ")}\n`) });
      const build = await runBuild(run.id, pack, project, action, settings, command, cwd, options);
      output = appendOutput(store.getCommandRun(run.id)?.output ?? output, build.output ? `${build.output}\n` : `Build exited ${build.exitCode}\n`);
      const currentRun = store.getCommandRun(run.id);
      if (currentRun?.status === "stopped") {
        return emptyResult(store, currentRun, options);
      }
      if (build.exitCode !== 0) {
        throw new Error(`${pack.name} local build failed with exit code ${build.exitCode}.`);
      }
    }

    const promoted = promoteJarArtifacts(project, settings, {
      buildRoot: options.buildRoot,
      quarantineDir: options.quarantineDir,
      now: options.now
    });
    const finalOutput = appendOutput(output, promoted.output);
    const finished = store.updateCommandRun(run.id, {
      status: "succeeded",
      finishedAt: new Date().toISOString(),
      exitCode: 0,
      durationMs: durationFrom(startedAt),
      metadata: {
        ...(run.metadata ?? {}),
        moved: promoted.moved.length,
        copied: promoted.copied.length,
        verified: promoted.verified.length
      },
      output: finalOutput
    }) as CommandRun;
    return {
      run: finished,
      summary: buildDistributionSummary(store, options),
      pack,
      moved: promoted.moved,
      copied: promoted.copied,
      verified: promoted.verified
    };
  } catch (error) {
    const currentRun = store.getCommandRun(run.id);
    if (currentRun?.status === "stopped") {
      return emptyResult(store, currentRun, options);
    }
    const message = error instanceof Error ? error.message : String(error);
    const failed = store.updateCommandRun(run.id, {
      status: "failed",
      finishedAt: new Date().toISOString(),
      exitCode: 1,
      durationMs: durationFrom(startedAt),
      output: appendOutput(store.getCommandRun(run.id)?.output ?? run.output, `Distribution export failed: ${message}\n`)
    }) as CommandRun;
    throw new DistributionPipelineError(message, 500, buildDistributionSummary(store, options), failed);
  }
}

export function stopDistributionRun(store: CommandCenterStore, runId: string, options: DistributionServiceOptions = {}): PackExportResult | null {
  const run = store.getCommandRun(runId);
  if (!run || run.projectSlug !== DISTRIBUTION_PROJECT_SLUG) {
    return null;
  }
  if (run.status !== "running") {
    return emptyResult(store, run, options);
  }
  const active = activeExports.get(runId);
  if (active) {
    active.stop();
    activeExports.delete(runId);
  }
  const stopped = store.updateCommandRun(runId, {
    status: "stopped",
    finishedAt: new Date().toISOString(),
    exitCode: 130,
    durationMs: durationFrom(run.startedAt),
    metadata: {
      ...(run.metadata ?? {}),
      stopResult: active ? "signal-sent" : "orphaned-running-run"
    },
    output: appendOutput(run.output, active ? "Stopped by Command Center.\n" : "Stopped by stale-run recovery.\n")
  }) as CommandRun;
  return emptyResult(store, stopped, options);
}

function packFromDefinition(definition: DistributionPackDefinition, repoRoot: string, store: CommandCenterStore): DistributionPack {
  const localPath = path.join(repoRoot, definition.folderName);
  const echoProject = store.getProject("echo");
  const moduleIds = echoProject?.modules.map((module) => module.modId) ?? [];
  const requiredMissing = definition.requiredModuleIds.filter((moduleId) => !moduleIds.includes(moduleId));
  const selectedModuleCount = definition.includeAllModules
    ? moduleIds.length
    : definition.requiredModuleIds.length + definition.optionalModuleIds.filter((moduleId) => moduleIds.includes(moduleId)).length;
  const blockers = [
    ...(!isDirectory(localPath) ? [`Local pack repo folder missing: ${localPath}`] : []),
    ...requiredMissing.map((moduleId) => `Required module is not in the ECHO seed catalog: ${moduleId}`)
  ];
  return {
    id: definition.id,
    name: definition.name,
    repo: definition.repo,
    repoUrl: repoUrl(definition.repo),
    localPath: toDisplayPath(localPath),
    channel: definition.channel,
    includeAllModules: definition.includeAllModules,
    requiredModuleIds: definition.requiredModuleIds,
    optionalModuleIds: definition.optionalModuleIds,
    selectedModuleCount,
    latestRelease: localReleaseStatus(definition.repo, path.join(localPath, "dist", "releases"), ["echo-release.json", "checksums.sha256"], ["-pack.zip", ".pack.json"]),
    blockers
  };
}

function localReleaseStatus(repo: string, directory: string, requiredFiles: string[], suffixes: string[] = []): DistributionReleaseStatus {
  const assets = listFiles(directory);
  const blockers = requiredFiles.filter((fileName) => !assets.some((asset) => path.basename(asset) === fileName));
  for (const suffix of suffixes) {
    if (!assets.some((asset) => asset.endsWith(suffix))) {
      blockers.push(`Missing asset ending in ${suffix}`);
    }
  }
  const latest = latestReleaseVersionFromAssets(assets);
  return {
    repo,
    repoUrl: repoUrl(repo),
    status: !isDirectory(directory) || blockers.length > 0 ? "missing" : "ready",
    latestVersion: latest,
    releaseUrl: latest ? `${repoUrl(repo)}/releases/tag/${latest}` : undefined,
    assets: assets.map((asset) => toDisplayPath(asset)),
    blockers
  };
}

function projectForPack(store: CommandCenterStore, pack: DistributionPack, includeOptionalModules: boolean): Project {
  const echoProject = store.getProject("echo");
  if (!echoProject) {
    throw new Error("ECHO project seed data is missing.");
  }
  const selectedIds = pack.includeAllModules
    ? new Set(echoProject.modules.map((module) => module.modId))
    : new Set([...pack.requiredModuleIds, ...(includeOptionalModules ? pack.optionalModuleIds : [])]);
  const modules = echoProject.modules.filter((module) => selectedIds.has(module.modId));
  const missing = pack.requiredModuleIds.filter((moduleId) => !modules.some((module) => module.modId === moduleId));
  if (missing.length > 0) {
    throw new Error(`Pack ${pack.name} is missing required module seed data: ${missing.join(", ")}`);
  }
  return {
    ...echoProject,
    name: pack.name,
    modules
  };
}

function buildActionForProject(project: Project): ReleaseAction {
  return {
    projectSlug: project.slug,
    commandId: "build-full-stack",
    label: "Build ECHO Workspace",
    description: "Build all ECHO modules before local pack export.",
    mode: "shell",
    risk: "high",
    executable: process.platform === "win32" ? "gradlew.bat" : "./gradlew",
    args: ["buildEchoWorkspace", "-PechoAddonSet=all"]
  };
}

async function runBuild(
  runId: string,
  pack: DistributionPack,
  project: Project,
  action: ReleaseAction,
  settings: AppSettings,
  command: string[],
  cwd: string,
  options: DistributionServiceOptions
): Promise<{ exitCode: number; output: string }> {
  if (options.runBuildCommand) {
    const controller = new AbortController();
    activeExports.set(runId, { stop: () => controller.abort() });
    try {
      return await options.runBuildCommand({ pack, project, action, settings, command, cwd, signal: controller.signal });
    } finally {
      activeExports.delete(runId);
    }
  }
  return new Promise((resolve) => {
    const child = spawn(action.executable, action.args, {
      cwd,
      shell: process.platform === "win32",
      windowsHide: true
    });
    activeExports.set(runId, { stop: () => killChild(child) });
    let output = "";
    const append = (chunk: Buffer): void => {
      output = appendOutput(output, chunk.toString());
    };
    child.stdout.on("data", append);
    child.stderr.on("data", append);
    child.on("error", (error) => {
      activeExports.delete(runId);
      resolve({ exitCode: 1, output: appendOutput(output, error.message) });
    });
    child.on("close", (code) => {
      activeExports.delete(runId);
      resolve({ exitCode: code ?? 1, output });
    });
  });
}

function hasRunningDistributionRun(store: CommandCenterStore): boolean {
  return store.listCommandRuns(DISTRIBUTION_PROJECT_SLUG, 10).some((run) => run.status === "running");
}

function emptyResult(store: CommandCenterStore, run: CommandRun, options: DistributionServiceOptions): PackExportResult {
  const summary = buildDistributionSummary(store, options);
  const packId = String(run.metadata?.packId ?? "echo-prime") as DistributionPackId;
  return {
    run,
    summary,
    pack: summary.packs.find((candidate) => candidate.id === packId) ?? summary.packs[0],
    moved: [],
    copied: [],
    verified: []
  };
}

function listFiles(directory: string): string[] {
  if (!isDirectory(directory)) {
    return [];
  }
  const files: string[] = [];
  const visit = (current: string): void => {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const target = path.join(current, entry.name);
      if (entry.isDirectory()) {
        visit(target);
      } else if (entry.isFile()) {
        files.push(target);
      }
    }
  };
  visit(directory);
  return files.sort((left, right) => left.localeCompare(right));
}

function latestReleaseVersionFromAssets(assets: string[]): string | undefined {
  const names = assets.map((asset) => path.basename(asset));
  const metadata = names.find((name) => name === "echo-release.json");
  if (metadata) {
    return "local";
  }
  const yml = names.find((name) => name === "latest.yml" || name === "latest-linux.yml");
  return yml ? "local" : undefined;
}

function repoUrl(repo: string): string {
  return `https://github.com/${repo}`;
}

function isDirectory(value: string): boolean {
  try {
    return fs.statSync(value).isDirectory();
  } catch {
    return false;
  }
}

function appendOutput(current: string, next: string): string {
  const output = `${current}${next}`;
  return output.length > OUTPUT_LIMIT ? output.slice(output.length - OUTPUT_LIMIT) : output;
}

function durationFrom(startedAt: string): number {
  return Math.max(0, Date.now() - new Date(startedAt).getTime());
}

function killChild(child: ChildProcessWithoutNullStreams): void {
  if (!child.killed) {
    child.kill();
  }
}
