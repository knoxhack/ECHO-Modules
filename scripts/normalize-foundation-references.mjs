import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");

const idMap = {
  fieldstone_bricks: "fieldstone_bricks",
  brick_block: "brick_block",
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
  copper_ore_chunk: "echomaterialcore:cupral_chunk",
  copper_ingot: "echomaterialcore:cupral_bar",
  tin_ore: "echomaterialcore:tinveil_vein",
  tin_ore_chunk: "echomaterialcore:tinveil_chunk",
  tin_ingot: "echomaterialcore:tinveil_bar",
  bronze_ingot: "echomaterialcore:bronze_cast",
  iron_ore: "echomaterialcore:ferrite_vein",
  iron_ore_chunk: "echomaterialcore:ferrite_chunk",
  iron_ingot: "echomaterialcore:ferrite_bar",
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
  crude_axe: "echotoolcore:crude_cutter",
  crude_pick: "echotoolcore:crude_breaker",
  crude_spade: "echotoolcore:crude_digger",
  flint_knife: "echotoolcore:flint_knife",
  wooden_hammer: "echotoolcore:field_hammer",
  copper_pick: "echotoolcore:cupral_breaker",
  bronze_pick: "echotoolcore:bronze_breaker",
  iron_pick: "echotoolcore:ferrite_breaker",
  handcrafting: "echostationcore:handcrafting",
  workbench: "echostationcore:field_bench",
  chest: "echostationcore:field_crate",
  kiln: "echostationcore:kiln",
  forge: "echostationcore:forge_hearth",
  loom: "echostationcore:loom",
  cookpot: "echostationcore:cookpot",
  mason_table: "echostationcore:mason_table",
  torch: "echoworldstarter:pitchlight",
  torch_bundle: "echoworldstarter:pitchlight_bundle",
  campfire: "echoworldstarter:campfire",
  bedroll: "echoworldstarter:bedroll",
  bedroll_block: "echoworldstarter:bedroll_block",
  copper_fitting: "echoopenlandsprotocol:cupral_fitting"
};

const recipeMap = {
  fiber_binding: "echomaterialcore:fiber_binding",
  branchwood_planks: "echomaterialcore:branchwood_planks",
  wooden_beam: "echomaterialcore:wooden_beam",
  wooden_post: "echomaterialcore:wooden_post",
  wooden_slab: "echomaterialcore:wooden_slab",
  wooden_stairs: "echomaterialcore:wooden_stairs",
  wooden_door: "echomaterialcore:wooden_door",
  wooden_trapdoor: "echomaterialcore:wooden_trapdoor",
  kiln_brick: "echomaterialcore:kiln_brick",
  kiln_glass: "echomaterialcore:kiln_glass",
  kiln_charcoal: "echomaterialcore:kiln_charcoal",
  forge_copper_ingot: "echomaterialcore:forge_cupral_bar",
  forge_tin_ingot: "echomaterialcore:forge_tinveil_bar",
  forge_bronze_ingot: "echomaterialcore:forge_bronze_cast",
  forge_iron_ingot: "echomaterialcore:forge_ferrite_bar",
  crude_axe: "echotoolcore:crude_cutter",
  crude_pick: "echotoolcore:crude_breaker",
  crude_spade: "echotoolcore:crude_digger",
  flint_knife: "echotoolcore:flint_knife",
  wooden_hammer: "echotoolcore:field_hammer",
  copper_pick: "echotoolcore:cupral_breaker",
  bronze_pick: "echotoolcore:bronze_breaker",
  iron_pick: "echotoolcore:ferrite_breaker",
  workbench: "echostationcore:field_bench",
  chest: "echostationcore:field_crate",
  kiln: "echostationcore:kiln",
  forge: "echostationcore:forge_hearth",
  torch_bundle: "echoworldstarter:pitchlight_bundle",
  campfire: "echoworldstarter:campfire",
  bedroll: "echoworldstarter:bedroll",
  copper_fitting: "echoopenlandsprotocol:cupral_fitting"
};

const tagMap = {
  "openlands:log": "foundation:wood/log",
  "openlands:planks": "foundation:wood/planks",
  "openlands:ore_chunk": "foundation:metal/raw",
  "openlands:ingot": "foundation:metal/bar",
  "openlands:tool": "foundation:tools",
  "openlands:pick": "foundation:tool_role/breaker"
};

