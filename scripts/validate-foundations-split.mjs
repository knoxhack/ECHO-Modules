
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const strict = process.argv.includes("--strict");
const foundationModules = [
  "echofoundationcore",
  "echomaterialcore",
  "echotoolcore",
  "echostationcore",
  "echoworldstarter",
  "echocommonloot",
  "echocreatureroles"
];
const experienceModules = ["echoopenlandsprotocol", "echoashfallprotocol", "echoarcanadivisionprotocol"];
const forbiddenExperienceDeps = new Set(experienceModules);
const warnings = [];
const errors = [];

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(path.join(root, relativePath), "utf8"));
}

function exists(relativePath) {
  return fs.existsSync(path.join(root, relativePath));
}

function requireFile(relativePath) {
  if (!exists(relativePath)) {
    errors.push(`Missing required file: ${relativePath}`);
  }
}

function requireArrayCount(relativePath, key, minCount) {
  requireFile(relativePath);
  if (!exists(relativePath)) return;
  const value = readJson(relativePath);
  const array = value[key];
  if (!Array.isArray(array) || array.length < minCount) {
    errors.push(`${relativePath} expected ${key} to contain at least ${minCount} entries`);
  }
}

function listFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...listFiles(full));
    else if (entry.isFile()) files.push(full);
  }
  return files;
}

function checkManifest(moduleId) {
  const manifestPath = `addons/${moduleId}/src/main/resources/META-INF/echo.mod.json`;
  requireFile(manifestPath);
  if (!exists(manifestPath)) return null;
  const manifest = readJson(manifestPath);
  if (manifest.id !== moduleId) {
    errors.push(`${moduleId} manifest id mismatch: ${manifest.id}`);
  }
  return manifest;
}

const settings = fs.readFileSync(path.join(root, "settings.gradle"), "utf8");
for (const moduleId of foundationModules) {
  if (!settings.includes(`'${moduleId}'`)) {
    errors.push(`settings.gradle does not include ${moduleId}`);
  }
}

for (const moduleId of foundationModules) {
  requireFile(`addons/${moduleId}/build.gradle`);
  requireFile(`addons/${moduleId}/gradle.properties`);
  requireFile(`addons/${moduleId}/README.md`);
  const manifest = checkManifest(moduleId);
  if (manifest) {
    for (const required of ["echocore", "echonetcore"]) {
      if (!manifest.requires?.includes(required)) {
        errors.push(`${moduleId} does not require ${required}`);
      }
    }
  }
}

const requiredData = {
  echofoundationcore: [
    "foundation/contracts/module_contracts.json",
    "foundation/contracts/ownership_audit.json",
    "foundation/contracts/id_aliases.json",
    "foundation/contracts/legal_identity.json",
    "foundation/contracts/release_channels.json"
  ],
  echomaterialcore: [
    "foundation/materials/material_catalog.json",
    "foundation/materials/material_tags.json",
    "foundation/materials/metal_progression.json",
    "foundation/materials/migration_sources.json",
    "foundation/materials/moved_openlands_materials.json"
  ],
  echotoolcore: [
    "foundation/tools/tool_catalog.json",
    "foundation/tools/tool_roles.json",
    "foundation/tools/tool_progression.json",
    "foundation/tools/moved_openlands_tools.json"
  ],
  echostationcore: [
    "foundation/stations/station_catalog.json",
    "foundation/stations/station_tags.json",
    "foundation/stations/station_recipe_contracts.json",
    "foundation/stations/moved_openlands_stations.json"
  ],
  echoworldstarter: [
    "foundation/starter/first_hour_route.json",
    "foundation/starter/spawn_contract.json",
    "foundation/starter/starter_items.json",
    "foundation/starter/experience_hooks.json",
    "foundation/starter/moved_openlands_worldstarter.json"
  ],
  echocommonloot: [
    "foundation/loot/loot_pool_catalog.json",
    "foundation/loot/loot_tags.json",
    "foundation/loot/experience_extension_rules.json",
    "foundation/loot/moved_openlands_loot.json"
  ],
  echocreatureroles: [
    "foundation/creatures/creature_role_catalog.json",
    "foundation/creatures/spawn_category_contracts.json",
    "foundation/creatures/experience_mapping_rules.json",
    "foundation/creatures/moved_openlands_creature_roles.json"
  ]
};

