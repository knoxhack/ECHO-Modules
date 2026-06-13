# ECHO: TelemetryCore

Privacy-safe local and session metrics for crashes, install health, progression checkpoints, and module load failures.

## Review Status

This is a Phase 3 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `telemetry.local_bundle`, `telemetry.privacy_policy`, `telemetry.qa_metrics`
- Consumes: `policy.manifest`, `reports.contracts`, `playtest.session_proofs`
- MVP contracts: `opt_in_local_telemetry_bundle`, `privacy_safe_metrics`, `qa_support_export`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
