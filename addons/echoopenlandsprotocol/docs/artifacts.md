# Openlands Protocol Artifact Notes

Openlands Protocol follows the standard ECHO module artifact contract.

## Runtime Families

| Artifact | Target |
| --- | --- |
| `echoopenlandsprotocol-0.1.0.echo-addon` | ECHO Native |
| `echoopenlandsprotocol-0.1.0-standalone.jar` | ECHO Standalone Runtime |
| `echoopenlandsprotocol-0.1.0-neoforge.jar` | NeoForge compatibility |
| `echoopenlandsprotocol-0.1.0-sources.jar` | Traceability |
| `echoopenlandsprotocol-0.1.0-content-graph.json` | Required; Release-Index catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `.echo/content-graph/*` | Required; embedded in every runtime archive and also available via the content-graph sidecar. |

## Source Of Truth

The Echo data IDs under `data/echoopenlandsprotocol/openlands` are canonical.
Native, Standalone, and NeoForge adapters must preserve those IDs.

## Compiled Release Command

From `ECHO-Modules`, generate the current compiled Openlands artifact set with:

```bash
./gradlew generateOpenlandsModuleRelease
```

That task builds the Openlands runtime closure, verifies `addons/echoopenlandsprotocol/build/libs` contains a compiled runtime jar, and runs the module release generator without `--package-from-source`. The resulting runtime artifacts in `dist/echo-module-release/echoopenlandsprotocol` must report `buildMode: "compiled-runtime"` in `dist/echo-module-release/echo-release.json`.

Use `node scripts/generate-module-release.mjs --module echoopenlandsprotocol --package-from-source` only for development dry runs when the compiled jar is intentionally unavailable.

## Player-Ready Gate

Do not mark Openlands Protocol player-ready until:

