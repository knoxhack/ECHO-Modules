# ECHO PowerGrid 1.3.0 Feature Matrix

## Legend
- **Impl** = Code exists and is wired
- **Test** = GameTest or smoke coverage exists
- **Docs** = README/SMOKE_TEST/docs match behavior
- **Gap** = Remaining follow-up

## Core Energy

| Feature | Impl | Test | Docs | Gap |
|---|---:|---:|---:|---|
| EP unit, snapshots, draw API | Yes | Yes | Yes | - |
| Generator buffers and battery debit | Yes | Yes | Yes | - |
| Hand crank burst/cooldown | Yes | Yes | Yes | - |
| Solar sky/day/weather/dimension checks | Yes | Partial | Yes | WorldCore hazard modifiers remain soft/no-op when absent |
| FE transaction rollback | Yes | Yes | Yes | - |
| Medium/Industrial battery progression | Yes | Yes | Yes | Uses existing texture style pending final art |

## Network Topology

| Feature | Impl | Test | Docs | Gap |
|---|---:|---:|---:|---|
| Network discovery and batching | Yes | Yes | Yes | - |
| Network split/merge on topology changes | Yes | Yes | Yes | Large stress suite still useful |
| Tripped breaker isolation | Yes | Yes | Yes | - |
| Path route summaries | Yes | Yes | Yes | - |
| Distance-based power loss | Yes | Yes | Yes | Delivery model is route-aware for explicit draws |
| Idle network sleep config | Partial | No | Yes | Budget hook exists; deeper sleep heuristics can be tuned |

## Overload, Brownout, And Safety

| Feature | Impl | Test | Docs | Gap |
|---|---:|---:|---:|---|
| Brownout state and partial consumer delivery | Yes | Partial | Yes | Demand-class distribution can be expanded per real consumers |
| Overload grace and breaker trip | Yes | Yes | Yes | - |
| Optional cable damage/explosion config | Yes | Partial | Yes | Kept disabled by default |
| Shared alert summaries | Yes | Partial | Yes | More alert codes can be added as integrations consume them |

## Control And UX

| Feature | Impl | Test | Docs | Gap |
|---|---:|---:|---:|---|
| Power node screen | Yes | Yes | Yes | - |
| Substation policy persistence | Yes | Partial | Yes | GUI is functional/status-oriented; richer assignment UX can follow |
| Power meter diagnostics | Partial | Partial | Yes | Uses shared node UI plus commands; dedicated art/screen can be polished |
| Commands for networks/alerts/route/inspect/set energy | Yes | Compile | Yes | - |

## ECHO Ecosystem Integrations

| Feature | Impl | Test | Docs | Gap |
|---|---:|---:|---:|---|
| Terminal network sync | Yes | Yes | Yes | Dashboard can consume new alerts/nodes next |
| HoloMap network and alert markers | Yes | Yes | Yes | - |
| Lens node/network scan data | Yes | Compile | Yes | - |
| MultiblockCore power provider | Yes | Yes | Yes | - |
| Industrial Nexus EP costs/status path | Yes | Yes | Yes | Direct hard dependency avoided |
| RuntimeGuard budgets/profiling | Yes | Compile | Yes | Depends on optional RuntimeGuard services |
| WorldCore solar/hazard hook | Partial | No | Yes | Solar uses vanilla weather now; WorldCore-specific hazard scaling remains extension work |

## Acceptance Definition

1. Public 1.2.0 API remains source-compatible.
2. New 1.3.0 summaries, alerts, policies, routes, and progression blocks compile.
3. PowerGrid, MultiblockCore, and Industrial Nexus compile at `1.3.0`.
4. PowerGrid GameTests cover core draw, breaker isolation, routes, HoloMap, Terminal packets, and 1.3.0 node summaries.
5. Full workspace build and relevant GameTests must pass before release tagging.
