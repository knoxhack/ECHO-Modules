import { createHash, randomUUID } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import type {
  AppSettings,
  CommandRun,
  JarArtifact,
  JarManifest,
  JarTargetEntry,
  JarTargetStatus,
  Project
} from "../shared/types.js";
import { CommandCenterStore } from "./db.js";
import { ECHO_ROOT, QUARANTINE_DIR, toDisplayPath } from "./paths.js";
import { projectWithLiveModuleVersions, projectWorkspaceRoot } from "./workspace.js";

export interface JarServiceOptions {
  buildRoot?: string;
  quarantineDir?: string;
  now?: () => Date;
}

interface PromoteOperation {
  manifest: JarManifest;
  moved: string[];
  copied: string[];
  verified: string[];
  output: string;
}

export class JarPipelineError extends Error {
  readonly statusCode: number;
  readonly manifest?: JarManifest;
  readonly run?: CommandRun;

  constructor(message: string, statusCode = 400, manifest?: JarManifest, run?: CommandRun) {
    super(message);
    this.name = "JarPipelineError";
    this.statusCode = statusCode;
    this.manifest = manifest;
    this.run = run;
  }
}

const EXTRA_ECHO_JAR_PREFIXES = ["echoindustrialrebirth"];

export function jarBuildCommandId(project: Project): string {
  return project.slug === "echo" ? "build-full-stack" : "build-module";
}

export function buildJarManifest(project: Project, settings: AppSettings, options: JarServiceOptions = {}): JarManifest {
  const liveProject = projectWithLiveModuleVersions(project, settings);
  const generatedAt = (options.now?.() ?? new Date()).toISOString();
  const workspaceRoot = projectWorkspaceRoot(liveProject, settings);
  const buildRoot = options.buildRoot ?? resolveGradleBuildRoot(liveProject, settings);
  const targetDir = settings.modpackModsDir.trim();
  const targetConfigured = targetDir.length > 0;
  const targetExists = targetConfigured && isDirectory(targetDir);
  const blockers: string[] = [];

  if (!targetConfigured) {
    blockers.push("Settings > Modpack Mods Folder is not configured. Copy and promote actions are blocked.");
  } else if (!targetExists) {
    blockers.push(`Configured Modpack Mods Folder does not exist: ${toDisplayPath(targetDir)}`);
  }

  const artifacts = [
    ...liveProject.modules.map((module) => buildArtifact(liveProject, module, workspaceRoot, buildRoot, targetConfigured ? targetDir : "")),
    ...discoverAdditionalEchoArtifacts(liveProject, buildRoot, targetConfigured ? targetDir : "")
  ];
  const targetEntries = targetExists ? scanTargetEntries(liveProject, artifacts, targetDir) : [];
  const entriesByName = new Map(targetEntries.map((entry) => [entry.fileName.toLowerCase(), entry]));
  for (const artifact of artifacts) {
    const targetEntry = entriesByName.get(artifact.expectedFileName.toLowerCase());
    artifact.current = artifact.exists && targetEntry?.status === "current";
    artifact.status = artifactStatus(artifact, targetEntry);
  }

  return {
    projectSlug: liveProject.slug,
    generatedAt,
    buildRoot: toDisplayPath(buildRoot),
    targetDir: toDisplayPath(targetDir),
    targetConfigured,
    targetExists,
    quarantineDir: toDisplayPath(options.quarantineDir ?? QUARANTINE_DIR),
    artifacts,
    targetEntries,
    blockers,
    summary: {
      expected: artifacts.length,
      built: artifacts.filter((artifact) => artifact.exists).length,
      missing: artifacts.filter((artifact) => !artifact.exists).length,
      current: artifacts.filter((artifact) => artifact.current).length,
      stale: targetEntries.filter((entry) => entry.status === "stale").length,
      duplicate: targetEntries.filter((entry) => entry.status === "duplicate").length,
      foreign: targetEntries.filter((entry) => entry.status === "foreign").length
    }
  };
}

