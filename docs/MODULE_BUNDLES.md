# Module Bundles

Bundles are curated module groups for Launcher browsing and pack-builder shortcuts. They do not create new addons, and they do not make Ashfall a dependency for reusable modules.

## Platform Defaults

- ECHO is the ecosystem.
- ECHO Launcher is the platform.
- Foundation modules are shared backbone contracts consumed by official packs.
- Creator tooling uses schemas, examples, generators, validators, and docs before deeper editor automation.
- Roadmap modules start contract-first so Launcher, Studio, and validation can reason about them before gameplay systems mutate runtime state.

## Curated Bundles

| ID | Name | Required | Optional | Best For |
| --- | --- | ---: | ---: | --- |
| foundation | Foundation Bundle | 10 | 0 | Baseline survival contracts, Official pack roots, Creator starter packs |
| openlands_official | Openlands Official Bundle | 11 | 12 | Openlands, Calm exploration, Homesteading |
| sky_relay_official | Sky Relay Official Bundle | 5 | 10 | Sky Relay, Storm routes, Restoration loops |
| arcana_division | Arcana Division Bundle | 12 | 13 | Arcana Division, Magic research, Anomaly containment |
| creator_tooling | Creator Tooling Bundle | 6 | 10 | Creator packs, Studio workflows, Release QA |

## Related Docs

- [ECHO platform roadmap](ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](module-artifact-contract.md)
