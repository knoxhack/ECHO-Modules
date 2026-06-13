# ECHO: PlaytestCore

Automated gameplay evidence runner for release readiness, session proof, save/load, install, update, repair, and rollback checks.

## Review Status

This is a Phase 1 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `playtest.scenarios`, `playtest.evidence_runner`, `playtest.release_readiness`, `playtest.session_proofs`
- Consumes: `reports.contracts`, `release.readiness`, `validation.pack`, `module.graph`, `runtime.guard`
- MVP contracts: `json_scenario_definitions`, `first_30_minutes_run`, `two_hour_run`, `completion_path`, `save_load_proof`, `crash_free_session`, `install_update_repair_rollback_proof`, `release_readiness_report`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
