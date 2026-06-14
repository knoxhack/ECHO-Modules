# Codex Active Goal Context: ECHO Runtime Parity

Last refreshed by Codex: 2026-06-14 after strict-full, strict-play, Native smokes, Standalone runtime smokes, and NeoForge launcher artifact-drift repair were rerun.

## Formal Active Goal

Status: active until the user accepts the result.

Objective:

Fully implement the ECHO strict-play/player-functional parity plan from the attached request: advance beyond strict-full by adding a real `--strict-play` gate in ECHO-Modules; generate machine-readable play audit artifacts including `echo-module-runtime-play-audit.json/md`, `manual-acceptance-matrix.json`, `evidence-manifest.json`, and `module-play-completion.json`; ingest NeoForge executed GameTest/live integration evidence, Native typed host mutation/UI/registry/worldgen/save-network evidence, Standalone runtime controller/voxel/client proof, and official pack/lane acceptance evidence for Ashfall, Openlands, Arcana Division, Sky Relay, and Galactic Survey across Native/NeoForge/Standalone; preserve dirty worktrees; do not revert unrelated user/Kimi changes; implement concrete code/reporting/gates first, then run verification commands and clearly report remaining true gameplay/manual QA gaps.

## Current Result

- `node scripts/generate-runtime-parity-audit.mjs --strict-full`: PASS.
- `node scripts/generate-runtime-parity-audit.mjs --strict-play`: PASS.
- Strict-full rows: 396 pass, 0 partial, 0 fail.
- Strict-play rows: 396 pass, 0 partial, 0 fail.
- Runtime breakdown: 132/132 NeoForge pass, 132/132 Native pass, 132/132 Standalone pass.
- Module completion: 132/132 complete.
- Pack acceptance: 15/15 official pack lanes pass.
- Strict-play backlog: 0 items.

## 2026-06-14 NeoForge Launcher Artifact-Drift Repair

Newest launcher logs for Ashfall, Arcana Division, Openlands, Sky Relay, and Galactic Survey showed installed NeoForge packs were loading a stale `echoadaptercore-1.0.0-neoforge.jar`.

- Constructor crashes in Ashfall/Arcana came from missing `EchoBackendLifecycleBridge.registerModListener(Object, String, Consumer<?>)`.
- Dependency failures in Openlands/Sky Relay/Galactic came from NeoForge TOML reporting AdapterCore as `1.0.0-RC1` while product modules require `[1.0.0,)`.
- Galactic Survey also had `echovehiclecore` installed without its required `echoassetcore`.

Repair completed:

- Set `addons/echoadaptercore/gradle.properties` `mod_version=1.0.0`.
- Rebuilt AdapterCore and regenerated the full 132-module `dist/echo-module-release`.
- Patched `scripts/sync-official-pack-module-selections.mjs` so pack snapshots decorate module requirements from `ECHO-Modules/dist/echo-module-release/echo-release.json` instead of preserving stale hashes.
- Synced official pack release snapshots/templates from the canonical module release.
- Replaced local ECHOLauncher NeoForge instance AdapterCore jars for Ashfall, Openlands, Arcana Division, Sky Relay, and Galactic Survey from the verified full release artifact; previous jars were copied into per-instance `echo-repair-backups/<timestamp>` folders.
- Added `echoassetcore-1.0.0-neoforge.jar` to the Galactic Survey NeoForge local instance from the verified full release artifact.

Focused verification after repair:

- `node scripts/verify-module-release.mjs --release-dir dist/echo-module-release`: PASS, 132 records.
- `node scripts/sync-official-pack-module-selections.mjs --check`: PASS.
- Installed NeoForge dependency scan: `NO_REQUIRED_ECHO_DEPENDENCY_GAPS`.
- Installed AdapterCore hashes in all five NeoForge ECHOLauncher instances match canonical release hash `ec8c4e2dbda2a14eea270d8f74aee95ae373978c422d4b0f61b5be20a1533bb0`.
- Installed Galactic `echoassetcore-1.0.0-neoforge.jar` matches canonical release hash `3bc6ff8dc17ed31ad878812e022e50ee977a4035e7fe6cc6e97860f25f88dccc`.
- Galactic Survey NeoForge validator: PASS.
- Sky Relay NeoForge validator: PASS.
- Arcana Division NeoForge validator: blocked only by its pre-existing NeoForge installer SHA placeholder, not by ECHO module dependency/ABI repair.

## Main Implemented Work

- Added and stabilized strict-play reporting/gating in `ECHO-Modules`.
- Generated and refreshed:
  - `reports/runtime-parity/echo-module-runtime-play-audit.json`
  - `reports/runtime-parity/echo-module-runtime-play-audit.md`
  - `reports/runtime-parity/evidence-manifest.json`
  - `reports/runtime-parity/manual-acceptance-matrix.json`
  - `reports/runtime-parity/module-play-completion.json`
  - `reports/runtime-parity/echo-module-runtime-play-fix-backlog.json`
  - `reports/runtime-parity/echo-module-runtime-play-fix-backlog.md`
