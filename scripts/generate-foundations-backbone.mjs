import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");

const foundationModules = [
  {
    id: "echofoundationcore",
    name: "ECHO Foundation Core",
    className: "EchoFoundationCore",
    role: "foundation_contracts",
    requires: ["echoadaptercore", "echocore", "echonetcore"],
    provides: [
      "foundation.core",
      "foundation.ownership",
      "foundation.aliases",
      "foundation.legal_identity",
      "foundation.registry_contracts"
    ],
    consumes: [],
    summary: "Shared survival/content ownership, dependency, alias, and legal identity contracts."
  },
  {
    id: "echomaterialcore",
    name: "ECHO Material Core",
    className: "EchoMaterialCore",
    role: "foundation_materials",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore"],
    provides: [
      "foundation.materials",
      "foundation.generic_blocks",
      "foundation.generic_items",
      "foundation.material_tags"
    ],
    consumes: ["foundation.core", "foundation.aliases", "foundation.legal_identity"],
    summary: "Baseline raw materials, refined materials, generic building blocks, and material tags."
  },
  {
    id: "echotoolcore",
    name: "ECHO Tool Core",
    className: "EchoToolCore",
    role: "foundation_tools",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore"],
    provides: [
      "foundation.tools",
      "foundation.tool_roles",
      "foundation.tool_progression"
    ],
    consumes: ["foundation.core", "foundation.materials", "foundation.material_tags"],
    summary: "Baseline hand tools, tool role tags, and survival tool progression."
  },
  {
    id: "echostationcore",
    name: "ECHO Station Core",
    className: "EchoStationCore",
    role: "foundation_stations",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore"],
    provides: [
      "foundation.stations",
      "foundation.station_roles",
      "foundation.shared_recipe_surfaces"
    ],
    consumes: ["foundation.core", "foundation.materials", "foundation.tools"],
    summary: "Baseline crafting/storage/processing stations and recipe surface contracts."
  },
  {
    id: "echoworldstarter",
    name: "ECHO World Starter",
    className: "EchoWorldStarter",
    role: "foundation_world_starter",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore", "echostationcore"],
    provides: [
      "foundation.spawn_safety",
      "foundation.first_hour",
      "foundation.shelter_rules",
      "foundation.starter_items"
    ],
    consumes: ["foundation.core", "foundation.materials", "foundation.tools", "foundation.stations"],
    summary: "Spawn safety, first-hour survival route, starter shelter, and early light contracts."
  },
  {
    id: "echocommonloot",
    name: "ECHO Common Loot",
    className: "EchoCommonLoot",
    role: "foundation_common_loot",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore", "echostationcore"],
    provides: [
      "foundation.common_loot",
      "foundation.block_drops",
      "foundation.starter_caches"
    ],
    consumes: ["foundation.core", "foundation.materials", "foundation.tools", "foundation.stations"],
    summary: "Generic loot pools, baseline block drops, and starter cache contracts."
  },
  {
    id: "echocreatureroles",
    name: "ECHO Creature Roles",
    className: "EchoCreatureRoles",
    role: "foundation_creature_roles",
    requires: ["echoadaptercore", "echocore", "echonetcore", "echofoundationcore"],
    provides: [
      "foundation.creature_roles",
      "foundation.spawn_roles",
      "foundation.ai_pressure_roles"
    ],
    consumes: ["foundation.core"],
    summary: "Shared creature role taxonomy for experience-specific mobs."
  }
];

const foundationIds = foundationModules.map((module) => module.id);

const ownershipAudit = {
  schema: "echo.foundation.content_ownership_audit.v1",
  sourceExperience: "echoopenlandsprotocol",
  frozenSource: {
    status: "frozen-for-extraction",
    reason: "Openlands contains the most complete baseline survival slice and is the extraction source for Foundation content.",
    sourceFiles: [
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/tags/mvp_tags.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/loot/mvp_loot.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/progression/first_hour_route.json",
      "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json"
    ]
  },
  foundationModules: {
    echofoundationcore: {
      owns: [
        "ownership taxonomy",
        "dependency rule",
        "save-safe alias contract",
        "blocked/watch legal term lists",
        "shared registry schemas"
      ]
    },
    echomaterialcore: {
      owns: [
        "branchwood_stick",
        "fieldstone_piece",
        "reed_fiber",
        "fiber_binding",
        "flint_shard",
        "clay_lump",
        "brick",
        "glass_pane",
        "hide_strip",
        "bone_shard",
        "pitch_resin",
        "resin",
        "charcoal_lump",
        "cupral_vein",
        "cupral_chunk",
        "cupral_bar",
        "tinveil_vein",
        "tinveil_chunk",
        "tinveil_bar",
        "bronze_cast",
        "ferrite_vein",
        "ferrite_chunk",
        "ferrite_bar",
        "fieldstone",
        "clay",
        "gravel",
        "sand",
        "branchwood_log",
        "branchwood_planks",
        "branchwood_beam",
        "branchwood_post",
        "wooden_slab",
        "wooden_stairs",
        "wooden_fence",
        "wooden_door",
        "wooden_trapdoor",
        "ladder"
      ]
    },
    echotoolcore: {
      owns: [
        "crude_cutter",
        "crude_breaker",
        "crude_digger",
        "flint_knife",
        "field_hammer",
        "cupral_breaker",
        "bronze_breaker",
        "ferrite_breaker"
      ]
    },
    echostationcore: {
      owns: [
        "handcrafting",
        "field_bench",
        "field_crate",
        "kiln",
        "forge_hearth",
        "loom",
        "cookpot",
        "mason_table"
      ]
    },
    echoworldstarter: {
      owns: [
        "first_hour_steps_1_6",
        "spawn_safety_rules",
        "campfire",
        "pitchlight",
        "pitchlight_bundle",
        "bedroll",
        "bedroll_block",
        "starter_shelter_score"
      ]
    },
    echocommonloot: {
      owns: [
        "starter_cache",
        "traveler_pack",
        "ruined_storage",
        "material_scrap",
        "generic_block_drops",
        "generic_ore_chunk_drops"
      ]
    },
    echocreatureroles: {
      owns: [
        "passive_small",
        "passive_large",
        "neutral_forager",
        "territorial_medium",
        "hostile_small",
        "hostile_large",
        "aquatic_passive",
        "night_stalker"
      ]
    }
  },
  openlandsOnly: [
    "meadow_grass_block",
    "forest_soil",
    "dry_soil",
    "mud",
    "limestone",
    "granite",
    "shale",
    "deepstone",
    "pine_log",
    "pine_planks",
    "pine_beam",
    "pine_post",
    "thatch_roof",
    "fieldstone_bricks",
    "brick_block",
    "glass_block",
    "shelf",
    "sign",
    "lantern",
    "map_table",
    "glow_crystal_cluster",
    "glow_crystal",
    "meadows",
    "woodlands",
    "stonehills",
    "marshlands",
    "old_road_block",
    "old_road_marker",
    "broken_waystone",
    "restored_waystone",
    "waystone_plinth",
    "waystone_core",
    "region_rubbing",
    "old_road_token",
    "route_binding",
    "berries",
    "mushroom",
    "raw_meat",
    "cooked_meat",
    "grain",
    "root_crop",
    "fish",
    "stew_bowl",
    "compost",
    "scaffold_bundle",
    "wooden_bowl",
    "small_pack",
    "repair_kit",
    "copper_fitting"
  ],
  ashfallFindings: {
    baselineDuplicates: [
      "animal_bone",
      "animal_hide",
      "plant_fiber",
      "fiber_rope",
      "bone_knife",
      "scrap_knife",
      "crude_spear"
    ],
    renameNeeded: [
      "map_table",
      "riftstone",
      "contaminated_redstone",
      "contaminated_lapis"
    ],
    ashfallOnly: [
      "storms",
      "heat",
      "ash_exposure",
      "scarcity",
      "shelters",
      "filtration",
      "atmospheric_scrubbers",
      "distillation",
      "black_rain",
      "ash_soil",
      "scoria",
      "basalt",
      "sulfur",
      "emberglass"
    ]
  },
  arcanaSplit: {
    misplacedInAshfallProfile: [
      "echoaetherworks",
      "echoarcanacore",
      "echoarcaneindex",
      "echocursecore",
      "echofamiliarcore",
      "echogrimoire",
      "echoriftworlds",
      "echoritualcore",
      "echospellcore"
    ],
    newOwner: "echoarcanadivisionprotocol"
  }
};

