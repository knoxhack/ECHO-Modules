# Native AdapterCore Guide

AdapterCore bridges module contracts across native, NeoForge, and standalone runtimes.

Use descriptor runtime targets to keep behavior explicit and avoid loading runtime-specific code in the wrong environment.

## Gameplay Mutation Receipts

AdapterCore release proof now requires receipt-backed gameplay mutation evidence. `EchoNativeRuntimeHost.NativeResult` can still report `NOOP`, `QUEUED`, `UNSUPPORTED`, or `FAILED`, but a `MUTATED` result is release-proof only when it carries `NativeMutationReceipt` evidence with a release proof kind.

Release-proof kinds are `HOST_STATE`, `SAVE_WRITE`, `HUD_EVENT`, and `PACKET_EVENT`. `DIAGNOSTIC_ONLY` and `QUEUED_ONLY` are explicit non-proof states and must not satisfy player-ready audits.

The runtime dispatcher normalizes action IDs through `EchoContentAliasResolver`, rejects undeclared actions/hosts/handlers, writes the mutation ledger, and rejects `MUTATED` results that are metadata-only. Existing Ashfall first-join and machine flows route through this dispatcher: item grants, drop-pod structure placement, teleport/respawn binding, save writes, HUD/packet feedback, machine ticks, capability changes, and energy changes all produce AdapterCore ledger entries with receipts.

Native Loader backends should prefer the SDK contract `EchoAdapterCoreGameplayMutationService`. Reflection remains a fallback for older beta backends, but it must return `UNSUPPORTED` unless the backend record can be converted into a typed mutation receipt.