for (const [moduleId, files] of Object.entries(requiredData)) {
  for (const file of files) {
    requireFile(`addons/${moduleId}/src/main/resources/data/${moduleId}/${file}`);
  }
}

requireArrayCount(
  "addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/moved_openlands_materials.json",
  "blocks",
  17
);
requireArrayCount(
  "addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/moved_openlands_materials.json",
  "items",
  20
);
requireArrayCount(
  "addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/moved_openlands_tools.json",
  "items",
  8
);
requireArrayCount(
  "addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/moved_openlands_stations.json",
  "blocks",
  7
);
requireArrayCount(
  "addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/moved_openlands_worldstarter.json",
  "firstHourSteps",
  6
);
requireArrayCount(
  "addons/echocommonloot/src/main/resources/data/echocommonloot/foundation/loot/moved_openlands_loot.json",
  "blockDrops",
  6
);
requireArrayCount(
  "addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation/creatures/moved_openlands_creature_roles.json",
  "openlandsCreatureMappings",
  10
);

for (const experience of ["echoopenlandsprotocol", "echoashfallprotocol"]) {
  const manifest = checkManifest(experience);
  if (!manifest) continue;
  for (const foundation of foundationModules) {
    if (!manifest.requires?.includes(foundation)) {
      errors.push(`${experience} does not require ${foundation}`);
    }
  }
  for (const required of manifest.requires ?? []) {
    if (forbiddenExperienceDeps.has(required) && required !== experience) {
      errors.push(`${experience} illegally depends on experience module ${required}`);
    }
  }
}

if (exists("addons/echoarcanadivisionprotocol/src/main/resources/META-INF/echo.mod.json")) {
  const manifest = checkManifest("echoarcanadivisionprotocol");
  for (const foundation of foundationModules) {
    if (!manifest.requires?.includes(foundation)) {
      errors.push(`echoarcanadivisionprotocol does not require ${foundation}`);
    }
  }
  requireFile("addons/echoarcanadivisionprotocol/src/main/resources/data/echoarcanadivisionprotocol/arcana_division/contracts/bootstrap_profile_routes.json");
} else {
  warnings.push("Arcana Division protocol shell is not present yet.");
}

