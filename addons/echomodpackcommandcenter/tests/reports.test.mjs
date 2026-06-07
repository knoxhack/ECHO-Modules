import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createApp } from "../src/server/index.ts";
import { CommandCenterStore } from "../src/server/db.ts";
import { loadEchoOperationalReports, loadEchoReportDrilldown } from "../src/server/reports.ts";

const EXPECTED_REPORTS = [
  "platform-verification.json",
  "workspace-scan.json",
  "scanned-modules.json",
  "module-graph.json",
  "dependency-graph.json",
  "role-graph.json",
  "feature-graph.json",
  "diagnostics.json",
  "pack-readiness.json",
  "pack-profile.json",
  "lockfile.json",
  "install-state.json",
  "lockfile-status.json",
  "repair-plan.json",
  "pack-doctor.json",
  "health.json",
  "runtime-health.json",
  "degraded-features.json",
  "recovery-state.json",
  "recovery-plan.json",
  "bridge-sessions.json",
  "codex-run-report.json",
  "ai-tasks.json",
  "release-readiness.json",
  "support-bundle.json",
  "missing-assets.json",
  "official-pack-asset-gates.json",
  "launcher-status.json",
  "command-center-catalog.json"
];

function tempRoot() {
  return fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-reports-"));
}

function writeReport(root, fileName, payload) {
  const reportPath = path.join(root, "reports", "echo", fileName);
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.writeFileSync(reportPath, typeof payload === "string" ? payload : JSON.stringify(payload, null, 2));
}

function minimalEnvelope(schema, data = {}) {
  return {
    schema,
    generatedAt: "2026-05-29T00:00:00.000Z",
    generator: "test",
    workspace: "ECHO",
    addonSet: "beta",
    packId: "ashfall",
    status: "PASS",
    summary: { warnings: 0, errors: 0, notices: 0, fatals: 0, issueCount: 0 },
    issues: [],
    data
  };
}

async function listen(app) {
  const server = await new Promise((resolve, reject) => {
    const listeningServer = app.listen(0, "127.0.0.1", () => resolve(listeningServer));
    listeningServer.once("error", reject);
  });
  const address = server.address();
  assert.ok(address && typeof address !== "string");
  return { server, baseUrl: `http://127.0.0.1:${address.port}` };
}

async function closeServer(server) {
  await new Promise((resolve) => server.close(resolve));
}

async function waitForReportJobs(baseUrl) {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const response = await fetch(`${baseUrl}/api/reports/echo/jobs`);
    assert.equal(response.status, 200);
    const body = await response.json();
    if (!body.runs.some((run) => run.status === "running")) {
      return body;
    }
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  const response = await fetch(`${baseUrl}/api/reports/echo/jobs`);
  return response.json();
}

test("report loader exposes the full Phase 4 report surface", () => {
  const root = tempRoot();
  const reports = loadEchoOperationalReports({ rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });

  assert.deepEqual(reports.artifacts.map((artifact) => artifact.fileName), EXPECTED_REPORTS);
  assert.equal(reports.reportCounts.total, EXPECTED_REPORTS.length);
  assert.equal(reports.reportCounts.missing, EXPECTED_REPORTS.length);
  assert.equal(reports.status, "degraded");
  assert.equal(reports.categories.some((category) => category.key === "packos" && category.reportKeys.includes("packDoctor")), true);
  assert.equal(reports.categories.some((category) => category.key === "bridge" && category.reportKeys.includes("bridgeSessions")), true);
  assert.equal(reports.panels.some((panel) => panel.title === "Launcher / Command Center"), true);
  assert.equal(reports.missingReports.every((reportPath) => reportPath.startsWith("reports/echo/")), true);
});

