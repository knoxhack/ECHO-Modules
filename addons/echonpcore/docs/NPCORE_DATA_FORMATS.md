# NPCore Data Formats

NPC profile: `data/<namespace>/npc_profiles/<id>.json`

```json
{
  "id": "example:radio_operator",
  "displayName": "Radio Operator",
  "role": "Signal Contact",
  "faction": "example:settlement",
  "visualProfile": "example:radio_operator",
  "dialogue": "example:radio_operator",
  "trades": "example:radio_operator_basic",
  "services": "example:radio_operator_services",
  "missions": [],
  "ambientLines": ["Signal is clean for once."],
  "interactionRange": 5.0,
  "behavior": {
    "mode": "settler_trader",
    "wanderRadius": 8,
    "returnRadius": 24,
    "ambientCooldown": 2400,
    "stationary": false,
    "homebound": true
  },
  "integrations": {
    "terminalContact": true,
    "mapMarker": true,
    "discoverOnInteract": true,
    "intelSummary": "Signal contacts can refresh local route context."
  }
}
```

`behavior` is optional. NPCore defaults to a non-combat settler/trader behavior: the NPC records a home position, wanders nearby, returns home when too far away, pauses during interaction, and emits ambient lines on cooldown. Set `stationary=true` for kiosk-style contacts.

`integrations` is optional. `terminalContact` and `discoverOnInteract` control whether the NPC becomes a Terminal contact after interaction. `mapMarker` lets discovered NPCs appear through NPCore's HoloMap data provider. `intelSummary` is used by world-intel services and Terminal contact summaries.

Visual profile: `data/<namespace>/npc_visual_profiles/<id>.json`

```json
{
  "id": "example:radio_operator",
  "model": "echonpcore:humanoid_basic",
  "texture": "example:textures/entity/npc/settlement/radio_operator.png",
  "portrait": "example:textures/gui/npc/portraits/radio_operator.png",
  "factionBadge": "example:textures/gui/npc/badges/settlement.png",
  "screenFrame": "example:textures/gui/npc/frames/survivor_frame.png",
  "theme": "example:signal"
}
```

Dialogue: `data/<namespace>/npc_dialogues/<id>.json`

```json
{
  "id": "example:radio_operator",
  "start": "intro",
  "nodes": {
    "intro": {
      "text": "Signal tower is listening.",
      "options": [
        { "id": "trade", "label": "Show supplies.", "action": "open_trade" },
        { "id": "intel", "label": "Log this contact.", "action": "discover_contact" },
        { "id": "exit", "label": "Close channel.", "action": "close" }
      ]
    }
  }
}
```

Dialogue options support the same server-side availability gates as services:

```json
{
  "id": "mission_line",
  "label": "Any contract work?",
  "action": "open_services",
  "requiresMission": "example:repair_the_relay",
  "requiresFactionStanding": 5,
  "disabledReason": "Earn the relay crew's trust first."
}
```

Supported built-in dialogue actions are `open_trade`, `open_services`, `open_intel`, `discover_contact`, and `close`.

Trade set: `data/<namespace>/npc_trades/<id>.json`

```json
{
  "id": "example:radio_operator_basic",
  "groups": [
    {
      "id": "signal",
      "title": "Signal Supplies",
      "offers": [
        {
          "id": "paper_for_compass",
          "title": "Bearing Note",
          "input": [{ "item": "minecraft:paper", "count": 2 }],
          "output": { "item": "minecraft:compass", "count": 1 },
          "stock": 1,
          "restockTime": 24000,
          "requiresMission": "example:repair_the_relay",
          "requiresFactionStanding": 5,
          "disabledReason": "Earn the relay crew's trust first."
        }
      ]
    }
  ]
}
```

`requiresMission`, `requiresFactionStanding`, and `disabledReason` are optional. Blank mission values are always allowed. NPCore checks mission gates server-side through MissionCore when `echomissioncore` is loaded. Accepted statuses are `ACTIVE`, `COMPLETED`, `CLAIMABLE`, `CLAIMED`, and `VIEW_ONLY`; `LOCKED`, `UNLOCKED`, invalid ids, and unknown missions reject the trade. If MissionCore is absent, NPCore fails open for compatibility and logs the ignored requirement once. Limited-stock offers use `stock` as the maximum stock and `restockTime` as the world-time delay before refilling after a purchase.

Service set: `data/<namespace>/npc_services/<id>.json`

```json
{
  "id": "example:radio_operator_services",
  "services": [
    {
      "id": "intel_hint",
      "title": "Local Intel",
      "description": "Provides a route hint.",
      "cost": [],
      "action": "world_intel",
      "amount": 1,
      "cooldown": 2400,
      "requiresMission": "",
      "requiresFactionStanding": 0,
      "target": "",
      "actionId": "",
      "disabledReason": ""
    }
  ]
}
```

Built-in service actions:
- `heal`, `feed`, `repair_held_item`: apply simple local player/item effects after cost and cooldown checks.
- `world_intel` / `intel_hint`: sends local NPC intel, includes WorldCore context when available, and mirrors the intel through EchoCore.
- `discover_contact`: records the NPC as a Terminal contact.
- `start_mission`: starts the mission in `target`.
- `mission_action`: calls MissionCore action `actionId` on mission `target`.
- `faction_action`: calls EchoCore faction action `target`, using `actionId` as the optional action target.
- `reveal_marker`: reveals a WorldCore/HoloMap marker at the NPC home position.
- `map_refresh`: asks map providers to rebuild visible markers.

Mission and faction gates are evaluated server-side for dialogue options, trades, and services. Missing item costs are still validated only on the server when a row is clicked.

Persistence notes:
- Trade stock is persisted in DataCore world data under `echonpcore:npc/trade_stocks` when DataCore is loaded.
- Trade restock timers are persisted in DataCore world data under `echonpcore:npc/trade_restocks` when DataCore is loaded.
- Service cooldowns are persisted in DataCore player data under `echonpcore:npc/service_cooldowns` when DataCore is loaded.
- Vanilla conversion records are persisted in DataCore world data under `echonpcore:npc/conversions` when DataCore is loaded.
- Without DataCore, the same systems use NPCore's in-memory fallback and reset with the server process.

Replacement mapping: `data/<namespace>/villager_replacements/<id>.json`

```json
{
  "id": "example:default",
  "replace": {
    "minecraft:farmer": "example:reclaimer_farmer"
  },
  "entityTypes": {
    "minecraft:wandering_trader": "example:roaming_scavenger"
  }
}
```
