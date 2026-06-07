import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createApp } from "../src/server/index.ts";
import { CommandCenterStore } from "../src/server/db.ts";

function tempRoot() {
  return fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-bridge-"));
}

async function withServer(callback, bridgeOptions = {}) {
  const root = tempRoot();
  const store = new CommandCenterStore(path.join(root, "command-center.sqlite"));
  const app = createApp(store, {
    bridge: {
      statePath: path.join(root, "bridge-state.json"),
      executorConfigPath: path.join(root, "bridge-executor.json"),
      rootDir: root,
      now: () => new Date("2026-05-29T00:00:00.000Z"),
      ...bridgeOptions
    }
  });
  const server = await new Promise((resolve, reject) => {
    const listeningServer = app.listen(0, "127.0.0.1", () => resolve(listeningServer));
    listeningServer.once("error", reject);
  });
  try {
    const address = server.address();
    assert.ok(address && typeof address !== "string");
    await callback(`http://127.0.0.1:${address.port}`, root);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    store.close();
  }
}

async function bridgeState(baseUrl, predicate, attempts = 40) {
  for (let index = 0; index < attempts; index += 1) {
    const response = await fetch(`${baseUrl}/api/bridge`);
    assert.equal(response.status, 200);
    const body = await response.json();
    if (!predicate || predicate(body)) return body;
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  const response = await fetch(`${baseUrl}/api/bridge`);
  return response.json();
}

test("bridge API records Codex job intents without launching Codex", async () => {
  await withServer(async (baseUrl) => {
    const initial = await fetch(`${baseUrl}/api/bridge`);
    assert.equal(initial.status, 200);
    const initialBody = await initial.json();
    assert.equal(initialBody.executorStatus, "not_configured");
    assert.equal(initialBody.executorProbe.status, "not_configured");

    const probe = await fetch(`${baseUrl}/api/bridge/executor/probe`);
    assert.equal(probe.status, 200);
    assert.equal((await probe.json()).status, "not_configured");

    const sessionResponse = await fetch(`${baseUrl}/api/bridge/sessions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "Test CyberDex" })
    });
    assert.equal(sessionResponse.status, 201);
    const sessionBody = await sessionResponse.json();
    const sessionId = sessionBody.sessions[0].id;

    const promptResponse = await fetch(`${baseUrl}/api/bridge/sessions/${encodeURIComponent(sessionId)}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ promptText: "Run a guarded bridge test", moduleId: "echobridgecore", agentLane: "cyberdex_agent" })
    });
    assert.equal(promptResponse.status, 202);
    const promptBody = await promptResponse.json();
    assert.equal(promptBody.jobs[0].status, "needs_confirmation");
    assert.equal(promptBody.jobs[0].diagnostics[0].code, "ECHO-BRIDGE-CODEX-NOT-CONFIGURED");
    assert.match(promptBody.jobs[0].streamCursors.state, /\.state:1$/);
    assert.equal(promptBody.safeActionRequests[0].status, "pending_confirmation");

    const logs = await fetch(`${baseUrl}/api/bridge/jobs/${encodeURIComponent(promptBody.jobs[0].id)}/logs`);
    assert.equal(logs.status, 200);
    assert.equal((await logs.json()).logs.length, 1);

    const confirmation = await fetch(`${baseUrl}/api/bridge/safe-actions/${encodeURIComponent(promptBody.safeActionRequests[0].id)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed: true })
    });
    assert.equal(confirmation.status, 200);
    const confirmationBody = await confirmation.json();
    assert.equal(confirmationBody.jobs[0].status, "blocked");
    assert.equal(confirmationBody.confirmationHistory[0].status, "blocked");
  });
});

test("bridge executor probe can reach configured and report unsupported executor failure", async () => {
  await withServer(async (baseUrl, root) => {
    const executablePath = process.execPath;
    fs.writeFileSync(path.join(root, "bridge-executor.json"), JSON.stringify({
      enabled: true,
      executablePath,
      workspaceRoot: root,
      allowlistedExecutableNames: [path.basename(process.execPath)]
    }, null, 2));

    const probe = await fetch(`${baseUrl}/api/bridge/executor/probe`);
    assert.equal(probe.status, 200);
    const probeBody = await probe.json();
    assert.equal(probeBody.status, "configured");
    assert.equal(probeBody.executablePath, `[localOnly:${path.basename(process.execPath).replaceAll("\\", "/")}]`);

    const sessionResponse = await fetch(`${baseUrl}/api/bridge/sessions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "Configured probe session" })
    });
    const sessionBody = await sessionResponse.json();
    const sessionId = sessionBody.sessions[0].id;

    const promptResponse = await fetch(`${baseUrl}/api/bridge/sessions/${encodeURIComponent(sessionId)}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ promptText: "Queue only after executor probe", moduleId: "echobridgecore", agentLane: "architect_agent" })
    });
    const promptBody = await promptResponse.json();
    assert.equal(promptBody.executorStatus, "configured");
    assert.equal(promptBody.jobs[0].status, "needs_confirmation");
    assert.equal(promptBody.jobs[0].diagnostics[0].code, "ECHO-BRIDGE-EXECUTOR-CONFIGURED");

    const confirmation = await fetch(`${baseUrl}/api/bridge/safe-actions/${encodeURIComponent(promptBody.safeActionRequests[0].id)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed: true })
    });
    const confirmationBody = await confirmation.json();
    assert.equal(confirmationBody.jobs[0].status, "failed");
    assert.match(confirmationBody.jobs[0].summary, /failed/);
    assert.equal(confirmationBody.confirmationHistory[0].status, "approved");
  });
});

