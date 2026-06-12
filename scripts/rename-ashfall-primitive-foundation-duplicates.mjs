import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const ashfallSrc = path.join(root, "addons/echoashfallprotocol/src");
const excludedFiles = new Set([
  path.normalize(path.join(ashfallSrc, "main/resources/data/echoashfallprotocol/registries/foundation_alias_bridge.json")),
  path.normalize(path.join(ashfallSrc, "main/resources/data/echoashfallprotocol/registries/arcana_division_bridge.json"))
]);

const preserved = [
  ["craft_bone_knife", "%%ASHFALL_LEGACY_MISSION_A%%"],
  ["craft_crude_spear", "%%ASHFALL_LEGACY_MISSION_B%%"]
];

const replacements = [
  ["ANIMAL_BONE", "ASHBONE_SHARD"],
  ["ANIMAL_HIDE", "SCORCHED_HIDE_STRIP"],
  ["PLANT_FIBER", "ASHGRASS_FIBER"],
  ["FIBER_ROPE", "SOOTCORD_BINDING"],
  ["BONE_KNIFE", "ASHBONE_SHIV"],
  ["CRUDE_SPEAR", "SCAVENGER_SPEAR"],
  ["Animal Bone", "Ashbone Shard"],
  ["Animal Hide", "Scorched Hide Strip"],
  ["Plant Fiber", "Ashgrass Fiber"],
  ["Fiber Rope", "Sootcord Binding"],
  ["Bone Knife", "Ashbone Shiv"],
  ["Crude Spear", "Scavenger Spear"],
  ["animal_bone", "ashbone_shard"],
  ["animal_hide", "scorched_hide_strip"],
  ["plant_fiber", "ashgrass_fiber"],
  ["fiber_rope", "sootcord_binding"],
  ["bone_knife", "ashbone_shiv"],
  ["crude_spear", "scavenger_spear"],
  ["BoneKnifeItem", "AshboneShivItem"],
  ["CrudeSpearItem", "ScavengerSpearItem"],
  ["boneKnife", "ashboneShiv"],
  ["crudeSpear", "scavengerSpear"],
  ["Bone knife", "Ashbone shiv"],
  ["Crude spear", "Scavenger spear"],
  ["bone knife", "ashbone shiv"],
  ["crude spear", "scavenger spear"]
];

function listFiles(dir) {
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...listFiles(full));
    } else if (entry.isFile()) {
      files.push(full);
    }
  }
  return files;
}

function shouldEdit(file) {
  if (excludedFiles.has(path.normalize(file))) return false;
  return [".java", ".json", ".toml", ".mcmeta", ".txt", ".md", ".properties"].includes(path.extname(file));
}

function preserve(text) {
  let next = text;
  for (const [from, token] of preserved) {
    next = next.replaceAll(from, token);
  }
  return next;
}

function restore(text) {
  let next = text;
  for (const [from, token] of preserved) {
    next = next.replaceAll(token, from);
  }
  return next;
}

function replaceAll(text) {
  let next = preserve(text);
  for (const [from, to] of replacements) {
    next = next.replaceAll(from, to);
  }
  return restore(next);
}

function renamePathIfNeeded(file) {
  const dir = path.dirname(file);
  const oldName = path.basename(file);
  const newName = replaceAll(oldName);
  if (oldName === newName) return;
  const target = path.join(dir, newName);
  if (fs.existsSync(target)) {
    throw new Error(`Cannot rename ${file} to ${target}: target already exists`);
  }
  fs.renameSync(file, target);
}

for (const file of listFiles(ashfallSrc)) {
  if (!shouldEdit(file)) continue;
  const before = fs.readFileSync(file, "utf8");
  const after = replaceAll(before);
  if (after !== before) {
    fs.writeFileSync(file, after, "utf8");
  }
}

const filesAfterContentRewrite = listFiles(ashfallSrc)
  .filter((file) => !excludedFiles.has(path.normalize(file)))
  .sort((a, b) => b.length - a.length);
for (const file of filesAfterContentRewrite) {
  renamePathIfNeeded(file);
}
