import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const openlandsRoot = "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands";

const foundationBlockMap = {
  sand: "echomaterialcore:sand",
  clay: "echomaterialcore:clay",
  gravel: "echomaterialcore:gravel",
  fieldstone: "echomaterialcore:fieldstone",
  branchwood_log: "echomaterialcore:branchwood_log",
  branchwood_planks: "echomaterialcore:branchwood_planks",
  branchwood_beam: "echomaterialcore:branchwood_beam",
  branchwood_post: "echomaterialcore:branchwood_post",
  wooden_slab: "echomaterialcore:wooden_slab",
  wooden_stairs: "echomaterialcore:wooden_stairs",
  wooden_fence: "echomaterialcore:wooden_fence",
  wooden_door: "echomaterialcore:wooden_door",
  wooden_trapdoor: "echomaterialcore:wooden_trapdoor",
  ladder: "echomaterialcore:ladder",
  copper_ore: "echomaterialcore:cupral_vein",
  tin_ore: "echomaterialcore:tinveil_vein",
  iron_ore: "echomaterialcore:ferrite_vein",
  chest: "echostationcore:field_crate",
  workbench: "echostationcore:field_bench",
  kiln: "echostationcore:kiln",
  forge: "echostationcore:forge_hearth",
  loom: "echostationcore:loom",
  cookpot: "echostationcore:cookpot",
  mason_table: "echostationcore:mason_table",
  campfire: "echoworldstarter:campfire",
  torch: "echoworldstarter:pitchlight",
  bedroll_block: "echoworldstarter:bedroll_block"
};

const foundationItemMap = {
  branchwood_stick: "echomaterialcore:branchwood_stick",
  fieldstone_piece: "echomaterialcore:fieldstone_piece",
  reed_fiber: "echomaterialcore:reed_fiber",
  fiber_binding: "echomaterialcore:fiber_binding",
  flint_shard: "echomaterialcore:flint_shard",
  raw_clay: "echomaterialcore:clay_lump",
  brick: "echomaterialcore:brick",
  glass_pane: "echomaterialcore:glass_pane",
  hide: "echomaterialcore:hide_strip",
  bone: "echomaterialcore:bone_shard",
  pitch: "echomaterialcore:pitch_resin",
  resin: "echomaterialcore:resin",
  charcoal: "echomaterialcore:charcoal_lump",
  copper_ore_chunk: "echomaterialcore:cupral_chunk",
  copper_ingot: "echomaterialcore:cupral_bar",
  tin_ore_chunk: "echomaterialcore:tinveil_chunk",
  tin_ingot: "echomaterialcore:tinveil_bar",
  bronze_ingot: "echomaterialcore:bronze_cast",
  iron_ore_chunk: "echomaterialcore:ferrite_chunk",
  iron_ingot: "echomaterialcore:ferrite_bar",
  crude_axe: "echotoolcore:crude_cutter",
  crude_pick: "echotoolcore:crude_breaker",
  crude_spade: "echotoolcore:crude_digger",
  flint_knife: "echotoolcore:flint_knife",
  wooden_hammer: "echotoolcore:field_hammer",
  copper_pick: "echotoolcore:cupral_breaker",
  bronze_pick: "echotoolcore:bronze_breaker",
  iron_pick: "echotoolcore:ferrite_breaker",
  torch_bundle: "echoworldstarter:pitchlight_bundle",
  bedroll: "echoworldstarter:bedroll",
  copper_fitting: "echoopenlandsprotocol:cupral_fitting"
};

const stationMap = {
  handcrafting: "echostationcore:handcrafting",
  workbench: "echostationcore:field_bench",
  kiln: "echostationcore:kiln",
  forge: "echostationcore:forge_hearth",
  cookpot: "echostationcore:cookpot",
  loom: "echostationcore:loom",
  mason_table: "echostationcore:mason_table"
};

