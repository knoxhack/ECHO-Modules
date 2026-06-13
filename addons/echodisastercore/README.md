# ECHO: DisasterCore

Blackouts, earthquakes, station failures, storm disasters, and recovery event contracts.

## Review Status

This is a Phase 5 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `disaster.events`, `disaster.recovery`, `disaster.station_failures`, `disaster.world_impacts`
- Consumes: `hazard.registry`, `weather.events`, `session.snapshot`, `world.regions`
- MVP contracts: `disaster_event_contract`, `recovery_event_contract`, `station_failure_contract`, `storm_disaster_contract`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
