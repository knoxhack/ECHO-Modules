import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { buildJarManifest, JarPipelineError, promoteJarArtifacts, runJarPromotion } from "../src/server/jars.ts";
import { CommandCenterStore } from "../src/server/db.ts";

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
      { modId: "echoashfallprotocol", label: "Ashfall", version: "1.0.0", path: "." },
      { modId: "echostationfall", label: "Stationfall", version: "1.0.0", path: "addons/echostationfall" },
      { modId: "signalos", label: "SignalOS", version: "1.0.0", path: "addons/echosignalos" }
    ]
  };
}

function makeArcanaProject(root) {
  return {
    slug: "arcana",
    name: "ARCANA: Veilbound Studies",
    kind: "Standalone Mod",
    status: "Fixture",
    currentMilestone: "Fixture",
    buildHealth: 0,
    criticalIssues: 0,
    polishTasks: 0,
    lastScanLabel: "Never",
    nextRecommendedAction: "Scan",
    accent: "#c084fc",
    description: "Fixture",
    workspacePath: root,
    modules: [{ modId: "arcanaveil", label: "ARCANA: Veilbound Studies", version: "0.1.0", path: "." }]
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

function write(file, content) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
}

function checksum(value) {
  return createHash("sha256").update(value).digest("hex");
}

test("jar manifest reports expected, missing, stale, duplicate, and current jars", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-manifest-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  const modsDir = path.join(root, "mods");
  fs.mkdirSync(modsDir);

  write(path.join(buildRoot, "root", "libs", "echoashfallprotocol-1.0.0.jar"), "ashfall-current");
  write(path.join(buildRoot, "echostationfall", "libs", "echostationfall-1.0.0.jar"), "station-current");
  write(path.join(modsDir, "echoashfallprotocol-1.0.0.jar"), "ashfall-current");
  write(path.join(modsDir, "echoashfallprotocol-0.9.0.jar"), "ashfall-old");
  write(path.join(modsDir, "echostationfall-1.0.0.jar"), "station-old-bytes");
  write(path.join(modsDir, "unrelatedmod-9.9.9.jar"), "foreign");

  const manifest = buildJarManifest(makeProject(), makeSettings(root, modsDir), { buildRoot });
  const statuses = new Map(manifest.targetEntries.map((entry) => [entry.fileName, entry.status]));

  assert.equal(manifest.summary.expected, 3);
  assert.equal(manifest.summary.built, 2);
  assert.equal(manifest.summary.missing, 1);
  assert.equal(manifest.summary.current, 1);
  assert.equal(statuses.get("echoashfallprotocol-1.0.0.jar"), "current");
  assert.equal(statuses.get("echoashfallprotocol-0.9.0.jar"), "duplicate");
  assert.equal(statuses.get("echostationfall-1.0.0.jar"), "stale");
  assert.equal(manifest.targetEntries.some((entry) => entry.fileName === "unrelatedmod-9.9.9.jar"), false);
});

test("jar manifest discovers standalone project build outputs", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-arcana-"));
  write(path.join(root, "build", "libs", "arcanaveil-0.1.0.jar"), "arcana-current");

  const manifest = buildJarManifest(makeArcanaProject(root), makeSettings(path.join(root, "unused-echo-root"), ""));

  assert.equal(manifest.summary.expected, 1);
  assert.equal(manifest.summary.built, 1);
  assert.match(manifest.buildRoot, /build$/);
  assert.equal(manifest.artifacts[0].expectedFileName, "arcanaveil-0.1.0.jar");
  assert.equal(fs.existsSync(manifest.artifacts[0].sourcePath), true);
});

test("promote blocks when the configured mods folder is missing", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-blocked-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  write(path.join(buildRoot, "root", "libs", "echoashfallprotocol-1.0.0.jar"), "ashfall-current");

  assert.throws(
    () => promoteJarArtifacts(makeProject(), makeSettings(root, path.join(root, "missing-mods")), { buildRoot }),
    /Modpack Mods Folder does not exist/
  );
});

