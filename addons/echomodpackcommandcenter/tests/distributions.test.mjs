import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { CommandCenterStore } from "../src/server/db.ts";
import { buildDistributionSummary, startPackExport } from "../src/server/distributions.ts";
import { createApp } from "../src/server/index.ts";

function tempStore() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-distributions-"));
  const echoRoot = path.join(dir, "Echo");
  const modsDir = path.join(dir, "mods");
  fs.mkdirSync(echoRoot, { recursive: true });
  fs.mkdirSync(modsDir, { recursive: true });
  const store = new CommandCenterStore(path.join(dir, "command-center.sqlite"));
  store.updateSettings({ echoRoot, modpackModsDir: modsDir });
  return { store, dir, echoRoot, modsDir };
}

function write(file, content) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
}

function moduleBuildDirectory(module) {
  const normalized = module.path.replaceAll("\\", "/");
  if (!normalized || normalized === ".") return "root";
  return normalized.split("/").filter(Boolean).at(-1) ?? module.modId;
}

function writePackBuildOutputs(store, buildRoot, packId) {
  const summary = buildDistributionSummary(store, { buildRoot });
  const pack = summary.packs.find((candidate) => candidate.id === packId);
  const echo = store.getProject("echo");
  assert.ok(pack);
  assert.ok(echo);
  const required = new Set(pack.requiredModuleIds);
  for (const module of echo.modules.filter((candidate) => required.has(candidate.modId))) {
    write(path.join(buildRoot, moduleBuildDirectory(module), "libs", `${module.modId}-${module.version}.jar`), `${module.modId}-current`);
  }
}

test("distribution summary exposes four official packs", () => {
  const { store } = tempStore();
  try {
    const summary = buildDistributionSummary(store);
    assert.deepEqual(summary.packs.map((pack) => pack.id), ["echo-prime", "ashfall", "orbital", "arcane-division"]);
    assert.equal(summary.packs.find((pack) => pack.id === "arcane-division")?.name, "Arcana Division");
    assert.equal(summary.launcherRelease.repo, "knoxhack/ECHOLauncher");
  } finally {
    store.close();
  }
});

test("local distribution export copies selected pack jars and quarantines stale managed jars", async () => {
  const { store, dir, modsDir } = tempStore();
  const buildRoot = path.join(dir, "EchoBuild", "Echo");
  const quarantineDir = path.join(dir, "quarantine");
  try {
    writePackBuildOutputs(store, buildRoot, "ashfall");
    write(path.join(modsDir, "echocore-0.0.1.jar"), "old-core");

    const result = await startPackExport(
      store,
      {
        packId: "ashfall",
        targetModsDir: modsDir,
        confirmed: true,
        buildFirst: false,
        includeOptionalModules: false
      },
      {
        buildRoot,
        quarantineDir,
        now: () => new Date("2026-05-10T00:00:00.000Z")
      }
    );

    assert.equal(result.run.status, "succeeded");
    assert.equal(result.copied.length, result.pack.requiredModuleIds.length);
    assert.equal(result.verified.length, result.pack.requiredModuleIds.length);
    assert.equal(result.moved.length, 1);
    assert.equal(fs.existsSync(path.join(modsDir, "echocore-0.0.1.jar")), false);
    const echocore = store.getProject("echo").modules.find((module) => module.modId === "echocore");
    assert.ok(echocore);
    assert.equal(fs.existsSync(path.join(modsDir, `echocore-${echocore.version}.jar`)), true);
  } finally {
    store.close();
  }
});

test("distribution export blocks missing target folders before copying", async () => {
  const { store, dir } = tempStore();
  try {
    await assert.rejects(
      () => startPackExport(store, { packId: "ashfall", targetModsDir: path.join(dir, "missing"), confirmed: true }),
      /Target dev mods folder does not exist/
    );
  } finally {
    store.close();
  }
});

test("distribution API exposes summary and stop recovery", async () => {
  const { store } = tempStore();
  const app = createApp(store);
  const server = await new Promise((resolve, reject) => {
    const listeningServer = app.listen(0, "127.0.0.1", () => resolve(listeningServer));
    listeningServer.once("error", reject);
  });
  try {
    const address = server.address();
    assert.ok(address && typeof address !== "string");
    const baseUrl = `http://127.0.0.1:${address.port}`;
    const summary = await fetch(`${baseUrl}/api/distributions/summary`);
    assert.equal(summary.status, 200);
    assert.equal((await summary.json()).packs.length, 4);

    const run = store.createCommandRun({
      id: "dist-run",
      projectSlug: "distributions",
      commandId: "pack-local-export",
      status: "running",
      risk: "high",
      command: ["pack-export", "ashfall"],
      startedAt: "2026-05-10T00:00:00.000Z",
      metadata: { packId: "ashfall" },
      output: ""
    });
    const stopResponse = await fetch(`${baseUrl}/api/distributions/runs/${run.id}/stop`, { method: "POST" });
    assert.equal(stopResponse.status, 200);
    assert.equal((await stopResponse.json()).run.status, "stopped");
  } finally {
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
    store.close();
  }
});
