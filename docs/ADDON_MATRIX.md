# Addon Matrix



| Addon | Required deps | Optional deps | Standalone features | ThemeCore | Terminal | Index | MissionCore | HoloMap | Ashfall |

| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

| Core | NeoForge, Minecraft | All ECHO modules | Services, diagnostics, provider registry | Tokens | Services | Services | Services | Services | Content host |

| NetCore | Core | RuntimeGuard | Packet/sync contracts | No UI | Debug pages | Docs | Objective hooks | Sync hooks | Optional |

| ThemeCore | Core, NetCore | Terminal, HoloMap, Lens, RenderCore, SoundCore | Theme registry and Default Dark fallback | Native | Skins | Docs | UI tokens | Map tokens | Ashfall theme |

| ScreenCore | Core, NetCore | ThemeCore, Terminal | EUI action/data/screen contracts and optional client bridges | Token bridge | Screen bridge | Docs | None | None | Optional UI framework |

| DataCore | Core, NetCore | Terminal, Command Center | Namespaced data and migrations | Viewer skin | Data viewer | Docs | Progression | Marker data | Optional content |

| WorldCore | Core, NetCore | HoloMap, Lens, RuntimeGuard | Regions, hazards, discoveries | Marker tokens | Pages | Docs | Objectives | Layers | Ashfall regions |

| MissionCore | Core, NetCore | Terminal, Index, DataCore | Missions, objectives, rewards | UI tokens | Feed/pages | Docs | Native | Markers | Ashfall campaign |

| Terminal | Core, NetCore | ThemeCore, Index, MissionCore, HoloMap | Generic command hub | Skins | Native | Search | Mission pages | Map widgets | Ashfall profile |

| SignalOS | Core, NetCore | Terminal, Index, MissionCore, ThemeCore | Chapter/content framework | Examples | Pages | Entries | Missions | None | Optional chapters |

| RenderCore | Core, NetCore | ThemeCore, RuntimeGuard | Visual profiles and previews | Visual tokens | Diagnostics | Docs | None | Markers | Ashfall visuals |

| Index | Core, NetCore | Terminal, ThemeCore, JEI/EMI | Standalone guidebook/index | Skins | Reference pages | Native | Unlocks | Marker docs | Ashfall archive |

| Lens | Core, NetCore | Index, HoloMap, MissionCore, ThemeCore | Scanner HUD and scan providers | Scan skin | Scan page | Unlocks | Scan objectives | Markers | Ashfall profile |

| HoloMap | Core, NetCore | WorldCore, MissionCore, ThemeCore, RuntimeGuard | Standalone map | Layer skin | Widget/page | Docs | Objectives | Native | Ashfall layers |

| PlayerCore | Core, NetCore | WorldCore, HoloMap, DataCore | Homes, back, spawn, RTP | UI skin | Travel page | Docs | Objectives | Waypoints | Optional restrictions |

| TutorialCore | Core, NetCore | Terminal, Index, ThemeCore | Data-driven tutorials | Cards | Tutorial page | Docs | Objectives | None | Ashfall tutorials |

| SoundCore | Core, NetCore | ThemeCore, RuntimeGuard, WeatherCore | Music and ambience rules | Sound profiles | Preview page | Docs | Stingers | Zones | Ashfall music |

| WeatherCore | Core, NetCore | WorldCore, HoloMap, SoundCore, RuntimeGuard | Generic weather events | Forecast skin | Forecast widget | Hazard docs | Objectives | Storm layer | Ashfall weather |

| RelicTech | Core, NetCore | Lens, PowerGrid, Index, Terminal | Relics, research, repair | UI skin | Research page | Relic pages | Objectives | Markers | Ashfall relics |

| PowerGrid | Core, NetCore | Terminal, HoloMap, RuntimeGuard | Energy grid and machines | Overlay skin | Dashboard | Docs | Objectives | Grid overlay | Ashfall grids |

| Armory | Core, NetCore | PowerGrid, Logistics, Index, Terminal | Gear, modules, loadouts | Bench skin | Loadouts | Gear docs | Objectives | None | Ashfall gear |

| MultiblockCore | Core, NetCore | Lens, PowerGrid, ThemeCore, JEI/EMI | Data-driven multiblocks | Overlays | Pages | Docs | Objectives | None | Examples only |

| Blockworks | Core, NetCore | Index, ThemeCore | Generic block palettes | Palette docs | None | Guide | None | None | Ashfall palette |

| TextureForge | Core, NetCore | Command Center, ThemeCore | Texture audit commands, source-sheet manifests, and refresh reports | Theme asset audit | Report links | Docs | None | None | Asset pipeline support |