const migrationRows = [
  ["echoopenlandsprotocol:workbench", "echostationcore:field_bench", "alias", "Vanilla-adjacent name; shared crafting station."],
  ["echoopenlandsprotocol:chest", "echostationcore:field_crate", "alias", "Vanilla-adjacent name; shared storage station."],
  ["echoopenlandsprotocol:torch", "echoworldstarter:pitchlight", "alias", "Vanilla-adjacent name; shared early light item/block."],
  ["echoopenlandsprotocol:torch_bundle", "echoworldstarter:pitchlight_bundle", "alias", "Bundle follows pitchlight public identity."],
  ["echoopenlandsprotocol:copper_ore", "echomaterialcore:cupral_vein", "alias", "Copper public identity replaced by cupral."],
  ["echoopenlandsprotocol:copper_ore_chunk", "echomaterialcore:cupral_chunk", "alias", "Copper public identity replaced by cupral."],
  ["echoopenlandsprotocol:copper_ingot", "echomaterialcore:cupral_bar", "alias", "Copper ingot replaced by cupral bar."],
  ["echoopenlandsprotocol:tin_ore", "echomaterialcore:tinveil_vein", "alias", "Tin public identity replaced by tinveil."],
  ["echoopenlandsprotocol:tin_ore_chunk", "echomaterialcore:tinveil_chunk", "alias", "Tin public identity replaced by tinveil."],
  ["echoopenlandsprotocol:tin_ingot", "echomaterialcore:tinveil_bar", "alias", "Tin ingot replaced by tinveil bar."],
  ["echoopenlandsprotocol:bronze_ingot", "echomaterialcore:bronze_cast", "alias", "Bronze ingot replaced by bronze cast."],
  ["echoopenlandsprotocol:iron_ore", "echomaterialcore:ferrite_vein", "alias", "Iron public identity replaced by ferrite."],
  ["echoopenlandsprotocol:iron_ore_chunk", "echomaterialcore:ferrite_chunk", "alias", "Iron public identity replaced by ferrite."],
  ["echoopenlandsprotocol:iron_ingot", "echomaterialcore:ferrite_bar", "alias", "Iron ingot replaced by ferrite bar."],
  ["echoopenlandsprotocol:crude_axe", "echotoolcore:crude_cutter", "alias", "Tool role name avoids vanilla axe identity."],
  ["echoopenlandsprotocol:crude_pick", "echotoolcore:crude_breaker", "alias", "Tool role name avoids vanilla pick identity."],
  ["echoopenlandsprotocol:crude_spade", "echotoolcore:crude_digger", "alias", "Tool role name avoids vanilla shovel/spade identity."],
  ["echoopenlandsprotocol:wooden_hammer", "echotoolcore:field_hammer", "alias", "Generic hammer belongs in Foundation tool set."],
  ["echoopenlandsprotocol:copper_pick", "echotoolcore:cupral_breaker", "alias", "Tool progression tied to cupral."],
  ["echoopenlandsprotocol:bronze_pick", "echotoolcore:bronze_breaker", "alias", "Tool progression tied to bronze cast."],
  ["echoopenlandsprotocol:iron_pick", "echotoolcore:ferrite_breaker", "alias", "Tool progression tied to ferrite."],
  ["echoopenlandsprotocol:raw_clay", "echomaterialcore:clay_lump", "alias", "Generic clay item belongs in Material Core."],
  ["echoopenlandsprotocol:charcoal", "echomaterialcore:charcoal_lump", "alias", "Generic fuel item belongs in Material Core."],
  ["echoopenlandsprotocol:pitch", "echomaterialcore:pitch_resin", "alias", "Generic adhesive/fuel item belongs in Material Core."],
  ["echoopenlandsprotocol:bone", "echomaterialcore:bone_shard", "alias", "Generic creature material belongs in Material Core."],
  ["echoopenlandsprotocol:hide", "echomaterialcore:hide_strip", "alias", "Generic creature material belongs in Material Core."],
  ["echoopenlandsprotocol:forge", "echostationcore:forge_hearth", "alias", "Shared processing station with distinct ECHO identity."],
  ["echoashfallprotocol:animal_bone", "echomaterialcore:bone_shard", "alias-or-tag", "Ashfall duplicate baseline creature material."],
  ["echoashfallprotocol:animal_hide", "echomaterialcore:hide_strip", "alias-or-tag", "Ashfall duplicate baseline creature material."],
  ["echoashfallprotocol:plant_fiber", "echomaterialcore:reed_fiber", "alias-or-tag", "Ashfall duplicate baseline fiber material."],
  ["echoashfallprotocol:fiber_rope", "echomaterialcore:fiber_binding", "alias-or-tag", "Ashfall duplicate baseline binding material."],
  ["echoashfallprotocol:map_table", "echoashfallprotocol:survey_table", "rename-needed", "Map table is Openlands fantasy; Ashfall needs survey/evac identity."],
  ["echoashfallprotocol:riftstone", "echoarcanadivisionprotocol:nexus_scar_stone", "move-or-rename", "Rift identity belongs to Arcana unless Ashfall makes it geological."],
  ["echoashfallprotocol:contaminated_redstone", "echoashfallprotocol:charged_ash_circuit", "blocked-rename", "Vanilla public identity leak."],
  ["echoashfallprotocol:contaminated_lapis", "echoashfallprotocol:blue_ash_salt", "blocked-rename", "Vanilla public identity leak."]
];

