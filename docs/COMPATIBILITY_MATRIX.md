# ECHO 1.3.4 Compatibility Matrix

Cell values: Required, Recommended, Optional, Not Needed, Experimental, or Official Pack Only. Reusable modules do not require Ashfall unless their metadata says so.

| Module | Vanilla+ | Tech | Magic/Mystic | Skyblock | RPG | Survival | Horror | Sci-Fi | Post-Apocalyptic | Kitchen Sink | Server SMP | Ashfall |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `echoagriculturereclamation` | Recommended | Recommended | Optional | Not Needed | Optional | Recommended | Optional | Optional | Recommended | Optional | Optional | Recommended |
| `echoarmory` | Optional | Optional | Optional | Not Needed | Recommended | Recommended | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echoashfallprotocol` | Optional | Optional | Optional | Not Needed | Optional | Recommended | Optional | Recommended | Recommended | Optional | Optional | Required |
| `echoblackboxprotocol` | Optional | Optional | Optional | Not Needed | Optional | Optional | Recommended | Optional | Recommended | Optional | Optional | Recommended |
| `echoblockworks` | Recommended | Optional | Optional | Not Needed | Optional | Recommended | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echoconvoyprotocol` | Optional | Recommended | Optional | Not Needed | Optional | Recommended | Optional | Optional | Recommended | Optional | Recommended | Recommended |
| `echocore` | Required | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echodatacore` | Optional | Optional | Optional | Not Needed | Recommended | Recommended | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echograves` | Internal | Internal | Internal | Internal | Internal | Internal | Internal | Internal | Internal | Internal | Internal | Internal |
| `echoholomap` | Optional | Recommended | Optional | Not Needed | Optional | Recommended | Optional | Recommended | Optional | Optional | Recommended | Recommended |
| `echoindex` | Recommended | Recommended | Recommended | Optional | Recommended | Recommended | Recommended | Optional | Optional | Optional | Optional | Recommended |
| `echoindustrialnexus` | Optional | Recommended | Optional | Not Needed | Optional | Recommended | Optional | Recommended | Optional | Optional | Optional | Recommended |
| `echolens` | Optional | Recommended | Recommended | Not Needed | Recommended | Recommended | Optional | Recommended | Optional | Optional | Optional | Recommended |
| `echologisticsnetwork` | Optional | Recommended | Optional | Optional | Optional | Optional | Optional | Optional | Recommended | Optional | Recommended | Recommended |
| `echomissioncore` | Optional | Optional | Optional | Not Needed | Recommended | Recommended | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echomultiblockcore` | Optional | Recommended | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echonetcore` | Required | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echonexusprotocol` | Optional | Optional | Recommended | Not Needed | Recommended | Optional | Optional | Recommended | Recommended | Optional | Optional | Recommended |
| `echoorbitalremnants` | Optional | Optional | Optional | Not Needed | Optional | Recommended | Optional | Recommended | Recommended | Optional | Optional | Recommended |
| `echoplayercore` | Recommended | Optional | Optional | Not Needed | Optional | Recommended | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echopowergrid` | Optional | Recommended | Optional | Optional | Optional | Recommended | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echorecovery` | Optional | Optional | Optional | Not Needed | Recommended | Recommended | Optional | Optional | Recommended | Optional | Recommended | Recommended |
| `echorelictech` | Optional | Optional | Recommended | Not Needed | Recommended | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echorendercore` | Optional | Optional | Optional | Not Needed | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echoruntimeguard` | Optional | Optional | Optional | Not Needed | Optional | Optional | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echoscreencore` | Optional | Optional | Optional | Not Needed | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echosoundcore` | Optional | Optional | Optional | Not Needed | Optional | Recommended | Recommended | Optional | Optional | Optional | Optional | Recommended |
| `echostationfall` | Optional | Optional | Optional | Not Needed | Optional | Optional | Recommended | Recommended | Recommended | Optional | Optional | Recommended |
| `echoterminal` | Optional | Recommended | Optional | Not Needed | Recommended | Recommended | Recommended | Recommended | Optional | Optional | Optional | Recommended |
| `echotextureforge` | Optional | Optional | Optional | Not Needed | Optional | Optional | Optional | Optional | Optional | Not Needed | Optional | Not Needed |
| `echothemecore` | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echotutorialcore` | Recommended | Optional | Optional | Not Needed | Recommended | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echoweathercore` | Optional | Optional | Optional | Not Needed | Optional | Recommended | Optional | Optional | Optional | Optional | Recommended | Recommended |
| `echowiki` | Optional | Optional | Optional | Not Needed | Recommended | Optional | Optional | Optional | Optional | Optional | Optional | Recommended |
| `echoworldcore` | Optional | Optional | Recommended | Not Needed | Recommended | Recommended | Optional | Optional | Optional | Optional | Optional | Recommended |
| `signalos` | Optional | Optional | Optional | Not Needed | Recommended | Optional | Optional | Recommended | Optional | Optional | Optional | Recommended |
| `signalosexample` | Optional | Optional | Optional | Not Needed | Optional | Optional | Optional | Optional | Optional | Not Needed | Optional | Not Needed |

## Starter Stack Notes

- Simple packs should start with `echocore`, `echonetcore`, `echoindex`, and only the player-facing systems they actually need.
- Tech packs should evaluate PowerGrid, Industrial Nexus, MultiblockCore, Logistics, and Convoy together.
- Story and horror packs benefit most from Terminal, Index, MissionCore, SignalOS, SoundCore, Stationfall, Nexus, and Blackbox.
- Server SMP packs should review PlayerCore permissions, Recovery behavior, RuntimeGuard budgets, DataCore persistence, HoloMap sharing, WeatherCore ticking, Logistics depots, and Convoy routes.
- `echograves` is obsolete/internal and replaced by `echorecovery`; do not revive it for 1.3.4.
