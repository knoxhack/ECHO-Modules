# ECHO Module Release Contract

`knoxhack/ECHO-Modules` is the canonical source for module artifacts consumed by Ashfall editions.

Every module release must publish artifacts generated from the module descriptor version, not a single global pack version.

## Per-Module Outputs

For each module, generate:

```txt
<module>-<version>-neoforge.jar
<module>-<version>.echo-addon
<module>-<version>-standalone.jar
<module>-<version>-sources.jar
<module>-<version>-content-graph.json
META-INF/echo.mod.json
META-INF/neoforge.mods.toml
echo-addon-package.json
checksums.sha256
```

The release root also includes `content-graph-evidence.json`, a single aggregate `echo.content_graph.evidence.v1` document for the whole module release.

Applicability:

| Output | Required when |
| --- | --- |
| `<module>-<version>-neoforge.jar` | The module supports the Minecraft/NeoForge runtime. |
| `<module>-<version>.echo-addon` | The module supports the ECHO native addon/module runtime. |
| `<module>-<version>-standalone.jar` | The module supports the standalone runtime. |
| `<module>-<version>-sources.jar` | Always, for traceability and developer debugging. |
| `<module>-<version>-content-graph.json` | Always, a Release-Index-catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `content-graph-evidence.json` | Always at the release root, aggregate release evidence for graph/module/node/edge/feature/export-plan/Hytale-blocker counts. |
| `META-INF/echo.mod.json` | Always, embedded in each runtime artifact where applicable. |
| `META-INF/neoforge.mods.toml` | NeoForge artifacts only. |
| `echo-addon-package.json` | `.echo-addon` packages only. |
| `checksums.sha256` | `.echo-addon` packages only, covering the embedded descriptor, package metadata, README, and runtime jar/source entries. |

## Generator

Use the repository release generator from `knoxhack/ECHO-Modules`:

```sh
node scripts/generate-module-release.mjs
```

The generator writes `dist/echo-module-release/` with per-module folders, `content-graph-evidence.json`, `echo-release.json`, canonical `checksums.sha256`, and a `checksums.txt` compatibility copy. `echo-release.json` uses `schemaVersion: "echo.module.release.v1"` and records the evidence artifact under `contentGraphEvidence` so Release Index imports can validate it without reshaping.

By default it is strict: runtime artifacts are only emitted from existing built jars under `addons/<module>/build/libs`, and the command fails if a required runtime jar is missing. This prevents publishing placeholder runnable jars.

Compiled runtime jars are not copied blindly. The generator opens each compiled NeoForge and standalone jar, preserves its compiled entries, overlays the required public metadata, then writes and checksums the final artifact:

| Runtime artifact | Required embedded metadata |
| --- | --- |
| `<module>-<version>-neoforge.jar` | `META-INF/echo.mod.json`, `META-INF/neoforge.mods.toml`, `.echo/content-graph/*` |
| `<module>-<version>-standalone.jar` | `META-INF/echo.mod.json`, `.echo/content-graph/*` |
| `<module>-<version>.echo-addon` | `META-INF/echo.mod.json`, `echo-addon-package.json`, `checksums.sha256`, `.echo/content-graph/*`, optional `lib/<module>-<version>-runtime.jar` |

Run `node scripts/verify-module-release.mjs --release-dir dist/echo-module-release` after generation. The verifier opens every archive, checks sidecars, validates package dependencies, confirms manifest and checksum agreement, and rejects metadata-only claims.
It also validates `content-graph-evidence.json` as the canonical release summary.

Useful options:

```sh
node scripts/generate-module-release.mjs --module echocore
node scripts/generate-module-release.mjs --module echocore --module echoadaptercore
node scripts/generate-module-release.mjs --allow-missing-runtime
node scripts/generate-module-release.mjs --package-from-source
```

Use `--allow-missing-runtime` only for metadata/source dry runs while the module build graph is being prepared; do not upload those outputs as player-facing runtime releases.

Use `--package-from-source` to create the visible per-module artifact set from checked-in source/resources when compiled jars are not available. Those artifacts are marked with `buildMode: "source-packaged"` in `echo-release.json`; replace them with compiled runtime jars before treating a release as player-ready.

## Galactic Survey Release Gate

The Galactic Survey module closure is the current strict compiled release lane:

```powershell
.\gradlew.bat generateGalacticSurveyModuleRelease --console=plain
node scripts\verify-module-release.mjs --release-dir dist\echo-module-release
```

As of June 13, 2026, this gate generates and verifies 23 compiled runtime module records for `galactic-survey-0.1.0-alpha`, including the Foundation dependencies required by the Galactic modules. The source-owned GitHub release must be produced by the `Release Modules` workflow:

```powershell
gh workflow run release-modules.yml `
  -f release_id=galactic-survey-0.1.0-alpha `
  -f player_ready=true
```

The workflow uploads unique runtime/source artifacts as individual GitHub Release assets, plus `echo-module-release.tar.gz` for the full foldered output containing duplicate-named sidecars such as `META-INF/echo.mod.json` and `META-INF/neoforge.mods.toml`. The Release Index may only promote those records after the workflow attaches the generated artifacts, checksum file, archive checksum, and attestation/provenance to the source-owned release.

## Edition Consumption

| Edition | Repo | Module artifact family |
| --- | --- | --- |
| Ashfall Native Edition | `knoxhack/ECHO-Ashfall-Native-Edition` | `<module>-<version>.echo-addon` |
| Ashfall NeoForge Edition | `knoxhack/ECHO-Ashfall-NeoForge-Edition` | `<module>-<version>-neoforge.jar` |
| Ashfall Standalone Edition | `knoxhack/ECHO-Ashfall-Standalone-Edition` | `<module>-<version>-standalone.jar` |

Pack manifests can still pin direct download URLs, SHA-256 hashes, sizes, module IDs, and versions for each module artifact. They can also declare module requirements and let ECHO Launcher resolve the correct artifact from the `knoxhack/ECHO-Modules` GitHub release feed.

## Launcher Module Resolution

Use `moduleRequirements` when a pack should update modules individually without hard-coding every module artifact URL:

```json
{
  "moduleArtifactFamily": "neoforge",
  "moduleRequirements": [
    {
      "id": "echocore",
      "version": "1.0.0"
    }
  ]
}
```

Default artifact names are derived from the pack family:

| Family | Default artifact |
| --- | --- |
| `echo-addon` | `<module>-<version>.echo-addon` |
| `neoforge` | `<module>-<version>-neoforge.jar` |
| `standalone` | `<module>-<version>-standalone.jar` |

Each requirement can override `assetName`, `path`, `sha256`, `size`, `required`, `side`, or `artifactFamily`. During install/update, the launcher expands those requirements into normal manifest `files`, downloads only changed module files when URLs are resolved, and falls back to the full pack archive only when a changed file has no individual release asset URL.