const aliases = migrationRows.map(([from, to, action, reason]) => ({ from, to, action, reason }));

const legalIdentity = {
  schema: "echo.foundation.legal_identity.v1",
  blockedPublicTerms: [
    "minecraft",
    "vanilla",
    "workbench",
    "chest",
    "torch",
    "copper",
    "iron",
    "redstone",
    "lapis",
    "diamond",
    "emerald",
    "nether",
    "ender",
    "enchanting_table",
    "lectern",
    "amethyst",
    "shulker"
  ],
  watchTerms: [
    "pick",
    "axe",
    "shovel",
    "sword",
    "ore",
    "ingot",
    "furnace",
    "anvil",
    "map",
    "beacon"
  ],
  requiredRules: [
    "Public display names must use ECHO fantasy names, not vanilla-adjacent names.",
    "Old IDs may remain only through explicit save-safe aliases.",
    "Recipes and tags should point at foundation roles instead of old experience-local IDs.",
    "Experience modules may add fantasy-specific variants but must not re-own baseline survival primitives."
  ]
};

const materials = {
  blocks: [
    ["fieldstone", "stone", "foundation:stone/basic", "Openlands generic"],
    ["clay", "earth", "foundation:earth/clay", "Openlands generic"],
    ["gravel", "earth", "foundation:earth/gravel", "Openlands generic"],
    ["sand", "earth", "foundation:earth/sand", "Openlands generic"],
    ["branchwood_log", "wood", "foundation:wood/log", "Openlands generic"],
    ["branchwood_planks", "wood", "foundation:wood/planks", "Openlands generic"],
    ["branchwood_beam", "wood", "foundation:wood/beam", "Openlands generic"],
    ["branchwood_post", "wood", "foundation:wood/post", "Openlands generic"],
    ["wooden_slab", "wood", "foundation:wood/slab", "Openlands generic"],
    ["wooden_stairs", "wood", "foundation:wood/stairs", "Openlands generic"],
    ["wooden_fence", "wood", "foundation:wood/fence", "Openlands generic"],
    ["wooden_door", "wood", "foundation:wood/door", "Openlands generic"],
    ["wooden_trapdoor", "wood", "foundation:wood/trapdoor", "Openlands generic"],
    ["ladder", "wood", "foundation:utility/ladder", "Openlands generic"],
    ["cupral_vein", "ore", "foundation:metal/cupral/vein", "Renamed from copper_ore"],
    ["tinveil_vein", "ore", "foundation:metal/tinveil/vein", "Renamed from tin_ore"],
    ["ferrite_vein", "ore", "foundation:metal/ferrite/vein", "Renamed from iron_ore"]
  ],
  items: [
    ["branchwood_stick", "wood", "foundation:stick", "Openlands generic"],
    ["fieldstone_piece", "stone", "foundation:stone_piece", "Openlands generic"],
    ["reed_fiber", "fiber", "foundation:fiber", "Openlands generic"],
    ["fiber_binding", "fiber", "foundation:binding", "Openlands generic"],
    ["flint_shard", "stone", "foundation:sharp_stone", "Openlands generic"],
    ["clay_lump", "earth", "foundation:clay/raw", "Renamed from raw_clay"],
    ["brick", "ceramic", "foundation:ceramic/brick", "Openlands generic"],
    ["glass_pane", "glass", "foundation:glass/pane", "Openlands generic"],
    ["hide_strip", "creature", "foundation:hide", "Renamed from hide"],
    ["bone_shard", "creature", "foundation:bone", "Renamed from bone"],
    ["pitch_resin", "resin", "foundation:pitch", "Renamed from pitch"],
    ["resin", "resin", "foundation:resin", "Openlands generic"],
    ["charcoal_lump", "fuel", "foundation:fuel/charcoal", "Renamed from charcoal"],
    ["cupral_chunk", "metal", "foundation:metal/cupral/raw", "Renamed from copper_ore_chunk"],
    ["cupral_bar", "metal", "foundation:metal/cupral/bar", "Renamed from copper_ingot"],
    ["tinveil_chunk", "metal", "foundation:metal/tinveil/raw", "Renamed from tin_ore_chunk"],
    ["tinveil_bar", "metal", "foundation:metal/tinveil/bar", "Renamed from tin_ingot"],
    ["bronze_cast", "metal", "foundation:metal/bronze/bar", "Renamed from bronze_ingot"],
    ["ferrite_chunk", "metal", "foundation:metal/ferrite/raw", "Renamed from iron_ore_chunk"],
    ["ferrite_bar", "metal", "foundation:metal/ferrite/bar", "Renamed from iron_ingot"]
  ]
};

const toolCatalog = [
  ["crude_cutter", "cutter", 0, ["foundation:stick", "foundation:sharp_stone", "foundation:binding"], "crude_axe"],
  ["crude_breaker", "breaker", 0, ["foundation:stick", "foundation:stone_piece", "foundation:binding"], "crude_pick"],
  ["crude_digger", "digger", 0, ["foundation:stick", "foundation:stone_piece", "foundation:binding"], "crude_spade"],
  ["flint_knife", "knife", 0, ["foundation:sharp_stone", "foundation:binding"], "flint_knife"],
  ["field_hammer", "hammer", 1, ["foundation:stick", "foundation:stone_piece", "foundation:binding"], "wooden_hammer"],
  ["cupral_breaker", "breaker", 1, ["foundation:metal/cupral/bar", "foundation:stick"], "copper_pick"],
  ["bronze_breaker", "breaker", 2, ["foundation:metal/bronze/bar", "foundation:stick"], "bronze_pick"],
  ["ferrite_breaker", "breaker", 3, ["foundation:metal/ferrite/bar", "foundation:stick"], "iron_pick"]
];