test("bridge trusted executor adapter streams a configured fake sidecar", async () => {
  await withServer(async (baseUrl, root) => {
    const sidecar = path.join(root, "fake-sidecar.cjs");
    fs.writeFileSync(sidecar, `
const fs = require("fs");
const args = process.argv.slice(2);
if (args.join(" ").includes("whoami")) process.exit(7);
const promptFile = args[args.indexOf("--prompt-file") + 1];
const jobId = args[args.indexOf("--echo-bridge-job") + 1];
console.log("fake stdout one " + jobId);
console.error("fake stderr token=secret-value");
setTimeout(() => {
  console.log("fake stdout two " + fs.existsSync(promptFile));
}, 25);
`);
    fs.writeFileSync(path.join(root, "bridge-executor.json"), JSON.stringify({
      enabled: true,
      executablePath: process.execPath,
      sidecarEntrypoint: sidecar,
      workspaceRoot: root,
      allowlistedExecutableNames: [path.basename(process.execPath)]
    }, null, 2));

    const sessionBody = await (await fetch(`${baseUrl}/api/bridge/sessions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "Streaming executor session" })
    })).json();
    const sessionId = sessionBody.sessions[0].id;
    const promptBody = await (await fetch(`${baseUrl}/api/bridge/sessions/${encodeURIComponent(sessionId)}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ promptText: "Do not pass this as shell text && whoami", moduleId: "echobridgecore", agentLane: "cyberdex_agent" })
    })).json();

    const confirmation = await fetch(`${baseUrl}/api/bridge/safe-actions/${encodeURIComponent(promptBody.safeActionRequests[0].id)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed: true })
    });
    assert.equal(confirmation.status, 200);
    const confirmed = await confirmation.json();
    assert.ok(["running", "streaming", "completed"].includes(confirmed.jobs[0].status));

    const completed = await bridgeState(baseUrl, (body) => body.jobs[0]?.status === "completed");
    assert.equal(completed.jobs[0].status, "completed");
    assert.match(completed.jobs[0].streamCursors.stdout, /\.stdout:[23]$/);
    assert.match(completed.jobs[0].streamCursors.stderr, /\.stderr:1$/);
    assert.ok(completed.jobs[0].recentLogChunks.some((chunk) => chunk.stream === "stdout" && chunk.text.includes("fake stdout two true")));
    assert.ok(completed.jobs[0].recentLogChunks.some((chunk) => chunk.stream === "stderr" && chunk.text.includes("[redacted-secret]")));
    assert.ok(!completed.jobs[0].recentLogChunks.some((chunk) => chunk.text.includes("whoami")));

    const cursor = `${completed.jobs[0].id}.stdout:0`;
    const logsResponse = await fetch(`${baseUrl}/api/bridge/jobs/${encodeURIComponent(completed.jobs[0].id)}/logs?after=${encodeURIComponent(cursor)}`);
    assert.equal(logsResponse.status, 200);
    const logsBody = await logsResponse.json();
    assert.ok(logsBody.logs.every((chunk) => chunk.stream === "stdout"));
    assert.ok(logsBody.logs.length >= 1);
    assert.equal(logsBody.streamCursors.stdout, completed.jobs[0].streamCursors.stdout);
  });
});

test("bridge trusted executor blocks invalid config before launch", async () => {
  await withServer(async (baseUrl, root) => {
    const outside = path.join(os.tmpdir(), `outside-sidecar-${Date.now()}.cjs`);
    const marker = path.join(root, "should-not-exist.txt");
    fs.writeFileSync(outside, `require("fs").writeFileSync(${JSON.stringify(marker)}, "launched");`);
    fs.writeFileSync(path.join(root, "bridge-executor.json"), JSON.stringify({
      enabled: true,
      executablePath: process.execPath,
      sidecarEntrypoint: outside,
      workspaceRoot: root,
      allowlistedExecutableNames: [path.basename(process.execPath)]
    }, null, 2));

    const sessionBody = await (await fetch(`${baseUrl}/api/bridge/sessions`, { method: "POST" })).json();
    const promptBody = await (await fetch(`${baseUrl}/api/bridge/sessions/${encodeURIComponent(sessionBody.sessions[0].id)}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ promptText: "Attempt blocked sidecar" })
    })).json();
    const confirmationBody = await (await fetch(`${baseUrl}/api/bridge/safe-actions/${encodeURIComponent(promptBody.safeActionRequests[0].id)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed: true })
    })).json();
    assert.equal(confirmationBody.executorProbe.status, "blocked");
    assert.equal(confirmationBody.jobs[0].status, "blocked");
    assert.equal(fs.existsSync(marker), false);
  });
});

test("bridge cancel only targets the bridge-owned executor process", async () => {
  await withServer(async (baseUrl, root) => {
    const sidecar = path.join(root, "slow-sidecar.cjs");
    fs.writeFileSync(sidecar, `
let tick = 0;
console.log("slow sidecar started");
const timer = setInterval(() => {
  tick += 1;
  console.log("slow tick " + tick);
}, 100);
process.on("SIGTERM", () => {
  clearInterval(timer);
  console.log("slow sidecar canceled");
  process.exit(0);
});
`);
    fs.writeFileSync(path.join(root, "bridge-executor.json"), JSON.stringify({
      enabled: true,
      executablePath: process.execPath,
      sidecarEntrypoint: sidecar,
      workspaceRoot: root,
      allowlistedExecutableNames: [path.basename(process.execPath)]
    }, null, 2));

    const sessionBody = await (await fetch(`${baseUrl}/api/bridge/sessions`, { method: "POST" })).json();
    const promptBody = await (await fetch(`${baseUrl}/api/bridge/sessions/${encodeURIComponent(sessionBody.sessions[0].id)}/prompts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ promptText: "Start slow fake executor" })
    })).json();
    const confirmed = await (await fetch(`${baseUrl}/api/bridge/safe-actions/${encodeURIComponent(promptBody.safeActionRequests[0].id)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed: true })
    })).json();
    const jobId = confirmed.jobs[0].id;
    const running = await bridgeState(baseUrl, (body) => ["running", "streaming"].includes(body.jobs[0]?.status));
    assert.ok(["running", "streaming"].includes(running.jobs[0].status));

    const cancel = await fetch(`${baseUrl}/api/bridge/jobs/${encodeURIComponent(jobId)}/cancel`, { method: "POST" });
    assert.equal(cancel.status, 200);
    const canceled = await bridgeState(baseUrl, (body) => body.jobs[0]?.status === "canceled");
    assert.equal(canceled.jobs[0].status, "canceled");
    assert.ok(canceled.jobs[0].recentLogChunks.some((chunk) => chunk.text.includes("Cancellation signal sent")));
  });
});
