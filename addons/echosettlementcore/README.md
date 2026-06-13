# ECHO: SettlementCore

Bases, shelters, NPC jobs, storage needs, defense score, comfort, and logistics request contracts.

## Review Status

This is a Phase 4 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `settlement.registry`, `settlement.jobs`, `settlement.defense_score`, `settlement.logistics_requests`
- Consumes: `basegrid.claims`, `npc.profiles`, `logistics.routes`, `world.regions`
- MVP contracts: `settlement_snapshot`, `npc_job_contract`, `defense_score_contract`, `logistics_request_contract`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
