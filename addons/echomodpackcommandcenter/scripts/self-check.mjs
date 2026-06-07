import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const seedPath = path.join(root, "src", "shared", "seed-data.json");
const packagePath = path.join(root, "package.json");
const serverPath = path.join(root, "src", "server", "index.ts");
const settingsPath = path.resolve(root, "..", "..", "settings.gradle");
const commandCenterCatalogPath = path.resolve(root, "..", "..", "reports", "echo", "command-center-catalog.json");
const seed = JSON.parse(fs.readFileSync(seedPath, "utf-8"));
const pkg = JSON.parse(fs.readFileSync(packagePath, "utf-8"));
const server = fs.readFileSync(serverPath, "utf-8");
const settings = fs.readFileSync(settingsPath, "utf-8");
const commandCenterCatalog = JSON.parse(fs.readFileSync(commandCenterCatalogPath, "utf-8"));

const requiredRoutes = [
  "/api/health",
  "/api/settings",
  "/api/projects",
  "/api/projects/:slug",
  "/api/projects/:slug/scan",
  "/api/projects/:slug/scans",
  "/api/projects/:slug/scans/:reportId",
  "/api/projects/:slug/qa/latest",
  "/api/projects/:slug/roadmap",
  "/api/projects/:slug/prompts",
  "/api/projects/:slug/prompts/render",
  "/api/projects/:slug/release",
  "/api/projects/:slug/release/:commandId/run",
  "/api/projects/:slug/jars",
  "/api/projects/:slug/readiness",
  "/api/projects/:slug/features",
  "/api/projects/:slug/jars/build",
  "/api/projects/:slug/jars/promote",
  "/api/projects/:slug/runs",
  "/api/runs/:runId",
  "/api/runs/:runId/stop",
  "/api/modpack/summary",
  "/api/modpack/rebuild",
  "/api/modpack/runs",
  "/api/distributions/summary",
  "/api/reports/echo",
  "/api/distributions/export",
  "/api/distributions/runs",
  "/api/distributions/runs/:runId/stop",
  "/api/projects/:slug/export"
];

const expectedCommands = [
  "build-beta-stack",
  "build-full-stack",
  "verify-release",
  "run-gametests",
  "scan-runtime-logs",
  "check-jar-set",
  "copy-jars",
  "remove-stale-jars",
  "generate-release-notes"
];

function readGradleList(name) {
  const match = settings.match(new RegExp(`def ${name} = \\[(.*?)\\]`, "s"));
  assert.ok(match, `${name} missing from settings.gradle`);
  return [...match[1].matchAll(/'([^']+)'/g)].map((entry) => entry[1]);
}

function sorted(values) {
  return [...values].sort();
}

const echoAddonProjects = [...new Set([...readGradleList("echoBetaAddons"), ...readGradleList("echoReleaseAddons")])];
const expectedEchoModulePaths = ["core/echocore", ".", ...echoAddonProjects.map((addon) => `addons/${addon}`)];
const allowedProjectSlugs = new Set(["echo", "echocore", "echoashfallprotocol", ...echoAddonProjects, "arcana"]);
const catalogModules = Array.isArray(commandCenterCatalog.modules) ? commandCenterCatalog.modules : [];
const catalogIds = catalogModules.map((module) => module.id);
const catalogConfiguredAddons = catalogModules.filter((module) => module.configuredAddon === true).map((module) => module.id);

assert.equal(pkg.scripts.dev.includes("vite"), true, "dev script should run Vite");
assert.equal(pkg.scripts.dev.includes("src/server/index.ts"), true, "dev script should run backend");
assert.ok(pkg.dependencies.react, "React dependency is required");
assert.ok(pkg.devDependencies.tailwindcss, "Tailwind dependency is required");
assert.ok(server.includes("Confirmation required"), "Medium/high release actions must require API confirmation");
assert.ok(server.includes("req.body?.confirmed"), "Release confirmation must be checked on the backend");

for (const route of requiredRoutes) {
  assert.ok(server.includes(route), `Missing API route: ${route}`);
}

const echo = seed.projects.find((project) => project.slug === "echo");
assert.ok(echo, "ECHO project must be seeded");
const projectSlugs = seed.projects.map((project) => project.slug);
assert.equal(new Set(projectSlugs).size, projectSlugs.length, "Project slugs must be unique");
for (const projectSlug of projectSlugs) {
  assert.ok(allowedProjectSlugs.has(projectSlug), `${projectSlug} must map to a real workspace module or explicit external project`);
}
for (const modulePath of echo.modules.map((module) => module.path)) {
  assert.ok(expectedEchoModulePaths.includes(modulePath), `${modulePath} must map to a configured workspace module path`);
}
assert.ok(echo.modules.some((module) => module.path === "."), "ECHO seed must include the root Ashfall module");
assert.ok(echo.modules.some((module) => module.path === "core/echocore"), "ECHO seed must include core/echocore");
assert.ok(echo.modules.length >= 40, "ECHO seed should retain a broad curated module set");

