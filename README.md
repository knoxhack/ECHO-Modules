# ECHO Modules

Canonical source for all shared ECHO module code, module descriptors, generated per-module docs, and module release artifact contracts.

## Purpose

Canonical source for all shared ECHO module code, module descriptors, generated per-module docs, and module release artifact contracts.

## What Lives Here

`addons/<module>` source trees, `META-INF/echo.mod.json` descriptors, NeoForge metadata, module release generator scripts, schemas, and per-module README/docs.

## Release And Update Role

Owns per-module artifact releases consumed individually by the launcher and by Ashfall Native, NeoForge, and Standalone editions.

## Public Or Private

Public is recommended if the launcher or external developers must download module artifacts without GitHub authentication. Private is acceptable only for internal-only modules.

## Build And Dev Commands

Run commands from the repository root.

- `No build command is currently tracked in this repo.`

## Artifact Ownership

Each module release owns `.echo-addon`, `-neoforge.jar`, `-standalone.jar`, `-sources.jar`, embedded `META-INF/echo.mod.json`, NeoForge TOML where applicable, and `echo-addon-package.json` where applicable.

## Docs Index

- [docs/module-artifact-contract.md](docs/module-artifact-contract.md)
- [docs/module-docs-index.md](docs/module-docs-index.md)
- [schemas/module-release-manifest.schema.json](schemas/module-release-manifest.schema.json)
- [docs/ADDON_MATRIX.md](docs/ADDON_MATRIX.md)
- [docs/COMPATIBILITY_MATRIX.md](docs/COMPATIBILITY_MATRIX.md)
- [docs/MODULE_BUNDLES.md](docs/MODULE_BUNDLES.md)
- [docs/MODULE_STATUS_1.6.0.md](docs/MODULE_STATUS_1.6.0.md)
- [docs/NATIVE_ADAPTERCORE_GUIDE.md](docs/NATIVE_ADAPTERCORE_GUIDE.md)
- [docs/NATIVE_COMPATIBILITY_MATRIX.md](docs/NATIVE_COMPATIBILITY_MATRIX.md)
- [docs/NATIVE_MOD_AUTHOR_GUIDE.md](docs/NATIVE_MOD_AUTHOR_GUIDE.md)
- [docs/NATIVE_PORTING_GUIDE.md](docs/NATIVE_PORTING_GUIDE.md)
- [docs/NATIVE_RELEASE_PACKAGING_GUIDE.md](docs/NATIVE_RELEASE_PACKAGING_GUIDE.md)
- [docs/NATIVE_ROLLBACK_GUIDE.md](docs/NATIVE_ROLLBACK_GUIDE.md)
- [PUBLIC_ALPHA_RELEASE_STATUS.md](PUBLIC_ALPHA_RELEASE_STATUS.md)

## Related Repos

- [knoxhack/ECHO-Launcher](https://github.com/knoxhack/ECHO-Launcher)
- [knoxhack/ECHO-Ashfall-Native-Edition](https://github.com/knoxhack/ECHO-Ashfall-Native-Edition)
- [knoxhack/ECHO-Ashfall-NeoForge-Edition](https://github.com/knoxhack/ECHO-Ashfall-NeoForge-Edition)
- [knoxhack/ECHO-Ashfall-Standalone-Edition](https://github.com/knoxhack/ECHO-Ashfall-Standalone-Edition)
- [knoxhack/ECHO-Release-Index](https://github.com/knoxhack/ECHO-Release-Index)
- [knoxhack/ECHO-Native-Platform](https://github.com/knoxhack/ECHO-Native-Platform)
- [knoxhack/ECHO-Standalone-Runtime](https://github.com/knoxhack/ECHO-Standalone-Runtime)
- [knoxhack/ECHO-SDK](https://github.com/knoxhack/ECHO-SDK)
- [knoxhack/ECHO-Developer-Studio](https://github.com/knoxhack/ECHO-Developer-Studio)
- [knoxhack/ECHO-Addons-Studio](https://github.com/knoxhack/ECHO-Addons-Studio)
- [knoxhack/ECHO-Platform-Website](https://github.com/knoxhack/ECHO-Platform-Website)
