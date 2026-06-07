import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const seed = JSON.parse(fs.readFileSync(path.join(root, "src", "shared", "seed-data.json"), "utf-8"));
const featureCatalog = JSON.parse(fs.readFileSync(path.join(root, "src", "shared", "feature-catalog.json"), "utf-8"));
const settings = fs.readFileSync(path.resolve(root, "..", "..", "settings.gradle"), "utf-8");

function readGradleList(name) {
  const match = settings.match(new RegExp(`def ${name} = \\[(.*?)\\]`, "s"));
  assert.ok(match, `${name} missing from settings.gradle`);
  return [...match[1].matchAll(/'([^']+)'/g)].map((entry) => entry[1]);
}

function readModuleVersion(modulePath) {
  const echoRoot = path.resolve(root, "..", "..");
  const gradlePropertiesPath = modulePath === "."
    ? path.join(echoRoot, "gradle.properties")
    : path.join(echoRoot, modulePath, "gradle.properties");
  const gradleProperties = fs.readFileSync(gradlePropertiesPath, "utf-8");
  const version = gradleProperties.match(/^mod_version=(.+)$/m)?.[1]?.trim();
  assert.ok(version, `${gradlePropertiesPath} missing mod_version`);
  return version;
}

function sorted(values) {
  return [...values].sort();
}

const echoBetaAddons = readGradleList("echoBetaAddons");
const echoReleaseAddons = readGradleList("echoReleaseAddons");
const echoAddonProjects = [...new Set([...echoBetaAddons, ...echoReleaseAddons])];
const echoModulePaths = ["core/echocore", ".", ...echoAddonProjects.map((addon) => `addons/${addon}`)];

test("ECHO project seeds release modules", () => {
  const echo = seed.projects.find((project) => project.slug === "echo");
  assert.ok(echo);
  assert.equal(echo.modules.length, echoModulePaths.length);
  assert.deepEqual(
    sorted(echo.modules.map((module) => module.path)),
    sorted(echoModulePaths)
  );
});

test("new ECHO addon projects are seeded as first-class modules", () => {
  const projects = new Map(seed.projects.map((project) => [project.slug, project]));
  for (const slug of echoAddonProjects) {
    assert.equal(projects.get(slug)?.modules[0]?.path, `addons/${slug}`, `${slug} missing first-class project seed`);
  }
});

test("ECHO modules follow the active Gradle workspace addon set", () => {
  const echo = seed.projects.find((project) => project.slug === "echo");
  const workspacePaths = new Set(echo.modules.map((module) => module.path));
  for (const addon of echoAddonProjects) {
    assert.equal(workspacePaths.has(`addons/${addon}`), true, `${addon} missing from ECHO full-stack modules`);
  }
});

test("ECHO full-stack seed mirrors settings.gradle addon paths", () => {
  const echo = seed.projects.find((project) => project.slug === "echo");
  const seededPaths = new Set(echo.modules.map((module) => module.path));
  for (const expectedPath of echoModulePaths) {
    assert.equal(seededPaths.has(expectedPath), true, `${expectedPath} missing from ECHO full-stack seed`);
  }
});

test("ECHO RenderCore uses the current module version", () => {
  const echo = seed.projects.find((project) => project.slug === "echo");
  assert.equal(echo.modules.find((module) => module.modId === "echorendercore").version, readModuleVersion("addons/echorendercore"));
});

test("ECHO full-stack module versions match Gradle metadata", () => {
  const echo = seed.projects.find((project) => project.slug === "echo");
  for (const module of echo.modules) {
    assert.equal(module.version, readModuleVersion(module.path), `${module.modId} version is out of sync`);
  }
});

test("project seeds contain real modules only", () => {
  assert.deepEqual(
    sorted(seed.projects.map((project) => project.slug)),
    sorted(["echo", "echocore", "echoashfallprotocol", ...echoAddonProjects, "arcana"])
  );
});

