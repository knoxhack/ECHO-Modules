<!-- CURSEFORGE_README_START -->
# NetCore by ECHO Labs

![NetCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echonetcore/brand-sheet.png)

**Packet bridge, sync helpers, optional-channel safe sends, server action validation, rate limits, and packet diagnostics.**

![NetCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echonetcore/features-portrait.png)

![NetCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echonetcore/features-landscape.png)

## CurseForge Summary

Packet bridge, sync helpers, optional-channel safe sends, server action validation, rate limits, and packet diagnostics.

## Main Features

- Optional packet registrar helpers for clientbound sync, serverbound actions, and debug packets.
- Rate-limited server action policies.
- Safe send helpers that catch missing-channel failures and emit debug events.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echonetcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echonetcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echonetcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: NetCore

ECHO: NetCore is the shared packet, sync, server action, rate limiting, and debug network layer for ECHO addons.

## Packet Categories

- Clientbound sync packets mirror server-owned state to the logical client.
- Serverbound action packets represent player intent only; handlers must validate permissions, distance, ownership, inventory, menu state, and world state on the server.
- Debug/dev packets are disabled by default and require operator permissions when enabled.
- Optional addon packets should use optional registration and safe send helpers so missing consumers do not crash a server or client.

## Registering Packets

Use `EchoNetPayloads.optional(event)` to create the shared optional registrar, then register packets by category:

```java
PayloadRegistrar registrar = EchoNetPayloads.optional(event);
EchoNetPayloads.clientboundSync(registrar, MySyncPacket.TYPE, MySyncPacket.CODEC, MyNetwork::handleSync);
EchoNetPayloads.serverboundAction(registrar, MyActionPacket.TYPE, MyActionPacket.CODEC,
        EchoRateLimitPolicy.of(10, "my_action"), MyNetwork::handleAction);
```

Serverbound handlers receive a `ServerPlayer`; packets from non-server contexts are dropped before the handler runs. Rate-limited packets are dropped without mutating gameplay state.

## Sync Helpers

`EchoCoreServices.networkBridge()` exposes no-op-safe helpers for player data, world data, mission progress, visual state, machine/block-entity state, debug data, faction sync, and discovery toasts. NetCore supplies the real bridge when loaded; ECHO Core falls back to `NoOpNetworkService`.

Client code can subscribe to generic NetCore sync packets with `EchoClientSyncRegistry.register(type, channelId, consumer)`.

## Safe Sends

Use `EchoNetSend.toPlayer(player, payload, kind)` for optional clientbound sends. It catches missing-channel failures and emits packet debug events. Use `EchoNetClientActions.sendServerboundAction(payload)` from client-only classes for terminal buttons and other UI actions, or `EchoNetClientActions.trySendServerboundAction(payload)` when optional cross-addon UI sends need a boolean success result.

## Packet Ownership

As of NetCore 1.2, addons may declare packet payload records and handlers, but direct NeoForge packet registration and distribution calls should stay inside NetCore. Addons should use `EchoNetPayloads` for registration, `EchoNetSend` for server-to-client sync, and `EchoNetClientActions` for client-to-server UI/action packets. This keeps optional-channel failures, debug hooks, and rate limits consistent across the ECHO stack.

As of NetCore 1.0.0, this is the required pattern for every ECHO addon that declares custom packets. Addons define packet records and domain handlers locally, but register only through `EchoNetPayloads.optional(event)`, send only through `EchoNetSend` or client-only `EchoNetClientActions`, and validate serverbound intent with addon logic plus `EchoServerActionGuards` where shared guards apply.

Serverbound action packets should choose an explicit rate limit:

```java
EchoNetPayloads.serverboundAction(registrar, MyActionPacket.TYPE, MyActionPacket.CODEC,
        EchoNetPayloads.defaultActionPolicy("my_action"), MyNetwork::handleAction);
```

Use `EchoRateLimitPolicy.NONE` only when an existing validated flow already owns its throttle semantics.

See `docs/NETCORE_MIGRATION_GUIDE.md` for the full migration checklist and audit command.

## Debug Logging

Packet logging is controlled by NetCore common config:

- `debugPacketLogging=false`
- `logDroppedPackets=false`
- `enableDebugPackets=false`

Debug logs and debug packet handlers stay silent unless explicitly enabled.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echonetcore.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echonetcore.md`.
