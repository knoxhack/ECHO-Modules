# ECHO: Galactic Survey Protocol

Galactic Survey Protocol is the first-party pack-root contract for `ECHO:
Galactic Survey`. It owns the pack identity, survey data contracts, starter
content IDs, probe-first route, HoloMap layers, Lens profiles, Terminal pages,
salvage gates, certification progression, and release-readiness gates.

The player fantasy is direct: wake at a quiet survey outpost, rebuild a
long-range survey network, launch probes, plot fuel-safe routes, salvage orbital
wreckage, and publish a sector atlas from the edge of known ECHO
infrastructure.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echogalacticsurveyprotocol` |
| Version | `0.1.0` |
| Type | `addon` |
| Kind | `pack_root` |
| Role | `official_pack` |
| Pack ID | `galactic-survey` |
| Default Mode | `long_range_survey` |
| Trust | `official` |

## Runtime Targets

| Runtime | Edition ID |
| --- | --- |
| ECHO native | `galactic-survey-native-edition` |
| Minecraft/NeoForge | `galactic-survey-neoforge-edition` |
| ECHO standalone | `galactic-survey-standalone-edition` |

## Data Roots

- `data/echogalacticsurveyprotocol/galacticsurvey/plan`
- `data/echogalacticsurveyprotocol/galacticsurvey/content`
- `data/echogalacticsurveyprotocol/galacticsurvey/survey`
- `data/echogalacticsurveyprotocol/galacticsurvey/progression`
- `data/echogalacticsurveyprotocol/galacticsurvey/integrations`
- `data/echogalacticsurveyprotocol/galacticsurvey/release`

## Build And Release

Galactic Survey Protocol should eventually emit:

```text
echogalacticsurveyprotocol-0.1.0-neoforge.jar
echogalacticsurveyprotocol-0.1.0.echo-addon
echogalacticsurveyprotocol-0.1.0-standalone.jar
echogalacticsurveyprotocol-0.1.0-sources.jar
```

Source-packaged artifacts are allowed only for visibility review. Player-facing
releases must use compiled runtime artifacts with checksums, Release Index
entries, and Launcher install/update/repair/rollback evidence.

## Validation

```text
node addons/echogalacticsurveyprotocol/scripts/validate-galactic-survey-contract.mjs --module-root addons/echogalacticsurveyprotocol
node addons/echogalacticsurveyprotocol/scripts/smoke-galactic-survey-route.mjs --module-root addons/echogalacticsurveyprotocol
./gradlew :echogalacticsurveyprotocol:runGalacticSurveyRuntimePlaytest
```

The gameplay route smoke validates the first 30 minutes, first 2 hours, and
Survey Array completion contract. It does not replace a visible in-game
playthrough.

The runtime playtest harness executes the compiled Java service and writes
`build/reports/galactic-survey/runtime-playtest.json`. It proves deterministic
runtime loops, HoloMap planning, Survey Array restoration, and save/reload
equivalence, but it still does not replace live client capture.

## Phase Plan

The canonical 10-phase implementation plan is maintained at:

```text
docs/GALACTIC_SURVEY_FULL_EXPERIENCE_PLAN.md
```
