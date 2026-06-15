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

## Scripts

```text
node scripts/generate-content-graph.mjs --all --write
node scripts/validate-content-graph.mjs --strict
node scripts/generate-content-feature-list.mjs --all --write
node scripts/generate-runtime-export-plan.mjs --target hytale --strict
node scripts/test-generate-content-graph.mjs
```

## Ownership

- Canonical schemas live in `ECHO-SDK`.
- Official module graph outputs are generated here in `ECHO-Modules`.
- Runtime consumers read generated graphs as optional evidence.
