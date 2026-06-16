# .ECHO Content Graph

ECHO-Modules generates a runtime-neutral `.ECHO Content Graph` for every first-party module.

## Generated Artifacts

Each module release outputs:

```text
dist/echo-module-release/<module-id>/<version>/.echo/content-graph/
  content-graph.json
  content-graph.md
  features.json
  provenance.json
  unresolved-references.json
  export-plans/neoforge.json
  export-plans/echo_native.json
  export-plans/echo_runtime_standalone.json
  export-plans/hytale.json
```

The release root also outputs one aggregate evidence artifact:

```text
dist/echo-module-release/content-graph-evidence.json
```

This artifact uses `schemaVersion: "echo.content_graph.evidence.v1"` and is the canonical release-level summary for graph counts, module counts, node/edge totals, feature totals, export-plan totals, Hytale blocker totals, per-module summaries, and diagnostics.

## Scripts

```text
node scripts/generate-content-graph.mjs --all --write
node scripts/validate-content-graph.mjs --strict --sdk-root ../ECHO-SDK
node scripts/generate-content-feature-list.mjs --all --write
node scripts/generate-runtime-export-plan.mjs --target hytale --strict
node scripts/test-generate-content-graph.mjs
```

## What Gets Modeled

The generator scans module descriptors and data files to produce nodes for:

- Modules, addons, and dependencies
- Blocks, items, creative tabs, recipes, entities, NPCs
- Regions, triggers, effects, missions, objectives
- UI intents, settings, systems
- Generic schema-backed catalogs (creatures, weather, missions, etc.)
- Minecraft datapack recipes and loot tables

Edges capture relationships such as `recipe_consumes_item`, `recipe_outputs_item`, `mission_has_objective`, `ui_intent_controls_node`, and `module_requires_module`.

## Validation

`validate-content-graph.mjs` checks that every edge points to a real node, flags portable-field pollution, and ensures each Hytale export plan covers all nodes.

```bash
node scripts/validate-content-graph.mjs --strict
```

With `--strict`, any unresolved required reference, portable-field violation, or invalid `echo.content_graph.evidence.v1` SDK summary exits non-zero, making it suitable for CI gates.

Current strict baseline: `133` module graphs, `4392` nodes, and `5911` edges validate. The Hytale export planner currently reports `9` explicit actor blockers, all from `echoopenlandsprotocol` entity nodes that need a future Hytale entity contract or fallback declaration.

## Hytale Boundary

`export-plans/hytale.json` is planning evidence only. The statuses `direct`, `adapter_required`, `fallback`, `blocked`, and `not_applicable` describe how a node could map to Hytale after an adapter/codegen layer exists. The generator does not produce Hytale runtime assets, and a graph with zero blockers would still need a real Hytale adapter gate before it can be called runtime-supported.

Entity and NPC blockers are typed decisions, not silent omissions. Blocked actor nodes include `blockedReasonCode`, `contract`, `requiredAdapter`, and `recommendedFix` fields so release and UI consumers can report the exact missing Hytale contract.

## Runtime Parity Gate

`generate-runtime-parity-audit.mjs --strict-full` validates the Content Graph and attaches graph evidence to each runtime row. Graph validation errors become strict-full blockers; Hytale `blocked` nodes remain planning evidence and do not fail strict-full by themselves.

## Embedding in Releases

`generate-module-release.mjs` embeds the `.echo/content-graph/` tree into every runtime artifact:

- `.echo-addon` archives
- `-neoforge.jar` modules
- `-standalone.jar` modules

It also emits a top-level `<module>-<version>-content-graph.json` sidecar artifact for Release-Index cataloging.

At the release root, it emits `content-graph-evidence.json` for aggregate release evidence. `echo-release.json` records this file under `contentGraphEvidence`, `checksums.sha256` covers it, and the release workflow uploads it beside the module artifacts.

`verify-module-release.mjs` confirms that embedded graph files are present and that checksums match.

## Consuming a Generated Graph

```bash
# Load a single module graph
node -e "console.log(JSON.parse(require('fs').readFileSync('dist/echo-module-release/echocore/1.0.0/.echo/content-graph/content-graph.json')).nodes.length)"

# Inspect Hytale export blockers
node -e "const p=JSON.parse(require('fs').readFileSync('dist/echo-module-release/echocore/1.0.0/.echo/content-graph/export-plans/hytale.json')); console.log(p.nodes.filter(n=>n.status==='blocked').map(n=>n.rationale))"
```

Consumers that only need health/evidence counts should emit the SDK summary contract `echo.content_graph.evidence.v1` instead of inventing local summary fields. Hytale blockers should be normalized from `nodes[]` entries where `status === "blocked"`; legacy `blockers[]` arrays are display-only compatibility input.

## Ownership

- Canonical schemas live in `ECHO-SDK`.
- Official module graph outputs are generated here in `ECHO-Modules`.
- Runtime consumers read generated graphs as optional evidence.
- For the full schema reference and JSON examples, see `ECHO-SDK/docs/schemas/content-graph.md`.