const stationCatalog = [
  ["handcrafting", "crafting_surface", "inventory", "No block placement; baseline emergency recipes."],
  ["field_bench", "crafting_surface", "placed", "Renamed from workbench; shared early recipe surface."],
  ["field_crate", "storage", "placed", "Renamed from chest; shared small storage."],
  ["kiln", "heat_processing", "placed", "Shared ceramic/glass/charcoal processing."],
  ["forge_hearth", "metal_processing", "placed", "Renamed from forge; shared primitive metal processing."],
  ["loom", "fiber_processing", "placed", "Shared fiber and cloth surface."],
  ["cookpot", "food_processing", "placed", "Shared meal surface."],
  ["mason_table", "stone_processing", "placed", "Shared block refinement surface."]
];

const firstHourSteps = [
  ["spawn_safety_check", 0, "Validate landing area, nearby hazard envelope, and starter resource radius."],
  ["collect_loose_materials", 1, "Collect branchwood, fieldstone pieces, fiber, and flint."],
  ["make_first_tool", 2, "Craft crude cutter/breaker/digger using tool role contracts."],
  ["make_pitchlight", 3, "Craft early light from pitch resin and branchwood stick."],
  ["make_field_bench", 4, "Place field bench as the first shared station."],
  ["make_shelter_marker", 5, "Build a minimal shelter footprint and calculate starter shelter score."],
  ["claim_starter_cache", 6, "Award starter cache only after shelter and station checks pass."]
];

const lootPools = [
  ["starter_cache", "starter", ["branchwood_stick", "fieldstone_piece", "reed_fiber", "pitch_resin", "flint_shard"], "First earned Foundation cache."],
  ["traveler_pack", "starter", ["fiber_binding", "hide_strip", "bone_shard", "charcoal_lump"], "Small generic supply pack."],
  ["ruined_storage", "ruin", ["brick", "glass_pane", "fieldstone_piece", "reed_fiber"], "Renamed from ruined_well_cache."],
  ["material_scrap", "salvage", ["cupral_chunk", "tinveil_chunk", "ferrite_chunk", "charcoal_lump"], "Generic metal/material salvage pool."],
  ["generic_block_drops", "block_drop", ["clay_lump", "fieldstone_piece", "flint_shard"], "Baseline block drop table hook."],
  ["generic_ore_chunk_drops", "block_drop", ["cupral_chunk", "tinveil_chunk", "ferrite_chunk"], "Baseline ore vein drop hook."]
];

const creatureRoles = [
  ["passive_small", "low", "forage", "Small low-threat ambient animals."],
  ["passive_large", "low", "graze", "Large low-threat animals used for presence and resources."],
  ["neutral_forager", "medium", "forage", "Creatures that ignore players until disturbed."],
  ["territorial_medium", "medium", "defend_area", "Mid-size animals that guard an area or resource."],
  ["hostile_small", "medium", "attack", "Small direct threats."],
  ["hostile_large", "high", "attack", "Large direct threats and regional pressure."],
  ["aquatic_passive", "low", "swim", "Passive water creatures."],
  ["night_stalker", "high", "hunt_at_night", "Night pressure role shared by survival experiences."]
];

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function writeText(relativePath, content) {
  const filePath = path.join(root, relativePath);
  ensureDir(filePath);
  fs.writeFileSync(filePath, `${content.trimEnd()}\n`, "utf8");
}

function writeJson(relativePath, value) {
  writeText(relativePath, JSON.stringify(value, null, 2));
}

function packageName(moduleId) {
  return `com.knoxhack.${moduleId}`;
}

function moduleBuildGradle(module) {
  const foundationDeps = module.requires
    .filter((id) => id.startsWith("echo") && foundationIds.includes(id) && id !== module.id)
    .map((id) => `    implementation project(':${id}')`)
    .join("\n");

  return `
plugins {
    id 'java'
    id 'net.neoforged.moddev' version '2.0.141'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = 'https://maven.neoforged.net/releases' }
}

neoForge {
    version = project.findProperty('neoForgeVersion') ?: '21.1.194'
}

def compileOnlyIfIncluded = { String projectPath ->
    if (project.findProject(projectPath) != null) {
        dependencies.add('compileOnly', project(projectPath))
    }
}

dependencies {
    implementation project(':echocore')
    implementation project(':echonetcore')
${foundationDeps}
    compileOnly project(':echo-native-contracts')
    compileOnlyIfIncluded(':echoadaptercore')
}

processResources {
    def props = [
        minecraft_version: project.findProperty('minecraftVersion') ?: '1.21.1',
        minecraft_version_range: project.findProperty('minecraftVersionRange') ?: '[1.21.1,1.22)',
        neo_version: project.findProperty('neoForgeVersion') ?: '21.1.194',
        neo_version_range: project.findProperty('neoForgeVersionRange') ?: '[21.1,)',
        loader_version_range: project.findProperty('loaderVersionRange') ?: '[4,)',
        mod_id: mod_id,
        mod_name: mod_name,
        mod_license: mod_license,
        mod_version: mod_version,
        mod_group_id: mod_group_id,
        mod_authors: mod_authors,
        mod_description: mod_description
    ]
    inputs.properties props
    filesMatching('META-INF/neoforge.mods.toml') {
        expand props
    }
}
`;
}

function moduleGradleProperties(module) {
  return `
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=false
mod_id=${module.id}
mod_name=${module.name}
mod_license=All Rights Reserved
mod_version=0.1.0
mod_group_id=com.knoxhack.${module.id}
mod_authors=KnoxHack
mod_description=${module.summary}
`;
}

function moduleJava(module) {
  const pkg = packageName(module.id);
  const provides = module.provides.map((entry) => `"${entry}"`).join(", ");
  const requires = module.requires.map((entry) => `"${entry}"`).join(", ");
  return `
package ${pkg};

import java.util.List;

/**
 * Lightweight runtime marker for ${module.name}.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class ${module.className} {
    public static final String MODID = "${module.id}";
    public static final List<String> REQUIRES = List.of(${requires});
    public static final List<String> PROVIDES = List.of(${provides});

    public ${module.className}() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("${module.name} online: " + String.join(", ", PROVIDES));
    }
}
`;
}

function moduleNeoForgeToml(module) {
  return `
modLoader="javafml"
loaderVersion="\${loader_version_range}"
license="\${mod_license}"

[[mods]]
modId="\${mod_id}"
version="\${mod_version}"
displayName="\${mod_name}"
authors="\${mod_authors}"
description='''\${mod_description}'''

[[dependencies.\${mod_id}]]
modId="neoforge"
type="required"
versionRange="\${neo_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.\${mod_id}]]
modId="minecraft"
type="required"
versionRange="\${minecraft_version_range}"
ordering="NONE"
side="BOTH"
`;
}