test("promote quarantines stale jars, copies current jars, and verifies checksums", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-promote-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  const modsDir = path.join(root, "mods");
  const quarantineDir = path.join(root, "quarantine");
  const project = {
    ...makeProject(),
    modules: [{ modId: "echoashfallprotocol", label: "Ashfall", version: "1.0.0", path: "." }]
  };
  fs.mkdirSync(modsDir);
  write(path.join(buildRoot, "root", "libs", "echoashfallprotocol-1.0.0.jar"), "ashfall-current");
  write(path.join(modsDir, "echoashfallprotocol-1.0.0.jar"), "old-same-name");
  write(path.join(modsDir, "echoashfallprotocol-0.9.0.jar"), "old-version");

  const result = promoteJarArtifacts(project, makeSettings(root, modsDir), {
    buildRoot,
    quarantineDir,
    now: () => new Date("2026-05-10T00:00:00.000Z")
  });

  assert.equal(result.moved.length, 2);
  assert.equal(result.copied.length, 1);
  assert.equal(result.verified.length, 1);
  assert.equal(fs.readFileSync(path.join(modsDir, "echoashfallprotocol-1.0.0.jar"), "utf-8"), "ashfall-current");
  assert.equal(result.manifest.summary.current, 1);
  assert.ok(fs.existsSync(path.join(quarantineDir, "2026-05-10T00-00-00-000Z", "echo", "echoashfallprotocol-0.9.0.jar")));
  assert.ok(fs.existsSync(path.join(quarantineDir, "2026-05-10T00-00-00-000Z", "echo", "echoashfallprotocol-1.0.0.jar")));
});

test("promote fails when copied checksum does not match source", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-checksum-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  const modsDir = path.join(root, "mods");
  const project = {
    ...makeProject(),
    modules: [{ modId: "echoashfallprotocol", label: "Ashfall", version: "1.0.0", path: "." }]
  };
  fs.mkdirSync(modsDir);
  write(path.join(buildRoot, "root", "libs", "echoashfallprotocol-1.0.0.jar"), "source-bytes");

  const originalCopy = fs.copyFileSync;
  fs.copyFileSync = (source, target) => {
    originalCopy(source, target);
    fs.writeFileSync(target, "corrupt-bytes");
  };
  try {
    assert.throws(
      () => promoteJarArtifacts(project, makeSettings(root, modsDir), { buildRoot }),
      /verification failed/
    );
  } finally {
    fs.copyFileSync = originalCopy;
  }

  assert.notEqual(checksum(fs.readFileSync(path.join(modsDir, "echoashfallprotocol-1.0.0.jar"))), checksum("source-bytes"));
});

test("promote explains locked target jars instead of leaking EBUSY", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-locked-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  const modsDir = path.join(root, "mods");
  const project = {
    ...makeProject(),
    modules: [{ modId: "echoashfallprotocol", label: "Ashfall", version: "1.0.0", path: "." }]
  };
  fs.mkdirSync(modsDir);
  write(path.join(buildRoot, "root", "libs", "echoashfallprotocol-1.0.0.jar"), "ashfall-current");
  write(path.join(modsDir, "echoashfallprotocol-1.0.0.jar"), "old-same-name");

  const originalRename = fs.renameSync;
  fs.renameSync = () => {
    const error = new Error("resource busy or locked");
    error.code = "EBUSY";
    throw error;
  };
  try {
    assert.throws(
      () => promoteJarArtifacts(project, makeSettings(root, modsDir), { buildRoot }),
      (error) =>
        error instanceof JarPipelineError &&
        error.statusCode === 423 &&
        /Managed jar is locked/.test(error.message) &&
        /Close the running Minecraft instance/.test(error.message)
    );
  } finally {
    fs.renameSync = originalRename;
  }
});

test("runJarPromotion records promote history", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-jars-run-"));
  const buildRoot = path.join(root, "EchoBuild", "Echo");
  const modsDir = path.join(root, "mods");
  const dbDir = path.join(root, "db");
  const store = new CommandCenterStore(path.join(dbDir, "command-center.sqlite"));
  try {
    fs.mkdirSync(modsDir);
    const seededProject = store.getProject("echostationfall");
    assert.ok(seededProject);
    const stationfallVersion = seededProject.modules[0]?.version ?? "1.1.1";
    const project = {
      ...seededProject,
      workspacePath: root,
      modules: [{ ...seededProject.modules[0], path: "." }]
    };
    write(path.join(buildRoot, "libs", `echostationfall-${stationfallVersion}.jar`), "station-current");
    const settings = store.updateSettings({ echoRoot: root, modpackModsDir: modsDir });

    const result = runJarPromotion(store, project, settings, { buildRoot });
    assert.equal(result.run.status, "succeeded");
    assert.equal(store.listCommandRuns("echostationfall", 5)[0].commandId, "promote-jars");
  } finally {
    store.close();
  }
});