test("full-stack release actions are a closed allowlist", () => {
  assert.deepEqual(
    seed.releaseActions.filter((action) => action.projectSlug === "echo").map((action) => action.commandId),
    [
      "build-beta-stack",
      "build-full-stack",
      "verify-release",
      "run-gametests",
      "scan-runtime-logs",
      "check-jar-set",
      "copy-jars",
      "remove-stale-jars",
      "generate-release-notes"
    ]
  );
});

test("module projects seed scoped release actions", () => {
  const moduleProjects = seed.projects.filter((project) => project.slug !== "echo" && project.slug !== "arcana");
  for (const project of moduleProjects) {
    const actions = seed.releaseActions.filter((action) => action.projectSlug === project.slug).map((action) => action.commandId).sort();
    assert.deepEqual(actions, ["build-module", "compile-java", "run-gametests", "validate-resources"]);
  }
});

test("ARCANA seeds standalone project actions", () => {
  const arcana = seed.projects.find((project) => project.slug === "arcana");
  assert.ok(arcana);
  assert.equal(arcana.workspacePath, "C:/Github/ARCANA");
  assert.deepEqual(arcana.modules, [
    {
      modId: "arcanaveil",
      label: "ARCANA: Veilbound Studies",
      version: "0.2.0",
      path: "."
    }
  ]);
  assert.deepEqual(
    seed.releaseActions.filter((action) => action.projectSlug === "arcana").map((action) => action.commandId).sort(),
    ["build-module", "compile-java", "run-gametests", "scan-runtime-logs", "validate-resources", "verify-release"]
  );
});

test("local actions cannot carry shell command payloads", () => {
  for (const action of seed.releaseActions.filter((candidate) => candidate.mode !== "shell")) {
    assert.equal(action.executable, "");
    assert.deepEqual(action.args, []);
  }
});

test("scanner tracks cover ECHO QA lanes", () => {
  const tracks = seed.qaTracks.filter((track) => track.projectSlug === "echo").map((track) => track.key).sort();
  assert.deepEqual(tracks, ["handoffs", "release-ops", "resources", "runtime-logs", "terminal"]);
});

test("exports have both prompt categories available", () => {
  const categories = new Set(seed.promptTemplates.map((prompt) => prompt.category));
  assert.equal(categories.has("Codex QA"), true);
  assert.equal(categories.has("Asset Prompt"), true);
});

test("feature catalog covers every seeded project with real source-backed rows", () => {
  const projectSlugs = new Set(seed.projects.map((project) => project.slug));
  const featureSlugs = new Set(featureCatalog.features.map((feature) => feature.projectSlug));
  for (const slug of projectSlugs) {
    assert.equal(featureSlugs.has(slug), true, `${slug} missing feature coverage`);
  }
  for (const feature of featureCatalog.features) {
    assert.equal(projectSlugs.has(feature.projectSlug), true, `${feature.id} references an unknown project`);
    assert.match(feature.id, /^[a-z0-9-]+$/);
    assert.ok(feature.title.trim());
    assert.ok(feature.playerPromise.trim());
    assert.ok(feature.loreContext.trim());
    assert.ok(feature.implementationSummary.trim());
    assert.ok(feature.nextAction.trim());
    assert.equal(feature.sources.length > 0, true, `${feature.id} missing sources`);
    assert.equal(feature.evidence.length > 0, true, `${feature.id} missing evidence`);
    assert.equal(JSON.stringify(feature).includes("placeholder"), false, `${feature.id} contains placeholder text`);
  }
});

test("feature catalog uses conservative status vocabulary", () => {
  const statuses = new Set(["implemented", "partial", "planned", "deferred", "blocked"]);
  for (const feature of featureCatalog.features) {
    assert.equal(statuses.has(feature.status), true, `${feature.id} has invalid status ${feature.status}`);
  }
  assert.equal(featureCatalog.features.some((feature) => feature.projectSlug === "echo" && feature.status === "partial"), true);
  assert.equal(featureCatalog.features.some((feature) => feature.projectSlug === "arcana" && feature.status === "implemented"), true);
});