function moduleManifest(module) {
  return {
    schema: "echo.mod.v1",
    id: module.id,
    name: module.name,
    version: "0.1.0",
    type: "addon",
    kind: "library",
    role: module.role,
    entrypoint: `${packageName(module.id)}.${module.className}`,
    publisher: "KnoxHack",
    channel: "foundation-dev",
    official: true,
    trustLevel: "official",
    standalone: true,
    clientOnly: false,
    serverOnly: false,
    side: "common",
    summary: module.summary,
    requires: module.requires,
    optional: [],
    provides: module.provides,
    consumes: module.consumes,
    gameModes: [
      "foundation_dev",
      "openlands_standard",
      "ashfall_survival",
      "arcana_division"
    ],
    permissions: [
      "registry:foundation",
      "data:foundation",
      "launcher:dependency"
    ],
    assets: [],
    transforms: [],
    access: {
      adapterCore: {
        domains: [
          "data",
          "diagnostics",
          "items",
          "loot",
          "progression",
          "recipes",
          "saves",
          "worldgen"
        ],
        runtimes: [
          "neoforge",
          "echo_native",
          "echo_runtime_standalone"
        ]
      },
      nativeClasspath: [],
      requiresConfirmationForWriteActions: false,
      notes: "ECHO Foundations data-first backbone module. Experience packs consume these contracts and must not re-own baseline survival content."
    },
    apiStability: "alpha",
    dependencyPolicy: {
      experiencesDependOnFoundation: true,
      foundationModulesMayDependOnEarlierFoundationModules: true,
      experiencesMayDependOnEachOther: false
    },
    ai: {
      readable: true,
      callableActions: false,
      requiresHumanReview: false,
      recommendedAgentLanes: [
        "metadata_agent",
        "validation_agent",
        "release_agent"
      ]
    },
    deprecatedFeatures: [],
    replacements: [],
    conflicts: []
  };
}

function writeModule(module) {
  const base = `addons/${module.id}`;
  writeText(`${base}/build.gradle`, moduleBuildGradle(module));
  writeText(`${base}/gradle.properties`, moduleGradleProperties(module));
  writeText(`${base}/README.md`, `
# ${module.name}

${module.summary}

This module is part of the ECHO Foundations backbone. Experience modules may
consume the contracts exposed here, but this module must not consume Openlands,
Ashfall, Arcana Division, or any future experience pack.
`);
  writeText(`${base}/src/main/java/${packageName(module.id).replaceAll(".", "/")}/${module.className}.java`, moduleJava(module));
  writeJson(`${base}/src/main/resources/META-INF/echo.mod.json`, moduleManifest(module));
  writeText(`${base}/src/main/templates/META-INF/neoforge.mods.toml`, moduleNeoForgeToml(module));
  writeJson(`${base}/src/main/resources/pack.mcmeta`, {
    pack: {
      description: module.name,
      pack_format: 15
    }
  });
}

function writeFoundationCoreData() {
  writeJson("addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/module_contracts.json", {
    schema: "echo.foundation.module_contracts.v1",
    rule: "Foundations owns baseline survival; experiences own only their unique fantasy, systems, and pressure.",
    dependencyRule: "Experiences depend on Foundations. Experiences never depend on each other.",
    modules: foundationModules.map(({ id, role, requires, provides, consumes, summary }) => ({ id, role, requires, provides, consumes, summary })),
    experiencePromises: {
      echoopenlandsprotocol: "Calm exploration, homesteading, old roads, and restored waystones.",
      echoashfallprotocol: "Harsh volcanic survival focused on shelter, filtration, heat control, and scarcity.",
      echoarcanadivisionprotocol: "Magical research, rituals, familiars, curses, rifts, and anomaly containment."
    }
  });
  writeJson("addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/ownership_audit.json", ownershipAudit);
  writeJson("addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/id_aliases.json", {
    schema: "echo.foundation.id_aliases.v1",
    aliases
  });
  writeJson("addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/legal_identity.json", legalIdentity);
  writeJson("addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/release_channels.json", {
    schema: "echo.foundation.release_channels.v1",
    channels: [
      "foundation-dev",
      "foundation-stable",
      "openlands-alpha",
      "ashfall-alpha",
      "arcana-alpha"
    ],
    launcherRules: [
      "Installing any experience must install all seven Foundation modules.",
      "Launcher must validate Foundation modules before experience launch.",
      "Repair must install or re-enable missing Foundation modules before launch.",
      "Foundation-stable is required before an experience can move from alpha to beta."
    ]
  });
}

function writeMaterialData() {
  writeJson("addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/material_catalog.json", {
    schema: "echo.foundation.material_catalog.v1",
    blocks: materials.blocks.map(([id, family, tag, source]) => ({ id, family, tag, source })),
    items: materials.items.map(([id, family, tag, source]) => ({ id, family, tag, source }))
  });
  writeJson("addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/material_tags.json", {
    schema: "echo.foundation.material_tags.v1",
    tags: {
      "foundation:wood": ["branchwood_log", "branchwood_planks", "branchwood_stick", "branchwood_beam", "branchwood_post"],
      "foundation:stone": ["fieldstone", "fieldstone_piece", "flint_shard"],
      "foundation:fiber": ["reed_fiber", "fiber_binding"],
      "foundation:earth": ["clay", "clay_lump", "gravel", "sand"],
      "foundation:fuel": ["charcoal_lump", "pitch_resin", "resin"],
      "foundation:metal/raw": ["cupral_chunk", "tinveil_chunk", "ferrite_chunk"],
      "foundation:metal/bar": ["cupral_bar", "tinveil_bar", "bronze_cast", "ferrite_bar"],
      "foundation:creature_material": ["hide_strip", "bone_shard"]
    }
  });
  writeJson("addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/metal_progression.json", {
    schema: "echo.foundation.metal_progression.v1",
    tiers: [
      { tier: 1, metal: "cupral", vein: "cupral_vein", raw: "cupral_chunk", bar: "cupral_bar" },
      { tier: 2, metal: "tinveil", vein: "tinveil_vein", raw: "tinveil_chunk", bar: "tinveil_bar" },
      { tier: 2, metal: "bronze", blend: ["cupral_bar", "tinveil_bar"], bar: "bronze_cast" },
      { tier: 3, metal: "ferrite", vein: "ferrite_vein", raw: "ferrite_chunk", bar: "ferrite_bar" }
    ]
  });
  writeJson("addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/migration_sources.json", {
    schema: "echo.foundation.material_migration_sources.v1",
    sourceExperience: "echoopenlandsprotocol",
    sourceIds: aliases.filter((alias) => alias.to.startsWith("echomaterialcore:"))
  });
}