const actionIds = seed.releaseActions.filter((action) => action.projectSlug === "echo").map((action) => action.commandId);
assert.deepEqual(actionIds, expectedCommands, "Release action allowlist changed unexpectedly");

for (const action of seed.releaseActions) {
  assert.ok(projectSlugs.includes(action.projectSlug), `${action.projectSlug} release action must target a seeded project`);
}

for (const project of seed.projects.filter((candidate) => candidate.slug !== "echo" && candidate.slug !== "arcana")) {
  const scopedActions = seed.releaseActions.filter((action) => action.projectSlug === project.slug).map((action) => action.commandId).sort();
  if (scopedActions.length > 0) {
    assert.deepEqual(scopedActions, ["build-module", "compile-java", "run-gametests", "validate-resources"], `${project.slug} scoped module actions changed unexpectedly`);
  }
}

const arcana = seed.projects.find((project) => project.slug === "arcana");
assert.ok(arcana, "ARCANA project must be seeded");
assert.equal(arcana.workspacePath, "C:/Github/ARCANA", "ARCANA should point at its checkout");
assert.deepEqual(arcana.modules.map((module) => module.modId), ["arcanaveil"], "ARCANA should seed its real mod id");
assert.deepEqual(
  seed.releaseActions.filter((action) => action.projectSlug === "arcana").map((action) => action.commandId).sort(),
  ["build-module", "compile-java", "run-gametests", "scan-runtime-logs", "validate-resources", "verify-release"],
  "ARCANA needs its standalone Gradle release actions"
);

for (const action of seed.releaseActions) {
  if (action.mode === "shell") {
    assert.equal(action.executable, ".\\gradlew.bat", `${action.commandId} must use Gradle wrapper`);
    assert.ok(Array.isArray(action.args) && action.args.length > 0, `${action.commandId} needs explicit args`);
  } else {
    assert.equal(action.executable, "", `${action.commandId} must not define shell executable`);
    assert.deepEqual(action.args, [], `${action.commandId} must not define shell args`);
  }
}

assert.equal(commandCenterCatalog.schema, "echo.commandcenter.generated_catalog", "Generated Command Center catalog schema changed unexpectedly");
assert.equal(commandCenterCatalog.deterministic, true, "Generated Command Center catalog must be deterministic");
assert.equal(commandCenterCatalog.localOnly, true, "Generated Command Center catalog must remain local-only");
assert.equal(commandCenterCatalog.requiresMinecraftLaunch, false, "Generated Command Center catalog must not require Minecraft launch");
assert.equal(commandCenterCatalog.redactionPolicy?.secretsRedacted, true, "Generated Command Center catalog must be redacted");
assert.equal(commandCenterCatalog.commandCenterBoundary?.configuredNeoForgeAddon, false, "Command Center catalog must not mark the local app as a configured NeoForge addon");
assert.equal(commandCenterCatalog.commandCenterBoundary?.runtimeDependency, false, "Command Center catalog must not create a runtime dependency");
assert.equal(commandCenterCatalog.commandCenterBoundary?.backendImplementedByThisArtifact, false, "Command Center catalog must remain a read-only artifact");
assert.equal(commandCenterCatalog.commandCenterBoundary?.consumptionMode, "read_only_catalog_parity_check", "Command Center catalog consumption mode must remain read-only");
assert.equal(commandCenterCatalog.catalogSummary?.generatedCatalogCoversEveryConfiguredAddon, true, "Generated catalog must cover every configured addon");
assert.deepEqual(sorted(catalogConfiguredAddons), sorted(echoAddonProjects), "Generated catalog configured addons must match settings.gradle");
assert.equal(new Set(catalogIds).size, catalogIds.length, "Generated catalog module ids must be unique");
for (const requiredCatalogId of ["echoashfallprotocol", "echocore", ...echoAddonProjects]) {
  assert.ok(catalogIds.includes(requiredCatalogId), `${requiredCatalogId} must be present in the generated Command Center catalog`);
}

const tracks = seed.qaTracks.filter((track) => track.projectSlug === "echo").map((track) => track.key).sort();
assert.deepEqual(tracks, ["handoffs", "release-ops", "resources", "runtime-logs", "terminal"], "Unexpected ECHO QA tracks");

const promptCategories = new Set(seed.promptTemplates.map((prompt) => prompt.category));
assert.ok(promptCategories.has("Codex QA"), "Codex QA prompts must be seeded");
assert.ok(promptCategories.has("Asset Prompt"), "Asset prompts must be seeded");

console.log("Noxhack Command Center self-check passed.");