test("report loader isolates invalid JSON to one artifact", () => {
  const root = tempRoot();
  writeReport(root, "pack-doctor.json", minimalEnvelope("echo.report.pack_doctor", {
    packDoctor: {
      packId: "ashfall",
      status: "ready",
      lockfileStatus: "valid",
      installStateStatus: "not_configured",
      repairPlanStatus: "no_repair_needed",
      safeForLauncher: true
    }
  }));
  writeReport(root, "diagnostics.json", "{ invalid json");

  const reports = loadEchoOperationalReports({ rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });
  const diagnostics = reports.artifacts.find((artifact) => artifact.key === "diagnostics");
  const packDoctor = reports.artifacts.find((artifact) => artifact.key === "packDoctor");

  assert.equal(diagnostics?.status, "invalid");
  assert.equal(packDoctor?.status, "loaded");
  assert.deepEqual(reports.invalidReports, ["reports/echo/diagnostics.json"]);
  assert.equal(reports.status, "blocked");
});

test("report drilldown lists every report and degrades missing reports", () => {
  const root = tempRoot();
  const reports = loadEchoOperationalReports({ rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });

  for (const artifact of reports.artifacts) {
    const drilldown = loadEchoReportDrilldown(artifact.key, { rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });
    assert.ok(drilldown, `missing drilldown for ${artifact.key}`);
    assert.equal(drilldown.artifact.key, artifact.key);
    assert.equal(drilldown.artifact.status, "missing");
    assert.equal(drilldown.status, "degraded");
    assert.equal(drilldown.issues[0]?.code, "ECHO-REPORT-MISSING");
    assert.equal(drilldown.safeCommands.every((command) => command.executesInUi === false), true);
  }
});

test("report drilldown isolates invalid JSON and keeps sanitized previews", () => {
  const root = tempRoot();
  writeReport(root, "diagnostics.json", "{ invalid json");
  writeReport(root, "pack-doctor.json", minimalEnvelope("echo.report.pack_doctor", {
    packDoctor: {
      packId: "ashfall",
      status: "ready",
      lockfileStatus: "valid",
      installStateStatus: "not_configured",
      repairPlanStatus: "no_repair_needed",
      safeForLauncher: true
    },
    localPath: "C:/Users/knox/private-token.txt"
  }));

  const invalid = loadEchoReportDrilldown("diagnostics", { rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });
  const loaded = loadEchoReportDrilldown("packDoctor", { rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });

  assert.equal(invalid?.artifact.status, "invalid");
  assert.equal(invalid?.issues[0]?.code, "ECHO-REPORT-INVALID");
  assert.equal(loaded?.artifact.status, "loaded");
  assert.doesNotMatch(JSON.stringify(loaded), /C:\/Users\/knox/);
  assert.match(JSON.stringify(loaded), /\[localOnly:private-token\.txt\]/);
});

test("report loader redacts absolute local issue paths", () => {
  const root = tempRoot();
  writeReport(root, "diagnostics.json", minimalEnvelope("echo.report.diagnostics", {
    diagnostics: [
      {
        code: "ECHO-TEST-ABSOLUTE-PATH",
        severity: "ERROR",
        title: "Absolute path",
        summary: "The API should not expose private absolute paths.",
        source: "test",
        category: "diagnostics",
        blocking: true,
        likelyFiles: [
          path.join(root, "addons", "echocore", "src", "main", "java", "EchoCore.java"),
          "C:/Users/knox/private-token.txt"
        ]
      }
    ]
  }));

  const reports = loadEchoOperationalReports({ rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });
  const serialized = JSON.stringify(reports.topIssues);

  assert.match(serialized, /addons\/echocore\/src\/main\/java\/EchoCore\.java/);
  assert.match(serialized, /\[localOnly:private-token\.txt\]/);
  assert.doesNotMatch(serialized, /C:\/Users\/knox/);
  assert.equal(reports.topIssues[0]?.reportPath, "reports/echo/diagnostics.json");
});

test("report drilldown returns compact issue tables with sanitized paths", () => {
  const root = tempRoot();
  writeReport(root, "diagnostics.json", minimalEnvelope("echo.report.diagnostics", {
    diagnostics: [
      {
        code: "ECHO-TEST-DRILLDOWN",
        severity: "WARNING",
        title: "Drilldown path",
        summary: "This should avoid C:/Users/knox/private-token.txt in summaries.",
        source: "test",
        category: "diagnostics",
        moduleId: "echocore",
        featureId: "echo.core",
        packId: "ashfall",
        likelyFiles: [path.join(root, "reports", "echo", "diagnostics.json")]
      }
    ]
  }));

  const drilldown = loadEchoReportDrilldown("diagnostics", { rootDir: root, now: new Date("2026-05-29T00:00:00.000Z") });
  assert.equal(drilldown?.issues.length, 1);
  assert.equal(drilldown?.issues[0]?.moduleId, "echocore");
  assert.equal(drilldown?.issues[0]?.featureId, "echo.core");
  assert.equal(drilldown?.issues[0]?.packId, "ashfall");
  assert.equal(drilldown?.issueCounts.WARNING, 1);
  assert.doesNotMatch(JSON.stringify(drilldown), /C:\/Users\/knox/);
});