export function promoteJarArtifacts(project: Project, settings: AppSettings, options: JarServiceOptions = {}): PromoteOperation {
  const manifest = buildJarManifest(project, settings, options);
  if (manifest.blockers.length > 0) {
    throw new JarPipelineError(manifest.blockers.join("\n"), 400, manifest);
  }

  const missing = manifest.artifacts.filter((artifact) => !artifact.exists);
  if (missing.length > 0) {
    throw new JarPipelineError(
      `Missing source jar(s): ${missing.map((artifact) => artifact.expectedFileName).join(", ")}`,
      409,
      manifest
    );
  }

  const targetDir = settings.modpackModsDir.trim();
  const promotedAt = options.now?.() ?? new Date();
  const quarantineDir = path.join(options.quarantineDir ?? QUARANTINE_DIR, stamp(promotedAt), project.slug);
  const moved: string[] = [];
  const copied: string[] = [];
  const verified: string[] = [];
  const quarantineStatuses: JarTargetStatus[] = ["stale", "duplicate"];

  for (const entry of manifest.targetEntries) {
    if (!quarantineStatuses.includes(entry.status)) {
      continue;
    }
    fs.mkdirSync(quarantineDir, { recursive: true });
    const destination = uniqueDestination(path.join(quarantineDir, entry.fileName));
    moveManagedJar(entry.path, destination, manifest);
    moved.push(toDisplayPath(destination));
  }

  for (const artifact of manifest.artifacts) {
    const targetPath = path.join(targetDir, artifact.expectedFileName);
    if (fs.existsSync(targetPath)) {
      const targetChecksum = checksumFile(targetPath);
      if (artifact.checksum && targetChecksum !== artifact.checksum) {
        fs.mkdirSync(quarantineDir, { recursive: true });
        const destination = uniqueDestination(path.join(quarantineDir, artifact.expectedFileName));
        moveManagedJar(targetPath, destination, manifest);
        moved.push(toDisplayPath(destination));
      }
    }
    copyManagedJar(artifact.sourcePath, targetPath, manifest);
    touchPromotedJar(targetPath, promotedAt);
    copied.push(toDisplayPath(targetPath));

    const sourceStat = fs.statSync(artifact.sourcePath);
    const targetStat = fs.statSync(targetPath);
    const sourceChecksum = artifact.checksum ?? checksumFile(artifact.sourcePath);
    const targetChecksum = checksumFile(targetPath);
    if (sourceStat.size !== targetStat.size || sourceChecksum !== targetChecksum) {
      throw new JarPipelineError(`Copied jar verification failed for ${artifact.expectedFileName}`, 500, manifest);
    }
    verified.push(`${artifact.expectedFileName} ${targetChecksum}`);
  }

  const finalManifest = buildJarManifest(project, settings, options);
  const output = [
    `Jar promotion completed for ${project.name}.`,
    `Target: ${toDisplayPath(targetDir)}`,
    `Quarantine: ${moved.length > 0 ? toDisplayPath(quarantineDir) : "not needed"}`,
    `Moved stale/conflicting jars: ${moved.length}`,
    ...moved.map((file) => `  moved ${file}`),
    `Copied current jars: ${copied.length}`,
    ...copied.map((file) => `  copied ${file}`),
    `Verified jars: ${verified.length}`,
    ...verified.map((line) => `  verified ${line}`)
  ].join("\n");

  return {
    manifest: finalManifest,
    moved,
    copied,
    verified,
    output
  };
}

