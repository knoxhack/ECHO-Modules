import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const openlandsDataRoot = path.join(root, "addons/echoopenlandsprotocol/src/main/resources/data/echoopenlandsprotocol/openlands");
const openlandsAssetRoot = path.join(root, "addons/echoopenlandsprotocol/src/main/resources/assets/echoopenlandsprotocol");

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function localId(id) {
  return id.includes(":") ? id.split(":")[1] : id;
}

function langKey(kind, namespacedId) {
  const [namespace, id] = namespacedId.includes(":") ? namespacedId.split(":") : ["echoopenlandsprotocol", namespacedId];
  return `${kind}.${namespace}.${id}`;
}

function titleFromId(id) {
  return localId(id).split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function collectMovedLang() {
  const specs = [
    {
      module: "echomaterialcore",
      file: "addons/echomaterialcore/src/main/resources/data/echomaterialcore/foundation/materials/moved_openlands_materials.json",
      blocks: "blocks",
      items: "items"
    },
    {
      module: "echotoolcore",
      file: "addons/echotoolcore/src/main/resources/data/echotoolcore/foundation/tools/moved_openlands_tools.json",
      items: "items"
    },
    {
      module: "echostationcore",
      file: "addons/echostationcore/src/main/resources/data/echostationcore/foundation/stations/moved_openlands_stations.json",
      blocks: "blocks"
    },
    {
      module: "echoworldstarter",
      file: "addons/echoworldstarter/src/main/resources/data/echoworldstarter/foundation/starter/moved_openlands_worldstarter.json",
      blocks: "blocks",
      items: "items"
    }
  ];

  for (const spec of specs) {
    const payload = readJson(path.join(root, spec.file));
    const lang = {};
    for (const block of payload[spec.blocks] ?? []) {
      const id = block.id.startsWith(`${spec.module}:`) ? block.id : `${spec.module}:${localId(block.id)}`;
      lang[langKey("block", id)] = block.displayName ?? titleFromId(id);
    }
    for (const item of payload[spec.items] ?? []) {
      const id = item.id.startsWith(`${spec.module}:`) ? item.id : `${spec.module}:${localId(item.id)}`;
      lang[langKey("item", id)] = item.displayName ?? titleFromId(id);
    }
    writeJson(path.join(root, `addons/${spec.module}/src/main/resources/assets/${spec.module}/lang/en_us.json`), lang);
  }
}

function syncOpenlandsAssets() {
  const blocks = readJson(path.join(openlandsDataRoot, "blocks/mvp_blocks.json")).blocks ?? [];
  const items = readJson(path.join(openlandsDataRoot, "items/mvp_items.json")).items ?? [];
  const blockIds = blocks.map((block) => localId(block.id));
  const itemIds = items.map((item) => localId(item.id));

  const manifestFile = path.join(openlandsAssetRoot, "asset_manifest.json");
  const manifest = readJson(manifestFile);
  manifest.status = "owned_openlands_coverage_after_foundation_split";
  manifest.mvpCoverage.blockIds = blockIds;
  manifest.mvpCoverage.itemIds = itemIds;
  manifest.foundationAssetDependencies = {
    echomaterialcore: "Generic materials, blocks, and metal progression assets.",
    echotoolcore: "Baseline tool assets.",
    echostationcore: "Baseline station assets.",
    echoworldstarter: "Starter shelter, light, campfire, and bedroll assets."
  };
  writeJson(manifestFile, manifest);

  const langFile = path.join(openlandsAssetRoot, "lang/en_us.json");
  const lang = readJson(langFile);
  const keep = {};
  for (const [key, value] of Object.entries(lang)) {
    if (key.startsWith("block.echoopenlandsprotocol.")) {
      const id = key.slice("block.echoopenlandsprotocol.".length);
      if (blockIds.includes(id)) keep[key] = value;
      continue;
    }
    if (key.startsWith("item.echoopenlandsprotocol.")) {
      const id = key.slice("item.echoopenlandsprotocol.".length);
      if (itemIds.includes(id)) keep[key] = value;
      continue;
    }
    keep[key] = value;
  }
  for (const block of blocks) {
    keep[langKey("block", block.id)] = block.displayName ?? titleFromId(block.id);
  }
  for (const item of items) {
    keep[langKey("item", item.id)] = item.displayName ?? titleFromId(item.id);
  }
  writeJson(langFile, keep);
}

collectMovedLang();
syncOpenlandsAssets();
