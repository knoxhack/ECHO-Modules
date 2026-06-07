# NPCore ScreenCore Integration

NPCore ships two NPC interaction surfaces:

- ScreenCore page: `echonpcore:npc_interaction`
- Classic fallback: `EchoNpcScreen`

When `echoscreencore` is loaded and `useScreenCoreNpcScreens=true`, the client opens `ScreenCoreNpcScreenBridge` first. If ScreenCore is missing, disabled, throws, or declines the page, NPCore falls back to the classic screen when `fallbackToClassicNpcScreens=true`.

## Page Resources

The ScreenCore resources live under:

- `assets/echonpcore/eui/pages/npc_interaction.eui.xml`
- `assets/echonpcore/eui/styles/npc_interaction.eui.css`
- `assets/echonpcore/eui/eui_manifest.json`

The page mirrors the classic NPC layout:

- Contact rail with portrait, badge, profile id, role, faction, and relationship.
- Channel list for Talk, Trade, Services, Intel, and Exit.
- Active content panel with dialogue options, trade offers, service requests, or integration notes.
- Footer status line plus Refresh and Close actions.

## Data Provider Contract

NPCore registers the ScreenCore provider root `npcore`.

Supported keys:

- `npcore.contact`
- `npcore.tabs`
- `npcore.dialogue`
- `npcore.trades`
- `npcore.services`
- `npcore.intel`
- `npcore.status`
- `npcore.integrations`
- `npcore.active`
- `npcore.activeTab`

The provider is derived from the latest server-built `EchoNpcScreenState`. The client only stores transient selected-tab state.

## Actions

The page registers these ScreenCore actions:

- `npcore.tab`
- `npcore.dialogue.select`
- `npcore.trade.request`
- `npcore.service.request`
- `npcore.close`
- `npcore.refresh`

Dialogue, trade, service, and close actions send the existing NPCore packets. The server still validates distance, entity type, profile data, requested ids, costs, cooldowns, and outputs.

## Sync Behavior

`OpenNpcScreenPacket` opens the ScreenCore page when possible. `SyncNpcScreenStatePacket` updates the active ScreenCore page and invalidates data. If the active page is gone or no longer belongs to the NPC, sync falls back to the classic screen path.

Future passes can add richer ScreenCore components, entity previews, and DataCore-backed contact memory without changing the server-authoritative packet contract. The `1.0.0` DataCore bridge persists trade stock, service cooldowns, and villager conversion records; it does not yet add a ScreenCore contact-memory surface.
