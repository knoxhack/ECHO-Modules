import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const ashfallSrc = path.join(root, "addons/echoashfallprotocol/src");
const excludedFiles = new Set([
  path.normalize(path.join(ashfallSrc, "main/resources/data/echoashfallprotocol/registries/foundation_alias_bridge.json")),
  path.normalize(path.join(ashfallSrc, "main/resources/data/echoashfallprotocol/registries/arcana_division_bridge.json"))
]);

const replacements = [
  ["CONTAMINATED_REDSTONE", "CHARGED_ASH_CIRCUIT"],
  ["CONTAMINATED_LAPIS", "BLUE_ASH_SALT"],
  ["contaminated_redstone", "charged_ash_circuit"],
  ["contaminated_lapis", "blue_ash_salt"],
  ["Contaminated Redstone", "Charged Ash Circuit"],
  ["Contaminated Lapis", "Blue Ash Salt"],
  ["MAP_TABLE_POI", "SURVEY_TABLE_POI"],
  ["MAP_TABLE_ITEM", "SURVEY_TABLE_ITEM"],
  ["MAP_TABLE", "SURVEY_TABLE"],
  ["map_table_poi", "survey_table_poi"],
  ["map_table", "survey_table"],
  ["Map Table", "Survey Table"],
  ["map table", "survey table"],
  ["nexus_scar_riftstone_scatter", "nexus_scar_stone_scatter"],
  ["riftstone_scatter", "nexus_scar_stone_scatter"],
  ["RIFTSTONE_ITEM", "NEXUS_SCAR_STONE_ITEM"],
  ["RIFTSTONE", "NEXUS_SCAR_STONE"],
  ["riftstone", "nexus_scar_stone"],
  ["Riftstone", "Nexus Scar Stone"]
];

function listFiles(dir) {
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...listFiles(full));
    else if (entry.isFile()) files.push(full);
  }
  return files;
}

function shouldEdit(file) {
  if (excludedFiles.has(path.normalize(file))) return false;
  return [".java", ".json", ".toml", ".mcmeta", ".txt", ".md"].includes(path.extname(file));
}

function replaceAll(text) {
  let next = text;
  for (const [from, to] of replacements) {
    next = next.replaceAll(from, to);
  }
  return next;
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
  if (after !== before) fs.writeFileSync(file, after, "utf8");
}

const filesAfterContentRewrite = listFiles(ashfallSrc)
  .filter((file) => !excludedFiles.has(path.normalize(file)))
  .sort((a, b) => b.length - a.length);
for (const file of filesAfterContentRewrite) {
  renamePathIfNeeded(file);
}