const toolRoleMap = {
  spade: "foundation:tool_role/digger",
  pick: "foundation:tool_role/breaker",
  crude_pick: "echotoolcore:crude_breaker",
  copper_pick: "echotoolcore:cupral_breaker",
  bronze_pick: "echotoolcore:bronze_breaker",
  iron_pick: "echotoolcore:ferrite_breaker",
  "pick:copper_or_better": "foundation:tool_role/breaker:cupral_or_better",
  "pick:bronze_or_better": "foundation:tool_role/breaker:bronze_or_better",
  "pick:iron_or_better": "foundation:tool_role/breaker:ferrite_or_better"
};

const tagMap = {
  "openlands:log": "foundation:wood/log",
  "openlands:planks": "foundation:wood/planks",
  "openlands:ore_chunk": "foundation:metal/raw",
  "openlands:ingot": "foundation:metal/bar",
  "openlands:tool": "foundation:tools",
  "openlands:pick": "foundation:tool_role/breaker"
};

const renamedOpenlandsIds = {
  copper_fitting: "cupral_fitting"
};

const materialBlockIds = new Set([
  "sand",
  "clay",
  "gravel",
  "fieldstone",
  "branchwood_log",
  "branchwood_planks",
  "branchwood_beam",
  "branchwood_post",
  "wooden_slab",
  "wooden_stairs",
  "wooden_fence",
  "wooden_door",
  "wooden_trapdoor",
  "ladder",
  "copper_ore",
  "tin_ore",
  "iron_ore"
]);

const materialItemIds = new Set([
  "branchwood_stick",
  "fieldstone_piece",
  "reed_fiber",
  "fiber_binding",
  "flint_shard",
  "raw_clay",
  "brick",
  "glass_pane",
  "hide",
  "bone",
  "pitch",
  "resin",
  "charcoal",
  "copper_ore_chunk",
  "copper_ingot",
  "tin_ore_chunk",
  "tin_ingot",
  "bronze_ingot",
  "iron_ore_chunk",
  "iron_ingot"
]);

const toolItemIds = new Set([
  "crude_axe",
  "crude_pick",
  "crude_spade",
  "flint_knife",
  "wooden_hammer",
  "copper_pick",
  "bronze_pick",
  "iron_pick"
]);

const stationBlockIds = new Set(["chest", "workbench", "kiln", "forge", "loom", "cookpot", "mason_table"]);
const starterBlockIds = new Set(["campfire", "torch", "bedroll_block"]);
const starterItemIds = new Set(["torch_bundle", "bedroll"]);

const materialRecipeIds = new Set([
  "fiber_binding",
  "branchwood_planks",
  "wooden_beam",
  "wooden_post",
  "wooden_slab",
  "wooden_stairs",
  "wooden_door",
  "wooden_trapdoor",
  "kiln_brick",
  "kiln_glass",
  "kiln_charcoal",
  "forge_copper_ingot",
  "forge_tin_ingot",
  "forge_bronze_ingot",
  "forge_iron_ingot"
]);

const toolRecipeIds = new Set(["crude_axe", "crude_pick", "crude_spade", "flint_knife", "wooden_hammer", "copper_pick", "bronze_pick", "iron_pick"]);
const stationRecipeIds = new Set(["workbench", "chest", "kiln", "forge"]);
const starterRecipeIds = new Set(["torch_bundle", "campfire", "bedroll"]);
const movedRecipeIds = new Set([...materialRecipeIds, ...toolRecipeIds, ...stationRecipeIds, ...starterRecipeIds]);

const movedBlockIds = new Set([...materialBlockIds, ...stationBlockIds, ...starterBlockIds]);
const movedItemIds = new Set([...materialItemIds, ...toolItemIds, ...starterItemIds]);