const openlandsRegistryPath = "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json";
if (exists(openlandsRegistryPath)) {
  const registry = readJson(openlandsRegistryPath);
  const bridgePath = "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/foundation/foundation_alias_bridge.json";
  const openlandsBridgeAliases = exists(bridgePath)
    ? new Set((readJson(bridgePath).aliases ?? []).map((alias) => alias.legacyId?.replace("echoopenlandsprotocol:", "")))
    : new Set();
  const movedOpenlandsBlocks = new Set([
    "sand", "clay", "gravel", "fieldstone", "branchwood_log", "branchwood_planks",
    "branchwood_beam", "branchwood_post", "wooden_slab", "wooden_stairs", "wooden_fence",
    "wooden_door", "wooden_trapdoor", "ladder", "copper_ore", "tin_ore", "iron_ore",
    "chest", "workbench", "kiln", "forge", "loom", "cookpot", "mason_table",
    "campfire", "torch", "bedroll_block"
  ]);
  const movedOpenlandsItems = new Set([
    "branchwood_stick", "fieldstone_piece", "reed_fiber", "fiber_binding", "flint_shard",
    "raw_clay", "brick", "glass_pane", "hide", "bone", "pitch", "resin", "charcoal",
    "copper_ore_chunk", "copper_ingot", "tin_ore_chunk", "tin_ingot", "bronze_ingot",
    "iron_ore_chunk", "iron_ingot", "crude_axe", "crude_pick", "crude_spade",
    "flint_knife", "wooden_hammer", "copper_pick", "bronze_pick", "iron_pick",
    "torch_bundle", "bedroll"
  ]);
  const movedOpenlandsRecipes = new Set([
    "fiber_binding", "crude_axe", "crude_pick", "crude_spade", "flint_knife", "torch_bundle",
    "campfire", "workbench", "branchwood_planks", "wooden_beam", "wooden_post",
    "wooden_slab", "wooden_stairs", "wooden_door", "wooden_trapdoor", "chest", "bedroll",
    "wooden_hammer", "kiln", "forge", "kiln_brick", "kiln_glass", "kiln_charcoal",
    "forge_copper_ingot", "forge_tin_ingot", "forge_bronze_ingot", "forge_iron_ingot",
    "copper_pick", "bronze_pick", "iron_pick"
  ]);
  for (const leaked of movedOpenlandsBlocks) {
    if ((registry.blockRegistry ?? []).includes(leaked)) {
      errors.push(`Openlands block registry still owns Foundation block ${leaked}`);
    }
  }
  for (const leaked of movedOpenlandsItems) {
    if ((registry.itemRegistry ?? []).includes(leaked)) {
      errors.push(`Openlands item registry still owns Foundation item ${leaked}`);
    }
  }
  for (const leaked of movedOpenlandsRecipes) {
    if ((registry.recipeRegistry ?? []).includes(leaked)) {
      errors.push(`Openlands recipe registry still owns Foundation recipe ${leaked}`);
    }
  }
  for (const alias of ["workbench", "chest", "torch", "copper_ingot", "iron_ingot", "crude_pick", "copper_fitting"]) {
    if (!openlandsBridgeAliases.has(alias)) {
      errors.push(`Openlands alias bridge does not preserve legacy id ${alias}`);
    }
  }
  const blocks = readJson("addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json").blocks ?? [];
  const items = readJson("addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json").items ?? [];
  const recipes = readJson("addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json").recipes ?? [];
  const route = readJson("addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/progression/first_hour_route.json");
  for (const block of blocks) {
    if (movedOpenlandsBlocks.has(block.id?.replace("echoopenlandsprotocol:", ""))) {
      errors.push(`Openlands mvp_blocks still defines moved block ${block.id}`);
    }
  }
  for (const item of items) {
    if (movedOpenlandsItems.has(item.id?.replace("echoopenlandsprotocol:", ""))) {
      errors.push(`Openlands mvp_items still defines moved item ${item.id}`);
    }
  }
  for (const recipe of recipes) {
    if (movedOpenlandsRecipes.has(recipe.id)) {
      errors.push(`Openlands mvp_recipes still defines moved recipe ${recipe.id}`);
    }
  }
  if ((route.firstHour ?? []).length !== 1 || route.firstHour?.[0]?.id !== "first_waystone") {
    errors.push("Openlands first_hour_route must extend Foundation with only first_waystone");
  }
  if ((route.foundationMovedSteps ?? []).length !== 6) {
    errors.push("Openlands first_hour_route does not record six moved Foundation steps");
  }
  const openlandsAssetManifest = readJson("addons/echoopenlandsprotocol/src/main/resources/assets/echoopenlandsprotocol/asset_manifest.json");
  const gameplayCatalog = readJson("addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/index/mvp_gameplay_catalog.json");
  const manifestBlocks = openlandsAssetManifest.mvpCoverage?.blockIds ?? [];
  const manifestItems = openlandsAssetManifest.mvpCoverage?.itemIds ?? [];
  const registryBlocks = blocks.map((block) => block.id?.replace("echoopenlandsprotocol:", ""));
  const registryItems = items.map((item) => item.id?.replace("echoopenlandsprotocol:", ""));
  if (JSON.stringify(manifestBlocks) !== JSON.stringify(registryBlocks)) {
    errors.push("Openlands asset manifest block coverage does not match trimmed Openlands block registry");
  }
  if (JSON.stringify(manifestItems) !== JSON.stringify(registryItems)) {
    errors.push("Openlands asset manifest item coverage does not match trimmed Openlands item registry");
  }
  if ((gameplayCatalog.blockEntries ?? []).length !== blocks.length || gameplayCatalog.counts?.blocks !== blocks.length) {
    errors.push("Openlands gameplay catalog block count is stale after Foundation split");
  }
  if ((gameplayCatalog.itemEntries ?? []).length !== items.length || gameplayCatalog.counts?.items !== items.length) {
    errors.push("Openlands gameplay catalog item count is stale after Foundation split");
  }
  if (gameplayCatalog.counts?.recipes !== recipes.length) {
    errors.push("Openlands gameplay catalog recipe count is stale after Foundation split");
  }
  const openlandsLangText = fs.readFileSync(path.join(root, "addons/echoopenlandsprotocol/src/main/resources/assets/echoopenlandsprotocol/lang/en_us.json"), "utf8");
  for (const leaked of ["copper_ingot", "iron_ingot", "crude_pick", "copper_pick", "iron_pick", "torch_bundle", "workbench", "chest", "torch"]) {
    if (openlandsLangText.includes(`echoopenlandsprotocol.${leaked}`)) {
      errors.push(`Openlands lang still exposes moved key ${leaked}`);
    }
  }
  for (const foundationLang of [
    "addons/echomaterialcore/src/main/resources/assets/echomaterialcore/lang/en_us.json",
    "addons/echotoolcore/src/main/resources/assets/echotoolcore/lang/en_us.json",
    "addons/echostationcore/src/main/resources/assets/echostationcore/lang/en_us.json",
    "addons/echoworldstarter/src/main/resources/assets/echoworldstarter/lang/en_us.json"
  ]) {
    requireFile(foundationLang);
  }
}