- all MVP registry IDs resolve
- all recipes resolve their inputs and outputs
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-gameplay-catalog.mjs --module-root addons/echoopenlandsprotocol` has refreshed `index/mvp_gameplay_catalog.json` so every MVP block/item has acquisition, gameplay role, player-use, progression-stage, and runtime parity detail
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-production-phase-matrix.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github` has refreshed `progression/production_phase_matrix.json` so all 10 production phases, the final launch phase, and 55 evidence-backed checkpoints stay current
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-contract.mjs --module-root addons/echoopenlandsprotocol --require-artifacts` passes
- first-hour route and `playtests/mvp_first_hour_acceptance.json` conformance pass
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-core.mjs --module-root addons/echoopenlandsprotocol` passes
- `asset_manifest.json` declares owned placeholder coverage for every MVP block/item, while `systems/legal_content_audit.json` keeps public release blocked until final asset/legal review
- each edition runs `node scripts/generate-adapter-boot-report.mjs` and produces an adapter-boot preflight report for descriptor identity, runtime target acceptance, adapter phases, load steps, success signals, runtime evidence IDs, and compiled artifact resource coverage
- each edition runs `node scripts/generate-registry-parity-report.mjs` and produces a registry-parity preflight report for 53 blocks, 50 items, 40 recipes, 4 biomes, 8 structures, 10 creatures, 8 waystone states, Standard-mode parity, first-hour save/load fields, and waystone state-machine parity
- each edition runs `node scripts/generate-crafting-station-report.mjs` and produces a crafting-station preflight report for 40 recipes, six MVP station surfaces, map-table route recipes, process timings, unlock hooks, and deferred loom/mason-table blocks
- each edition runs `node scripts/generate-worldgen-exploration-report.mjs` and produces a worldgen/exploration preflight report for four biomes, eight landmarks, ten creatures, starter spawn safety, HoloMap layers, hint types, ambience/sound keys, semantic markers, and compiled artifact resource coverage
- each edition runs `node scripts/generate-creature-roster-report.mjs` and produces a creature-roster preflight report for ten creatures, spawn rules, drop tables, AI hints, sound keys, safe-start distances, and compiled artifact creature coverage
- each edition runs `node scripts/generate-old-road-network-report.mjs` and produces an old-road network preflight report for old-road blocks, route records, map-table route recipes, HoloMap oldRoadSegments, linked route ids, and public travel permission fields
- each edition runs `node scripts/generate-alpha-systems-report.mjs` and produces an alpha-systems preflight report for relaxed homestead rules, crops, cookpot meals, animal pens, trader surplus, builder hammer/scaffold UX, inventory commands, co-op permissions, and storage transaction surfaces
- each edition runs `node scripts/generate-distribution-roadmap-report.mjs` and produces a distribution/roadmap preflight report for distribution alpha gates, launch roadmap phases, local compiled artifact SHA-256/size metadata, Release Index warning state, edition matrix alignment, Public Alpha MVP floors, launcher-flow mapping, and remaining real-execution blockers
- each edition runs `node scripts/generate-legal-audit-report.mjs` and produces a legal/content preflight report for public names, IDs, recipe identity, asset paths, generated artifact paths, and placeholder release blocking
- each edition runs `node scripts/generate-first-hour-playtest-report.mjs` and produces a first-hour preflight report for route/scenario/save-load fixture alignment
- each edition runs `node scripts/generate-waystone-save-load-report.mjs` and produces a waystone preflight report for state persistence, HoloMap fields, multiplayer permissions, and fast-travel unlock requirements
- `systems/launcher_flow_acceptance.json` conformance passes for install, update, repair, rollback, hash checks, and world/config preservation
- each edition runs `node scripts/generate-launcher-flow-report.mjs` and produces a launcher-flow preflight report against the compiled release artifacts
- `systems/runtime_execution_acceptance.json` defines the future `evidence/<edition>-runtime-execution-report.json` shape required to clear real runtime gates; preflight reports must not mark those gates complete
- `systems/runtime_execution_harness_plan.json` defines the concrete real-runtime harness driver surfaces, actions, assertions, captures, saved artifacts, and report assembly rules that adapters must implement before they can produce passing runtime execution reports
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` can rehearse all runtime execution scenarios against current fixtures, pure runtime hooks, captures, and saved preflight artifacts without clearing real adapter gates
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` validates the saved local runtime rehearsal and its scenario artifacts under the edition evidence tree
- `systems/harness_driver_manifest_contract.json` defines the edition-owned driver manifest templates that list available and missing runtime, launcher, final-review, and distribution driver surfaces before a harness runner can execute real drivers
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-harness-driver-manifest.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` can regenerate each edition driver manifest from the source contract
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-harness-driver-manifest.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` validates template, partial, or ready driver manifests before harness runners consume them
- each edition exposes `node scripts/generate-runtime-execution-report.mjs` for honest blocked reports and `node scripts/validate-runtime-execution-report.mjs` for strict validation; a blocked report must keep all runtime gates remaining and `publicAlphaReady: false`
- `systems/launcher_execution_acceptance.json` defines the future `evidence/<edition>-launcher-execution-report.json` shape required to clear real launcher gates; preflight reports must not mark those gates complete
- `systems/launcher_execution_harness_plan.json` defines the concrete ECHO Launcher harness driver surfaces, preconditions, actions, assertions, captures, saved artifacts, and world-state preservation rules required before launcher execution reports can pass
- each edition exposes `node scripts/generate-launcher-execution-report.mjs` for honest blocked reports and `node scripts/validate-launcher-execution-report.mjs` for strict validation; a blocked report must keep all launcher gates remaining and `publicAlphaReady: false`
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` can rehearse local artifact cache install, update, repair, rollback, descriptor inspection, and Openlands Standard world/config preservation without clearing real launcher gates
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition` validates the saved rehearsal report and saved artifacts under the edition evidence tree
- `systems/final_release_review_acceptance.json` defines the future `evidence/<edition>-final-release-review-report.json` shape required to clear final art/audio/legal review gates; preflight reports must not mark those gates complete
- `systems/final_release_review_harness_plan.json` defines the concrete final-review harness driver surfaces, review-area bindings, checklist inputs, required captures, saved artifacts, and report assembly rules required before final review reports can pass
- each edition exposes `node scripts/generate-final-release-review-report.mjs` for honest blocked reports and `node scripts/validate-final-release-review-report.mjs` for strict validation; a blocked report must keep all final review gates remaining and `publicReleaseReady: false`
- `systems/distribution_approval_acceptance.json` defines the future `evidence/<edition>-distribution-approval-report.json` shape required to clear public artifact publication, hash verification, edition manifest indexing, co-op public-alpha session, and release approval gates; preflight reports must not mark those gates complete
- `systems/distribution_approval_harness_plan.json` defines the concrete distribution approval harness driver surfaces, approval-area bindings, dependency checks, co-op captures, approval signatures, saved artifacts, and report assembly rules required before distribution approval reports can pass
- each edition exposes `node scripts/generate-distribution-approval-report.mjs` for honest blocked reports and `node scripts/validate-distribution-approval-report.mjs` for strict validation; a blocked report must keep all distribution gates remaining and `publicAlphaReady: false`
- `systems/release_publication_manifest_contract.json` defines the module-level publication manifest required before Release Index download URL blockers can clear
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol` has refreshed `dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json` after the compiled module release exists
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol` passes and confirms all four artifact records match local SHA-256/size metadata, with blocked templates keeping URLs empty until public downloads are verified
- after upload, `node addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs --module-root addons/echoopenlandsprotocol --url-map <url-map.json>` must download all four public artifact URLs, verify SHA-256 and size, save verification artifacts, and write `openlands-release-publication-manifest.verified.json`
- after verification and approval signoff, `node addons/echoopenlandsprotocol/scripts/approve-openlands-release-publication.mjs --module-root addons/echoopenlandsprotocol --approval <approval.json> --apply-release-index` must reopen the Native, NeoForge, and Standalone passed distribution approval reports named in `distributionApproval.reports`, then write the approved manifest and patch `echo-release.json` download URLs from the verified manifest
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol` has refreshed `dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-rehearsal-report.json` and saved local download-back plus patch-preview evidence under `openlands-release-publication-rehearsal-artifacts`
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol` passes and confirms the local download-back hashes match without clearing public URL, Release Index patch approval, or distribution approval blockers
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github` has refreshed `dist/echo-module-release/echoopenlandsprotocol/openlands-edition-manifest-index-preview.json` and saved `edition-manifest-index-report.json`, `module-requirement-resolution.json`, `launcher-channel-listing.json`, and per-edition modpack entry previews under `openlands-edition-manifest-index-preview-artifacts`
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github` passes and confirms the three edition manifests, module requirement graph, local artifact hashes/sizes, descriptor requirements, and launcher channel listing preview without clearing real launcher or distribution gates
- `node addons/echoopenlandsprotocol/scripts/generate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github` has refreshed the aggregate readiness report under `dist/echo-module-release/echoopenlandsprotocol/openlands-release-readiness-report.json`
- waystone state survives save/load
- Native, Standalone, and NeoForge artifacts build from the same source data
- edition manifests resolve this module through `moduleRequirements`
- edition `evidence/runtime-evidence.template.json` files match runtime evidence, playtest scenarios, save/load checkpoints, and waystone public-alpha proof
- edition `evidence/<edition>-harness-driver-manifest.template.json` files are generated from `systems/harness_driver_manifest_contract.json`, validate with `validate-openlands-harness-driver-manifest.mjs`, and list all missing real runtime, launcher, final-review, and distribution driver surfaces
- edition `evidence/<edition>-local-runtime-rehearsal-report.json` files validate fixture mapping, pure runtime hooks, scenario captures, and saved preflight artifacts for all 17 runtime execution scenarios while preserving `publicAlphaReady: false`
- edition `evidence/<edition>-local-launcher-rehearsal-report.json` files validate local cache/update/repair/rollback mechanics and saved rehearsal artifacts while preserving `publicAlphaReady: false`
- `node addons/echoopenlandsprotocol/scripts/validate-openlands-editions.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github` passes