const foundationNameOverrides = {
  cupral_vein: "Cupral Vein",
  cupral_chunk: "Cupral Chunk",
  cupral_bar: "Cupral Bar",
  tinveil_vein: "Tinveil Vein",
  tinveil_chunk: "Tinveil Chunk",
  tinveil_bar: "Tinveil Bar",
  bronze_cast: "Bronze Cast",
  ferrite_vein: "Ferrite Vein",
  ferrite_chunk: "Ferrite Chunk",
  ferrite_bar: "Ferrite Bar",
  clay_lump: "Clay Lump",
  hide_strip: "Hide Strip",
  bone_shard: "Bone Shard",
  pitch_resin: "Pitch Resin",
  charcoal_lump: "Charcoal Lump",
  crude_cutter: "Crude Cutter",
  crude_breaker: "Crude Breaker",
  crude_digger: "Crude Digger",
  field_hammer: "Field Hammer",
  cupral_breaker: "Cupral Breaker",
  bronze_breaker: "Bronze Breaker",
  ferrite_breaker: "Ferrite Breaker",
  field_bench: "Field Bench",
  field_crate: "Field Crate",
  forge_hearth: "Forge Hearth",
  pitchlight: "Pitchlight",
  pitchlight_bundle: "Pitchlight Bundle",
  cupral_fitting: "Cupral Fitting"
};

function fullPath(relativePath) {
  return path.join(root, relativePath);
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(fullPath(relativePath), "utf8"));
}

function writeJson(relativePath, value) {
  const filePath = fullPath(relativePath);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function openlandsPath(relativePath) {
  return `${openlandsRoot}/${relativePath}`;
}

function shortId(id) {
  if (typeof id !== "string") return id;
  return id.includes(":") ? id.split(":").pop() : id;
}

function canonicalForShort(id) {
  return foundationBlockMap[id] ?? foundationItemMap[id] ?? stationMap[id] ?? null;
}

function canonicalLocalId(id) {
  const canonical = canonicalForShort(shortId(id));
  return canonical ? canonical.split(":")[1] : shortId(id);
}

function rewriteString(value) {
  if (typeof value !== "string") return value;
  if (toolRoleMap[value]) return toolRoleMap[value];
  if (tagMap[value]) return tagMap[value];
  if (value.startsWith("echoopenlandsprotocol:")) {
    const local = value.slice("echoopenlandsprotocol:".length);
    if (renamedOpenlandsIds[local]) return `echoopenlandsprotocol:${renamedOpenlandsIds[local]}`;
    return canonicalForShort(local) ?? value;
  }
  if (renamedOpenlandsIds[value]) return renamedOpenlandsIds[value];
  return canonicalForShort(value) ?? value;
}

function rewriteValue(value) {
  if (Array.isArray(value)) return value.map(rewriteValue);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, entryValue]) => [key, rewriteValue(entryValue)]));
  }
  return rewriteString(value);
}

function canonicalizeEntry(entry, idKind = "id") {
  const clone = rewriteValue(entry);
  const legacy = shortId(entry[idKind]);
  if (entry[idKind]) {
    clone.legacyOpenlandsId = `echoopenlandsprotocol:${legacy}`;
    clone[idKind] = rewriteString(entry[idKind]);
  }
  const local = shortId(clone[idKind]);
  if (foundationNameOverrides[local]) {
    clone.displayName = foundationNameOverrides[local];
  }
  return clone;
}

function moveEntries(entries, ids, idKind = "id") {
  return entries.filter((entry) => ids.has(shortId(entry[idKind]))).map((entry) => canonicalizeEntry(entry, idKind));
}

function keepEntries(entries, ids, idKind = "id") {
  return entries.filter((entry) => !ids.has(shortId(entry[idKind]))).map((entry) => rewriteValue(entry));
}

function rewriteOpenlandsItem(item) {
  const rewritten = rewriteValue(item);
  if (item.id === "copper_fitting") {
    rewritten.id = "cupral_fitting";
    rewritten.displayName = "Cupral Fitting";
    rewritten.texture = "cupral_fitting";
    rewritten.legacyOpenlandsId = "echoopenlandsprotocol:copper_fitting";
  }
  return rewritten;
}

