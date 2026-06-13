# ECHO: MigrationCore

Versioned save, data-key, renamed-ID, removed-module, deprecated-content, and rollback compatibility migration contracts.

## Review Status

This is a Phase 1 ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: `migration.manifest`, `migration.dry_run`, `migration.rollback_report`, `migration.id_aliases`
- Consumes: `data.contracts`, `schema.registry`, `validation.pack`
- MVP contracts: `migration_manifest`, `dry_run_report`, `rollback_compatibility_report`, `renamed_id_map`, `removed_module_notes`

## Native Probe

The native entrypoint reports the standard roadmap activation map: `activated`, `activationStage`, `adapterCoreUsed`, `nativeAdapterCodeExecuted`, `moduleId`, `packId`, `registeredFeatureContracts`, `logicalRegistrationCount`, `adapterDomains`, `runtimeTargets`, `referenceProbe`, `registryMutated: false`, and `transformsPerformed: false`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
