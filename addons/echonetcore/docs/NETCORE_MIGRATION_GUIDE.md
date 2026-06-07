# ECHO NetCore Migration Guide

NetCore 1.3.0 is the required networking backbone for ECHO addons that declare custom packets.

## Standard Packet Pattern

- Define packet records in the owning addon.
- Register packets only through `EchoNetPayloads.optional(event)`.
- Register serverbound intent packets with `EchoNetPayloads.serverboundAction(...)`.
- Send server-to-client packets through `EchoNetSend`.
- Send client-to-server packets through `EchoNetClientActions` from client-only code.

## Serverbound Actions

Serverbound packets are requests, not authority. Handlers must validate the requested action on the server before mutating state.

Use `EchoServerActionGuards` for shared checks such as operator permission, distance to a block, loaded block entity type, and same-level ownership. Keep addon-specific ownership, inventory, menu, faction, and progression checks in the owning addon.

## Dependency Metadata

Any addon with custom packets should declare:

```toml
[[dependencies.<modid>]]
modId="echonetcore"
type="required"
reason="Provides shared ECHO packet registration, safe sends, sync, and action rate limiting."
versionRange="[1.3.0,)"
ordering="AFTER"
side="BOTH"
```

## Audit

The only expected direct NeoForge send calls are inside NetCore:

```bash
rg 'PacketDistributor\.send|ClientPacketDistributor\.send|event\.registrar\("1"\)' addons -g '*.java'
```

After migration, addon code should use `EchoNetPayloads`, `EchoNetSend`, and `EchoNetClientActions` instead.
