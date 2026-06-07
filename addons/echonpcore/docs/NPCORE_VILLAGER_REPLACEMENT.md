# NPCore Villager Replacement

NPCore can replace vanilla villagers and wandering traders with `echonpcore:echo_npc` while preserving useful source metadata.

Config toggles:
- `replaceVanillaVillagers`
- `replaceWanderingTrader`
- `replaceZombieVillagers`
- `replaceOnSpawn`
- `replaceOnChunkLoad`
- `replaceOnFirstInteract`
- `preserveCustomName`
- `preserveProfession`
- `debugReplacementLogs`
- `conversionMode`

Supported first-pass conversion modes:
- `off`
- `convert_on_spawn`
- `convert_on_first_interact`

Replacement mapping files live under `data/<namespace>/villager_replacements/`.

```json
{
  "id": "example:default",
  "replace": {
    "minecraft:farmer": "example:reclaimer_farmer"
  },
  "entityTypes": {
    "minecraft:wandering_trader": "example:roaming_scavenger"
  }
}
```

Bundled profession mapping:
- farmer -> `reclaimer_farmer`
- librarian -> `data_archivist`
- cleric -> `field_medic`
- armorer -> `armor_tech`
- weaponsmith -> `ballistics_tech`
- toolsmith -> `salvage_engineer`
- cartographer -> `signal_analyst`
- none/nitwit -> `settlement_survivor`
- wandering_trader -> `roaming_scavenger`

Converted NPCs store source entity type, source profession, and a stable NPC id. In `1.0.0`, conversion records persist through DataCore world data when `echodatacore` is loaded; otherwise they use NPCore's in-memory fallback.
