# ECHO: Armory 1.3.0 Release Notes

ECHO: Armory 1.3.0 turns the addon into the complete combat-prep layer for the ECHO stack. Gear identity, station workflows, projectile combat, route-kit readiness, Core diagnostics, Terminal receipts, Logistics dispatch, MissionCore side ops, and optional sibling hooks now share one Armory report model.

## Player Path

Players craft and tune Armory gear, install modules, recharge energy, stage route kits on racks and stands, then use Terminal or Loadout Terminal surfaces to select the best route kit. Readiness reports now include state, score, route family, protections, staged/missing items, locks, energy/ammo state, synergies, and the next action.

Ranged Armory weapons now use projectile entities: energy bolts, veil arrows, and sigil chakrams. The server only spends ammo or energy after accepting a valid shot, and failed fire attempts leave ammo, energy, and gear untouched.

## Datapack Contract

Armory v2 datapack roots are:

- `data/<namespace>/echoarmory/gear/`
- `data/<namespace>/echoarmory/modules/`
- `data/<namespace>/echoarmory/loadouts/`
- `data/<namespace>/echoarmory/station_recipes/`
- `data/<namespace>/echoarmory/firing_modes/`
- `data/<namespace>/echoarmory/faction_unlocks/`
- `data/<namespace>/echoarmory/boss_recommendations/`
- `data/<namespace>/echoarmory/route_profiles/`

Bundled loadouts now use `requiredProtections`. Beta `minProtection` is no longer used by bundled data; no-schema legacy loadouts still fail soft by mapping it to fracture when `requiredProtections` is absent.

## Release Safety

- ECHO Core remains the only required dependency.
- Optional sibling integrations are guarded and no-op safely when absent.
- Existing ItemStack components and station inventories are preserved.
- Failed install, equip, recharge, dispatch, bind, fire, and station operations do not consume items, ammo, energy, or fuel.
- No new mobs or companion entities are added; 1.3.0 adds projectiles only.

## Known Limits

- Projectile visuals currently rely on entity tracking plus particles rather than bespoke RenderCore art.
- Full-stack gameplay-data validation can still be blocked by known non-Armory source-token expectations.