export function runJarPromotion(
  store: CommandCenterStore,
  project: Project,
  settings: AppSettings,
  options: JarServiceOptions = {}
): PromoteOperation & { run: CommandRun } {
  const startedAt = new Date().toISOString();
  const run = store.createCommandRun({
    id: randomUUID(),
    projectSlug: project.slug,
    commandId: "promote-jars",
    status: "running",
    risk: "high",
    command: ["jar-promote", project.slug],
    startedAt,
    metadata: { targetDir: settings.modpackModsDir },
    output: ""
  });

  try {
    const operation = promoteJarArtifacts(project, settings, options);
    const finished = store.updateCommandRun(run.id, {
      status: "succeeded",
      finishedAt: new Date().toISOString(),
      exitCode: 0,
      durationMs: durationFrom(startedAt),
      metadata: {
        ...(run.metadata ?? {}),
        moved: operation.moved.length,
        copied: operation.copied.length,
        verified: operation.verified.length,
        manifestGeneratedAt: operation.manifest.generatedAt
      },
      output: operation.output
    }) as CommandRun;
    return { ...operation, run: finished };
  } catch (error) {
    const status = error instanceof JarPipelineError && error.statusCode < 500 ? "rejected" : "failed";
    const manifest = error instanceof JarPipelineError ? error.manifest : undefined;
    const message = error instanceof Error ? error.message : String(error);
    const failed = store.updateCommandRun(run.id, {
      status,
      finishedAt: new Date().toISOString(),
      exitCode: 1,
      durationMs: durationFrom(startedAt),
      metadata: {
        ...(run.metadata ?? {}),
        blockers: manifest?.blockers ?? [],
        manifestGeneratedAt: manifest?.generatedAt
      },
      output: message
    }) as CommandRun;
    if (error instanceof JarPipelineError) {
      throw new JarPipelineError(message, error.statusCode, manifest, failed);
    }
    throw new JarPipelineError(message, 500, manifest, failed);
  }
}

export function resolveGradleBuildRoot(project: Project, settings: AppSettings): string {
  if (project.slug !== "echo" && !isEchoWorkspaceProject(project)) {
    return path.join(projectWorkspaceRoot(project, settings), "build");
  }
  const base = process.env.ECHO_GRADLE_BUILD_DIR
    ? path.resolve(process.env.ECHO_GRADLE_BUILD_DIR)
    : path.join(process.env.LOCALAPPDATA ?? os.tmpdir(), "EchoGradleBuild");
  const echoRoot = path.resolve(settings.echoRoot || ECHO_ROOT);
  return path.join(base, path.basename(echoRoot));
}

function buildArtifact(project: Project, module: Project["modules"][number], workspaceRoot: string, buildRoot: string, targetDir: string): JarArtifact {
  const expectedFileName = `${module.modId}-${module.version}.jar`;
  const sourcePath = artifactSourcePath(project, module, workspaceRoot, buildRoot, expectedFileName);
  const stat = safeStat(sourcePath);
  return {
    moduleId: module.modId,
    label: module.label,
    version: module.version,
    expectedFileName,
    sourcePath: toDisplayPath(sourcePath),
    targetPath: targetDir ? toDisplayPath(path.join(targetDir, expectedFileName)) : undefined,
    exists: Boolean(stat?.isFile()),
    current: false,
    status: stat?.isFile() ? "built" : "missing",
    size: stat?.isFile() ? stat.size : undefined,
    modifiedAt: stat?.isFile() ? stat.mtime.toISOString() : undefined,
    checksum: stat?.isFile() ? checksumFile(sourcePath) : undefined
  };
}

function discoverAdditionalEchoArtifacts(project: Project, buildRoot: string, targetDir: string): JarArtifact[] {
  if (project.slug !== "echo" || !isDirectory(buildRoot)) {
    return [];
  }
  const knownModuleIds = new Set(project.modules.map((module) => module.modId.toLowerCase()));
  const discovered: JarArtifact[] = [];
  for (const directory of fs.readdirSync(buildRoot, { withFileTypes: true })) {
    if (!directory.isDirectory()) {
      continue;
    }
    const libsDir = path.join(buildRoot, directory.name, "libs");
    if (!isDirectory(libsDir)) {
      continue;
    }
    for (const jar of fs.readdirSync(libsDir, { withFileTypes: true })) {
      if (!jar.isFile() || !jar.name.toLowerCase().endsWith(".jar")) {
        continue;
      }
      const parsed = parseJarFileName(jar.name);
      if (!parsed || knownModuleIds.has(parsed.moduleId.toLowerCase())) {
        continue;
      }
      if (!isManagedEchoModuleId(parsed.moduleId)) {
        continue;
      }
      const sourcePath = path.join(libsDir, jar.name);
      const stat = fs.statSync(sourcePath);
      discovered.push({
        moduleId: parsed.moduleId,
        label: labelFromModuleId(parsed.moduleId),
        version: parsed.version,
        expectedFileName: jar.name,
        sourcePath: toDisplayPath(sourcePath),
        targetPath: targetDir ? toDisplayPath(path.join(targetDir, jar.name)) : undefined,
        exists: true,
        current: false,
        status: "built",
        size: stat.size,
        modifiedAt: stat.mtime.toISOString(),
        checksum: checksumFile(sourcePath)
      });
      knownModuleIds.add(parsed.moduleId.toLowerCase());
    }
  }
  return discovered.sort((left, right) => left.moduleId.localeCompare(right.moduleId));
}

