# ECHO: Openlands Protocol

Openlands Protocol is the first-party relaxed sandbox contract for Openlands,
Official ECHO Pack #2. It owns Openlands-specific content, progression, old
roads, waystones, first-hour guidance, and cross-runtime parity fixtures.

Openlands is intentionally softer than Ashfall. The default experience is about
building, farming, exploring, and reconnecting the world rather than managing
hardcore survival meters.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echoopenlandsprotocol` |
| Version | `0.1.0` |
| Type | `addon` |
| Kind | `pack_root` |
| Role | `official_pack` |
| Side | `common` |
| Trust | `official` |

## Runtime Targets

| Runtime | Status |
| --- | --- |
| ECHO native | Planned through `.echo-addon` packaging. |
| Minecraft/NeoForge | Planned through `-neoforge.jar` packaging. |
| ECHO standalone | Planned through `-standalone.jar` packaging. |

Declared adapter runtimes: `echo_native`, `echo_runtime_standalone`, `neoforge`

## Default Gameplay Contract

`openlands_standard` keeps the baseline friendly:

- gentle hunger
- no stamina bar
- no hydration
- no food spoilage
- no temperature damage
- recoverable death pack
- moderate and readable hostile creatures
- waystone-based travel

Harder survival behavior belongs in the optional `hardlands` overlay.

## MVP Player Promise

The first playable route must prove this sequence:

1. Spawn near trees, loose stones, fiber, berries, water, and one landmark.
2. Gather `branchwood_stick`, `fieldstone_piece`, `reed_fiber`, and `berries`.
3. Craft crude tools, a campfire, torches, a bedroll, and a workbench.
4. Build a forgiving shelter and sleep safely.
5. Explore a cave, road fragment, or small ruin.
6. Mine copper and tin.
7. Repair the first broken waystone.
8. Reveal the local map and return home stronger.

## Data Roots

- `data/echoopenlandsprotocol/openlands/config`
- `data/echoopenlandsprotocol/openlands/blocks`
- `data/echoopenlandsprotocol/openlands/items`
- `data/echoopenlandsprotocol/openlands/recipes`
- `data/echoopenlandsprotocol/openlands/loot`
- `data/echoopenlandsprotocol/openlands/tags`
- `data/echoopenlandsprotocol/openlands/biomes`
- `data/echoopenlandsprotocol/openlands/structures`
- `data/echoopenlandsprotocol/openlands/creatures`
- `data/echoopenlandsprotocol/openlands/waystones`
- `data/echoopenlandsprotocol/openlands/progression`
- `data/echoopenlandsprotocol/openlands/playtests`
- `data/echoopenlandsprotocol/openlands/tutorials`
- `data/echoopenlandsprotocol/openlands/index`
- `data/echoopenlandsprotocol/openlands/holomap`
- `data/echoopenlandsprotocol/openlands/sounds`
- `data/echoopenlandsprotocol/openlands/systems`
- `data/echoopenlandsprotocol/openlands/conformance`

## Build And Release

Openlands Protocol should eventually emit:

```text
echoopenlandsprotocol-0.1.0-neoforge.jar
echoopenlandsprotocol-0.1.0.echo-addon
echoopenlandsprotocol-0.1.0-standalone.jar
echoopenlandsprotocol-0.1.0-sources.jar
```

Use source-packaged artifacts only for visibility reviews. Player-facing releases
must be replaced by compiled runtime artifacts before approval.

## Validation

Regenerate the MVP gameplay catalog after changing blocks, items, recipes, loot, or first-hour progression:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-gameplay-catalog.mjs --module-root addons/echoopenlandsprotocol
```

This writes `data/echoopenlandsprotocol/openlands/index/mvp_gameplay_catalog.json`, which gives every MVP block and item an acquisition path, gameplay role, player use, progression stage, and runtime parity note for the ECHO Index/Guide layer.

Regenerate the production phase matrix after changing phase evidence, edition reports, artifact names, or launch roadmap state:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-production-phase-matrix.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

This writes `data/echoopenlandsprotocol/openlands/progression/production_phase_matrix.json`, which keeps all ten production phases plus the final launch phase as 55 checkable subphases with concrete module, edition, artifact, and runtime-gate evidence.

Run the Openlands contract validator before packaging:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-contract.mjs --module-root addons/echoopenlandsprotocol
```

Compile and smoke-test the shared first-hour runtime core:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-core.mjs --module-root addons/echoopenlandsprotocol
```

