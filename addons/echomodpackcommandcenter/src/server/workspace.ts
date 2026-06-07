import fs from "node:fs";
import path from "node:path";
import type { AppSettings, Project } from "../shared/types.js";
import { ECHO_ROOT } from "./paths.js";

export function projectWorkspaceRoot(project: Project, settings: Pick<AppSettings, "echoRoot">): string {
  const configuredEchoRoot = path.resolve(settings.echoRoot || ECHO_ROOT);
  const workspacePath = project.workspacePath.trim();
  if (isSeededEchoWorkspace(workspacePath)) {
    return configuredEchoRoot;
  }
  if (workspacePath && path.isAbsolute(workspacePath)) {
    return path.resolve(workspacePath);
  }
  if (workspacePath) {
    return path.resolve(configuredEchoRoot, workspacePath);
  }
  return configuredEchoRoot;
}

export function projectWithLiveModuleVersions(project: Project, settings: Pick<AppSettings, "echoRoot">): Project {
  const workspaceRoot = projectWorkspaceRoot(project, settings);
  let changed = false;
  const modules = project.modules.map((module) => {
    const version = readModuleVersion(workspaceRoot, module.path);
    if (!version || version === module.version) {
      return module;
    }
    changed = true;
    return { ...module, version };
  });
  return changed ? { ...project, modules } : project;
}

function readModuleVersion(workspaceRoot: string, modulePath: string): string | null {
  const gradlePropertiesPath = path.join(workspaceRoot, modulePath === "." ? "" : modulePath, "gradle.properties");
  if (!fs.existsSync(gradlePropertiesPath)) {
    return null;
  }
  const match = fs.readFileSync(gradlePropertiesPath, "utf-8").match(/^mod_version=(.+)$/m);
  return match?.[1]?.trim() || null;
}

function isSeededEchoWorkspace(workspacePath: string): boolean {
  const normalizedWorkspace = workspacePath.replaceAll("\\", "/").toLowerCase();
  const normalizedSeedRoot = ECHO_ROOT.replaceAll("\\", "/").toLowerCase();
  return normalizedWorkspace === normalizedSeedRoot || normalizedWorkspace.startsWith(`${normalizedSeedRoot}/`);
}
