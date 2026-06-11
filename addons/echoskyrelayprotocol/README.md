# ECHO: Sky Relay Protocol

Sky Relay Protocol is the first-party pack-root contract for `ECHO: Sky Relay`,
Official ECHO Pack #3. It owns the Sky Relay content IDs, platform fragment
progression, terminal pages, Lens scan profiles, HoloMap layers, weather routes,
anchor rules, release gates, and cross-runtime parity contracts.

The player fantasy is direct: wake on a broken relay above a lethal storm layer,
restore power, scan drifting fragments, anchor platforms, survive weather, and
bring the Signal Crown back online.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echoskyrelayprotocol` |
| Version | `0.1.0` |
| Type | `addon` |
| Kind | `pack_root` |
| Role | `official_pack` |
| Pack ID | `sky-relay` |
| Default Mode | `skyrelay_restoration` |
| Trust | `official` |

## Runtime Targets

| Runtime | Edition ID |
| --- | --- |
| ECHO native | `sky-relay-native-edition` |
| Minecraft/NeoForge | `sky-relay-neoforge-edition` |
| ECHO standalone | `sky-relay-standalone-edition` |

## Data Roots

- `data/echoskyrelayprotocol/skyrelay/plan`
- `data/echoskyrelayprotocol/skyrelay/content`
- `data/echoskyrelayprotocol/skyrelay/fragments`
- `data/echoskyrelayprotocol/skyrelay/progression`
- `data/echoskyrelayprotocol/skyrelay/integrations`
- `data/echoskyrelayprotocol/skyrelay/release`

## Build And Release

Sky Relay Protocol should eventually emit:

```text
echoskyrelayprotocol-0.1.0-neoforge.jar
echoskyrelayprotocol-0.1.0.echo-addon
echoskyrelayprotocol-0.1.0-standalone.jar
echoskyrelayprotocol-0.1.0-sources.jar
```

Source-packaged artifacts are allowed only for visibility review. Player-facing
releases must use compiled runtime artifacts with checksums, Release Index
entries, and Launcher install/update/repair/rollback evidence.

## Validation

```text
node addons/echoskyrelayprotocol/scripts/validate-skyrelay-contract.mjs --module-root addons/echoskyrelayprotocol
node addons/echoskyrelayprotocol/scripts/smoke-skyrelay-gameplay-route.mjs --module-root addons/echoskyrelayprotocol
```

The gameplay route smoke validates the first 30 minutes, first 2 hours, and
Signal Crown route contracts. It does not replace a visible in-game playthrough.

## Phase Plan

The canonical 10-phase, 5-subphase implementation plan is maintained at:

```text
docs/SKY_RELAY_FULL_EXPERIENCE_PLAN.md
```