| Wiki | Core, NetCore, ScreenCore | Index, Terminal, ThemeCore | Wiki article/content registry and in-game documentation surfaces | Article skin | Wiki page | Cross-links | None | None | Ashfall article packs |

| Logistics Network | Core, NetCore | Terminal, HoloMap, Convoy, RuntimeGuard | Storage, requests, depots | UI skin | Dashboard | Docs | Contracts | Depots | Ashfall depots |

| Convoy Protocol | Core, NetCore | Logistics, HoloMap, PowerGrid, MissionCore | Vehicles and routes | UI skin | Ops page | Docs | Contracts | Routes | Ashfall field ops |

| Reclamation | Core, NetCore | WeatherCore, Logistics, ThemeCore | Ecology and farming restoration | Styles | Dashboard | Docs | Milestones | Zones | Ashfall recovery |

| Industrial Nexus | Core, NetCore | MultiblockCore, PowerGrid, Logistics, Terminal | Machines and factories | Panel skin | Dashboard | Docs | Work orders | Sites | Ashfall factories |

| Orbital Remnants | Core, NetCore | HoloMap, Terminal, SoundCore | Orbital events and launch chains | UI skin | Pages | Docs | Objectives | Scans | Ashfall route mode |

| Stationfall | Core, NetCore | Orbital Remnants, Terminal, PowerGrid, SoundCore | Standalone horror station route | Horror skin | Missions | Crew logs | Objectives | Station map | Optional handoff |

| Nexus Protocol | Core, NetCore | HoloMap, Terminal, SoundCore, ThemeCore | Anomaly/corruption field system | Corruption UI | Page | Docs | Outcomes | Risk map | Ashfall endgame |

| Blackbox Protocol | Core, NetCore | Stationfall, Nexus, Terminal, Lens, SoundCore | Mystery/finale framework | UI skin | Board | Archives | Directives | Evidence | Ashfall finale |

| Command Center | Node/Vite app | RuntimeGuard, ThemeCore | Modpack/admin dashboard | Selector | Optional links | Docs | Reports | Reports | Ashfall report |



## 1.3.5 Public Launch Metadata



The authoritative launcher-facing module matrix now lives in `metadata/modules/`. Use `metadata/modules/index.json` for machine-readable status and `docs/release_pages/` for public page drafts. Ashfall remains Official ECHO Pack #1 and is described in `metadata/official_packs/ashfall.json`.

## 1.3.5 Public Launch Launcher Metadata Matrix

