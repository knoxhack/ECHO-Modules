import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { scanFailureBlocksPipeline } from "../src/server/modpack.ts";

function makeProject() {
  return {
    slug: "echo",
    name: "ECHO Full Stack",
    kind: "Full Stack",
    status: "Test",
    currentMilestone: "Fixture",
    buildHealth: 0,
    criticalIssues: 0,
    polishTasks: 0,
    lastScanLabel: "Never",
    nextRecommendedAction: "Scan",
    accent: "#58d7ff",
    description: "Fixture",
    workspacePath: "",
    modules: []
  };
}

function makeSettings(root) {
  return {
    echoRoot: root,
    modpackModsDir: "",
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  };
}

function makeReport(findings) {
  return {
    id: 1,
    projectSlug: "echo",
    createdAt: "2026-05-25T01:00:00.000Z",
    mode: "quick",
    status: "failed",
    startedAt: "2026-05-25T01:00:00.000Z",
    durationMs: 10,
    source: {},
    rawOutput: "",
    summary: {
      status: "Quick scan failed",
      buildHealth: 0,
      criticalIssues: findings.length,
      polishTasks: findings.length,
      inventory: {}
    },
    findings
  };
}

function write(file, content = "") {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content, "utf-8");
}

test("pipeline scan gate treats pre-existing runtime crash signatures as non-blocking", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-modpack-scan-gate-"));
  const logPath = path.join(root, "addons", "echoweathercore", "run", "logs", "latest.log");
  write(logPath, "NoClassDefFoundError: old dev-run crash");
  fs.utimesSync(logPath, new Date("2026-05-25T00:55:00.000Z"), new Date("2026-05-25T00:55:00.000Z"));

  const report = makeReport([
    {
      track: "release-ops",
      title: "Runtime crash signature",
      severity: "critical",
      status: "Failed",
      detail: "NoClassDefFoundError: old dev-run crash",
      path: "addons/echoweathercore/run/logs/latest.log",
      line: 1,
      code: "RUNTIME_CRASH_SIGNATURE",
      source: "quick"
    }
  ]);

  assert.equal(
    scanFailureBlocksPipeline(report, makeProject(), makeSettings(root), "2026-05-25T01:00:00.000Z"),
    false
  );
});

test("pipeline scan gate still blocks runtime crashes created during the pipeline", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-modpack-scan-gate-"));
  const logPath = path.join(root, "addons", "echoweathercore", "run", "logs", "latest.log");
  write(logPath, "NoClassDefFoundError: new crash");
  fs.utimesSync(logPath, new Date("2026-05-25T01:05:00.000Z"), new Date("2026-05-25T01:05:00.000Z"));

  const report = makeReport([
    {
      track: "release-ops",
      title: "Runtime crash signature",
      severity: "critical",
      status: "Failed",
      detail: "NoClassDefFoundError: new crash",
      path: "addons/echoweathercore/run/logs/latest.log",
      line: 1,
      code: "RUNTIME_CRASH_SIGNATURE",
      source: "quick"
    }
  ]);

  assert.equal(
    scanFailureBlocksPipeline(report, makeProject(), makeSettings(root), "2026-05-25T01:00:00.000Z"),
    true
  );
});
