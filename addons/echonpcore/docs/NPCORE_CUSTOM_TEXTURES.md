# NPCore Custom Textures

NPCore visual profiles reference texture identifiers directly. Entity textures can be 64x64, 128x128, or 256x256 PNGs, as long as they match the active humanoid model layout. GUI portraits, badges, and frames should be transparent PNGs where empty space should show the NPC screen behind them.

Recommended paths:
- Entity: `assets/<namespace>/textures/entity/npc/<group>/npc_<role>_<variant>.png`
- Emissive: `assets/<namespace>/textures/entity/npc/<group>/npc_<role>_<variant>_emissive.png`
- Portrait: `assets/<namespace>/textures/gui/npc/portraits/portrait_<role>_<variant>.png`
- Badge: `assets/<namespace>/textures/gui/npc/badges/badge_<faction>.png`
- Frame: `assets/<namespace>/textures/gui/npc/frames/frame_<npc_type>.png`

Bundled examples use compact names such as `textures/entity/npc/villagers/reclaimer_farmer.png`, but datapacks can use the longer naming rules above.

Visual profile example:

```json
{
  "id": "example:radio_operator",
  "model": "echonpcore:humanoid_basic",
  "texture": "example:textures/entity/npc/settlement/npc_radio_operator_01.png",
  "emissiveTexture": "example:textures/entity/npc/settlement/npc_radio_operator_01_emissive.png",
  "portrait": "example:textures/gui/npc/portraits/portrait_radio_operator_01.png",
  "factionBadge": "example:textures/gui/npc/badges/badge_settlement.png",
  "screenFrame": "example:textures/gui/npc/frames/frame_survivor.png",
  "nameplateStyle": "survivor",
  "theme": "example:radio_contact"
}
```

If a texture is missing, the renderer falls back to `echonpcore:textures/entity/npc/missing_npc.png`.