| Module | Category | Status | Required deps | Optional integrations | Standalone | Ashfall required | Release page |
|---|---|---:|---|---|---:|---:|---|
| `echoagriculturereclamation` | Survival | Beta | echocore, echonetcore | echoweathercore, echopowergrid, echologisticsnetwork, echoindex, echolens, echoterminal | Yes | No | docs/release_pages/echoagriculturereclamation.md |
| `echoarmory` | Combat | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echologisticsnetwork, echopowergrid | Yes | No | docs/release_pages/echoarmory.md |
| `echoashfallprotocol` | Official Pack | Beta | echocore, echonetcore | echoterminal, echoindex, echomissioncore, echoholomap, echolens, echothemecore, echoworldcore, echosoundcore, echodatacore, echoruntimeguard | No | Yes | docs/release_pages/echoashfallprotocol.md |
| `echoblackboxprotocol` | Story | Beta | echocore, echonetcore | echostationfall, echonexusprotocol, echoterminal, echoindex, echosoundcore, echomissioncore | Yes | No | docs/release_pages/echoblackboxprotocol.md |
| `echoblockworks` | Content | Beta | echocore, echonetcore | echoindex, echoterminal, echothemecore | Yes | No | docs/release_pages/echoblockworks.md |
| `echoconvoyprotocol` | Tech | Beta | echocore, echonetcore | echoholomap, echologisticsnetwork, echopowergrid, echoterminal, echoindex | Yes | No | docs/release_pages/echoconvoyprotocol.md |
| `echocore` | Foundation | Stable | none | echonetcore, echodatacore, echoruntimeguard | No | No | docs/release_pages/echocore.md |
| `echodatacore` | Foundation | Stable | echocore, echonetcore | echoruntimeguard | No | No | docs/release_pages/echodatacore.md |
| `echograves` | Utility | Internal | none | echorecovery | No | No | docs/release_pages/echograves.md |
| `echoholomap` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echomissioncore, echolens, echoworldcore, echoruntimeguard | Yes | No | docs/release_pages/echoholomap.md |
| `echoindex` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echomissioncore, echolens, echoholomap, echowiki | Yes | No | docs/release_pages/echoindex.md |
| `echoindustrialnexus` | Tech | Beta | echocore, echonetcore | echopowergrid, echomultiblockcore, echologisticsnetwork, echoindex, echolens, echoterminal | Yes | No | docs/release_pages/echoindustrialnexus.md |
| `echolens` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoindex, echomissioncore, echoholomap, echodatacore | Yes | No | docs/release_pages/echolens.md |
| `echologisticsnetwork` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoholomap, echoconvoyprotocol, echopowergrid, echoarmory | Yes | No | docs/release_pages/echologisticsnetwork.md |
| `echomissioncore` | Foundation | Beta | echocore, echonetcore | echodatacore, echoterminal, echoindex, echotutorialcore | Yes | No | docs/release_pages/echomissioncore.md |
| `echomodpackcommandcenter` | Developer Tool | Beta | none | echocore, echotextureforge | Yes | No | docs/release_pages/echomodpackcommandcenter.md |
| `echomultiblockcore` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echoholomap, echopowergrid | Yes | No | docs/release_pages/echomultiblockcore.md |
| `echonetcore` | Foundation | Stable | echocore, echonetcore | echoruntimeguard | No | No | docs/release_pages/echonetcore.md |
| `echonexusprotocol` | Story | Beta | echocore, echonetcore | echoholomap, echolens, echosoundcore, echomissioncore, echoterminal, echoindex | Yes | No | docs/release_pages/echonexusprotocol.md |
| `echoorbitalremnants` | Story | Beta | echocore, echonetcore | echoholomap, echosoundcore, echoterminal, echoindex, echomissioncore | Yes | No | docs/release_pages/echoorbitalremnants.md |
| `echoplayercore` | Utility | Beta | echocore, echonetcore | echoworldcore, echoholomap, echoruntimeguard | Yes | No | docs/release_pages/echoplayercore.md |
| `echopowergrid` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoholomap, echomultiblockcore, echoruntimeguard | Yes | No | docs/release_pages/echopowergrid.md |
| `echorecovery` | Survival | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echoholomap, echolens, echoruntimeguard, echorendercore | Yes | No | docs/release_pages/echorecovery.md |
| `echorelictech` | Story | Experimental | echocore, echonetcore | echoindex, echolens, echomissioncore, echoterminal, echonexusprotocol | Yes | No | docs/release_pages/echorelictech.md |
| `echorendercore` | Developer Tool | Beta | echocore, echonetcore | echothemecore, echoscreencore, echoruntimeguard | No | No | docs/release_pages/echorendercore.md |
| `echoruntimeguard` | Foundation | Stable | echocore, echonetcore | echodatacore | No | No | docs/release_pages/echoruntimeguard.md |
| `echoscreencore` | UI/UX | Beta | echocore, echonetcore | echothemecore | Yes | No | docs/release_pages/echoscreencore.md |
| `echosoundcore` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echoweathercore, echostationfall | Yes | No | docs/release_pages/echosoundcore.md |
| `echostationfall` | Story | Beta | echocore, echonetcore | echoterminal, echoindex, echosoundcore, echoholomap, echoblackboxprotocol | Yes | No | docs/release_pages/echostationfall.md |
| `echoterminal` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoindex, echomissioncore, echoholomap, echolens, signalos, echodatacore | Yes | No | docs/release_pages/echoterminal.md |
| `echotextureforge` | Developer Tool | Experimental | echocore, echonetcore | echothemecore, echoscreencore, echomodpackcommandcenter | No | No | docs/release_pages/echotextureforge.md |
| `echothemecore` | UI/UX | Stable | echocore, echonetcore | echoscreencore | Yes | No | docs/release_pages/echothemecore.md |
| `echotutorialcore` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echomissioncore | Yes | No | docs/release_pages/echotutorialcore.md |
| `echoweathercore` | World | Beta | echocore, echonetcore | echoworldcore, echoholomap, echosoundcore, echoterminal, echoindex | Yes | No | docs/release_pages/echoweathercore.md |
| `echowiki` | UI/UX | Experimental | echocore, echonetcore | echoscreencore, echothemecore, echoindex, echoterminal | Yes | No | docs/release_pages/echowiki.md |
| `echoworldcore` | World | Beta | echocore, echonetcore | echoholomap, echoindex, echolens, echodatacore, echoruntimeguard | Yes | No | docs/release_pages/echoworldcore.md |
| `signalos` | Story | Beta | echocore, echonetcore | echoterminal, echoindex, echomissioncore, echodatacore | Yes | No | docs/release_pages/signalos.md |
| `signalosexample` | Developer Tool | Internal | echocore, echonetcore, signalos | echoterminal, echoindex | No | No | docs/release_pages/signalosexample.md |
