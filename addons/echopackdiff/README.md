# ECHO: PackDiff

Explains differences between pack or module versions across gameplay, dependencies, migrations, and renamed content.

## Review Status

This is a Phase 2 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `packdiff.json`, `packdiff.markdown`, `packdiff.changelog`
- Consumes: `migration.manifest`, `metadata.manifest`, `reports.contracts`
- MVP contracts: `markdown_changelog`, `json_changelog`, `dependency_diff`, `migration_diff`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