function writeToolData() {
  writeJson("addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/tool_catalog.json", {
    schema: "echo.foundation.tool_catalog.v1",
    tools: toolCatalog.map(([id, role, tier, inputs, oldId]) => ({
      id,
      role,
      tier,
      inputs,
      oldOpenlandsId: `echoopenlandsprotocol:${oldId}`
    }))
  });
  writeJson("addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/tool_roles.json", {
    schema: "echo.foundation.tool_roles.v1",
    roles: {
      cutter: { replaces: ["axe"], verbs: ["cut", "strip", "harvest_wood"] },
      breaker: { replaces: ["pick"], verbs: ["break_stone", "mine_veins", "salvage"] },
      digger: { replaces: ["spade"], verbs: ["dig_soil", "shape_ground"] },
      knife: { replaces: [], verbs: ["cut_fiber", "prepare_hide", "small_harvest"] },
      hammer: { replaces: [], verbs: ["shape_blocks", "repair", "assemble"] }
    }
  });
  writeJson("addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/tool_progression.json", {
    schema: "echo.foundation.tool_progression.v1",
    order: [
      "crude_cutter",
      "crude_breaker",
      "crude_digger",
      "flint_knife",
      "field_hammer",
      "cupral_breaker",
      "bronze_breaker",
      "ferrite_breaker"
    ],
    rule: "Experiences may add fantasy tools, but baseline harvesting roles resolve through these Foundation roles."
  });
}

function writeStationData() {
  writeJson("addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/station_catalog.json", {
    schema: "echo.foundation.station_catalog.v1",
    stations: stationCatalog.map(([id, role, placement, note]) => ({ id, role, placement, note }))
  });
  writeJson("addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/station_tags.json", {
    schema: "echo.foundation.station_tags.v1",
    tags: {
      "foundation:crafting_surface": ["handcrafting", "field_bench"],
      "foundation:storage": ["field_crate"],
      "foundation:heat_processing": ["kiln", "forge_hearth", "cookpot"],
      "foundation:fiber_processing": ["loom"],
      "foundation:stone_processing": ["mason_table"]
    }
  });
  writeJson("addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/station_recipe_contracts.json", {
    schema: "echo.foundation.station_recipe_contracts.v1",
    contracts: [
      { station: "handcrafting", accepts: ["emergency_tools", "simple_materials"] },
      { station: "field_bench", accepts: ["baseline_tools", "baseline_blocks", "simple_stations"] },
      { station: "kiln", accepts: ["ceramics", "glass", "charcoal"] },
      { station: "forge_hearth", accepts: ["raw_metal_to_bar", "metal_tool_heads"] },
      { station: "loom", accepts: ["bindings", "cloth", "packs"] },
      { station: "cookpot", accepts: ["simple_meals"] },
      { station: "mason_table", accepts: ["stone_blocks", "road_materials"] }
    ]
  });
}

function writeStarterData() {
  writeJson("addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/first_hour_route.json", {
    schema: "echo.foundation.first_hour_route.v1",
    sourceMovedFrom: "echoopenlandsprotocol:openlands/progression/first_hour_route.json",
    steps: firstHourSteps.map(([id, order, goal]) => ({ id, order, goal })),
    experienceExtensionPoint: "Experiences append step 7+ with their own fantasy goal, such as Openlands first waystone or Ashfall first sealed shelter."
  });
  writeJson("addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/spawn_contract.json", {
    schema: "echo.foundation.spawn_contract.v1",
    starterRadiusBlocks: 96,
    requiredNearbyTags: ["foundation:wood", "foundation:stone", "foundation:fiber"],
    blockedImmediateHazards: ["lava", "void_drop", "unavoidable_hostile_cluster", "instant_storm_exposure"],
    shelterScore: {
      minimumForStarterCache: 3,
      roof: 1,
      wallsOrNaturalCover: 1,
      light: 1,
      bedroll: 1,
      temperatureOrExposureMitigation: 1
    }
  });
  writeJson("addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/starter_items.json", {
    schema: "echo.foundation.starter_items.v1",
    items: [
      { id: "campfire", role: "warmth_and_cooking", oldOpenlandsId: "echoopenlandsprotocol:campfire" },
      { id: "pitchlight", role: "early_light", oldOpenlandsId: "echoopenlandsprotocol:torch" },
      { id: "pitchlight_bundle", role: "early_light_bundle", oldOpenlandsId: "echoopenlandsprotocol:torch_bundle" },
      { id: "bedroll", role: "starter_rest", oldOpenlandsId: "echoopenlandsprotocol:bedroll" },
      { id: "bedroll_block", role: "placed_starter_rest", oldOpenlandsId: "echoopenlandsprotocol:bedroll_block" }
    ]
  });
  writeJson("addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/experience_hooks.json", {
    schema: "echo.foundation.starter_experience_hooks.v1",
    hooks: {
      echoopenlandsprotocol: "Append first_waystone after Foundation step 6.",
      echoashfallprotocol: "Append first_filtration_and_shelter after Foundation step 6.",
      echoarcanadivisionprotocol: "Append first_containment_sigil after Foundation step 6."
    }
  });
}

function writeLootData() {
  writeJson("addons/echocommonloot/src/main/resources/data/echocommonloot/foundation/loot/loot_pool_catalog.json", {
    schema: "echo.foundation.loot_pool_catalog.v1",
    pools: lootPools.map(([id, family, entries, note]) => ({ id, family, entries, note }))
  });
  writeJson("addons/echocommonloot/src/main/resources/data/echocommonloot/foundation/loot/loot_tags.json", {
    schema: "echo.foundation.loot_tags.v1",
    tags: {
      "foundation:starter_cache": ["starter_cache", "traveler_pack"],
      "foundation:ruin_generic": ["ruined_storage", "material_scrap"],
      "foundation:block_drops": ["generic_block_drops", "generic_ore_chunk_drops"]
    }
  });
  writeJson("addons/echocommonloot/src/main/resources/data/echocommonloot/foundation/loot/experience_extension_rules.json", {
    schema: "echo.foundation.loot_extension_rules.v1",
    rules: [
      "Experience loot may reference Foundation pools as parents.",
      "Experience loot may add fantasy rewards but must not duplicate baseline materials under local ownership.",
      "Starter caches must remain small enough to preserve the first-hour loop."
    ]
  });
}

