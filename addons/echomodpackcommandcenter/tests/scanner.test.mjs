import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { parseValidatorOutput, runHybridScan } from "../src/server/scanner.ts";

function makeProject() {
  return {
    slug: "echo",
    name: "ECHO",
    kind: "Minecraft Modpack",
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
    modules: [{ modId: "echoashfallprotocol", label: "Ashfall", version: "1.0.0", path: "." }]
  };
}

function write(file, content) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content, "utf-8");
}

test("quick scan reports real resource, lang, jar, JSON, and runtime-log issues", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-"));
  const modsDir = path.join(root, "mods");
  const assetRoot = path.join(root, "src", "main", "resources", "assets", "echoashfallprotocol");

  fs.mkdirSync(modsDir);
  write(path.join(assetRoot, "models", "item", "bad_item.json"), JSON.stringify({
    parent: "minecraft:item/generated",
    textures: { layer0: "echoashfallprotocol:item/missing_texture" }
  }));
  write(path.join(assetRoot, "lang", "en_us.json"), "{}");
  write(path.join(root, "src", "main", "resources", "data", "echoashfallprotocol", "recipes", "bad.json"), "{ nope");
  write(path.join(root, "run", "logs", "latest.log"), [
    "[main/WARN] java.lang.NoClassDefFoundError: test missing recipe bridge",
    "[main/ERROR] NoClassDefFoundError: com/example/Missing"
  ].join("\n"));
  write(path.join(root, "docs", "chapter_handoff_ids.md"), "Ashfall");
  fs.writeFileSync(path.join(modsDir, "echoashfallprotocol-0.9.0.jar"), "");

  const report = runHybridScan(makeProject(), [], {
    echoRoot: root,
    modpackModsDir: modsDir,
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  const codes = new Set(report.findings.map((finding) => finding.code));
  assert.equal(report.mode, "quick");
  assert.equal(report.status, "failed");
  assert.equal(report.source.quickChecks.includes("json"), true);
  assert.equal(report.summary.inventory.jsonFiles >= 2, true);
  assert.equal(codes.has("JSON_PARSE"), true);
  assert.equal(codes.has("MISSING_TEXTURE_REF"), true);
  assert.equal(codes.has("MISSING_LANG_KEY"), true);
  assert.equal(codes.has("RUNTIME_CRASH_SIGNATURE"), true);
  assert.equal(codes.has("STALE_JAR"), true);
  assert.equal(report.findings.filter((finding) => finding.code === "RUNTIME_CRASH_SIGNATURE").length, 1);
});

test("quick scan ignores generated runtime JSON", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-runtime-json-"));
  const modsDir = path.join(root, "mods");
  fs.mkdirSync(modsDir);
  write(path.join(root, "run", "downloads", "log.json"), "{ nope");

  const report = runHybridScan(makeProject(), [], {
    echoRoot: root,
    modpackModsDir: modsDir,
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  assert.equal(report.findings.some((finding) => finding.code === "JSON_PARSE"), false);
});

test("quick scan resolves live module versions for jar expectations", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-live-version-"));
  const modsDir = path.join(root, "mods");
  const moduleRoot = path.join(root, "addons", "echostationfall");
  fs.mkdirSync(modsDir);
  write(path.join(moduleRoot, "gradle.properties"), "mod_version=2.0.0\n");
  fs.writeFileSync(path.join(modsDir, "echostationfall-2.0.0.jar"), "");

  const report = runHybridScan({
    ...makeProject(),
    slug: "echostationfall",
    workspacePath: root,
    modules: [{ modId: "echostationfall", label: "Stationfall", version: "1.0.0", path: "addons/echostationfall" }]
  }, [], {
    echoRoot: root,
    modpackModsDir: modsDir,
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  assert.equal(report.findings.some((finding) => finding.code === "STALE_JAR" || finding.code === "JAR_COUNT_MISMATCH"), false);
});

test("quick scan accepts block lang for block item models but still flags true items", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-block-items-"));
  const modsDir = path.join(root, "mods");
  const assetRoot = path.join(root, "src", "main", "resources", "assets", "echoashfallprotocol");

  fs.mkdirSync(modsDir);
  write(path.join(assetRoot, "models", "item", "fixture_block.json"), JSON.stringify({
    parent: "echoashfallprotocol:block/fixture_block"
  }));
  write(path.join(assetRoot, "models", "block", "fixture_block.json"), JSON.stringify({
    parent: "minecraft:block/cube_all"
  }));
  write(path.join(assetRoot, "blockstates", "fixture_block.json"), JSON.stringify({
    variants: { "": { model: "echoashfallprotocol:block/fixture_block" } }
  }));
  write(path.join(assetRoot, "models", "item", "true_gadget.json"), JSON.stringify({
    parent: "minecraft:item/generated",
    textures: { layer0: "echoashfallprotocol:item/true_gadget" }
  }));
  write(path.join(assetRoot, "textures", "item", "true_gadget.png"), "");
  write(path.join(assetRoot, "lang", "en_us.json"), JSON.stringify({
    "block.echoashfallprotocol.fixture_block": "Fixture Block"
  }));

  const report = runHybridScan(makeProject(), [], {
    echoRoot: root,
    modpackModsDir: modsDir,
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  const missingLangDetails = report.findings
    .filter((finding) => finding.code === "MISSING_LANG_KEY")
    .map((finding) => finding.detail);
  assert.equal(missingLangDetails.includes("item.echoashfallprotocol.fixture_block"), false);
  assert.equal(missingLangDetails.includes("block.echoashfallprotocol.fixture_block"), false);
  assert.equal(missingLangDetails.includes("item.echoashfallprotocol.true_gadget"), true);
});

test("quick scan supports scoped module projects", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-module-"));
  const assetRoot = path.join(root, "addons", "echostationfall", "src", "main", "resources", "assets", "echostationfall");
  write(path.join(assetRoot, "models", "item", "station_probe.json"), JSON.stringify({
    parent: "minecraft:item/generated",
    textures: { layer0: "echostationfall:item/station_probe" }
  }));
  write(path.join(assetRoot, "textures", "item", "station_probe.png"), "");
  write(path.join(assetRoot, "lang", "en_us.json"), "{}");
  write(path.join(root, "src", "main", "resources", "data", "echoashfallprotocol", "recipes", "unrelated-bad.json"), "{ nope");

  const report = runHybridScan({
    ...makeProject(),
    slug: "echostationfall",
    modules: [{ modId: "echostationfall", label: "Stationfall", version: "1.0.0", path: "addons/echostationfall" }]
  }, [], {
    echoRoot: root,
    modpackModsDir: "",
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  const codes = new Set(report.findings.map((finding) => finding.code));
  assert.equal(codes.has("PROJECT_SCANNER_SKIPPED"), false);
  assert.equal(codes.has("MISSING_LANG_KEY"), true);
  assert.equal(codes.has("JSON_PARSE"), false);
  assert.equal(report.summary.inventory.itemModels, 1);
});

test("quick scan supports standalone non-ECHO projects", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-scan-arcana-"));
  const assetRoot = path.join(root, "src", "main", "resources", "assets", "arcanaveil");
  write(path.join(assetRoot, "models", "item", "veil_lens.json"), JSON.stringify({
    parent: "minecraft:item/generated",
    textures: { layer0: "arcanaveil:item/veil_lens" }
  }));
  write(path.join(assetRoot, "textures", "item", "veil_lens.png"), "");
  write(path.join(assetRoot, "lang", "en_us.json"), "{}");
  write(path.join(root, "src", "main", "resources", "data", "arcanaveil", "recipes", "bad.json"), "{ nope");

  const report = runHybridScan({
    ...makeProject(),
    slug: "arcana",
    name: "ARCANA: Veilbound Studies",
    kind: "Standalone Mod",
    workspacePath: root,
    modules: [{ modId: "arcanaveil", label: "ARCANA: Veilbound Studies", version: "0.1.0", path: "." }]
  }, [], {
    echoRoot: path.join(root, "unused-echo-root"),
    modpackModsDir: "",
    pythonExecutable: "python",
    runtimeLogMaxAgeMinutes: 180,
    defaultScanMode: "quick"
  }, "quick");

  const codes = new Set(report.findings.map((finding) => finding.code));
  assert.equal(codes.has("JSON_PARSE"), true);
  assert.equal(codes.has("MISSING_LANG_KEY"), true);
  assert.equal(report.summary.inventory.itemModels, 1);
});

test("validator output parser preserves source, code, path, line, and severity", () => {
  const findings = parseValidatorOutput(
    "validate_resources",
    "MISSING_TEXTURE assets/echo/models/item/foo.json:12 missing texture\nBAD_CONFIG config/echo.toml:4 wrong default",
    1
  );

  assert.equal(findings.length, 2);
  assert.equal(findings[0].source, "validate_resources");
  assert.equal(findings[0].code, "MISSING_TEXTURE");
  assert.equal(findings[0].path, "assets/echo/models/item/foo.json");
  assert.equal(findings[0].line, 12);
  assert.equal(findings[0].severity, "critical");
  assert.equal(findings[1].track, "resources");
  assert.equal(findings[1].metadata?.exitCode, 1);
});
