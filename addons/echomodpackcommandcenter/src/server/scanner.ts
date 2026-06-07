import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import type { AppSettings, Project, QaFinding, QaTrack, ScanMode, ScanReport, ScanStatus } from "../shared/types.js";
import { DEFAULT_PYTHON_EXECUTABLE, toDisplayPath } from "./paths.js";
import { projectWithLiveModuleVersions, projectWorkspaceRoot } from "./workspace.js";

const SKIPPED_DIRS = new Set([".git", ".gradle", "build", "dist", "dist-server", "node_modules", ".local"]);
const FINDING_LIMIT_PER_CODE = 40;
const RAW_OUTPUT_LIMIT = 180_000;
const TERMINAL_EXPECTED_PAGES = [
  "Command Deck",
  "Signal Leads",
  "Route Map",
  "Field Archive",
  "Vitals Scan",
  "Companion Link",
  "Survival Index",
  "Nexus Core",
  "Industrial Nexus"
];
const HANDOFF_TOKENS = [
  "Ashfall",
  "Orbital Remnants",
  "Stationfall",
  "Nexus Protocol",
  "Industrial Nexus",
  "Blackbox Protocol",
  "-PechoAddonSet=all"
];
const CRASH_SIGNATURES = [
  "Crash report saved to",
  "Encountered exception during server tick loop",
  "NoClassDefFoundError",
  "ClassNotFoundException",
  "ExceptionInInitializerError",
  "Could not execute entrypoint",
  "Mod loading error has occurred",
  "Loading errors encountered",
  "Error during pre-loading phase",
  "Mixin apply failed"
];
const IGNORED_RUNTIME_LOG_SIGNATURE_LINES = [
  "test missing recipe bridge"
];

interface ScannerOutput {
  projectSlug: string;
  mode: ScanMode;
  status: ScanStatus;
  startedAt: string;
  finishedAt: string;
  durationMs: number;
  source: Record<string, unknown>;
  rawOutput: string;
  summary: ScanReport["summary"];
  findings: QaFinding[];
}

interface ResourceSet {
  modId: string;
  resourceRoot: string;
  assetRoot: string;
  models: Set<string>;
  textures: Set<string>;
  lang: Record<string, string>;
}

interface ValidatorResult {
  source: string;
  status: "passed" | "failed";
  exitCode: number | null;
  durationMs: number;
  output: string;
  findings: QaFinding[];
}

export function runHybridScan(project: Project, tracks: QaTrack[], settings: AppSettings, mode: ScanMode): ScannerOutput {
  const liveProject = projectWithLiveModuleVersions(project, settings);
  const startedAt = new Date().toISOString();
  const start = Date.now();
  const workspaceRoot = projectWorkspaceRoot(liveProject, settings);
  const scanRoot = projectScanRoot(liveProject, workspaceRoot);
  const runtimeRoot = projectRuntimeRoot(liveProject, workspaceRoot);
  const fullStack = liveProject.slug === "echo";
  const findings: QaFinding[] = [];
  const rawOutput: string[] = [];
  const source: Record<string, unknown> = {
    quickChecks: fullStack
      ? ["json", "resources", "terminal", "handoffs", "runtime-logs", "jar-set"]
      : ["json", "resources", "runtime-logs", "jar-set"],
    deepValidators: []
  };

  const inventory = workspaceInventory(scanRoot);
  findings.push(...quickScan(liveProject, workspaceRoot, scanRoot, runtimeRoot, settings));

  if (mode === "deep" && fullStack) {
    const validators = runDeepValidators(settings, workspaceRoot);
    source.deepValidators = validators.map((result) => ({
      source: result.source,
      status: result.status,
      exitCode: result.exitCode,
      durationMs: result.durationMs
    }));
    for (const validator of validators) {
      rawOutput.push(`## ${validator.source}\n${validator.output}`);
      findings.push(...validator.findings);
    }
  }

  const uniqueFindings = capFindings(dedupeFindings(findings));
  const status = statusFromFindings(uniqueFindings);
  const buildHealth = buildHealthFromFindings(uniqueFindings);
  const summary: ScanReport["summary"] = {
    status: `${mode === "deep" ? "Deep" : "Quick"} scan ${status}`,
    buildHealth,
    criticalIssues: uniqueFindings.filter((finding) => finding.severity === "critical").length,
    polishTasks: uniqueFindings.filter((finding) => finding.severity !== "low").length,
    inventory,
    readinessScore: buildHealth
  };
  const finishedAt = new Date().toISOString();

  return {
    projectSlug: project.slug,
    mode,
    status,
    startedAt,
    finishedAt,
    durationMs: Date.now() - start,
    source,
    rawOutput: trimRawOutput(rawOutput.join("\n\n")),
    summary,
    findings: uniqueFindings
  };
}

