
# ECHO Foundations Architecture

## Master Goal

Create ECHO Foundations as the shared survival/content backbone, then refactor
Openlands, Ashfall, and Arcana Division so each experience owns only its unique
fantasy, systems, and pressure.

## Locked Rule

Foundations owns baseline survival. Experiences consume Foundation contracts and
may extend them, but they do not re-own baseline materials, tools, starter
stations, starter loot, spawn safety, first-hour survival, or shared creature
roles.

## Dependency Rule

Experience modules depend on Foundation modules. Experience modules never depend
on each other.

~~~mermaid
flowchart LR
  F["ECHO Foundations"] --> O["Openlands"]
  F --> A["Ashfall"]
  F --> C["Arcana Division"]
  O -. forbidden .- A
  A -. forbidden .- C
  C -. forbidden .- O
~~~

## Locked Foundation Modules

| Module | Owns |
| --- | --- |
| echofoundationcore | Ownership rules, aliases, legal identity, release/dependency contracts |
| echomaterialcore | Generic materials, generic blocks, material tags, metal progression |
| echotoolcore | Generic tools, tool roles, shared tool progression |
| echostationcore | Generic stations, storage, shared recipe surfaces |
| echoworldstarter | Spawn safety, starter route, shelter score, first-hour items |
| echocommonloot | Generic loot pools, starter caches, block drops |
| echocreatureroles | Shared creature pressure/spawn role taxonomy |

## Experience Ownership

Openlands owns calm exploration, homesteading, old roads, waystones, map table,
regional rubbings, route bindings, and Openlands biomes.

Ashfall owns volcanic survival pressure: storms, heat, ash exposure, scarcity,
shelters, filtration, atmospheric scrubbers, distillation, black rain, and
Ashfall-specific hazards.

Arcana Division owns magical research, rituals, familiars, curses, rifts,
anomaly containment, Arcana stations, Arcana creatures, and Arcana loot rules.

## Save Migration

Old IDs do not disappear abruptly. Foundation uses explicit aliases from the
migration table. New recipes, tags, display names, docs, and UI text must point
at the new Foundation IDs.

## Launcher Contract

The launcher must install all seven Foundation modules with any experience. It
must validate dependency presence before launch and repair missing Foundation
modules before attempting to load an experience pack.