- Added/updated strict evidence generators for Native, Standalone, pack acceptance, and NeoForge runtime evidence.
- Fixed NeoForge Ashfall gameplay regressions found by real GameTests:
  - Campfire shelter pulse targeting.
  - Faction contract completion state.
  - Machine hopper insertion/extraction for key machines.
  - Duplicate bottle return behavior.
  - Native host delegation/mutation truth.
  - First-spawn quest state mirror into QuestData.
  - Existing-player save migration/relog handling.
- Fixed Standalone runtime gates:
  - Wrapped two Groovy line continuations in `build.gradle` that failed when external launcher artifacts were absent.
  - Made `EchoAshfallNativeModule` standalone-safe by removing its compile-time dependency on NeoForge/Minecraft-bound `ModCreativeTabs`; it now uses reflection when that class is present and falls back to standalone-safe declared item data when it is not.
- Preserved existing dirty worktrees and did not revert unrelated changes.

## Verification Already Run

ECHO-Modules:

- `.\gradlew.bat buildEchoWorkspace -PechoAddonSet=all --console=plain`: PASS.
- `node scripts/generate-core-module-integration-audit.mjs`: PASS.
- `node scripts/generate-module-release.mjs`: PASS, 132 release records generated.
- `node scripts/verify-module-release.mjs --release-dir dist/echo-module-release`: PASS, 132 release records verified.
- `node scripts/test-generate-pack-acceptance-reports.mjs`: PASS.
- `node scripts/generate-runtime-parity-audit.test.mjs`: PASS.
- `node scripts/generate-strict-play-evidence.mjs`: PASS.
- `node scripts/run-neoforge-gametests-and-write-evidence.mjs --modules echoashfallprotocol --timeout-ms 300000`: PASS earlier in this goal, 108/108 required Ashfall GameTests passed.

ECHO-Native-Platform:

- `.\gradlew.bat check --console=plain`: PASS before the final Ashfall native-entrypoint standalone-safety patch.
- After that patch, reran the Native parity smokes and all passed:
  - `runNativeSdkTestkitSmoke`
  - `runNativeAllBridgeableModuleLoadStateSmoke`
  - `runNativeAllBridgeableModuleArtifactLoadStateSmoke`
  - `runNativeAgent2ClientRouteOwnershipSmoke`
  - `runNativeAgent5UiBridgeContractSmoke`
  - `runNativeAgent4RegistryContentSmoke`
  - `runNativeAgent4WorldStartupSmoke`
  - `runNativeAgent9MachineRuntimeHostSmoke`
  - `runNativeMutationTruthGate`

ECHO-Standalone-Runtime:

- Runtime/module smoke list from the requested plan passed:
  - `runStandaloneFullCatalogModuleStatusSmoke`
  - `runStandaloneAdapterCoreModuleCoverageSmoke`
  - `runStandaloneRealModuleExecutionSmoke`
  - `runStandaloneNativeLoaderAbiSmoke`
  - `runStandaloneAgent5UiParitySmoke`
  - `runStandaloneClientScreenCatalogSmoke`
  - `runStandaloneClientModsRuntimeContentSmoke`
  - `runStandaloneClientWorldInteractionSmoke`
  - `runStandaloneClientHeldItemOverlaySmoke`
- `.\gradlew.bat check --console=plain` advanced through runtime/module/playable/client checks and then stopped only at `runStandaloneBetaReadinessGate`.
- `runStandaloneBetaReadinessGate` remains intentionally BLOCKED by external release handoff requirements, not module runtime parity:
  - Missing current launcher setup artifact `ECHO-Launcher-1.1.3-Setup.exe`.
  - Visible packaged EXE/window probes require explicit `-PechoStandaloneAllowWindowLaunch=true`.
  - Human/manual wall-clock playtest evidence remains pending.
  - Signing, clean install/uninstall, hardware audio, and public release evidence remain outside this automated module parity run.

## Remaining Non-Code / Manual Release Gaps

These are not strict-play module parity blockers because strict-play is green, but they still block Standalone public/beta release promotion:

- Build/provide current ECHO Launcher setup artifacts and matching update metadata.
- Run visible packaged EXE probes with explicit window-launch opt-in.
- Complete manual wall-clock playtest capture.
- Complete signing evidence on the release signing machine.
- Complete clean install/uninstall evidence in a fresh VM or disposable Windows profile.
- Complete real hardware audio evidence.

## Resume Guidance

If resuming after compaction, do not restart the audit from scratch. The code/reporting side is implemented and strict-play is green. Only revisit code if new source changes are made or if the user asks to convert the remaining Standalone release-handoff/manual evidence blockers into runnable release artifacts.
