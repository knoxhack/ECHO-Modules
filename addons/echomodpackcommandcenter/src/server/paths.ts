import path from "node:path";

export const APP_ROOT = process.cwd();
export const DEFAULT_PYTHON_EXECUTABLE = "python";
export const ECHO_ROOT = process.env.ECHO_ROOT
  ? path.resolve(process.env.ECHO_ROOT)
  : path.resolve(APP_ROOT, "../..");
export const LOCAL_DATA_DIR = path.join(ECHO_ROOT, ".local", "noxhack-command-center");
export const DB_PATH = path.join(LOCAL_DATA_DIR, "command-center.sqlite");
export const QUARANTINE_DIR = path.join(LOCAL_DATA_DIR, "quarantine");
export const RELEASE_NOTES_DIR = path.join(LOCAL_DATA_DIR, "release-notes");

export function toDisplayPath(value: string): string {
  return value.replaceAll("\\", "/");
}