export function parseValidatorOutput(source: string, output: string, exitCode: number | null): QaFinding[] {
  const lines = output.split(/\r?\n/);
  const findings: QaFinding[] = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("Resource validation passed") || trimmed.includes("validation passed")) {
      continue;
    }
    const normalized = trimmed.replace(/^-\s*/, "");
    const code = normalized.match(/^([A-Z0-9_]+)/)?.[1] ?? (exitCode === 0 ? "VALIDATOR_INFO" : "VALIDATOR_FAILURE");
    if (exitCode === 0 && code === "VALIDATOR_INFO") {
      continue;
    }
    const location = parseLocation(normalized);
    findings.push({
      track: trackForCode(code),
      title: titleForCode(code),
      severity: severityForCode(code, exitCode ?? 1),
      status: "Validator finding",
      detail: normalized,
      path: location.path,
      line: location.line,
      code,
      source,
      metadata: { exitCode }
    });
  }
  if (exitCode !== 0 && findings.length === 0) {
    findings.push({
      track: "release-ops",
      title: `${source} failed`,
      severity: "high",
      status: "Validator failed",
      detail: output.trim() || `${source} exited with code ${exitCode}`,
      code: "VALIDATOR_FAILED",
      source,
      metadata: { exitCode }
    });
  }
  return findings;
}

function quickScan(project: Project, workspaceRoot: string, scanRoot: string, runtimeRoot: string, settings: AppSettings): QaFinding[] {
  if (!fs.existsSync(workspaceRoot)) {
    return [
      {
        track: "release-ops",
        title: "Project workspace missing",
        severity: "critical",
        status: "Failed",
        detail: `Configured project workspace does not exist: ${workspaceRoot}`,
        code: "PROJECT_WORKSPACE_MISSING",
        source: "quick"
      }
    ];
  }
  if (!fs.existsSync(scanRoot)) {
    return [
      {
        track: "release-ops",
        title: "Project workspace missing",
        severity: "critical",
        status: "Failed",
        detail: `Configured project workspace does not exist: ${scanRoot}`,
        code: "PROJECT_ROOT_MISSING",
        source: "quick"
      }
    ];
  }

  const findings = [
    ...checkJsonValidity(scanRoot),
    ...checkResources(project, workspaceRoot),
    ...checkRuntimeLogs(runtimeRoot, settings.runtimeLogMaxAgeMinutes),
    ...checkJarSet(project, settings.modpackModsDir, project.slug === "echo")
  ];
  if (project.slug === "echo") {
    findings.push(...checkTerminalPages(workspaceRoot), ...checkHandoffs(workspaceRoot));
  }
  return findings;
}

function checkJsonValidity(echoRoot: string): QaFinding[] {
  const findings: QaFinding[] = [];
  for (const file of walk(echoRoot)) {
    if (!shouldValidateJsonFile(echoRoot, file)) {
      continue;
    }
    try {
      JSON.parse(fs.readFileSync(file, "utf-8"));
    } catch (error) {
      findings.push({
        track: "resources",
        title: "Malformed JSON",
        severity: "critical",
        status: "Failed",
        detail: error instanceof Error ? error.message : String(error),
        path: rel(echoRoot, file),
        code: "JSON_PARSE",
        source: "quick"
      });
    }
  }
  return findings;
}