const ashfallProfile = "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallBootstrapProductProfile.java";
if (exists(ashfallProfile)) {
  const text = fs.readFileSync(path.join(root, ashfallProfile), "utf8");
  for (const arcana of ["echoaetherworks", "echoarcanacore", "echospellcore", "echoritualcore", "echoriftworlds"]) {
    if (text.includes(arcana)) {
      errors.push(`Ashfall profile still owns Arcana module reference ${arcana}`);
    }
  }
  for (const blocked of ["contaminated_redstone", "contaminated_lapis", "map_table", "riftstone"]) {
    if (text.includes(blocked)) {
      errors.push(`Ashfall profile still contains blocked or moved id ${blocked}`);
    }
  }
  for (const required of ["charged_ash_circuit", "blue_ash_salt", "survey_table"]) {
    if (!text.includes(required)) {
      errors.push(`Ashfall profile does not contain renamed id ${required}`);
    }
  }
  const ashfallModItems = fs.readFileSync(path.join(root, "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModItems.java"), "utf8");
  for (const required of [
    "ashbone_shard",
    "scorched_hide_strip",
    "ashgrass_fiber",
    "sootcord_binding",
    "ashbone_shiv",
    "scavenger_spear"
  ]) {
    if (!ashfallModItems.includes(required)) {
      errors.push(`Ashfall item registry does not contain primitive Foundation variant id ${required}`);
    }
  }
  const arcanaRoutes = readJson("addons/echoarcanadivisionprotocol/src/main/resources/data/echoarcanadivisionprotocol/arcana_division/contracts/bootstrap_profile_routes.json");
  for (const arcana of ["echoaetherworks", "echoarcanacore", "echospellcore", "echoritualcore", "echoriftworlds"]) {
    const routeText = JSON.stringify(arcanaRoutes);
    if (!routeText.includes(arcana)) {
      errors.push(`Arcana Division bootstrap routes missing moved module ${arcana}`);
    }
  }
  const ashfallBridgePath = "addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/registries/foundation_alias_bridge.json";
  const ashfallBridge = readJson(ashfallBridgePath);
  const variantAliases = new Map((ashfallBridge.ashfallVariantAliases ?? []).map((alias) => [alias.legacyId, alias.canonicalId]));
  const requiredVariantAliases = new Map([
    ["echoashfallprotocol:animal_bone", "echoashfallprotocol:ashbone_shard"],
    ["echoashfallprotocol:animal_hide", "echoashfallprotocol:scorched_hide_strip"],
    ["echoashfallprotocol:plant_fiber", "echoashfallprotocol:ashgrass_fiber"],
    ["echoashfallprotocol:fiber_rope", "echoashfallprotocol:sootcord_binding"],
    ["echoashfallprotocol:bone_knife", "echoashfallprotocol:ashbone_shiv"],
    ["echoashfallprotocol:crude_spear", "echoashfallprotocol:scavenger_spear"]
  ]);
  for (const [legacyId, canonicalId] of requiredVariantAliases) {
    if (variantAliases.get(legacyId) !== canonicalId) {
      errors.push(`Ashfall alias bridge missing primitive variant alias ${legacyId} -> ${canonicalId}`);
    }
  }
  const missionAliases = new Map((ashfallBridge.legacyMissionAliases ?? []).map((alias) => [alias.legacyMissionId, alias.replacementMissionId]));
  for (const legacyMission of ["echoashfallprotocol:craft_bone_knife", "echoashfallprotocol:craft_crude_spear"]) {
    if (missionAliases.get(legacyMission) !== "echoashfallprotocol:assemble_wasteland_field_kit") {
      errors.push(`Ashfall alias bridge missing legacy mission alias ${legacyMission}`);
    }
  }
  const ashfallForbiddenIdentity = [
    "contaminated_redstone",
    "contaminated_lapis",
    "map_table",
    "riftstone",
    "CONTAMINATED_REDSTONE",
    "CONTAMINATED_LAPIS",
    "MAP_TABLE",
    "RIFTSTONE"
  ];
  const ashfallPrimitiveForbiddenIdentity = [
    "echoashfallprotocol:animal_bone",
    "echoashfallprotocol:animal_hide",
    "echoashfallprotocol:plant_fiber",
    "echoashfallprotocol:fiber_rope",
    "echoashfallprotocol:bone_knife",
    "echoashfallprotocol:crude_spear",
    "EchoAshfallProtocol.animal_bone",
    "EchoAshfallProtocol.animal_hide",
    "EchoAshfallProtocol.plant_fiber",
    "EchoAshfallProtocol.fiber_rope",
    "EchoAshfallProtocol.bone_knife",
    "EchoAshfallProtocol.crude_spear",
    "echoashfallprotocol.animal_bone",
    "echoashfallprotocol.animal_hide",
    "echoashfallprotocol.plant_fiber",
    "echoashfallprotocol.fiber_rope",
    "echoashfallprotocol.bone_knife",
    "echoashfallprotocol.crude_spear",
    "ANIMAL_BONE",
    "ANIMAL_HIDE",
    "PLANT_FIBER",
    "FIBER_ROPE",
    "BONE_KNIFE",
    "CRUDE_SPEAR",
    "Animal Bone",
    "Animal Hide",
    "Plant Fiber",
    "Fiber Rope",
    "Bone Knife",
    "Crude Spear"
  ];
  const ashfallPrimitiveForbiddenPathFragments = [
    "animal_bone",
    "animal_hide",
    "plant_fiber",
    "fiber_rope",
    "bone_knife",
    "crude_spear"
  ];
  const ashfallExcluded = new Set([
    path.normalize(path.join(root, "addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/registries/foundation_alias_bridge.json")),
    path.normalize(path.join(root, "addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/registries/arcana_division_bridge.json"))
  ]);
  for (const file of listFiles(path.join(root, "addons/echoashfallprotocol/src"))) {
    if (ashfallExcluded.has(path.normalize(file))) continue;
    if (![".java", ".json", ".toml", ".mcmeta", ".txt", ".md", ".properties"].includes(path.extname(file))) continue;
    const relativeFile = path.relative(root, file).replaceAll("\\", "/");
    for (const forbidden of ashfallPrimitiveForbiddenPathFragments) {
      if (relativeFile.includes(forbidden)) {
        errors.push(`Ashfall source/resource path still contains primitive Foundation duplicate ${forbidden}: ${relativeFile}`);
      }
    }
    const fileText = fs.readFileSync(file, "utf8");
    for (const forbidden of ashfallForbiddenIdentity) {
      if (fileText.includes(forbidden)) {
        errors.push(`Ashfall source/resource still contains moved identity ${forbidden}: ${path.relative(root, file)}`);
      }
    }
    for (const forbidden of ashfallPrimitiveForbiddenIdentity) {
      if (fileText.includes(forbidden)) {
        errors.push(`Ashfall source/resource still contains active primitive Foundation duplicate ${forbidden}: ${path.relative(root, file)}`);
      }
    }
  }
}

if (strict && warnings.length > 0) {
  errors.push(...warnings.map((warning) => `STRICT: ${warning}`));
}

if (warnings.length > 0) {
  console.log("Foundation split warnings:");
  for (const warning of warnings) console.log(`- ${warning}`);
}

if (errors.length > 0) {
  console.error("Foundation split validation failed:");
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

console.log(`Foundation split validation passed with ${warnings.length} warning(s).`);