Validate real adapter execution reports once an edition produces them:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Real adapter harnesses must follow `systems/runtime_execution_harness_plan.json`. It maps every `runtime_execution_acceptance` scenario to the exact driver surfaces, actions, assertions, required captures, and saved artifacts needed for Native, NeoForge, and Standalone reports. The current `run-openlands-runtime-execution-harness.mjs` runner validates that plan and emits an honest blocked report until real driver surfaces are implemented.
Edition driver availability must follow `systems/harness_driver_manifest_contract.json`. Each edition owns `evidence/<edition>-harness-driver-manifest.template.json`, and the `run-*harness.mjs` scripts auto-load that template to record which runtime, launcher, final-review, and distribution driver surfaces are still missing.

Regenerate and validate an edition driver manifest from the source contract:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-harness-driver-manifest.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-harness-driver-manifest.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

When real adapter drivers exist, pass an implementation declaration with `--implemented-drivers`; the validator checks adapter entrypoints, driver versions, commits, implemented methods, captures, and evidence roots before the harness runner is allowed to treat those surfaces as available.

Generate blocked runtime execution reports before real adapter execution exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Run the local runtime rehearsal after runtime-core reports and local release artifacts exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

The rehearsal maps all 17 runtime execution scenarios to fixtures, pure runtime hook proofs, captures, and saved preflight artifacts. It is deliberately marked `rehearsalOnly: true`; it does not clear real adapter runtime gates or make Public Alpha ready.

Validate real launcher execution reports once the launcher harness produces them:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Real launcher harnesses must follow `systems/launcher_execution_harness_plan.json`. It maps install, update, repair, and rollback to exact launcher driver surfaces, preconditions, actions, assertions, world-state preservation policies, and saved artifacts. The current `run-openlands-launcher-execution-harness.mjs` runner validates that plan and emits an honest blocked report until real launcher drivers are implemented.

Generate blocked launcher execution reports before real launcher install/update/repair/rollback exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Run the local launcher rehearsal after local release artifacts exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

The rehearsal copies the local compiled artifact through install, update, repair, and rollback cache flows, verifies SHA-256/size and descriptor entries, and preserves Openlands Standard world/config state. It is deliberately marked `rehearsalOnly: true`; it does not clear real launcher gates or make Public Alpha ready.

Validate final release review reports once human art/audio/legal review produces them:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Real final review harnesses must follow `systems/final_release_review_harness_plan.json`. It maps public identity, block assets, item assets, audio sources, and generated runtime outputs to exact review driver surfaces, checklist inputs, required captures, and saved artifacts. The current `run-openlands-final-release-review-harness.mjs` runner validates that plan and emits an honest blocked report until reviewer and asset/audio/legal drivers are implemented.

Generate blocked final release review reports before human signoff exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Validate distribution approval reports once artifact publication, index upload, co-op session, and release approval evidence exists:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Real distribution approval harnesses must follow `systems/distribution_approval_harness_plan.json`. It maps Release Index publication, artifact download verification, edition manifest indexing, dependency gates, co-op public-alpha session evidence, roadmap scope, rollback plan, and approval signature to exact captures and saved artifacts. The current `run-openlands-distribution-approval-harness.mjs` runner validates that plan and emits an honest blocked report until release approval drivers are implemented.

Generate blocked distribution approval reports before public artifact URLs and approval exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Generate and validate the Release Index publication manifest after the local module release exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol
```

`systems/release_publication_manifest_contract.json` defines the required Native, Standalone, NeoForge, and sources artifact URLs, download hash verification, Release Index patch rules, and approval handoff. The generated template remains blocked until all four public download URLs are recorded, downloaded back, hash/size verified, and approved for `echo-release.json`.

After artifacts are uploaded, verify the public downloads and write the verified publication manifest:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-publication-url-map-template.mjs --module-root addons/echoopenlandsprotocol
node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol
node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --url-map C:/path/to/openlands-publication-urls.json --require-urls
node addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs --module-root addons/echoopenlandsprotocol --url-map C:/path/to/openlands-publication-urls.json
```

The template validation command keeps `openlands-publication-url-map.template.json` blank and checks artifact metadata before any upload happens. The filled URL map may use `{"urls":{"native":"https://..."}}` or `{"artifactUrls":[{"id":"native","downloadUrl":"https://..."}]}`. The URL-map validator checks artifact IDs, filenames, expected hashes, expected byte sizes, and public HTTPS URL shape without downloading. The verifier enforces that validation for `--url-map`, then downloads all four public URLs, checks SHA-256 and byte size against `echo-release.json`, writes `openlands-release-publication-manifest.verified.json`, and keeps Release Index patch approval plus distribution approval blocked.