function foundationRecipe(recipe) {
  const moved = canonicalizeEntry(recipe);
  moved.station = rewriteString(recipe.station);
  return moved;
}

function openlandsRecipe(recipe) {
  const moved = rewriteValue(recipe);
  moved.station = rewriteString(recipe.station);
  if (recipe.id === "copper_fitting") {
    moved.id = "cupral_fitting";
    moved.outputs = [{ item: "cupral_fitting", count: 4 }];
    moved.legacyOpenlandsId = "echoopenlandsprotocol:copper_fitting";
  }
  return moved;
}

function loadOpenlands() {
  return {
    blocks: readJson(openlandsPath("blocks/mvp_blocks.json")),
    items: readJson(openlandsPath("items/mvp_items.json")),
    recipes: readJson(openlandsPath("recipes/mvp_recipes.json")),
    tags: readJson(openlandsPath("tags/mvp_tags.json")),
    loot: readJson(openlandsPath("loot/mvp_loot.json")),
    creatures: readJson(openlandsPath("creatures/mvp_creatures.json")),
    route: readJson(openlandsPath("progression/first_hour_route.json")),
    conformance: readJson(openlandsPath("conformance/openlands_mvp_registry.json"))
  };
}

function writeMovedFoundationData(data) {
  const movedMaterialRecipes = data.recipes.recipes.filter((recipe) => materialRecipeIds.has(recipe.id)).map(foundationRecipe);
  writeJson("addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/moved_openlands_materials.json", {
    schema: "echo.foundation.moved_openlands_materials.v1",
    movedOrder: 1,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echomaterialcore",
    blocks: moveEntries(data.blocks.blocks, materialBlockIds),
    items: moveEntries(data.items.items, materialItemIds),
    recipes: movedMaterialRecipes
  });

  writeJson("addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/moved_openlands_tools.json", {
    schema: "echo.foundation.moved_openlands_tools.v1",
    movedOrder: 2,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echotoolcore",
    items: moveEntries(data.items.items, toolItemIds),
    recipes: data.recipes.recipes.filter((recipe) => toolRecipeIds.has(recipe.id)).map(foundationRecipe)
  });

  writeJson("addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/moved_openlands_stations.json", {
    schema: "echo.foundation.moved_openlands_stations.v1",
    movedOrder: 3,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echostationcore",
    blocks: moveEntries(data.blocks.blocks, stationBlockIds),
    stations: data.recipes.stations
      .filter((station) => station.id !== "map_table")
      .map((station) => {
        const moved = canonicalizeEntry(station);
        moved.requiresBlock = station.requiresBlock ? rewriteString(station.requiresBlock) : null;
        return moved;
      }),
    recipes: data.recipes.recipes.filter((recipe) => stationRecipeIds.has(recipe.id)).map(foundationRecipe)
  });

  writeJson("addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/moved_openlands_worldstarter.json", {
    schema: "echo.foundation.moved_openlands_worldstarter.v1",
    movedOrder: 4,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echoworldstarter",
    blocks: moveEntries(data.blocks.blocks, starterBlockIds),
    items: moveEntries(data.items.items, starterItemIds),
    recipes: data.recipes.recipes.filter((recipe) => starterRecipeIds.has(recipe.id)).map(foundationRecipe),
    firstHourSteps: data.route.firstHour.filter((step) => step.step <= 6).map(rewriteValue),
    shelterScore: rewriteValue(data.route.shelterScore)
  });

  const movedLootBlockIds = new Set(["gravel", "clay", "fieldstone", "copper_ore", "tin_ore", "iron_ore"]);
  const movedLootTables = new Set(["ruined_well_cache", "tiny_camp_supply", "old_mine_cache"]);
  writeJson("addons/echocommonloot/src/main/resources/data/echocommonloot/foundation/loot/moved_openlands_loot.json", {
    schema: "echo.foundation.moved_openlands_loot.v1",
    movedOrder: 5,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echocommonloot",
    blockDrops: data.loot.blockDrops.filter((drop) => movedLootBlockIds.has(drop.block)).map(rewriteValue),
    chestTables: data.loot.chestTables
      .filter((table) => movedLootTables.has(table.id))
      .map((table) => {
        const moved = rewriteValue(table);
        moved.legacyOpenlandsId = `echoopenlandsprotocol:${table.id}`;
        if (table.id === "ruined_well_cache") moved.id = "ruined_storage";
        if (table.id === "tiny_camp_supply") moved.id = "starter_cache";
        if (table.id === "old_mine_cache") moved.id = "material_scrap";
        return moved;
      })
  });

  const roleMap = {
    passive_small: "echocreatureroles:passive_small",
    passive_large: "echocreatureroles:passive_large",
    neutral: "echocreatureroles:territorial_medium",
    aquatic_passive: "echocreatureroles:aquatic_passive",
    hostile_small: "echocreatureroles:hostile_small",
    hostile_large: "echocreatureroles:hostile_large",
    hostile_rare: "echocreatureroles:night_stalker"
  };
  const sourceCategories = [...new Set(data.creatures.creatures.map((creature) => creature.category))].sort();
  writeJson("addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation/creatures/moved_openlands_creature_roles.json", {
    schema: "echo.foundation.moved_openlands_creature_roles.v1",
    movedOrder: 6,
    source: "echoopenlandsprotocol",
    canonicalOwner: "echocreatureroles",
    sourceCategories: sourceCategories.map((category) => ({
      openlandsCategory: category,
      foundationRole: roleMap[category] ?? `echocreatureroles:${category}`
    })),
    openlandsCreatureMappings: data.creatures.creatures.map((creature) => ({
      creature: `echoopenlandsprotocol:${creature.id}`,
      legacyCategory: creature.category,
      foundationRole: roleMap[creature.category] ?? `echocreatureroles:${creature.category}`
    }))
  });
}

