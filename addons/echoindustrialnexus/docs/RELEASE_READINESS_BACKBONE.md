# Industrial Nexus Full-Stack Backbone Readiness

Status: implementation pass started for `1.3.x` full-stack backbone.

## Automated Gates

- `:echoindustrialnexus:compileJava` must pass.
- `python tools\validate_resources.py --addon-set all` must pass.
- Existing Industrial GameTests must stay green.
- New backbone coverage must include public API snapshots and Index source facts.

## Implemented And Tested

- Machine runtime: recipe processing, Thermal Flux generation/transfer, item ducts, fluid tanks, fluid pipes, side config, persistence, menu sync, upgrades, heat, emergency shutdown, and break drops have focused GameTests.
- Multiblock runtime: six Industrial facilities are data-driven through MultiblockCore definitions, with robotic tasks, queue controls, provider snapshots, Lens scans, HoloMap markers, and Nexus Furnace Array task/mission coverage.
- Terminal/Mission surfaces: Industrial route placement, mission snapshots, cache claim idempotency, Factory Command actions, recipe parsing, and MissionCore hook coverage are wired.
- Logistics: every current Industrial automation task has a mapped Logistics loadout id and controller auto-restock state persists with save-data defaults.
- Index: Industrial machine recipes are exported as recipe views, and the backbone pass adds source facts for machine outputs, byproducts, factory task outputs, POI schematics, and Warden sources.
- Public API: read-only machine telemetry, factory snapshots, process sources, and sibling support hooks are exposed under `com.knoxhack.echoindustrialnexus.api`.

## Implemented, Manual Sign-Off Required

- POI progression loop: Abandoned Thermal Plant, Rusted Factory Complex, Geothermal Drill Site, Reactor Cooling Station, and Nexus Heat Exchanger Ruins generate resources and record hints, but final placement/balance needs a world playthrough.
- Furnace Warden route: activation, combat phases, reward idempotency, and participant credit have coverage, but encounter tuning needs human combat review.
- RenderCore visuals and sounds: assets and optional hooks are present; particle density, alarm cadence, and ambience mix need client-side review.
- Full-stack optional matrix: Terminal, Index, Lens, HoloMap, Logistics, RenderCore, Ashfall, Nexus, Orbital, Stationfall, Blackbox, Convoy, and Armory should be checked both absent and present before release.

## Remaining Backbone Work

- Add RuntimeGuard budget hooks around repeated factory scans, duct/pipe traversal, scrubber effects, POI retries, Warden AI, particles, and Factory Command sync.
- Mirror richer Industrial world/team data into DataCore when present while preserving local fallback storage.
- Replace Logistics reflection with a stable optional Java contract once Logistics exposes that bridge API.
- Add WorldCore marker/hazard publication for Industrial POIs, scrubber safe zones, fluid leaks, and Warden arena state.
- Expand source facts to include generated loot-table evidence once Echo Index supports loot-table source expansion without forcing server reload scans.
- Add TutorialCore guide cards for early, mid, and late Industrial progression when TutorialCore service APIs stabilize.

## Manual Survival Route

1. Build `scrap_dynamo`, `copper_flux_duct`, `ore_grinder`, and `thermal_wrench`.
2. Generate Thermal Flux, process ore/scrap, and confirm heat and GUI state.
3. Build fluid processing and Industrial Scrubber support.
4. Locate Industrial POIs and recover schematics.
5. Form Scrap Processor, Plate Press, Industrial Assembly Line, Circuit Fabricator, Recipe Matrix Core, and Nexus Furnace Array.
6. Queue Hybrid Thermal Core and Core Key Assembly tasks.
7. Enable Logistics auto-restock with a connected depot/dock network.
8. Activate and defeat the Furnace Warden.
9. Verify Terminal, Index, Lens, HoloMap, Logistics, and route records agree on the same Industrial state.
