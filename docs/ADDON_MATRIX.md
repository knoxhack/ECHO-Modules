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



The authoritative launcher-facing module matrix now lives in `metadata/modules/index.json`, generated from addon descriptors. Use each module descriptor for public metadata source-of-truth. Ashfall remains Official ECHO Pack #1 and is described in `metadata/official_packs/ashfall.json`.

## 1.3.5 Public Launch Launcher Metadata Matrix

| Module | Category | Status | Required deps | Optional integrations | Standalone | Ashfall required | Descriptor |
|---|---|---:|---|---|---:|---:|---|
| `echoagriculturereclamation` | Survival | Beta | echocore, echonetcore | echoweathercore, echopowergrid, echologisticsnetwork, echoindex, echolens, echoterminal | Yes | No | addons/echoagriculturereclamation/src/main/resources/META-INF/echo.mod.json |
| `echoarmory` | Combat | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echologisticsnetwork, echopowergrid | Yes | No | addons/echoarmory/src/main/resources/META-INF/echo.mod.json |
| `echoashfallprotocol` | Official Pack | Beta | echocore, echonetcore | echoterminal, echoindex, echomissioncore, echoholomap, echolens, echothemecore, echoworldcore, echosoundcore, echodatacore, echoruntimeguard | No | Yes | addons/echoashfallprotocol/src/main/resources/META-INF/echo.mod.json |
| `echoblackboxprotocol` | Story | Beta | echocore, echonetcore | echostationfall, echonexusprotocol, echoterminal, echoindex, echosoundcore, echomissioncore | Yes | No | addons/echoblackboxprotocol/src/main/resources/META-INF/echo.mod.json |
| `echoblockworks` | Content | Beta | echocore, echonetcore | echoindex, echoterminal, echothemecore | Yes | No | addons/echoblockworks/src/main/resources/META-INF/echo.mod.json |
| `echoconvoyprotocol` | Tech | Beta | echocore, echonetcore | echoholomap, echologisticsnetwork, echopowergrid, echoterminal, echoindex | Yes | No | addons/echoconvoyprotocol/src/main/resources/META-INF/echo.mod.json |
| `echocore` | Foundation | Stable | none | echonetcore, echodatacore, echoruntimeguard | No | No | addons/echocore/src/main/resources/META-INF/echo.mod.json |
| `echodatacore` | Foundation | Stable | echocore, echonetcore | echoruntimeguard | No | No | addons/echodatacore/src/main/resources/META-INF/echo.mod.json |
| `echograves` | Utility | Internal | none | echorecovery | No | No | addons/echograves/src/main/resources/META-INF/echo.mod.json |
| `echoholomap` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echomissioncore, echolens, echoworldcore, echoruntimeguard | Yes | No | addons/echoholomap/src/main/resources/META-INF/echo.mod.json |
| `echoindex` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echomissioncore, echolens, echoholomap, echowiki | Yes | No | addons/echoindex/src/main/resources/META-INF/echo.mod.json |
| `echoindustrialnexus` | Tech | Beta | echocore, echonetcore | echopowergrid, echomultiblockcore, echologisticsnetwork, echoindex, echolens, echoterminal | Yes | No | addons/echoindustrialnexus/src/main/resources/META-INF/echo.mod.json |
| `echolens` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoindex, echomissioncore, echoholomap, echodatacore | Yes | No | addons/echolens/src/main/resources/META-INF/echo.mod.json |
| `echologisticsnetwork` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoholomap, echoconvoyprotocol, echopowergrid, echoarmory | Yes | No | addons/echologisticsnetwork/src/main/resources/META-INF/echo.mod.json |
| `echomissioncore` | Foundation | Beta | echocore, echonetcore | echodatacore, echoterminal, echoindex, echotutorialcore | Yes | No | addons/echomissioncore/src/main/resources/META-INF/echo.mod.json |
| `echomultiblockcore` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echoholomap, echopowergrid | Yes | No | addons/echomultiblockcore/src/main/resources/META-INF/echo.mod.json |
| `echonetcore` | Foundation | Stable | echocore, echonetcore | echoruntimeguard | No | No | addons/echonetcore/src/main/resources/META-INF/echo.mod.json |
| `echonexusprotocol` | Story | Beta | echocore, echonetcore | echoholomap, echolens, echosoundcore, echomissioncore, echoterminal, echoindex | Yes | No | addons/echonexusprotocol/src/main/resources/META-INF/echo.mod.json |
| `echoorbitalremnants` | Story | Beta | echocore, echonetcore | echoholomap, echosoundcore, echoterminal, echoindex, echomissioncore | Yes | No | addons/echoorbitalremnants/src/main/resources/META-INF/echo.mod.json |
| `echoplayercore` | Utility | Beta | echocore, echonetcore | echoworldcore, echoholomap, echoruntimeguard | Yes | No | addons/echoplayercore/src/main/resources/META-INF/echo.mod.json |
| `echopowergrid` | Tech | Beta | echocore, echonetcore | echothemecore, echoterminal, echoholomap, echomultiblockcore, echoruntimeguard | Yes | No | addons/echopowergrid/src/main/resources/META-INF/echo.mod.json |
| `echorecovery` | Survival | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echoholomap, echolens, echoruntimeguard, echorendercore | Yes | No | addons/echorecovery/src/main/resources/META-INF/echo.mod.json |
| `echorelictech` | Story | Experimental | echocore, echonetcore | echoindex, echolens, echomissioncore, echoterminal, echonexusprotocol | Yes | No | addons/echorelictech/src/main/resources/META-INF/echo.mod.json |
| `echorendercore` | Developer Tool | Beta | echocore, echonetcore | echothemecore, echoscreencore, echoruntimeguard | No | No | addons/echorendercore/src/main/resources/META-INF/echo.mod.json |
| `echoruntimeguard` | Foundation | Stable | echocore, echonetcore | echodatacore | No | No | addons/echoruntimeguard/src/main/resources/META-INF/echo.mod.json |
| `echoscreencore` | UI/UX | Beta | echocore, echonetcore | echothemecore | Yes | No | addons/echoscreencore/src/main/resources/META-INF/echo.mod.json |
| `echosoundcore` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echoweathercore, echostationfall | Yes | No | addons/echosoundcore/src/main/resources/META-INF/echo.mod.json |
| `echostationfall` | Story | Beta | echocore, echonetcore | echoterminal, echoindex, echosoundcore, echoholomap, echoblackboxprotocol | Yes | No | addons/echostationfall/src/main/resources/META-INF/echo.mod.json |
| `echoterminal` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoindex, echomissioncore, echoholomap, echolens, echosignalos, echodatacore | Yes | No | addons/echoterminal/src/main/resources/META-INF/echo.mod.json |
| `echotextureforge` | Developer Tool | Experimental | echocore, echonetcore | echothemecore, echoscreencore | No | No | addons/echotextureforge/src/main/resources/META-INF/echo.mod.json |
| `echothemecore` | UI/UX | Stable | echocore, echonetcore | echoscreencore | Yes | No | addons/echothemecore/src/main/resources/META-INF/echo.mod.json |
| `echotutorialcore` | UI/UX | Beta | echocore, echonetcore | echothemecore, echoterminal, echoindex, echomissioncore | Yes | No | addons/echotutorialcore/src/main/resources/META-INF/echo.mod.json |
| `echoweathercore` | World | Beta | echocore, echonetcore | echoworldcore, echoholomap, echosoundcore, echoterminal, echoindex | Yes | No | addons/echoweathercore/src/main/resources/META-INF/echo.mod.json |
| `echowiki` | UI/UX | Experimental | echocore, echonetcore | echoscreencore, echothemecore, echoindex, echoterminal | Yes | No | addons/echowiki/src/main/resources/META-INF/echo.mod.json |
| `echoworldcore` | World | Beta | echocore, echonetcore | echoholomap, echoindex, echolens, echodatacore, echoruntimeguard | Yes | No | addons/echoworldcore/src/main/resources/META-INF/echo.mod.json |
| `echosignalos` | Story | Beta | echocore, echonetcore | echoterminal, echoindex, echomissioncore, echodatacore | Yes | No | addons/echosignalos/src/main/resources/META-INF/echo.mod.json |
| `signalosexample` | Developer Tool | Internal | echocore, echonetcore, echosignalos | echoterminal, echoindex | No | No | addons/signalosexample/src/main/resources/META-INF/echo.mod.json |