function updateOpenlandsData(data) {
  data.blocks.blocks = keepEntries(data.blocks.blocks, movedBlockIds);
  data.items.items = data.items.items
    .filter((item) => !movedItemIds.has(item.id))
    .map(rewriteOpenlandsItem);

  data.recipes.foundationStations = data.recipes.stations
    .filter((station) => station.id !== "map_table")
    .map((station) => rewriteString(station.id));
  data.recipes.stations = data.recipes.stations
    .filter((station) => station.id === "map_table")
    .map(rewriteValue);
  data.recipes.recipes = data.recipes.recipes
    .filter((recipe) => !movedRecipeIds.has(recipe.id))
    .map(openlandsRecipe);

  data.tags.blockTags = {
    "openlands:natural_ground": ["meadow_grass_block", "forest_soil", "dry_soil", "mud"],
    "openlands:soil": ["meadow_grass_block", "forest_soil", "dry_soil"],
    "openlands:mud": ["mud"],
    "openlands:water_edge": ["mud", "echomaterialcore:sand", "echomaterialcore:clay", "echomaterialcore:gravel"],
    "openlands:stone": ["limestone", "granite", "shale", "deepstone", "fieldstone_bricks", "brick_block", "echomaterialcore:fieldstone"],
    "openlands:ore": ["glow_crystal_cluster"],
    "openlands:log": ["pine_log", "echomaterialcore:branchwood_log"],
    "openlands:planks": ["pine_planks", "echomaterialcore:branchwood_planks"],
    "openlands:beam": ["pine_beam", "echomaterialcore:branchwood_beam"],
    "openlands:post": ["pine_post", "echomaterialcore:branchwood_post"],
    "openlands:wood_family": [
      "pine_log",
      "pine_planks",
      "pine_beam",
      "pine_post",
      "echomaterialcore:branchwood_log",
      "echomaterialcore:branchwood_planks",
      "echomaterialcore:branchwood_beam",
      "echomaterialcore:branchwood_post"
    ],
    "openlands:shelter_roof": ["pine_planks", "echomaterialcore:branchwood_planks", "echomaterialcore:wooden_slab", "echomaterialcore:wooden_stairs", "thatch_roof", "fieldstone_bricks", "brick_block"],
    "openlands:shelter_wall": ["pine_planks", "pine_log", "echomaterialcore:branchwood_planks", "echomaterialcore:branchwood_log", "echomaterialcore:fieldstone", "limestone", "fieldstone_bricks", "brick_block"],
    "openlands:station": ["map_table"],
    "openlands:waystone": ["broken_waystone", "restored_waystone", "waystone_plinth"],
    "openlands:old_road": ["old_road_block", "old_road_marker"]
  };
  data.tags.foundationBlockTagRefs = {
    "foundation:natural_ground": ["foundation:earth", "foundation:stone"],
    "foundation:building_wood": ["foundation:wood"],
    "foundation:stations": ["foundation:crafting_surface", "foundation:storage", "foundation:heat_processing"]
  };
  data.tags.itemTags = {
    "openlands:food": data.tags.itemTags["openlands:food"],
    "openlands:waystone_repair": ["repair_kit", "cupral_fitting", "waystone_core", "region_rubbing", "old_road_token", "route_binding", "glow_crystal"],
    "openlands:builder_ux": ["echotoolcore:field_hammer", "scaffold_bundle"]
  };
  data.tags.foundationItemTagRefs = {
    "foundation:raw_material": ["foundation:wood", "foundation:stone", "foundation:fiber", "foundation:earth", "foundation:fuel"],
    "foundation:tools": ["foundation:tools"],
    "foundation:breakers": ["foundation:tool_role/breaker"],
    "foundation:metals": ["foundation:metal/raw", "foundation:metal/bar"]
  };

  const movedLootBlockIds = new Set(["gravel", "clay", "fieldstone", "copper_ore", "tin_ore", "iron_ore"]);
  const movedLootTables = new Set(["ruined_well_cache", "tiny_camp_supply", "old_mine_cache"]);
  data.loot.blockDrops = data.loot.blockDrops
    .filter((drop) => !movedLootBlockIds.has(drop.block))
    .map(rewriteValue);
  data.loot.chestTables = data.loot.chestTables
    .filter((table) => !movedLootTables.has(table.id))
    .map(rewriteValue);
  data.loot.foundationLootParents = {
    starterCache: "echocommonloot:starter_cache",
    ruinedStorage: "echocommonloot:ruined_storage",
    materialScrap: "echocommonloot:material_scrap",
    genericBlockDrops: "echocommonloot:generic_block_drops"
  };

  const roleMap = {
    passive_small: "echocreatureroles:passive_small",
    passive_large: "echocreatureroles:passive_large",
    neutral: "echocreatureroles:territorial_medium",
    aquatic_passive: "echocreatureroles:aquatic_passive",
    hostile_small: "echocreatureroles:hostile_small",
    hostile_large: "echocreatureroles:hostile_large",
    hostile_rare: "echocreatureroles:night_stalker"
  };
  data.creatures.creatures = data.creatures.creatures.map((creature) => {
    const rewritten = rewriteValue(creature);
    rewritten.legacyCategory = creature.category;
    rewritten.category = roleMap[creature.category] ?? `echocreatureroles:${creature.category}`;
    rewritten.foundationRole = rewritten.category;
    return rewritten;
  });
  data.creatures.foundationRoleSource = "echocreatureroles:creature_role_catalog";

  data.route.foundationRoute = "echoworldstarter:foundation/starter/first_hour_route";
  data.route.foundationMovedSteps = data.route.firstHour.filter((step) => step.step <= 6).map((step) => step.id);
  data.route.firstHour = data.route.firstHour.filter((step) => step.step >= 7).map(rewriteValue);
  data.route.playerPromise = "Openlands extends the Foundation first-hour route with the first waystone milestone.";

  const blockRegistry = new Set(data.conformance.blockRegistry);
  const itemRegistry = new Set(data.conformance.itemRegistry);
  const recipeRegistry = new Set(data.conformance.recipeRegistry);
  for (const id of movedBlockIds) blockRegistry.delete(id);
  for (const id of movedItemIds) itemRegistry.delete(id);
  for (const id of movedRecipeIds) recipeRegistry.delete(id);
  if (itemRegistry.delete("copper_fitting")) itemRegistry.add("cupral_fitting");
  if (recipeRegistry.delete("copper_fitting")) recipeRegistry.add("cupral_fitting");
  data.conformance.blockRegistry = [...blockRegistry];
  data.conformance.itemRegistry = [...itemRegistry];
  data.conformance.recipeRegistry = [...recipeRegistry];
  data.conformance.foundationRegistries = {
    blocksMovedToFoundation: [...movedBlockIds].sort().map((id) => foundationBlockMap[id]).filter(Boolean),
    itemsMovedToFoundation: [...movedItemIds].sort().map((id) => foundationItemMap[id]).filter(Boolean),
    recipesMovedToFoundation: [...movedRecipeIds].sort(),
    sourceAliases: "addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/id_aliases.json"
  };
  data.conformance.acceptanceChecks = data.conformance.acceptanceChecks.map((check) =>
    check
      .replace("Every block has id, displayName, hardness, tool, drops, tags, model, texture, and inherited runtimeParity.", "Every Openlands-owned block has id, displayName, hardness, tool/foundation tool role, drops, tags, model, texture, and inherited runtimeParity.")
      .replace("Every item has id, displayName, stackSize, useType, tags, model, texture, recipeRefs, and inherited runtimeParity.", "Every Openlands-owned item has id, displayName, stackSize, useType, tags, model, texture, recipeRefs, and inherited runtimeParity.")
      .replace("All registry IDs resolve in Native, Standalone, and NeoForge adapters.", "All Openlands registry IDs and Foundation dependency IDs resolve in Native, Standalone, and NeoForge adapters.")
  );
}

