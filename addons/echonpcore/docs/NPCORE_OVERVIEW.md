# ECHO: NPCore Overview

ECHO: NPCore is the shared first-party NPC runtime for the ECHO ecosystem. It provides the neutral foundation for ECHO-authored NPCs instead of vanilla-style villager interactions: data-driven profiles, custom textures, dialogue trees, trade and service definitions, server-authoritative interaction state, and a dedicated NPC UI.

NPCore is intentionally addon-neutral. Ashfall-style example content ships with the addon, but engine classes do not require Ashfall Protocol. Other ECHO addons can point their own datapacks at NPCore folders and spawn `echonpcore:echo_npc` with their own profile identifiers.

Current vertical slice:
- Custom `EchoNpcEntity` and spawn egg.
- JSON-loaded NPC, visual, dialogue, trade, service, faction, and replacement definitions.
- Right-click interaction that opens an NPC screen.
- Server-validated dialogue actions, trades, and services.
- Vanilla villager and wandering trader conversion mappings.
- ScreenCore-backed NPC page when `echoscreencore` is installed and `useScreenCoreNpcScreens=true`.
- Classic NPC screen fallback when ScreenCore is absent, disabled, or declines the page.
- Terminal addon-info integration when `echoterminal` is installed.
- DataCore-backed trade stock, service cooldown, and conversion-record persistence when `echodatacore` is installed.
- MissionCore-backed trade `requiresMission` checks when `echomissioncore` is installed.

Current limitations:
- Without DataCore, trade stock, service cooldowns, and conversion records fall back to in-memory state.
- Mission gates currently apply to trade offers only; service and dialogue mission gates are future schema work.
- WorldCore region context, HoloMap contact markers, and richer terminal contact surfaces remain future bridges.
- Terminal integration is informational only in `1.0.0`; no full NPC contacts tab exists yet.
- Renderer uses the shared humanoid model with profile-selected textures; layered/emissive rendering is reserved for a later RenderCore pass.
