# Noxhack Modpack Command Center

Local release-operations dashboard for ECHO and future Minecraft projects. The current pass is focused on real ECHO operations: hybrid QA scans, editable local settings, release command history, guarded release actions, and JSON/Markdown exports.

## Commands

This workstation currently exposes `node.exe` but may not expose `npm.cmd`. If `npm.cmd` is not on PATH, use the bundled npm CLI at `.local\tooling\npm\package\bin\npm-cli.js`.

On Windows PowerShell, bare `npm test` may resolve through `npm.ps1` and be blocked by the local execution policy. Use `npm.cmd test` from this folder for the supported test path; it currently passes the Command Center suite with 65 tests.

```powershell
cd "C:\Github\Echo\addons\echomodpackcommandcenter"
npm.cmd install
npm.cmd run dev
npm.cmd test
npm.cmd run build
npm.cmd run self-check
```

The Vite UI runs at `http://127.0.0.1:5177` and proxies API calls to the local API at `http://127.0.0.1:4177`.

## Operations

- Quick Scan inspects JSON validity, resource references, lang coverage signals, runtime log signatures, and the configured modpack jar set. Full-stack ECHO scans also check terminal page signals and handoff docs.
- Deep Scan adds optional Python validator adapters for resource, gameplay, structure, and runtime-log checks.
- Settings persist the ECHO root, optional modpack mods folder, Python executable, runtime log age, and default scan mode in the local SQLite database.
- Operational Reports reads the Phase 4 `reports/echo` surface through `/api/reports/echo`, exposes sanitized per-report drilldowns at `/api/reports/echo/:reportKey`, lists safe report jobs at `/api/reports/echo/jobs`, opens sanitized report-job details at `/api/reports/echo/jobs/:runId`, and summarizes platform, workspace, graph, PackOS, diagnostics, health, recovery, bridge, AI task, asset, release, support, Launcher, and Command Center catalog state without launching Minecraft or executing repairs.
- Bridge Sessions reads and writes support-safe local bridge state through `/api/bridge`, exposes `/api/bridge/executor/probe`, shows Codex job intents, diagnostics, cursor-tagged log chunks, protected files, safe edit zones, confirmation requests/history, and next prompt previews. Approved jobs can start only the trusted local executor configured in `.local/noxhack-command-center/bridge-executor.json` with fixed `echo_bridge_sidecar_v1` arguments.
- Release Deck actions are allowlisted only. Medium and high risk actions require UI confirmation, active shell runs can be stopped best-effort, and run output/history is retained.
- Exports include the settings snapshot, latest scan, recent scans, release run history, readiness score, roadmap, prompt templates, and raw validator summaries.

## Safety Model

The backend binds to `127.0.0.1` and exposes no arbitrary shell execution endpoint. Bridge executor start is confirmation-gated, config-gated, `shell: false`, and command arguments are fixed by the bridge sidecar protocol. Release buttons can run only explicit allowlisted ECHO actions. Report jobs accept only allowlisted report-regeneration command IDs, reject raw commands, and never run Minecraft, downloads, jar deletion, config resets, save mutation, or repair execution. Stale jar cleanup moves matching stale ECHO jars into `.local/noxhack-command-center/quarantine` under the ECHO repo root instead of deleting them.

## 1.0.0 Public Launch Quickstart

1. Install required dependencies: none.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echomodpackcommandcenter.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echomodpackcommandcenter.md`.
