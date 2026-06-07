import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { buildJarManifest } from "../src/server/jars.ts";
import { buildReadinessReport } from "../src/server/readiness.ts";

function makeProject() {
  return {
    slug: "echo",
    name: "ECHO",
    kind: "Full Stack",
    status: "Fixture",
    currentMilestone: "Fixture",
    buildHealth: 0,
    criticalIssues: 0,
    polishTasks: 0,
    lastScanLabel: "Never",
    nextRecommendedAction: "Scan",
    accent: "#58d7ff",
    description: "Fixture",
    workspacePath: "",
    modules: [
      { modId: "echocore", label: "Core", version: "1.0.0", path: "core/echocore" },
      { modId: "echostationfall", label: "Stationfall", version: "1.0.0", path: "addons/echostationfall" }
    ]
  };
}

function makeSettings(root, modsDir) {
  return {
    echoRoot: root,
    modpackModsDir: modsDir,
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  };
}

function scan({ id = 1, mode = "quick", status = "passed", findings = [], score = 100 } = {}) {
  return {
    id,
    projectSlug: "echo",
    createdAt: "2026-05-10T00:00:00.000Z",
    mode,
    status,
    startedAt: "2026-05-10T00:00:00.000Z",
    finishedAt: "2026-05-10T00:00:01.000Z",
    durationMs: 1000,
    source: {},
    rawOutput: "",
    summary: {
      status: `${mode} ${status}`,
      buildHealth: score,
      criticalIssues: findings.filter((finding) => finding.severity === "critical").length,
      polishTasks: findings.filter((finding) => finding.severity !== "low").length,
      inventory: {},
      readinessScore: score
    },
    findings
  };
}

function finding(code, severity = "high") {
  return {
    track: "release-ops",
    title: code,
    severity,
    status: "Failed",
    detail: code,
    code,
    source: "quick"
  };
}

function write(file, content) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
}

function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-readiness-"));
  return {
    root,
    buildRoot: path.join(root, "EchoBuild", "Echo"),
    modsDir: path.join(root, "mods"),
    project: makeProject()
  };
}

function writeBuiltJars(buildRoot, project) {
  for (const module of project.modules) {
    const moduleDir = module.path.split(/[\\/]/).filter(Boolean).at(-1) ?? module.modId;
    write(path.join(buildRoot, moduleDir, "libs", `${module.modId}-${module.version}.jar`), `${module.modId}-current`);
  }
}

function writeCurrentTargetJars(modsDir, project) {
  fs.mkdirSync(modsDir, { recursive: true });
  for (const module of project.modules) {
    write(path.join(modsDir, `${module.modId}-${module.version}.jar`), `${module.modId}-current`);
  }
}

function item(report, id) {
  return report.items.find((candidate) => candidate.id === id);
}

test("readiness explains current ECHO blocker when jars are built but mods folder is unset", () => {
  const { root, buildRoot, project } = fixture();
  writeBuiltJars(buildRoot, project);
  const settings = makeSettings(root, "");
  const manifest = buildJarManifest(project, settings, { buildRoot });

  const report = buildReadinessReport(project, settings, [scan({
    status: "warning",
    score: 93,
    findings: [finding("MODS_DIR_UNSET")]
  })], manifest);

  assert.equal(report.score, 93);
  assert.equal(item(report, "built-jars")?.status, "done");
  assert.equal(item(report, "mods-folder")?.status, "blocked");
  assert.equal(item(report, "mods-folder")?.relatedFindingCodes.includes("MODS_DIR_UNSET"), true);
  assert.equal(report.nextAction?.id, "mods-folder");
});

test("clean quick scan plus current target jars reaches 100", () => {
  const { root, buildRoot, modsDir, project } = fixture();
  writeBuiltJars(buildRoot, project);
  writeCurrentTargetJars(modsDir, project);
  const settings = makeSettings(root, modsDir);
  const manifest = buildJarManifest(project, settings, { buildRoot });

  const report = buildReadinessReport(project, settings, [scan()], manifest);

  assert.equal(report.score, 100);
  assert.equal(report.counts.done, report.counts.total);
  assert.equal(report.nextAction, null);
});

test("missing built jar creates a missing jar checklist item", () => {
  const { root, buildRoot, modsDir, project } = fixture();
  write(path.join(buildRoot, "echocore", "libs", "echocore-1.0.0.jar"), "echocore-current");
  fs.mkdirSync(modsDir, { recursive: true });
  const settings = makeSettings(root, modsDir);
  const manifest = buildJarManifest(project, settings, { buildRoot });

  const report = buildReadinessReport(project, settings, [scan()], manifest);

  assert.equal(item(report, "built-jars")?.status, "missing");
  assert.match(item(report, "built-jars")?.detail ?? "", /echostationfall-1\.0\.0\.jar/);
  assert.equal(report.score < 100, true);
});

test("stale target jar creates promote and quarantine checklist work", () => {
  const { root, buildRoot, modsDir, project } = fixture();
  writeBuiltJars(buildRoot, project);
  fs.mkdirSync(modsDir, { recursive: true });
  write(path.join(modsDir, "echocore-1.0.0.jar"), "old-core-bytes");
  write(path.join(modsDir, "echostationfall-1.0.0.jar"), "old-stationfall");
  const settings = makeSettings(root, modsDir);
  const manifest = buildJarManifest(project, settings, { buildRoot });

  const report = buildReadinessReport(project, settings, [scan()], manifest);

  assert.equal(item(report, "current-jars")?.status, "missing");
  assert.equal(item(report, "stale-jars")?.status, "blocked");
  assert.equal(item(report, "stale-jars")?.commandId, "promote-jars");
});

test("missing quick scan asks for a quick scan", () => {
  const { root, buildRoot, modsDir, project } = fixture();
  writeBuiltJars(buildRoot, project);
  writeCurrentTargetJars(modsDir, project);
  const settings = makeSettings(root, modsDir);
  const manifest = buildJarManifest(project, settings, { buildRoot });

  const report = buildReadinessReport(project, settings, [], manifest);

  assert.equal(report.score, 0);
  assert.equal(item(report, "quick-scan")?.status, "missing");
  assert.equal(report.nextAction?.id, "quick-scan");
});
