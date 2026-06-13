# ECHO: DependencyDoctor

Human-readable explanations for broken module graphs, conflicts, version gaps, missing artifacts, and bad optional integrations.

## Review Status

This is a Phase 1 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `dependency.explanations`, `dependency.launch_report`, `dependency.conflict_diagnostics`, `dependency.artifact_diagnostics`
- Consumes: `module.graph`, `feature.graph`, `capability.registry`, `reports.contracts`
- MVP contracts: `why_pack_wont_launch_report`, `conflict_explanations`, `missing_artifact_report`, `optional_integration_diagnostics`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