const skipKeys = new Set([
  "legacyId",
  "legacyOpenlandsId",
  "oldOpenlandsId",
  "sourceMovedFrom",
  "sourceAliases",
  "source",
  "reason"
]);

const exactMap = { ...idMap, ...recipeMap, ...tagMap };
const protectedNamespacePrefixes = new Set([
  "echoopenlandsprotocol",
  "echomaterialcore",
  "echotoolcore",
  "echostationcore",
  "echoworldstarter",
  "echocommonloot",
  "echocreatureroles",
  "echoarcanadivisionprotocol"
]);

function listJsonFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...listJsonFiles(full));
    else if (entry.isFile() && entry.name.endsWith(".json")) files.push(full);
  }
  return files;
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function replaceEmbedded(value) {
  let next = value;
  const ordered = Object.entries(exactMap).sort((a, b) => b[0].length - a[0].length);
  for (const [from, to] of ordered) {
    next = next.replaceAll(`echoopenlandsprotocol:${from}`, to);
    next = next.replaceAll(
      `openlands:${from}`,
      to.startsWith("echoopenlandsprotocol:") ? `openlands:${to.split(":")[1]}` : to
    );
    next = next.replace(new RegExp(`${escapeRegex(from)}(?![A-Za-z0-9_])`, "g"), (match, offset, source) => {
      const before = source.slice(0, offset);
      const previous = source[offset - 1] ?? "";
      if (/[A-Za-z0-9_]/.test(previous)) return match;
      if (previous === ":") {
        const prefix = before.slice(0, -1).match(/[A-Za-z0-9_]+$/)?.[0] ?? "";
        if (protectedNamespacePrefixes.has(prefix)) return match;
      }
      return to;
    });
  }
  return next;
}

function rewriteValue(value, key = "") {
  if (skipKeys.has(key)) return value;
  if (Array.isArray(value)) return value.map((entry) => rewriteValue(entry));
  if (value && typeof value === "object") {
    const rewritten = {};
    for (const [entryKey, entryValue] of Object.entries(value)) {
      rewritten[entryKey] = rewriteValue(entryValue, entryKey);
    }
    return rewritten;
  }
  if (typeof value !== "string") return value;
  if (exactMap[value]) return exactMap[value];
  if (value.startsWith("echoopenlandsprotocol:")) {
    const local = value.slice("echoopenlandsprotocol:".length);
    if (exactMap[local]) return exactMap[local];
  }
  return replaceEmbedded(value);
}

function normalizeJsonFile(file) {
  if (file.endsWith(path.join("openlands", "foundation", "foundation_alias_bridge.json"))) return;
  const parsed = JSON.parse(fs.readFileSync(file, "utf8"));
  const normalized = rewriteValue(parsed);
  fs.writeFileSync(file, `${JSON.stringify(normalized, null, 2)}\n`, "utf8");
}

function ensureFoundationAlias() {
  const file = path.join(root, "addons/echofoundationcore/src/main/resources/data/echofoundationcore/foundation/contracts/id_aliases.json");
  const payload = JSON.parse(fs.readFileSync(file, "utf8"));
  payload.aliases ??= [];
  if (!payload.aliases.some((alias) => alias.from === "echoopenlandsprotocol:copper_fitting")) {
    payload.aliases.push({
      from: "echoopenlandsprotocol:copper_fitting",
      to: "echoopenlandsprotocol:cupral_fitting",
      action: "alias",
      reason: "Openlands waystone fitting renamed to avoid vanilla-adjacent copper public identity."
    });
  }
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

function main() {
  const targets = [
    path.join(root, "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands"),
    path.join(root, "addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation"),
    path.join(root, "addons/echotoolcore/src/main/resources/data/echotoolcore/foundation"),
    path.join(root, "addons/echostationcore/src/main/resources/data/echostationcore/foundation"),
    path.join(root, "addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation"),
    path.join(root, "addons/echocommonloot/src/main/resources/data/echocommonloot/foundation"),
    path.join(root, "addons/echocreatureroles/src/main/resources/data/echocreatureroles/foundation")
  ];
  for (const target of targets) {
    for (const file of listJsonFiles(target)) normalizeJsonFile(file);
  }
  ensureFoundationAlias();
}

main();