function writeCreatureData() {
  writeJson("addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation/creatures/creature_role_catalog.json", {
    schema: "echo.foundation.creature_role_catalog.v1",
    roles: creatureRoles.map(([id, pressure, behavior, note]) => ({ id, pressure, behavior, note }))
  });
  writeJson("addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation/creatures/spawn_category_contracts.json", {
    schema: "echo.foundation.spawn_category_contracts.v1",
    categories: {
      ambient: ["passive_small", "passive_large", "aquatic_passive"],
      pressure: ["neutral_forager", "territorial_medium"],
      hostile: ["hostile_small", "hostile_large", "night_stalker"]
    }
  });
  writeJson("addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation/creatures/experience_mapping_rules.json", {
    schema: "echo.foundation.creature_mapping_rules.v1",
    openlandsExamples: {
      hare: "passive_small",
      deer: "passive_large",
      boar: "territorial_medium",
      fish: "aquatic_passive",
      hollow_stalker: "night_stalker"
    },
    rule: "Experience mobs keep their fantasy names and models, but pressure and spawn behavior map through Foundation roles."
  });
}

function writeDocs() {
  writeText("docs/echo-foundations-architecture.md", `
# ECHO Foundations Architecture

## Master Goal

Create ECHO Foundations as the shared survival/content backbone, then refactor
Openlands, Ashfall, and Arcana Division so each experience owns only its unique
fantasy, systems, and pressure.

## Locked Rule

Foundations owns baseline survival. Experiences consume Foundation contracts and
may extend them, but they do not re-own baseline materials, tools, starter
stations, starter loot, spawn safety, first-hour survival, or shared creature
roles.

## Dependency Rule

Experience modules depend on Foundation modules. Experience modules never depend
on each other.

~~~mermaid
flowchart LR
  F["ECHO Foundations"] --> O["Openlands"]
  F --> A["Ashfall"]
  F --> C["Arcana Division"]
  O -. forbidden .- A
  A -. forbidden .- C
  C -. forbidden .- O
~~~

## Locked Foundation Modules

| Module | Owns |
| --- | --- |
| echofoundationcore | Ownership rules, aliases, legal identity, release/dependency contracts |
| echomaterialcore | Generic materials, generic blocks, material tags, metal progression |
| echotoolcore | Generic tools, tool roles, shared tool progression |
| echostationcore | Generic stations, storage, shared recipe surfaces |
| echoworldstarter | Spawn safety, starter route, shelter score, first-hour items |
| echocommonloot | Generic loot pools, starter caches, block drops |
| echocreatureroles | Shared creature pressure/spawn role taxonomy |

## Experience Ownership

Openlands owns calm exploration, homesteading, old roads, waystones, map table,
regional rubbings, route bindings, and Openlands biomes.

Ashfall owns volcanic survival pressure: storms, heat, ash exposure, scarcity,
shelters, filtration, atmospheric scrubbers, distillation, black rain, and
Ashfall-specific hazards.

Arcana Division owns magical research, rituals, familiars, curses, rifts,
anomaly containment, Arcana stations, Arcana creatures, and Arcana loot rules.

## Save Migration

Old IDs do not disappear abruptly. Foundation uses explicit aliases from the
migration table. New recipes, tags, display names, docs, and UI text must point
at the new Foundation IDs.

## Launcher Contract

The launcher must install all seven Foundation modules with any experience. It
must validate dependency presence before launch and repair missing Foundation
modules before attempting to load an experience pack.
`);

  writeText("docs/content-ownership-audit.md", `
# Content Ownership Audit

This is the coding-ready ownership split for the Foundations refactor.

## Openlands Extraction Source

Openlands is frozen as the extraction source for baseline survival. Its MVP
registry currently contains baseline materials, tools, stations, loot,
first-hour progression, and creature pressure categories. Those contracts now
belong to Foundation modules.

## Foundation-Owned Content

echomaterialcore owns branchwood, fieldstone, reed fiber, flint, clay, glass,
hide, bone, pitch/resin, charcoal, cupral, tinveil, bronze cast, ferrite, and
generic wood construction blocks.

echotoolcore owns crude cutter, crude breaker, crude digger, flint knife, field
hammer, cupral breaker, bronze breaker, and ferrite breaker.

echostationcore owns handcrafting, field bench, field crate, kiln, forge hearth,
loom, cookpot, and mason table.

echoworldstarter owns campfire, pitchlight, pitchlight bundle, bedroll,
bedroll block, spawn safety, starter shelter score, and first-hour steps 1-6.

echocommonloot owns starter cache, traveler pack, ruined storage, material
scrap, generic block drops, and generic ore chunk drops.

echocreatureroles owns passive_small, passive_large, neutral_forager,
territorial_medium, hostile_small, hostile_large, aquatic_passive, and
night_stalker.

## Openlands-Only Content

Openlands keeps meadows, woodlands, stonehills, marshlands, pine construction,
thatch, old roads, waystones, map table, region rubbing, old road token, route
binding, glow crystal, homestead food, small pack, repair kit, copper fitting,
and Openlands creature identities.

## Ashfall Audit

Ashfall keeps storms, heat, ash exposure, scarcity, shelters, filtration,
atmospheric scrubbers, distillation, black rain, ash soil, scoria, basalt,
sulfur, and emberglass.

Ashfall duplicated baseline survival through animal_bone, animal_hide,
plant_fiber, fiber_rope, bone_knife, scrap_knife, and crude_spear. These should
resolve through Foundation tags or explicit Ashfall fantasy variants.

Ashfall rename blockers are map_table, riftstone, contaminated_redstone, and
contaminated_lapis.

## Arcana Split

Arcana modules currently referenced from the Ashfall product profile are
echoaetherworks, echoarcanacore, echoarcaneindex, echocursecore,
echofamiliarcore, echogrimoire, echoriftworlds, echoritualcore, and
echospellcore. Arcana Division becomes the owning experience protocol for these
surfaces.
`);

  const table = [
    "# ID Migration Table",
    "",
    "| Old ID | New ID | Action | Reason |",
    "| --- | --- | --- | --- |",
    ...migrationRows.map(([from, to, action, reason]) => `| ${from} | ${to} | ${action} | ${reason} |`)
  ];
  writeText("docs/id-migration-table.md", table.join("\n"));
}