function writeOpenlandsData(data) {
  writeJson(openlandsPath("blocks/mvp_blocks.json"), data.blocks);
  writeJson(openlandsPath("items/mvp_items.json"), data.items);
  writeJson(openlandsPath("recipes/mvp_recipes.json"), data.recipes);
  writeJson(openlandsPath("tags/mvp_tags.json"), data.tags);
  writeJson(openlandsPath("loot/mvp_loot.json"), data.loot);
  writeJson(openlandsPath("creatures/mvp_creatures.json"), data.creatures);
  writeJson(openlandsPath("progression/first_hour_route.json"), data.route);
  writeJson(openlandsPath("conformance/openlands_mvp_registry.json"), data.conformance);
}

function updateOpenlandsAliasBridge() {
  const bridgePath = openlandsPath("foundation/foundation_alias_bridge.json");
  const bridge = readJson(bridgePath);
  const existing = new Set((bridge.aliases ?? []).map((alias) => alias.legacyId));
  const extra = [
    {
      legacyId: "echoopenlandsprotocol:copper_fitting",
      canonicalId: "echoopenlandsprotocol:cupral_fitting",
      owner: "echoopenlandsprotocol",
      reason: "Openlands waystone component renamed to avoid vanilla-adjacent copper public identity."
    }
  ];
  for (const alias of extra) {
    if (!existing.has(alias.legacyId)) bridge.aliases.push(alias);
  }
  writeJson(bridgePath, bridge);
}

function main() {
  const data = loadOpenlands();
  writeMovedFoundationData(data);
  updateOpenlandsData(data);
  writeOpenlandsData(data);
  updateOpenlandsAliasBridge();
}

main();