function checkResources(project: Project, echoRoot: string): QaFinding[] {
  const findings: QaFinding[] = [];
  for (const resourceSet of resourceSets(project, echoRoot)) {
    for (const file of filesUnder(resourceSet.assetRoot, ".json")) {
      const data = readJson(file);
      if (!isRecord(data)) {
        continue;
      }
      const display = toDisplayPath(file);
      if (display.includes("/models/")) {
        const parent = typeof data.parent === "string" ? data.parent : "";
        if (parent && !parent.startsWith("minecraft:") && !modelExists(parent, resourceSet)) {
          findings.push(resourceFinding("Missing parent model", "MISSING_PARENT_MODEL", file, echoRoot, parent));
        }
        for (const value of walkJson(data)) {
          if (!isRecord(value) || !isRecord(value.textures)) {
            continue;
          }
          for (const texture of Object.values(value.textures)) {
            if (typeof texture === "string" && !texture.startsWith("#") && !texture.startsWith("minecraft:") && !textureExists(texture, resourceSet)) {
              findings.push(resourceFinding("Missing texture reference", "MISSING_TEXTURE_REF", file, echoRoot, texture));
            }
          }
        }
      }
      if (display.includes("/blockstates/") || display.includes("/items/") || display.includes("/models/item/")) {
        for (const value of walkJson(data)) {
          if (isRecord(value) && typeof value.model === "string" && !value.model.startsWith("minecraft:") && !modelExists(value.model, resourceSet)) {
            findings.push(resourceFinding("Missing model reference", "MISSING_MODEL_REF", file, echoRoot, value.model));
          }
        }
      }
    }

    for (const itemModel of filesUnder(path.join(resourceSet.assetRoot, "models", "item"), ".json")) {
      const id = path.basename(itemModel, ".json");
      const key = `item.${resourceSet.modId}.${id}`;
      const blockKey = `block.${resourceSet.modId}.${id}`;
      const blockItem = isBlockItemModel(itemModel, resourceSet, id);
      if (blockItem && (resourceSet.lang[blockKey] || resourceSet.lang[key])) {
        continue;
      }
      if (!blockItem && !resourceSet.lang[key]) {
        findings.push(resourceFinding("Missing item lang key", "MISSING_LANG_KEY", itemModel, echoRoot, key, "medium"));
      } else if (blockItem && !resourceSet.lang[blockKey]) {
        findings.push(resourceFinding("Missing block lang key", "MISSING_LANG_KEY", itemModel, echoRoot, blockKey, "medium"));
      }
    }
    for (const blockstate of filesUnder(path.join(resourceSet.assetRoot, "blockstates"), ".json")) {
      const id = path.basename(blockstate, ".json");
      const key = `block.${resourceSet.modId}.${id}`;
      if (!resourceSet.lang[key]) {
        findings.push(resourceFinding("Missing block lang key", "MISSING_LANG_KEY", blockstate, echoRoot, key, "medium"));
      }
    }
  }
  return findings;
}

