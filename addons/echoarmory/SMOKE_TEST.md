# ECHO: Armory Smoke Tests

Run these in a survival world with ECHO Core and Armory loaded. Terminal and Logistics checks require their optional sibling mods.

## First Survival Loop

1. Craft `armory_alloy_plate`.
2. Craft and place `armory_bench`.
3. Craft `alloy_sword` or `thermal_chestplate`.
4. Craft and place `module_upgrade_table`.
5. Craft `stability_rune`, `frost_core`, or `gas_mask_filter`.
6. Insert gear in slot 1 and module in slot 2, then apply.
7. Confirm the module item is consumed only on success and the gear tooltip shows installed module data.

## Energy Loop

1. Craft an energy-capable item such as `frost_blade` or `veil_bow`.
2. Spend energy through combat or ranged use.
3. Place `energy_core_charging_station`.
4. Insert gear plus `veil_crystal` or `resonance_shard` in AUX.
5. Apply and confirm one fuel item is consumed and stored energy refills to capacity.

## Ranged Safety Loop

1. Equip `veil_bow` with `ammo_crystals` in inventory.
2. Use it with no target in front of the player.
3. Confirm no ammo or energy is consumed.
4. Aim at a valid hostile target and fire.
5. Confirm one ammo crystal is consumed before stored energy and a veil-arrow projectile appears.
6. Repeat with `energy_rifle` and `sigil_chakram` to confirm energy-bolt and sigil-chakram projectile behavior.

## Station Preview Loop

1. Open each Armory station screen.
2. Insert valid and invalid gear/module/AUX combinations.
3. Confirm the preview line reports `READY` or `BLOCKED`, fuel count, and the station-specific action hint before `APPLY`.
4. Press `APPLY` on a blocked operation and confirm gear, module, fuel, ammo, and energy counts are unchanged.

## Faction Depot Loop

1. Inspect depot offers for `veil_sabre`, `energy_rifle`, `construct_gauntlet`, `veil_resistant_helm`, `drone_leggings`, or `construct_harness`.
2. Try to use/equip one while reputation is missing.
3. Confirm the item is not deleted and reports the lock requirement.
4. Grant the matching faction reputation and repeat.
5. Confirm use/equip/protection now applies.

## Terminal Armory Loop

1. Open `SYSTEM > Armory`.
2. Click a mission kit row, augment row, and boss row.
3. Confirm the selected mission kit reports `READY`, `STAGED`, `MISSING`, or `LOCKED` with a first blocker.
4. Use `EQUIP`, `INSTALL`, `RECHARGE`, and `PREVIEW`.
5. Confirm actions report missing, locked, duplicate, incompatible, no fuel, or readiness status without consuming items on failure.

## Route Kit Readiness Loop

1. Equip `alloy_sword` and `thermal_chestplate`.
2. Install `gas_mask_filter` into the chestplate.
3. Open `SYSTEM > Armory` and select `Toxic Breach Kit`.
4. Confirm Toxic Breach reports ready once toxic protection reaches the required threshold and shows score, route family, energy/ammo state, and next action.
5. Select `Fracture Guardian Kit` without the required reputation.
6. Confirm it reports locked instead of requesting or equipping gated gear.
7. Place required kit gear in a nearby `weapon_rack` or `armor_stand` and confirm readiness can report the item as staged.

## Logistics Delivery Loop

1. Load Logistics Network with an eligible dispatch endpoint.
2. Select an Armory loadout with a Logistics preset.
3. Click `LOGISTICS`.
4. Confirm dispatch is queued or a clear unavailable/blocked message is shown.
5. Confirm successful dispatch advances the Armory route-kit MissionCore side op when MissionCore is loaded.

## Reload Persistence

1. Install a module and partially spend energy.
2. Save and reload the world.
3. Confirm installed modules, tier, stance, cosmetic trim, instability, and energy state remain on the gear stack.
4. Place a station with inventory, save/reload, and confirm stored gear/resources remain.

Automated coverage: `stack_component_round_trip_persists_armory_state` and `station_inventory_round_trip_persists_items_and_gear_state` exercise the same persistence contract through the GameTest server.

## Safety Proof Coverage

The GameTest server also covers protected station slot rules, active-operation extraction blocking, selected Terminal recharge/preview/logistics action paths, duplicate/incompatible/full module install rejection, no-target ranged shots, ammo-before-energy ranged consumption, projectile spawn safety, faction lock safety, and fuel-costed recharge.
Armory 1.3.0 adds GameTest coverage for v2 content contracts, extended readiness reports, station preview consistency, staged rack readiness, projectile spawn/resource safety, required-protection parsing, readiness state transitions, and route-kit MissionCore content.

## Release Commands

```powershell
.\gradlew.bat :echoarmory:build
.\gradlew.bat validateEchoResources
.\gradlew.bat validateEchoGameplayData
.\gradlew.bat --no-configuration-cache :echoarmory:runGameTestServer --warning-mode all
.\gradlew.bat -PechoAddonSet=all buildEchoWorkspace --warning-mode all
.\gradlew.bat --no-configuration-cache validateReleaseArtifacts printReleaseManifest
```
