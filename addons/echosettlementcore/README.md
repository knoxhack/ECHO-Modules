# ECHO: SettlementCore

Bases, shelters, NPC jobs, storage needs, defense score, comfort, and logistics request contracts.

## Review Status

This is a Phase 4 ECHO platform roadmap module. Runtime implementation is present. The module provides `SettlementService`, habitat blocks, block entities, NPC jobs, logistics requests, and hazard resistance integration.

## Public Contracts

- Provides: `settlement.registry`, `settlement.jobs`, `settlement.defense_score`, `settlement.logistics_requests`
- Consumes: `basegrid.claims`, `npc.profiles`, `logistics.routing`, `world.regions`, `hazard.registry`, `hazard.resistance`
- MVP contracts: `settlement_snapshot`, `npc_job_contract`, `defense_score_contract`, `logistics_request_contract`

## Native Probe

The native entrypoint reports the runtime activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `registeredBlocks`, `registeredJobs`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: true`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes the settlement runtime surface: habitat registry, job definitions, defense scoring, and logistics requests. Experience packs define habitat structures and consume settlement safety through the `IHazardResistanceProvider` integration.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