function checkTerminalPages(echoRoot: string): QaFinding[] {
  const findings: QaFinding[] = [];
  const files = [
    path.join(echoRoot, "addons", "echoterminal", "src", "main", "java", "com", "knoxhack", "echoterminal", "client", "BuiltinTerminalTabs.java"),
    path.join(echoRoot, "src", "main", "java", "com", "knoxhack", "echoashfallprotocol", "integration", "AshfallTerminalIntegration.java"),
    path.join(echoRoot, "addons", "echoindustrialnexus", "src", "main", "java", "com", "knoxhack", "echoindustrialnexus", "integration", "IndustrialTerminalClientIntegration.java")
  ];
  const text = files.filter(fs.existsSync).map((file) => fs.readFileSync(file, "utf-8")).join("\n");
  for (const page of TERMINAL_EXPECTED_PAGES) {
    if (!text.includes(page)) {
      findings.push({
        track: "terminal",
        title: "Missing terminal page signal",
        severity: "high",
        status: "Needs review",
        detail: `Expected terminal page/chrome text was not found: ${page}`,
        code: "MISSING_TERMINAL_PAGE",
        source: "quick"
      });
    }
  }
  const tabCount = (text.match(/TerminalTabChrome\.of\(/g) ?? []).length;
  if (tabCount < 10) {
    findings.push({
      track: "terminal",
      title: "Low terminal tab count",
      severity: "medium",
      status: "Needs review",
      detail: `Found ${tabCount} TerminalTabChrome declarations; expected a richer ECHO terminal surface.`,
      code: "LOW_TERMINAL_TAB_COUNT",
      source: "quick"
    });
  }
  return findings;
}

function checkHandoffs(echoRoot: string): QaFinding[] {
  const handoffDoc = path.join(echoRoot, "docs", "chapter_handoff_ids.md");
  const overview = path.join(echoRoot, "MODPACK_OVERVIEW.md");
  const text = [handoffDoc, overview].filter(fs.existsSync).map((file) => fs.readFileSync(file, "utf-8")).join("\n");
  const findings: QaFinding[] = [];
  for (const token of HANDOFF_TOKENS) {
    if (!text.includes(token)) {
      findings.push({
        track: "handoffs",
        title: "Missing handoff documentation",
        severity: "high",
        status: "Needs review",
        detail: `Expected handoff/release token is missing from docs: ${token}`,
        path: fs.existsSync(handoffDoc) ? rel(echoRoot, handoffDoc) : "",
        code: "MISSING_HANDOFF_DOC",
        source: "quick"
      });
    }
  }
  return findings;
}

function checkRuntimeLogs(echoRoot: string, maxAgeMinutes: number): QaFinding[] {
  const cutoff = Date.now() - Math.max(1, maxAgeMinutes) * 60_000;
  const findings: QaFinding[] = [];
  for (const file of walk(echoRoot)) {
    if (!isRuntimeLogFile(echoRoot, file)) {
      continue;
    }
    const normalized = toDisplayPath(path.relative(echoRoot, file));
    const stat = fs.statSync(file);
    if (stat.mtimeMs < cutoff) {
      continue;
    }
    if (normalized.includes("/crash-reports/")) {
      findings.push({
        track: "release-ops",
        title: "Fresh crash report",
        severity: "critical",
        status: "Failed",
        detail: "A recent crash report is present.",
        path: normalized,
        code: "FRESH_CRASH_REPORT",
        source: "quick"
      });
      continue;
    }
    const lines = fs.readFileSync(file, "utf-8").split(/\r?\n/);
    lines.forEach((line, index) => {
      if (isIgnoredRuntimeLogSignatureLine(line)) {
        return;
      }
      if (CRASH_SIGNATURES.some((signature) => line.includes(signature))) {
        findings.push({
          track: "release-ops",
          title: "Runtime crash signature",
          severity: "critical",
          status: "Failed",
          detail: line.trim(),
          path: normalized,
          line: index + 1,
          code: "RUNTIME_CRASH_SIGNATURE",
          source: "quick"
        });
      }
    });
  }
  return findings;
}

function shouldValidateJsonFile(root: string, file: string): boolean {
  if (!file.endsWith(".json")) {
    return false;
  }
  const segments = toDisplayPath(path.relative(root, file)).split("/");
  return !segments.some((segment) => segment === "run" || segment.startsWith("run-") || segment === "logs" || segment === "crash-reports" || segment === "downloads" || segment === "cache" || segment === ".cache");
}

function isRuntimeLogFile(root: string, file: string): boolean {
  const segments = toDisplayPath(path.relative(root, file)).split("/");
  const parent = segments.at(-2);
  return (parent === "logs" && file.endsWith(".log")) || (parent === "crash-reports" && file.endsWith(".txt"));
}

function checkJarSet(project: Project, modsDir: string, requireConfigured: boolean): QaFinding[] {
  if (!modsDir) {
    if (!requireConfigured) {
      return [];
    }
    return [
      {
        track: "release-ops",
        title: "Modpack mods folder not configured",
        severity: "high",
        status: "Needs settings",
        detail: "Set the modpack mods folder before jar-set checks can pass.",
        code: "MODS_DIR_UNSET",
        source: "quick"
      }
    ];
  }
  if (!fs.existsSync(modsDir)) {
    return [
      {
        track: "release-ops",
        title: "Modpack mods folder missing",
        severity: "high",
        status: "Needs settings",
        detail: `Configured folder does not exist: ${modsDir}`,
        code: "MODS_DIR_MISSING",
        source: "quick"
      }
    ];
  }
  const findings: QaFinding[] = [];
  const jars = fs.readdirSync(modsDir).filter((name) => name.endsWith(".jar"));
  for (const module of project.modules) {
    const matches = jars.filter((jar) => jar.toLowerCase().startsWith(`${module.modId.toLowerCase()}-`));
    const expected = `${module.modId}-${module.version}.jar`;
    if (matches.length !== 1) {
      findings.push({
        track: "release-ops",
        title: "Jar count mismatch",
        severity: "high",
        status: "Failed",
        detail: `Expected exactly one ${module.modId} jar, found ${matches.length}: ${matches.join(", ") || "none"}`,
        path: modsDir,
        code: "JAR_COUNT_MISMATCH",
        source: "quick"
      });
    } else if (matches[0] !== expected) {
      findings.push({
        track: "release-ops",
        title: "Stale jar",
        severity: "high",
        status: "Failed",
        detail: `Found ${matches[0]}, expected ${expected}`,
        path: modsDir,
        code: "STALE_JAR",
        source: "quick"
      });
    }
  }
  return findings;
}

function runDeepValidators(settings: AppSettings, echoRoot: string): ValidatorResult[] {
  const python = settings.pythonExecutable || DEFAULT_PYTHON_EXECUTABLE;
  const validators = [
    {
      source: "validate_resources",
      args: ["tools/validate_resources.py", "--addon-set", "all"]
    },
    {
      source: "validate_gameplay_data",
      args: ["tools/validate_gameplay_data.py"]
    },
    {
      source: "check_poi_structures",
      args: ["tools/structure_generator/generator.py", "--check"]
    },
    {
      source: "check_runtime_logs",
      args: ["tools/check_echo_runtime_logs.py", "--max-age-minutes", String(settings.runtimeLogMaxAgeMinutes)]
    },
    {
      source: "validate_echo_metadata",
      args: ["tools/validate_echo_metadata.py"]
    },
    {
      source: "validate_pack_export",
      args: ["tools/validate_pack_export.py"]
    },
    {
      source: "validate_echo_templates",
      args: ["tools/validate_echo_templates.py"]
    },
    {
      source: "validate_config_presets",
      args: ["tools/validate_config_presets.py"]
    },
    {
      source: "validate_theme_templates",
      args: ["tools/validate_theme_templates.py"]
    },
    {
      source: "validate_echo_datapacks",
      args: ["tools/validate_echo_datapacks.py"]
    },
    {
      source: "validate_docs_links",
      args: ["tools/validate_docs_links.py"]
    }
  ];
  return validators.map((validator) => runValidator(python, validator.source, validator.args, echoRoot));
}

function runValidator(python: string, source: string, args: string[], cwd: string): ValidatorResult {
  const start = Date.now();
  if (isPathLikeExecutable(python) && !fs.existsSync(python)) {
    const output = `Python executable not found: ${python}`;
    return {
      source,
      status: "failed",
      exitCode: 1,
      durationMs: Date.now() - start,
      output,
      findings: parseValidatorOutput(source, output, 1)
    };
  }
  const result = spawnSync(python, args, {
    cwd,
    encoding: "utf-8",
    windowsHide: true,
    timeout: 120_000,
    maxBuffer: 1024 * 1024 * 8
  });
  const output = [result.stdout, result.stderr, result.error ? result.error.message : ""].filter(Boolean).join("\n");
  const exitCode = typeof result.status === "number" ? result.status : 1;
  return {
    source,
    status: exitCode === 0 ? "passed" : "failed",
    exitCode,
    durationMs: Date.now() - start,
    output,
    findings: parseValidatorOutput(source, output, exitCode)
  };
}

function isIgnoredRuntimeLogSignatureLine(line: string): boolean {
  const lower = line.toLowerCase();
  return IGNORED_RUNTIME_LOG_SIGNATURE_LINES.some((token) => lower.includes(token));
}

function projectScanRoot(project: Project, echoRoot: string): string {
  if (project.slug === "echo") {
    return echoRoot;
  }
  const modulePath = project.modules[0]?.path ?? project.workspacePath;
  if (!modulePath || modulePath === ".") {
    return path.join(echoRoot, "src", "main");
  }
  return path.resolve(echoRoot, modulePath);
}

function projectRuntimeRoot(project: Project, echoRoot: string): string {
  if (project.slug === "echo") {
    return echoRoot;
  }
  const modulePath = project.modules[0]?.path ?? project.workspacePath;
  if (!modulePath || modulePath === ".") {
    return echoRoot;
  }
  return path.resolve(echoRoot, modulePath);
}

function isPathLikeExecutable(executable: string): boolean {
  return path.isAbsolute(executable) || executable.includes("/") || executable.includes("\\");
}

function workspaceInventory(root: string): Record<string, number> {
  if (!fs.existsSync(root)) {
    return {};
  }
  const counts = {
    javaFiles: 0,
    jsonFiles: 0,
    langFiles: 0,
    itemModels: 0,
    blockModels: 0,
    blockstates: 0,
    recipes: 0,
    lootTables: 0,
    tags: 0,
    runtimeLogs: 0,
    jars: 0
  };
  for (const file of walk(root)) {
    const display = toDisplayPath(path.relative(root, file));
    if (file.endsWith(".java")) counts.javaFiles += 1;
    if (file.endsWith(".json")) counts.jsonFiles += 1;
    if (display.includes("/lang/") && file.endsWith(".json")) counts.langFiles += 1;
    if (display.includes("/models/item/") && file.endsWith(".json")) counts.itemModels += 1;
    if (display.includes("/models/block/") && file.endsWith(".json")) counts.blockModels += 1;
    if (display.includes("/blockstates/") && file.endsWith(".json")) counts.blockstates += 1;
    if (display.includes("/recipe/") && file.endsWith(".json")) counts.recipes += 1;
    if (display.includes("/loot_table/") && file.endsWith(".json")) counts.lootTables += 1;
    if (display.includes("/tags/") && file.endsWith(".json")) counts.tags += 1;
    if (display.includes("/logs/") && file.endsWith(".log")) counts.runtimeLogs += 1;
    if (file.endsWith(".jar")) counts.jars += 1;
  }
  return counts;
}

function resourceSets(project: Project, echoRoot: string): ResourceSet[] {
  return project.modules
    .map((module) => {
      const resourceRoot =
        module.path === "."
          ? path.join(echoRoot, "src", "main", "resources")
          : path.join(echoRoot, module.path, "src", "main", "resources");
      const assetRoot = path.join(resourceRoot, "assets", module.modId);
      if (!fs.existsSync(assetRoot)) {
        return null;
      }
      return {
        modId: module.modId,
        resourceRoot,
        assetRoot,
        models: collectSet(path.join(assetRoot, "models"), ".json"),
        textures: collectSet(path.join(assetRoot, "textures"), ".png"),
        lang: readLang(path.join(assetRoot, "lang", "en_us.json"))
      };
    })
    .filter((set): set is ResourceSet => Boolean(set));
}

function resourceFinding(
  title: string,
  code: string,
  file: string,
  root: string,
  detail: string,
  severity: QaFinding["severity"] = "high"
): QaFinding {
  return {
    track: "resources",
    title,
    severity,
    status: "Failed",
    detail,
    path: rel(root, file),
    code,
    source: "quick"
  };
}

function collectSet(root: string, suffix: string): Set<string> {
  if (!fs.existsSync(root)) {
    return new Set();
  }
  return new Set(filesUnder(root, suffix).map((file) => toDisplayPath(path.relative(root, file)).slice(0, -suffix.length)));
}

function filesUnder(root: string, suffix: string): string[] {
  if (!fs.existsSync(root)) {
    return [];
  }
  return [...walk(root)].filter((file) => file.endsWith(suffix));
}

function readLang(file: string): Record<string, string> {
  const data = readJson(file);
  if (!isRecord(data)) {
    return {};
  }
  const lang: Record<string, string> = {};
  for (const [key, value] of Object.entries(data)) {
    if (typeof value === "string") {
      lang[key] = value;
    }
  }
  return lang;
}

function isBlockItemModel(itemModel: string, set: ResourceSet, id: string): boolean {
  const data = readJson(itemModel);
  const parent = isRecord(data) && typeof data.parent === "string" ? data.parent : "";
  const parsedParent = parent ? parseNamespacedRef(parent, set.modId) : null;
  return (
    fs.existsSync(path.join(set.assetRoot, "blockstates", `${id}.json`)) ||
    set.models.has(`block/${id}`) ||
    (parsedParent?.namespace === set.modId && parsedParent.path === `block/${id}`)
  );
}

function modelExists(ref: string, set: ResourceSet): boolean {
  const parsed = parseNamespacedRef(ref, set.modId);
  return parsed.namespace !== set.modId || set.models.has(parsed.path);
}

function textureExists(ref: string, set: ResourceSet): boolean {
  const parsed = parseNamespacedRef(ref, set.modId);
  return parsed.namespace !== set.modId || set.textures.has(parsed.path);
}

function parseNamespacedRef(ref: string, fallbackNamespace: string): { namespace: string; path: string } {
  if (ref.includes(":")) {
    const [namespace, rest] = ref.split(":", 2);
    return { namespace, path: rest };
  }
  return { namespace: fallbackNamespace, path: ref };
}

function readJson(file: string): unknown | null {
  if (!fs.existsSync(file)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(file, "utf-8"));
  } catch {
    return null;
  }
}

