# ECHO: HazardCore

Generic hazards for heat, cold, radiation, oxygen, pressure, corruption, disease, and storm exposure.

## Review Status

This is a Phase 4 ECHO platform roadmap module. Runtime implementation is present. The module provides `HazardService`, built-in hazard types, exposure sources, resistance provider integration, and player tick damage/status application. Additional hazards and polish may be added by experience packs.

## Public Contracts

- Provides: `hazard.registry`, `hazard.exposure`, `hazard.resistance`, `hazard.world_hooks`
- Consumes: `status.exposure`, `health.damage_model`, `weather.events`, `world.hazards`
- MVP contracts: `hazard_registry`, `exposure_contract`, `resistance_contract`, `world_hazard_hooks`

## Native Probe

The native entrypoint reports the runtime activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `registeredHazardCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: true`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes the hazard runtime surface: registry, exposure evaluation, resistance aggregation, and world hooks. Experience packs register additional hazard sources and consume final exposure through `HazardService`. Destructive writes and player damage application are gated by the tick handler and respect difficulty scaling.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
