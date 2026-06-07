import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createApp } from "../src/server/index.ts";
import { CommandCenterStore } from "../src/server/db.ts";
import { startModpackRebuild, stopModpackRebuild } from "../src/server/modpack.ts";

function tempStore() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-modpack-stop-"));
  const modsDir = path.join(dir, "mods");
  fs.mkdirSync(modsDir);
  const store = new CommandCenterStore(path.join(dir, "command-center.sqlite"));
  store.updateSettings({ echoRoot: dir, modpackModsDir: modsDir });
  return { store, dir, modsDir };
}

function createModpackRun(store, status = "running") {
  return store.createCommandRun({
    id: randomUUID(),
    projectSlug: "modpack",
    commandId: "modpack-rebuild",
    status,
    risk: "high",
    command: ["modpack-rebuild", "echo", "arcana"],
    startedAt: "2026-05-23T21:59:10.866Z",
    finishedAt: status === "running" ? undefined : "2026-05-23T22:00:00.000Z",
    exitCode: status === "running" ? undefined : 0,
    durationMs: status === "running" ? undefined : 49134,
    metadata: {
      targetSlugs: ["echo", "arcana"],
      steps: [
        { id: "preflight", label: "Preflight", status: "succeeded", detail: "Ready." },
        { id: "promote-echo", label: "Promote ECHO Full Stack", status: "running", detail: "Promoting jars." }
      ]
    },
    output: "Modpack rebuild and update pipeline queued.\n"
  });
}

async function tempServer() {
  const { store, dir } = tempStore();
  const app = createApp(store);
  const server = await new Promise((resolve, reject) => {
    const listeningServer = app.listen(0, "127.0.0.1", () => resolve(listeningServer));
    listeningServer.once("error", reject);
  });
  const address = server.address();
  if (!address || typeof address === "string") {
    throw new Error("Expected TCP test server address");
  }
  return { store, dir, server, baseUrl: `http://127.0.0.1:${address.port}` };
}

async function closeServer(server, store) {
  await new Promise((resolve, reject) => {
    server.close((error) => {
      if (error) reject(error);
      else resolve();
    });
  });
  store.close();
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((promiseResolve, promiseReject) => {
    resolve = promiseResolve;
    reject = promiseReject;
  });
  return { promise, resolve, reject };
}

async function withTimeout(promise, label, timeoutMs = 3000) {
  let timer;
  try {
    return await Promise.race([
      promise,
      new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error(`${label} timed out`)), timeoutMs);
      })
    ]);
  } finally {
    clearTimeout(timer);
  }
}

test("stopModpackRebuild recovers orphaned persisted running run", () => {
  const { store } = tempStore();
  try {
    const run = createModpackRun(store);
    const result = stopModpackRebuild(store, run.id);
    const recovered = store.getCommandRun(run.id);

    assert.equal(result?.run.status, "stopped");
    assert.equal(recovered?.status, "stopped");
    assert.equal(recovered?.exitCode, 130);
    assert.equal(recovered?.metadata?.stopResult, "orphaned-running-run");
    assert.equal(recovered?.metadata?.previousStep, "promote-echo");
    assert.match(recovered?.output ?? "", /Stopped by stale-run recovery/);
    assert.equal(result?.run.steps.find((step) => step.id === "promote-echo")?.status, "stopped");
  } finally {
    store.close();
  }
});

test("stopModpackRebuild aborts active modpack build command", async () => {
  const { store } = tempStore();
  const started = deferred();
  const aborted = deferred();
  try {
    const result = await startModpackRebuild(store, true, {
      runInBackground: true,
      skipScans: true,
      runBuildCommand: ({ signal }) => {
        started.resolve();
        return new Promise((resolve) => {
          signal.addEventListener("abort", () => {
            aborted.resolve();
            resolve({ exitCode: 130, output: "build aborted" });
          }, { once: true });
        });
      }
    });

    await withTimeout(started.promise, "build start");
    const stopped = stopModpackRebuild(store, result.run.id);
    await withTimeout(aborted.promise, "build abort");

    assert.equal(stopped?.run.status, "stopped");
    assert.equal(store.getCommandRun(result.run.id)?.status, "stopped");
    assert.equal(store.getCommandRun(result.run.id)?.metadata?.stopResult, "signal-sent");
  } finally {
    store.close();
  }
});

test("stopped modpack runs do not block a new rebuild", async () => {
  const { store } = tempStore();
  const started = deferred();
  try {
    createModpackRun(store, "stopped");
    const result = await startModpackRebuild(store, true, {
      runInBackground: true,
      skipScans: true,
      runBuildCommand: ({ signal }) => {
        started.resolve();
        return new Promise((resolve) => {
          signal.addEventListener("abort", () => resolve({ exitCode: 130, output: "build aborted" }), { once: true });
        });
      }
    });

    assert.equal(result.run.status, "running");
    await withTimeout(started.promise, "build start");
    stopModpackRebuild(store, result.run.id);
  } finally {
    store.close();
  }
});

test("modpack stop API handles missing, non-running, and orphaned running runs", async () => {
  const { store, server, baseUrl } = await tempServer();
  try {
    const missing = await fetch(`${baseUrl}/api/modpack/runs/not-real/stop`, { method: "POST" });
    assert.equal(missing.status, 404);

    const succeeded = createModpackRun(store, "succeeded");
    const nonRunning = await fetch(`${baseUrl}/api/modpack/runs/${succeeded.id}/stop`, { method: "POST" });
    assert.equal(nonRunning.status, 200);
    assert.equal((await nonRunning.json()).run.status, "succeeded");

    const running = createModpackRun(store);
    const stopped = await fetch(`${baseUrl}/api/modpack/runs/${running.id}/stop`, { method: "POST" });
    assert.equal(stopped.status, 200);
    const body = await stopped.json();
    assert.equal(body.run.status, "stopped");
    assert.equal(store.getCommandRun(running.id)?.metadata?.stopResult, "orphaned-running-run");
  } finally {
    await closeServer(server, store);
  }
});