function* walkJson(value: unknown): Generator<unknown> {
  yield value;
  if (Array.isArray(value)) {
    for (const item of value) {
      yield* walkJson(item);
    }
  } else if (isRecord(value)) {
    for (const item of Object.values(value)) {
      yield* walkJson(item);
    }
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function* walk(root: string): Generator<string> {
  if (!fs.existsSync(root)) {
    return;
  }
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    if (SKIPPED_DIRS.has(entry.name)) {
      continue;
    }
    const next = path.join(root, entry.name);
    if (entry.isDirectory()) {
      yield* walk(next);
    } else if (entry.isFile()) {
      yield next;
    }
  }
}

function rel(root: string, file: string): string {
  return toDisplayPath(path.relative(root, file));
}

function parseLocation(line: string): { path?: string; line?: number } {
  const match = line.match(/((?:[A-Za-z]:)?[^:\s]+(?:\.json|\.java|\.md|\.py|\.toml|\.gradle|\.properties|\.txt|\.log))(?:[:](\d+))?/);
  return {
    path: match?.[1],
    line: match?.[2] ? Number(match[2]) : undefined
  };
}

function trackForCode(code: string): string {
  if (code.includes("TERMINAL") || code.includes("MISSION")) return "terminal";
  if (code.includes("HANDOFF") || code.includes("SAGA") || code.includes("NEXUS")) return "handoffs";
  if (code.includes("RUNTIME") || code.includes("JAR") || code.includes("VALIDATOR") || code.includes("GAME")) return "release-ops";
  return "resources";
}

function titleForCode(code: string): string {
  return code
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function severityForCode(code: string, exitCode: number): QaFinding["severity"] {
  if (code.includes("CRASH") || code.includes("JSON_PARSE") || code.includes("MISSING_MODEL") || code.includes("MISSING_TEXTURE")) {
    return "critical";
  }
  if (exitCode !== 0 || code.includes("MISSING") || code.includes("STALE") || code.includes("BAD_") || code.includes("WRONG")) {
    return "high";
  }
  return "medium";
}

function dedupeFindings(findings: QaFinding[]): QaFinding[] {
  const seen = new Set<string>();
  const next: QaFinding[] = [];
  for (const finding of findings) {
    const key = [finding.code, finding.path, finding.line, finding.detail].join("|");
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    next.push(finding);
  }
  return next;
}

function capFindings(findings: QaFinding[]): QaFinding[] {
  const counts = new Map<string, number>();
  return findings.filter((finding) => {
    const key = finding.code ?? "UNKNOWN";
    const count = counts.get(key) ?? 0;
    counts.set(key, count + 1);
    return count < FINDING_LIMIT_PER_CODE;
  });
}

function statusFromFindings(findings: QaFinding[]): ScanStatus {
  if (findings.some((finding) => finding.severity === "critical")) return "failed";
  if (findings.some((finding) => finding.severity === "high" || finding.severity === "medium")) return "warning";
  return "passed";
}

function buildHealthFromFindings(findings: QaFinding[]): number {
  const penalty = findings.reduce((total, finding) => {
    if (finding.severity === "critical") return total + 15;
    if (finding.severity === "high") return total + 7;
    if (finding.severity === "medium") return total + 3;
    return total + 1;
  }, 0);
  return Math.max(0, Math.min(100, 100 - penalty));
}

function trimRawOutput(output: string): string {
  if (output.length <= RAW_OUTPUT_LIMIT) {
    return output;
  }
  return output.slice(output.length - RAW_OUTPUT_LIMIT);
}