function writeValidationScript() {
  writeText("scripts/validate-foundations-split.mjs", `
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const strict = process.argv.includes("--strict");
const foundationModules = ${JSON.stringify(foundationIds, null, 2)};
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
    errors.push(\`Missing required file: \${relativePath}\`);
  }
}

function checkManifest(moduleId) {
  const manifestPath = \`addons/\${moduleId}/src/main/resources/META-INF/echo.mod.json\`;
  requireFile(manifestPath);
  if (!exists(manifestPath)) return null;
  const manifest = readJson(manifestPath);
  if (manifest.id !== moduleId) {
    errors.push(\`\${moduleId} manifest id mismatch: \${manifest.id}\`);
  }
  return manifest;
}

const settings = fs.readFileSync(path.join(root, "settings.gradle"), "utf8");
for (const moduleId of foundationModules) {
  if (!settings.includes(\`'\${moduleId}'\`)) {
    errors.push(\`settings.gradle does not include \${moduleId}\`);
  }
}

for (const moduleId of foundationModules) {
  requireFile(\`addons/\${moduleId}/build.gradle\`);
  requireFile(\`addons/\${moduleId}/gradle.properties\`);
  requireFile(\`addons/\${moduleId}/README.md\`);
  const manifest = checkManifest(moduleId);
  if (manifest) {
    for (const required of ["echocore", "echonetcore"]) {
      if (!manifest.requires?.includes(required)) {
        errors.push(\`\${moduleId} does not require \${required}\`);
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
    "foundation/materials/migration_sources.json"
  ],
  echotoolcore: [
    "foundation/tools/tool_catalog.json",
    "foundation/tools/tool_roles.json",
    "foundation/tools/tool_progression.json"
  ],
  echostationcore: [
    "foundation/stations/station_catalog.json",
    "foundation/stations/station_tags.json",
    "foundation/stations/station_recipe_contracts.json"
  ],
  echoworldstarter: [
    "foundation/starter/first_hour_route.json",
    "foundation/starter/spawn_contract.json",
    "foundation/starter/starter_items.json",
    "foundation/starter/experience_hooks.json"
  ],
  echocommonloot: [
    "foundation/loot/loot_pool_catalog.json",
    "foundation/loot/loot_tags.json",
    "foundation/loot/experience_extension_rules.json"
  ],
  echocreatureroles: [
    "foundation/creatures/creature_role_catalog.json",
    "foundation/creatures/spawn_category_contracts.json",
    "foundation/creatures/experience_mapping_rules.json"
  ]
};

for (const [moduleId, files] of Object.entries(requiredData)) {
  for (const file of files) {
    requireFile(\`addons/\${moduleId}/src/main/resources/data/\${moduleId}/\${file}\`);
  }
}

for (const experience of ["echoopenlandsprotocol", "echoashfallprotocol"]) {
  const manifest = checkManifest(experience);
  if (!manifest) continue;
  for (const foundation of foundationModules) {
    if (!manifest.requires?.includes(foundation)) {
      errors.push(\`\${experience} does not require \${foundation}\`);
    }
  }
  for (const required of manifest.requires ?? []) {
    if (forbiddenExperienceDeps.has(required) && required !== experience) {
      errors.push(\`\${experience} illegally depends on experience module \${required}\`);
    }
  }
}

if (exists("addons/echoarcanadivisionprotocol/src/main/resources/META-INF/echo.mod.json")) {
  const manifest = checkManifest("echoarcanadivisionprotocol");
  for (const foundation of foundationModules) {
    if (!manifest.requires?.includes(foundation)) {
      errors.push(\`echoarcanadivisionprotocol does not require \${foundation}\`);
    }
  }
} else {
  warnings.push("Arcana Division protocol shell is not present yet.");
}

const openlandsRegistryPath = "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json";
if (exists(openlandsRegistryPath)) {
  const registryText = fs.readFileSync(path.join(root, openlandsRegistryPath), "utf8");
  const bridgePath = "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands/foundation/foundation_alias_bridge.json";
  const openlandsBridgeAliases = exists(bridgePath)
    ? new Set((readJson(bridgePath).aliases ?? []).map((alias) => alias.legacyId?.replace("echoopenlandsprotocol:", "")))
    : new Set();
  for (const leaked of ["workbench", "chest", "torch", "copper_ingot", "iron_ingot", "crude_pick"]) {
    if (registryText.includes(\`"\${leaked}"\`) && !openlandsBridgeAliases.has(leaked)) {
      warnings.push(\`Openlands registry still carries Foundation-owned legacy id \${leaked}; alias is defined, destructive data removal remains pending.\`);
    }
  }
}

const ashfallProfile = "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallBootstrapProductProfile.java";
if (exists(ashfallProfile)) {
  const text = fs.readFileSync(path.join(root, ashfallProfile), "utf8");
  const arcanaBridgePath = "addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/registries/arcana_division_bridge.json";
  const movedArcanaModules = exists(arcanaBridgePath)
    ? new Set(readJson(arcanaBridgePath).movedModules ?? [])
    : new Set();
  for (const arcana of ["echoaetherworks", "echoarcanacore", "echospellcore", "echoritualcore", "echoriftworlds"]) {
    if (text.includes(arcana) && !movedArcanaModules.has(arcana)) {
      warnings.push(\`Ashfall profile still references Arcana module \${arcana}; move route into Arcana Division when the profile is split.\`);
    }
  }
  const foundationBridgePath = "addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/registries/foundation_alias_bridge.json";
  const ashfallRenameAliases = exists(foundationBridgePath)
    ? new Set((readJson(foundationBridgePath).ashfallRenames ?? []).map((alias) => alias.legacyId?.replace("echoashfallprotocol:", "")))
    : new Set();
  for (const blocked of ["contaminated_redstone", "contaminated_lapis", "map_table", "riftstone"]) {
    if (text.includes(blocked) && !ashfallRenameAliases.has(blocked)) {
      warnings.push(\`Ashfall still contains rename-needed id \${blocked}.\`);
    }
  }
}

if (strict && warnings.length > 0) {
  errors.push(...warnings.map((warning) => \`STRICT: \${warning}\`));
}

if (warnings.length > 0) {
  console.log("Foundation split warnings:");
  for (const warning of warnings) console.log(\`- \${warning}\`);
}

if (errors.length > 0) {
  console.error("Foundation split validation failed:");
  for (const error of errors) console.error(\`- \${error}\`);
  process.exit(1);
}

console.log(\`Foundation split validation passed with \${warnings.length} warning(s).\`);
`);
}

function main() {
  for (const module of foundationModules) {
    writeModule(module);
  }
  writeFoundationCoreData();
  writeMaterialData();
  writeToolData();
  writeStationData();
  writeStarterData();
  writeLootData();
  writeCreatureData();
  writeDocs();
  writeValidationScript();
}

main();