The production phase matrix, adapter-boot, registry-parity, crafting-station, worldgen/exploration, creature-roster, old-road network, alpha-systems, distribution/roadmap, first-hour, waystone, and local runtime rehearsal preflights are not substitutes for real runtime execution or release approval. They keep Public Alpha blocked until adapter boot, registry parity, station crafting, biome/landmark/creature/HoloMap worldgen, creature spawn/AI/drop/sound behavior, old-road generation and route binding, homestead/builder/co-op alpha systems, uploaded Release Index URLs, public download hash verification, real Native/Standalone/NeoForge parity execution, co-op session testing, first-hour playtest, and waystone save/load execution reports exist from the actual Native, NeoForge, and Standalone adapters. The legal/content preflight is not a substitute for final human art/legal review. It keeps public release blocked while placeholder assets are present. The launcher-flow and local launcher rehearsal preflights are not substitutes for real launcher execution. They keep `publicAlphaReady: false` until install, update, repair, rollback, and world/config preservation have been run through the launcher, recorded in `launcher_execution` reports, and Release Index download URLs exist. The final release review blocked report is not a substitute for human signoff; it only records the required checklist until a reviewer, review date, final assets, audio source manifest, generated-output audit, and saved review artifacts exist. The release publication manifest is not a substitute for uploading artifacts; its blocked template records missing public URLs, missing download verification, and missing Release Index patch approval. The public download verifier proves uploaded URL bytes match the release metadata, but it still does not approve or apply the Release Index patch. The approval tool requires explicit signoff before patching the Release Index; it is not a substitute for the real approval evidence itself. The release publication rehearsal is also not a substitute for uploading artifacts; it only proves local copy/download-back mechanics and patch-preview shape. The edition manifest index preview proves the three thin edition manifests can become launcher channel entries and resolve module requirements from local metadata, but it is not a substitute for real launcher channel indexing or distribution approval. The distribution approval blocked report is not a substitute for public release approval; it records the required URL, hash, manifest, co-op session, and approval evidence until those artifacts exist.
