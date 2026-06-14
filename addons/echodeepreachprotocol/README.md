# ECHO: Deep Reach Protocol

Pressure-suit survival pack root module for the ECHO platform.

Deep Reach drops players into the flooded caverns, sunken ruins, and abyssal trenches beneath the ECHO world. Oxygen, depth pressure, suit integrity, and light discipline replace surface survival as the core loop.

## What this module owns

- `deepreach_survival` game mode definition and pack-root contract.
- Depth-zone worldgen scaffolding (shallows, twilight trench, abyssal plain, geothermal rifts).
- Pressure-suit, habitat, vehicle, and mission content placeholders.
- Official pack metadata, native surface description, and AdapterCore domain registration.

## What this module does not own

Foundation survival primitives (materials, tools, stations, creature roles, loot, first-hour spawn safety) are consumed from the Foundation modules. Hazard, equipment, settlement, mission, expedition, ruin, vehicle, and season systems are consumed through their respective core modules.

## Current state

This is a scaffold. Registries exist but contain no gameplay content yet. Data-driven depth zones, suits, habitats, vehicles, and campaign missions will land in later implementation work.