test("reports API returns grouped panels and backward-compatible summary", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, { reports: { rootDir: storeRoot } });
  const { server, baseUrl } = await listen(app);
  try {
    const response = await fetch(`${baseUrl}/api/reports/echo`);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(typeof body.summary.packOsStatus, "string");
    assert.equal(body.reportCounts.total, EXPECTED_REPORTS.length);
    assert.equal(body.panels.some((panel) => panel.id === "packos"), true);
    assert.equal(body.panels.some((panel) => panel.id === "bridge"), true);
    assert.equal(body.categories.some((category) => category.key === "workspace"), true);
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("reports API returns a single sanitized report drilldown", async () => {
  const storeRoot = tempRoot();
  writeReport(storeRoot, "diagnostics.json", minimalEnvelope("echo.report.diagnostics", {
    diagnostics: [
      {
        code: "ECHO-API-DRILLDOWN",
        severity: "ERROR",
        title: "API drilldown",
        summary: "Report endpoint should isolate one report.",
        likelyFiles: ["C:/Users/knox/private-token.txt"]
      }
    ]
  }));
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, { reports: { rootDir: storeRoot } });
  const { server, baseUrl } = await listen(app);
  try {
    const response = await fetch(`${baseUrl}/api/reports/echo/diagnostics`);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.artifact.key, "diagnostics");
    assert.equal(body.issues[0].code, "ECHO-API-DRILLDOWN");
    assert.equal(body.safeCommands.every((command) => command.executesInUi === false), true);
    assert.doesNotMatch(JSON.stringify(body), /C:\/Users\/knox/);

    const missing = await fetch(`${baseUrl}/api/reports/echo/notAReport`);
    assert.equal(missing.status, 404);
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("report jobs API exposes only non-destructive allowlisted commands", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, { reportJobs: { rootDir: storeRoot } });
  const { server, baseUrl } = await listen(app);
  try {
    const response = await fetch(`${baseUrl}/api/reports/echo/jobs`);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.status, "idle");
    assert.ok(body.definitions.length >= 5);
    assert.ok(body.panelActions.packos.includes("echoPackDoctor"));
    assert.ok(body.panelActions.release.includes("validateEchoReports"));
    assert.ok(body.panelActions.launcher.includes("validateEchoReports"));
    for (const definition of body.definitions) {
      const commandText = definition.command.join(" ").toLowerCase();
      assert.equal(definition.destructive, false);
      assert.equal(definition.launchesMinecraft, false);
      assert.equal(definition.modifiesGameFiles, false);
      assert.doesNotMatch(commandText, /runclient|runserver|copyechojars|promote|download|delete|remove|reset|repairinstall|launch/);
    }
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("report jobs API returns sanitized detail for report job runs only", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, {
    reportJobs: {
      rootDir: storeRoot,
      commandOverrides: {
        scanEchoWorkspace: {
          executable: process.execPath,
          args: ["-e", "console.log(process.cwd()); console.error('detail ok')"],
          shell: false
        }
      }
    }
  });
  const { server, baseUrl } = await listen(app);
  try {
    const started = await fetch(`${baseUrl}/api/reports/echo/jobs`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ commandId: "scanEchoWorkspace" })
    });
    assert.equal(started.status, 202);
    const jobs = await waitForReportJobs(baseUrl);
    const runId = jobs.runs[0].id;

    const detailResponse = await fetch(`${baseUrl}/api/reports/echo/jobs/${runId}`);
    assert.equal(detailResponse.status, 200);
    const detail = await detailResponse.json();
    assert.equal(detail.run.id, runId);
    assert.equal(detail.definition.id, "scanEchoWorkspace");
    assert.deepEqual(detail.reportKeys, ["workspaceScan", "scannedModules"]);
    assert.equal(detail.safety.rawCommandsAccepted, false);
    assert.equal(detail.safety.launchesMinecraft, false);
    assert.match(detail.run.outputPreview, /detail ok/);
    assert.doesNotMatch(JSON.stringify(detail), new RegExp(storeRoot.replaceAll("\\", "\\\\")));

    const nonReport = store.createCommandRun({
      id: "non-report-run",
      projectSlug: "echo",
      commandId: "not-report-job",
      status: "succeeded",
      risk: "low",
      command: ["node", "-v"],
      startedAt: "2026-05-29T00:00:00.000Z",
      finishedAt: "2026-05-29T00:00:01.000Z",
      exitCode: 0,
      durationMs: 1000,
      metadata: { kind: "release-action" },
      output: "v0.0.0"
    });
    const blocked = await fetch(`${baseUrl}/api/reports/echo/jobs/${nonReport.id}`);
    assert.equal(blocked.status, 404);
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("report jobs API rejects raw, unknown, and destructive commands", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, { reportJobs: { rootDir: storeRoot } });
  const { server, baseUrl } = await listen(app);
  try {
    const raw = await fetch(`${baseUrl}/api/reports/echo/jobs`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ command: ".\\gradlew.bat runClient" })
    });
    assert.equal(raw.status, 400);
    assert.match(await raw.text(), /commandId is required/);

    const unknown = await fetch(`${baseUrl}/api/reports/echo/jobs`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ commandId: "copyEchoJarsToModpack" })
    });
    assert.equal(unknown.status, 400);
    assert.match(await unknown.text(), /not allowlisted/);
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("report jobs API starts allowlisted jobs and records history", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, {
    reportJobs: {
      rootDir: storeRoot,
      commandOverrides: {
        scanEchoWorkspace: {
          executable: process.execPath,
          args: ["-e", "process.stdout.write('scan ok')"],
          shell: false
        }
      }
    }
  });
  const { server, baseUrl } = await listen(app);
  try {
    const started = await fetch(`${baseUrl}/api/reports/echo/jobs`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ commandId: "scanEchoWorkspace" })
    });
    assert.equal(started.status, 202);

    const jobs = await waitForReportJobs(baseUrl);
    assert.equal(jobs.runs[0].commandId, "scanEchoWorkspace");
    assert.equal(jobs.runs[0].status, "succeeded");
    assert.match(jobs.runs[0].output, /scan ok/);
    assert.equal(jobs.runs[0].metadata.kind, "echo-report-job");
    assert.deepEqual(jobs.runs[0].metadata.reportKeys, ["workspaceScan", "scannedModules"]);
  } finally {
    await closeServer(server);
    store.close();
  }
});

test("report jobs API stops active allowlisted jobs", async () => {
  const storeRoot = tempRoot();
  const store = new CommandCenterStore(path.join(storeRoot, "command-center.sqlite"));
  const app = createApp(store, {
    reportJobs: {
      rootDir: storeRoot,
      commandOverrides: {
        scanEchoWorkspace: {
          executable: process.execPath,
          args: ["-e", "setTimeout(() => {}, 10000)"],
          shell: false
        }
      }
    }
  });
  const { server, baseUrl } = await listen(app);
  try {
    const started = await fetch(`${baseUrl}/api/reports/echo/jobs`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ commandId: "scanEchoWorkspace" })
    });
    assert.equal(started.status, 202);
    const running = await started.json();
    assert.equal(running.runningRun.commandId, "scanEchoWorkspace");

    const stopped = await fetch(`${baseUrl}/api/reports/echo/jobs/${running.runningRun.id}/stop`, { method: "POST" });
    assert.equal(stopped.status, 200);
    const jobs = await stopped.json();
    assert.equal(jobs.runs[0].status, "stopped");
    assert.match(jobs.runs[0].output, /Stopped by Command Center/);
  } finally {
    await closeServer(server);
    store.close();
  }
});
