# ECHO: PolicyCore

Trust, permissions, write-action approval, server-rule, blocked-module, content-flag, and creator-governance policy contracts.

## Review Status

This is a Phase 1 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `policy.manifest`, `policy.validation`, `policy.runtime_hooks`, `policy.trust_metadata`
- Consumes: `validation.pack`, `reports.contracts`, `metadata.manifest`
- MVP contracts: `policy_manifest`, `launcher_validation`, `studio_validation`, `runtime_enforcement_hooks`, `blocked_module_rules`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