After verified downloads and release approval signoff exist, generate the approval draft template, then approve and apply the Release Index patch:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-approval-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
node addons/echoopenlandsprotocol/scripts/approve-openlands-release-publication.mjs --module-root addons/echoopenlandsprotocol --approval C:/path/to/openlands-publication-approval.json --apply-release-index
```

The approval template writes `openlands-release-publication-approval.template.json` with `templateOnly: true`, current distribution approval report paths and hashes, the required checklist IDs, and an embedded `approvalDraft`. Do not pass the template itself to the approval tool. Copy `approvalDraft` into a real approval JSON after the verified manifest and passed distribution approval reports exist, then fill the approval/signoff fields and checklist statuses.

The approval JSON must use `echo.openlands.release_publication_approval.v1`, include a patch id, release index commit, distribution approval signoff, and `distributionApproval.reports` entries for Native, NeoForge, and Standalone with each expected report path and SHA-256. The approval tool reopens those reports and requires them to be passed, public-alpha-ready, fully cleared, and bound to the current Release Index. It also requires passed checklist entries for public downloads, patch review, distribution signoff, and rollback plan. Without `--apply-release-index`, `--dry-run`, or `--release-index-out`, the tool refuses to write anything.

Generate and validate the local release publication rehearsal after the blocked manifest exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol
```

The rehearsal copies each local artifact into a saved local download cache, hashes it back, writes Release Index patch-preview artifacts, and keeps `publicAlphaReady: false` because no public URLs, public download verification, patch approval, or distribution approval has happened.

Generate and validate the edition manifest index preview after the local release artifacts and edition manifests exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
node addons/echoopenlandsprotocol/scripts/validate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

The preview writes `openlands-edition-manifest-index-preview.json` plus `edition-manifest-index-report.json`, `module-requirement-resolution.json`, and `launcher-channel-listing.json` saved artifacts. It proves the Native, NeoForge, and Standalone manifest templates can be indexed from the local Release Index, but keeps distribution and launcher gates blocked until real launcher channel indexing, public URLs, and approval exist.

Generate the aggregate release readiness report:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

Generate and validate the Public Alpha evidence intake packet from the current readiness report:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

This writes `openlands-public-alpha-evidence-intake.json` and `openlands-public-alpha-evidence-intake.md` beside the readiness report. It maps every active readiness blocker to impacted phases, exact evidence target files, required proof, and validation commands without clearing any release gate. The intake also includes `phaseHandoff`, which groups active blockers, owner hints, proof requirements, handoff files, and validation commands by production phase.

Generate and validate the Public Alpha approval packet template for the final distribution approval handoff:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

This writes `openlands-public-alpha-approval-packet.template.json`, a Markdown summary, and draft saved-artifact templates for `approval-input-report-index.json`, `dependency-gate-summary.json`, `release-readiness-hash.txt`, `public-alpha-approval.md`, `rollback-plan-snapshot.md`, `approved-readiness-report.json`, and `approved-readiness-report-by-phase.md`. These are template-only handoff files; do not attach them to a passed distribution approval report until real evidence replaces the template language and readiness is blocker-free.
The packet also copies the active evidence-intake requirements into `externalEvidenceRequirements`, including impacted phases, proof required, target files, owner hints, and validation commands for every current blocker. The generated `approval-input-report-index.template.json` carries the same requirements so the required distribution approval draft remains self-contained.

Generate launcher-flow preflight reports from each edition root:

```text
node scripts/generate-launcher-flow-report.mjs
```

These reports verify the compiled artifact, SHA-256, size, descriptors, and install/update/repair/rollback acceptance mapping for that edition. They deliberately keep Public Alpha blocked until the real launcher executes those flows.

Validate the three edition repo templates against the shared adapter load plan:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-editions.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

## Production Audit

The machine-readable production audit lives in `data/echoopenlandsprotocol/openlands/progression/production_phase_matrix.json`, and the prose phase-to-file audit lives in `docs/production-phase-audit.md`. The launch roadmap data lives in `data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json`.
The cross-runtime boot contract lives in `docs/runtime-adapter-load-plan.md` and `data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json`.
The shared playable first-hour runtime contract lives in `data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json` and `src/main/java/com/knoxhack/echoopenlandsprotocol/runtime`.
The real adapter execution report contract lives in `data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json`.
The real adapter execution harness plan lives in `data/echoopenlandsprotocol/openlands/systems/runtime_execution_harness_plan.json`.
The local runtime rehearsal reports live in each edition repo under `evidence/<edition>-local-runtime-rehearsal-report.json`.
The edition driver manifest contract lives in `data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json`.
The real launcher execution report contract lives in `data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json`.
The real launcher execution harness plan lives in `data/echoopenlandsprotocol/openlands/systems/launcher_execution_harness_plan.json`.
The local launcher rehearsal reports live in each edition repo under `evidence/<edition>-local-launcher-rehearsal-report.json`.
The final human art/audio/legal review report contract lives in `data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json`.
The final human art/audio/legal review harness plan lives in `data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json`.
The distribution approval report contract lives in `data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json`.
The distribution approval harness plan lives in `data/echoopenlandsprotocol/openlands/systems/distribution_approval_harness_plan.json`.
The release publication manifest contract lives in `data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json`.
