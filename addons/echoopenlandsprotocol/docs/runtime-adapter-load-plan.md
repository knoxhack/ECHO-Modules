# Openlands Runtime Adapter Load Plan

Openlands is a data-first pack. Native, Standalone, and NeoForge adapters must
load the same Echo IDs, the same relaxed Standard rules, the same first-hour
state, and the same waystone/HoloMap state before the pack can be marked ready.

The source contract lives in two places:

- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/runtime_execution_harness_plan.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/launcher_execution_harness_plan.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_approval_harness_plan.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json`
- `src/main/resources/data/echoopenlandsprotocol/openlands/progression/production_phase_matrix.json`
- `com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts`
- `com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsFirstHourRuntime`

## Required Phases

| Order | Phase | Adapter Work |
| --- | --- | --- |
| 10 | `discover` | Read `META-INF/echo.mod.json`, verify `echoopenlandsprotocol`, version `0.1.0`, official pack root status, and current runtime target. |
| 20 | `load_data` | Parse game modes, content policy, registries, world data, first-hour data, production phase matrix, gameplay catalog, waystones, HoloMap, sounds, and system contracts from Echo-owned paths. |
| 30 | `register_content` | Register blocks, items, recipes, tags, loot, and station surfaces from canonical Echo IDs. |
| 40 | `bind_worldgen` | Bind Meadows, Woodlands, Stonehills, Marshlands, starter landmarks, creature spawns, ambience, and starter resource guarantees. |
| 50 | `bind_gameplay_state` | Bind tutorial triggers, shelter score, inventory/chest/bedroll/campfire save data, waystones, HoloMap, multiplayer permissions, homestead, builder UX, and sounds. |
| 60 | `ready` | Report pack readiness only after a smoke test proves Standard starts, interaction works, and hardcore meters are off. |
| 70 | `release_gate` | Promote only after validator, artifacts, launcher flows, runtime parity, first-hour playtest, waystone reload, legal audit, final review, and distribution approval evidence pass. |

## Adapter-Specific Responsibilities

`echo_native` consumes common JSON through Echo content contracts and returns
`adapterBootstrapStepIds`, `requiredRuntimeEvidence`, and
`requiredPublicAlphaEvidence` through the Native module description.

`echo_runtime_standalone` must not depend on Minecraft classes. It owns the
standalone block, item, worldgen, inventory, shelter, waystone, HoloMap, and
co-op state surfaces while persisting the same Echo schema keys.

`neoforge` may convert Echo IDs to NeoForge `ResourceLocation` values only at
adapter output time. Echo IDs remain authoritative, and generated NeoForge
resources must still pass the original-content policy.

## Release Evidence Groups

Discovery evidence proves descriptor, module identity, runtime target, and source
roots. Config evidence proves Standard is relaxed, Hardlands is optional, and the
legal policy was accepted. Data and registration evidence prove JSON parsing,
content counts, id resolution, canonical IDs, loot, tags, recipes, and station
surfaces.

Worldgen evidence proves biome palettes, spawn tables, landmarks, and starter
resource guarantees. Gameplay evidence proves tutorial triggers, forgiving
shelter scoring, first-hour save/load, waystone state, HoloMap reveal,
multiplayer permissions, homestead state, builder UX, and sounds.

`systems/playable_runtime_contract.json` defines the shared Java hooks that
adapters must call for relaxed Standard rules, starter spawn validation,
forgiving shelter scoring, first-hour route order, waystone state transitions,
homestead crop growth, cookpot readiness, builder/storage action validation,
and adapter readiness metadata.

`playtests/mvp_first_hour_acceptance.json` is the machine-readable script for
the first-hour route, save/load checkpoints, HoloMap persistence, and waystone
state progression. Runtime adapters should map those assertions to their own
test harnesses without renaming Echo IDs.

`index/mvp_gameplay_catalog.json` is the generated Index/Guide contract for the
MVP block and item roster. Runtime adapters should expose its acquisition paths,
gameplay roles, player uses, progression stages, and runtime parity notes using
Echo IDs as the source of truth.

`progression/production_phase_matrix.json` is the generated production plan
contract for all ten phases plus the final launch phase. Runtime adapters and
edition repos should use its checkpoint and runtime-gate evidence to report
what is contract-ready, what is preflight-ready, and what still requires live
runtime, launcher, co-op, or final art/legal execution.

`systems/runtime_execution_acceptance.json` defines the real execution report
schema for those live gates. It maps each runtime gate to scenario actions,
assertions, required captures, and edition report paths such as
`evidence/native-runtime-execution-report.json`. Preflight reports are useful
for contract readiness, but only reports matching this contract can clear real
runtime execution gates.
`systems/runtime_execution_harness_plan.json` defines how adapters actually run
those scenarios. It names the world, worldgen, shelter, route, save/load,
creature, waystone, homestead, builder/storage, distribution, launcher, and
review driver surfaces, plus the saved artifacts that a passing report must
attach.
`systems/harness_driver_manifest_contract.json` defines how each edition reports
which real driver surfaces are implemented. The current edition templates live
at `evidence/<edition>-harness-driver-manifest.template.json`; the blocked
harness runners auto-load those templates, record the missing driver ids, and
keep `real_harness_execution_not_run` until actual driver entrypoints run.

Ready evidence proves adapter-ready metadata, a runtime smoke test, and default
hardcore meters off. Release evidence proves local contract validation, uploaded
artifacts with hashes, public download verification, Release Index patch
approval, local publication rehearsal, edition manifest index preview, local
runtime rehearsal, local launcher rehearsal, real launcher
install/update/repair/rollback, first-hour playtest, waystone save/load, and
legal content audit. The local publication rehearsal only verifies local
download-back mechanics and patch preview shape; the edition manifest index
preview only verifies the three manifest templates, module requirement graph,
and launcher channel listing shape; public URL download verification still
requires real hosted artifacts, and Release Index patch approval remains
separate from hash verification.

`systems/legal_content_audit.json` is the machine-readable legal gate. It lets
internal policy docs mention compatibility targets while keeping public pack
text, registry names, asset paths, recipe identity, and generated runtime output
free of prohibited copied identity.

`systems/launcher_flow_acceptance.json` is the machine-readable launcher gate.
It defines install, update, repair, and rollback evidence for every edition and
keeps artifact hash requirements tied to Release Index warning/approval state.
`evidence/<edition>-local-runtime-rehearsal-report.json` is a local runtime
preflight that maps every real runtime execution scenario to current fixtures,
pure runtime hook proofs, captures, and saved artifacts. It is rehearsal-only
evidence and must not clear `systems/runtime_execution_acceptance.json`.
`systems/launcher_execution_acceptance.json` is the real launcher execution gate.
It defines the per-edition report schema, exact install/update/repair/rollback
actions, flow assertions, saved artifacts, and launcher gates that must remain
uncleared until the ECHO Launcher actually runs those flows.
`systems/launcher_execution_harness_plan.json` defines how the ECHO Launcher
actually runs those flows. It names channel, manifest, artifact cache,
descriptor, world lifecycle, state preservation, repair, rollback, and log
driver surfaces, plus the saved artifacts and world-state policies that passing
reports must attach.
`evidence/<edition>-local-launcher-rehearsal-report.json` is a local preflight
that exercises the artifact cache, update, repair, rollback, descriptor, and
world/config preservation mechanics against local compiled artifacts. It is
rehearsal-only evidence and must not clear `systems/launcher_execution_acceptance.json`.
`systems/final_release_review_acceptance.json` is the final human review gate.
It defines the per-edition report schema, public identity checklist, block and
item asset review, audio source review, generated-output audit, saved review
artifacts, and final review gates that must remain uncleared until human
signoff exists.
`systems/final_release_review_harness_plan.json` defines how that review is run.
It maps each final review area to exact driver surfaces, fixture inputs,
checklist IDs, required captures, saved artifacts, and report assembly rules.
`systems/distribution_approval_acceptance.json` is the final publication gate.
It defines the per-edition report schema, artifact URL publication, download
hash verification, edition manifest indexing, co-op public-alpha session
evidence, saved approval artifacts, and distribution gates that must remain
uncleared until release approval exists.
`systems/distribution_approval_harness_plan.json` defines how publication
approval is run. It maps Release Index, artifact download, edition manifest,
dependency, co-op, roadmap, rollback, approval signature, and saved-artifact
drivers to the exact approval areas required before Public Alpha can be ready.
`systems/release_publication_manifest_contract.json` defines the module-level
publication handoff after local artifacts exist. It requires Native,
Standalone, NeoForge, and sources artifact records to carry public download
URLs, download-back SHA-256/size verification, Release Index patch state, and
approval evidence before URL-related blockers can clear.

## Hard Stop Rules

- Missing descriptor, invalid module identity, or unsupported runtime stops load
  before any registry data is read.
- Invalid registry IDs stop registration before partial content reaches a save.
- Failed starter spawn guarantees stop Openlands world creation.
- Failed first-hour save/load, waystone persistence, or HoloMap persistence keeps
  Public Alpha blocked.
- Missing artifacts, hashes, public download URLs, download verification,
  launcher evidence, runtime parity evidence, legal audit, final review,
  Release Index patch approval, or distribution approval keeps Release Index
  state at `warning`.
