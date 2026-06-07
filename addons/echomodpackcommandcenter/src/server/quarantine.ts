import fs from "node:fs";
import path from "node:path";
import type { Project } from "../shared/types.js";
import { QUARANTINE_DIR } from "./paths.js";

const FORBIDDEN_PREFIXES = ["echostationfall", "echoblackboxprotocol", "echoindustrialnexus", "echoindustrialrebirth"];

export function quarantineStaleJars(project: Project, modsDir: string): string {
  if (!modsDir || !fs.existsSync(modsDir)) {
    throw new Error(`Modpack mods folder not found: ${modsDir || "(unset)"}`);
  }

  const expectedNames = new Set(project.modules.map((module) => `${module.modId}-${module.version}.jar`));
  const expectedPrefixes = new Set(project.modules.map((module) => module.modId.toLowerCase()));
  const knownPrefixes = new Set([...expectedPrefixes, ...FORBIDDEN_PREFIXES]);
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  const destination = path.join(QUARANTINE_DIR, stamp);
  const moved: string[] = [];

  for (const entry of fs.readdirSync(modsDir, { withFileTypes: true })) {
    if (!entry.isFile() || !entry.name.endsWith(".jar")) {
      continue;
    }
    const lower = entry.name.toLowerCase();
    const matchingPrefix = [...knownPrefixes].find((prefix) => lower.startsWith(`${prefix}-`));
    if (!matchingPrefix) {
      continue;
    }
    const isExpected = expectedNames.has(entry.name);
    const isForbidden = FORBIDDEN_PREFIXES.includes(matchingPrefix) && !expectedPrefixes.has(matchingPrefix);
    if (isExpected && !isForbidden) {
      continue;
    }
    fs.mkdirSync(destination, { recursive: true });
    fs.renameSync(path.join(modsDir, entry.name), path.join(destination, entry.name));
    moved.push(entry.name);
  }

  if (moved.length === 0) {
    return `No stale ECHO jars found in ${modsDir}.`;
  }
  return `Moved ${moved.length} stale ECHO jar(s) to ${destination}:\n${moved.map((name) => `- ${name}`).join("\n")}`;
}
