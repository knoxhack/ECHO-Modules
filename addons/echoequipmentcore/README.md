# ECHO: EquipmentCore

Gear slots, durability rules, upgrades, modifiers, and loadout validation contracts.

## Review Status

This is a Phase 4 ECHO platform roadmap module. Runtime implementation is present. The module provides `EquipmentService`, suit/rebreather/light/tool items, upgrade modifiers, durability, and hazard resistance integration.

## Public Contracts

- Provides: `equipment.slots`, `equipment.durability`, `equipment.upgrades`, `equipment.loadout_validation`
- Consumes: `armory.gear`, `combat.stats`, `foundation.tools`, `hazard.registry`, `hazard.resistance`
- MVP contracts: `gear_slot_contract`, `durability_rules`, `upgrade_modifiers`, `loadout_validation`

## Native Probe

The native entrypoint reports the runtime activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `registeredSlots`, `registeredItems`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: true`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes the equipment runtime surface: slots, stats, durability, upgrades, and loadout validation. Experience packs register equipment items and upgrades. Hazard resistance is contributed through the `IHazardResistanceProvider` integration.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
