<!-- CURSEFORGE_README_START -->
# PowerGrid by ECHO Labs

![PowerGrid by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echopowergrid/brand-sheet.png)

**Mod ID: echopowergrid<br**

![PowerGrid by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echopowergrid/features-portrait.png)

![PowerGrid by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echopowergrid/features-landscape.png)

## CurseForge Summary

Mod ID: echopowergrid<br

## Main Features

- Energy routing networks.
- Battery banks and substations.
- Power-grid control systems.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echopowergrid/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echopowergrid/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echopowergrid/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: PowerGrid

**Mod ID:** `echopowergrid`<br>
**Version:** 1.0.0<br>
**Tagline:** Restore the grid. Power the signal.

## What is ECHO PowerGrid?

ECHO PowerGrid is the shared first-party power generation, storage, cable, substation, overload, brownout, and facility power system for the full ECHO/Ashfall ecosystem. It provides a real playable energy backbone for terminals, signal towers, industrial machines, multiblock facilities, convoy depots, orbital launchpads, agriculture domes, blackbox archives, nexus stabilizers, and armory fabricators.

## Power Unit

**EP** = Echo Power. All internal energy math uses EP.

## Blocks

### Generators
- **Hand Crank Generator** - 5 EP/t, manual/scrap tier, no fuel
- **Scrap Burner Generator** - 40 EP/t, burns fuel, outpost tier
- **Solar Panel** - 10 EP/t during day, outpost tier
- **Creative Power Source** - Infinite EP, admin/testing only

### Storage
- **Small Battery Bank** - 20,000 EP capacity, 100 EP/t in/out, outpost tier
- **Medium Battery Bank** - 80,000 EP capacity, 400 EP/t in/out, relay tier
- **Industrial Battery Bank** - 320,000 EP capacity, 1,200 EP/t in/out, industrial tier

### Cables
- **Low Voltage Cable** - 100 EP/t transfer, scrap/outpost tier
- **Industrial Cable** - 500 EP/t transfer, industrial tier

### Control
- **Outpost Substation** - Network coordinator, monitor, and policy control
- **Relay Substation** - Higher-transfer industrial relay and map anchor
- **Nexus Stabilizer Coupler** - Nexus-ready grid control point
- **Emergency Breaker** - Trips on overload, player-resettable
- **Power Meter** - Shows local network, route, loss, and alert diagnostics

### Test
- **Creative Power Sink** - Consumes unlimited EP for testing
- **Test Power Consumer** - 20 EP/t consumer for validation

## Items

- **Copper Coil** - Crafting component
- **Scrap Wire** - Basic wire material
- **Insulated Wire** - Protected wire for cables
- **Power Cell** - Small energy component
- **Battery Core** - Battery crafting core
- **Fuse** - Breaker component
- **Breaker Switch** - Control component
- **Grid Diagnostic Tool** - Right-click to inspect nodes and networks

## Building Your First Grid

1. Craft a **Scrap Burner Generator**.
2. Place a **Small Battery Bank** next to it (or connect with **Low Voltage Cable**).
3. Place a **Test Power Consumer** on the network.
4. Add fuel to the generator.
5. Watch the battery charge and the consumer receive power.
6. Use a **Power Meter** or **Substation** to check network status.

## Brownout and Overload

- **Brownout**: Demand exceeds supply. Consumers receive partial power. Network state becomes `BROWNOUT`.
- **Overload**: Flow exceeds path transfer limits. If enabled, breakers trip after the configured grace window. Network state becomes `OVERLOADED` or `TRIPPED`.
- **Breaker Reset**: Right-click a tripped Emergency Breaker to restore the circuit.
- **Power Loss**: When enabled, long cable paths lose a deterministic percentage of delivered EP. Route diagnostics expose distance, loss, and transfer limits.
- **Substation Policy**: Substations persist a policy mode: `BALANCED`, `LIFE_SUPPORT_FIRST`, `INDUSTRIAL_FIRST`, `NEXUS_STABILIZATION`, or `MANUAL`.

## Integration APIs

- `PowerGridSnapshot` remains the stable lightweight status view.
- `PowerGridNetworkSummary` exposes loaded network id, dimension, anchor, state, quality, generation, demand, drawable EP, storage, node count, and transfer limit for UI/map/debug surfaces.
- `PowerGridNodeSummary` exposes node-level status for Lens, Terminal, commands, and debug output.
- `PowerGridRouteSummary` exposes route distance, path transfer, loss, deliverable EP, and blocked reasons.
- `PowerGridAlert` exposes brownout, overload, tripped breaker, quality, and power-starved style alert data.
- `EchoPowerGridApi.drawPower(level, pos, ep, simulate)` simulates or commits real EP draws from generator buffers and batteries.
- `EchoPowerGridApi.loadedNetworkSummaries(serverLevel)`, `loadedNodeSummaries(serverLevel)`, `alerts(serverLevel)`, `routeSummary(level, from, to)`, and `networkAt(level, pos)` provide read-only integration views.

## FE Compatibility

The FE bridge is active when `enableFeBridge` is true and honors NeoForge transaction rollback for insert/extract probes.

## Commands

- `/echo_power status` - Show grid status at your position
- `/echo_power inspect` - Inspect the targeted power node
- `/echo_power networks` - Summary of loaded networks
- `/echo_power alerts` - List current grid alerts
- `/echo_power route <from> <to>` - Diagnose path transfer/loss between nodes (op only)
- `/echo_power debug_chunk` - Count power nodes in current chunk
- `/echo_power give_test_kit` - Give creative test blocks (op only)
- `/echo_power set_energy <amount>` - Set battery energy (op only)
- `/echo_power reset_network` - Mark network dirty/rebuild (op only)

## Config

See `echopowergrid-common.toml`:
- `general.enabled` - Enable/disable PowerGrid
- `network.maxNetworkSize` - Node cap per network
- `network.idleNetworkSleep` - Skip idle, stable network updates until a local change wakes them
- `loss` - Distance-based EP loss over cable routes
- `overload` - Overload and breaker behavior
- `brownout` - Brownout thresholds
- `compat` - FE bridge ratios
- `performance` - Tick budgets and update intervals

## Optional Integrations

- **Terminal** - Synced PowerGrid dashboard tab plus addon guide metadata
- **Lens** - Power node and network scan data
- **HoloMap** - `Power Networks` layer with network and alert markers
- **MultiblockCore** - Registers PowerGrid as a soft `echo:power_input` provider
- **Industrial Nexus** - Higher-tier multiblock tasks draw EP through MultiblockCore capability costs
- **WorldCore** - Solar weather and hazard-aware extension point when present
- **RuntimeGuard** - Grid rebuild/update budget and profiling hooks when present

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echopowergrid.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echopowergrid.md`.