function parseJarFileName(fileName: string): { moduleId: string; version: string } | null {
  const match = /^(.+)-(\d[^/\\]*)\.jar$/i.exec(fileName);
  if (!match) {
    return null;
  }
  return { moduleId: match[1], version: match[2] };
}

function isManagedEchoModuleId(moduleId: string): boolean {
  const normalized = moduleId.toLowerCase();
  return normalized.startsWith("echo") || normalized.startsWith("signalos");
}

function labelFromModuleId(moduleId: string): string {
  const withoutPrefix = moduleId.replace(/^echo/i, "ECHO ");
  return withoutPrefix
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function artifactSourcePath(project: Project, module: Project["modules"][number], workspaceRoot: string, buildRoot: string, expectedFileName: string): string {
  if (project.slug === "echo" || isEchoWorkspaceProject(project)) {
    return path.join(buildRoot, moduleBuildDirectory(module), "libs", expectedFileName);
  }
  if (!module.path || module.path === ".") {
    return path.join(buildRoot, "libs", expectedFileName);
  }
  return path.join(workspaceRoot, module.path, "build", "libs", expectedFileName);
}

function moveManagedJar(sourcePath: string, destination: string, manifest: JarManifest): void {
  try {
    fs.renameSync(sourcePath, destination);
  } catch (error) {
    throw translateManagedJarFileError(error, "quarantined", sourcePath, manifest);
  }
}

function copyManagedJar(sourcePath: string, targetPath: string, manifest: JarManifest): void {
  try {
    fs.copyFileSync(sourcePath, targetPath);
  } catch (error) {
    throw translateManagedJarFileError(error, "replaced", targetPath, manifest);
  }
}

function touchPromotedJar(targetPath: string, promotedAt: Date): void {
  try {
    fs.utimesSync(targetPath, promotedAt, promotedAt);
  } catch (error) {
    if (!isLockedFileError(error)) {
      throw error;
    }
  }
}

function translateManagedJarFileError(error: unknown, action: string, filePath: string, manifest: JarManifest): Error {
  if (isLockedFileError(error)) {
    return new JarPipelineError(
      [
        `Managed jar is locked and cannot be ${action}: ${toDisplayPath(filePath)}`,
        "Close the running Minecraft instance or CurseForge profile using this mods folder, then run Rebuild & Update All again.",
        "No files are deleted. Stale jars remain in place until the app can quarantine or replace them."
      ].join("\n"),
      423,
      manifest
    );
  }
  return error instanceof Error ? error : new Error(String(error));
}

function isLockedFileError(error: unknown): boolean {
  const code = typeof error === "object" && error !== null && "code" in error ? String((error as { code?: unknown }).code) : "";
  return code === "EBUSY" || code === "EPERM" || code === "EACCES";
}

function artifactStatus(artifact: JarArtifact, targetEntry: JarTargetEntry | undefined): JarArtifact["status"] {
  if (!artifact.exists) {
    return "missing";
  }
  if (!targetEntry) {
    return "built";
  }
  return targetEntry.status === "current" ? "current" : "stale";
}

function scanTargetEntries(project: Project, artifacts: JarArtifact[], targetDir: string): JarTargetEntry[] {
  const files = fs
    .readdirSync(targetDir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.toLowerCase().endsWith(".jar"))
    .map((entry) => entry.name);
  const artifactsByPrefix = new Map(artifacts.map((artifact) => [`${artifact.moduleId.toLowerCase()}-`, artifact]));
  const scopedPrefixes = new Set(artifactsByPrefix.keys());
  if (project.slug === "echo") {
    for (const prefix of EXTRA_ECHO_JAR_PREFIXES) {
      scopedPrefixes.add(`${prefix}-`);
    }
  }
  const relevant = files
    .map((fileName) => {
      const lowerName = fileName.toLowerCase();
      const prefix = Array.from(scopedPrefixes).find((candidate) => lowerName.startsWith(candidate));
      return prefix ? { fileName, lowerName, prefix } : null;
    })
    .filter((entry): entry is { fileName: string; lowerName: string; prefix: string } => Boolean(entry));
  const countsByPrefix = relevant.reduce<Map<string, number>>((counts, entry) => {
    counts.set(entry.prefix, (counts.get(entry.prefix) ?? 0) + 1);
    return counts;
  }, new Map());

  return relevant
    .map((entry) => {
      const artifact = artifactsByPrefix.get(entry.prefix);
      const filePath = path.join(targetDir, entry.fileName);
      const stat = fs.statSync(filePath);
      const expectedName = artifact?.expectedFileName.toLowerCase();
      const sourceChecksum = artifact?.checksum;
      const targetChecksum = checksumFile(filePath);
      let status: JarTargetStatus = "stale";
      if (!artifact) {
        status = "stale";
      } else if (entry.lowerName !== expectedName) {
        status = (countsByPrefix.get(entry.prefix) ?? 0) > 1 ? "duplicate" : "stale";
      } else if (sourceChecksum && sourceChecksum !== targetChecksum) {
        status = "stale";
      } else {
        status = "current";
      }
      return {
        moduleId: artifact?.moduleId,
        version: artifact?.version,
        expectedFileName: artifact?.expectedFileName,
        fileName: entry.fileName,
        path: toDisplayPath(filePath),
        status,
        size: stat.size,
        modifiedAt: stat.mtime.toISOString(),
        checksum: targetChecksum
      };
    })
    .sort((left, right) => left.fileName.localeCompare(right.fileName));
}

function moduleBuildDirectory(module: Project["modules"][number]): string {
  const normalized = module.path.replaceAll("\\", "/");
  if (!normalized || normalized === ".") {
    return "root";
  }
  return normalized.split("/").filter(Boolean).at(-1) ?? module.modId;
}

function isEchoWorkspaceProject(project: Project): boolean {
  const normalizedWorkspace = project.workspacePath.replaceAll("\\", "/").toLowerCase();
  const normalizedSeedRoot = ECHO_ROOT.replaceAll("\\", "/").toLowerCase();
  return normalizedWorkspace === normalizedSeedRoot || normalizedWorkspace.startsWith(`${normalizedSeedRoot}/`);
}

function isDirectory(value: string): boolean {
  try {
    return fs.statSync(value).isDirectory();
  } catch {
    return false;
  }
}

function safeStat(value: string): fs.Stats | null {
  try {
    return fs.statSync(value);
  } catch {
    return null;
  }
}

function checksumFile(filePath: string): string {
  return createHash("sha256").update(fs.readFileSync(filePath)).digest("hex");
}

function uniqueDestination(filePath: string): string {
  if (!fs.existsSync(filePath)) {
    return filePath;
  }
  const parsed = path.parse(filePath);
  let index = 2;
  while (true) {
    const candidate = path.join(parsed.dir, `${parsed.name}-${index}${parsed.ext}`);
    if (!fs.existsSync(candidate)) {
      return candidate;
    }
    index += 1;
  }
}

function stamp(date: Date): string {
  return date.toISOString().replace(/[:.]/g, "-");
}

function durationFrom(startedAt: string): number {
  return Math.max(0, Date.now() - new Date(startedAt).getTime());
}
