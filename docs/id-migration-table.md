# ID Migration Table

| Old ID | New ID | Action | Reason |
| --- | --- | --- | --- |
| echoopenlandsprotocol:workbench | echostationcore:field_bench | alias | Vanilla-adjacent name; shared crafting station. |
| echoopenlandsprotocol:chest | echostationcore:field_crate | alias | Vanilla-adjacent name; shared storage station. |
| echoopenlandsprotocol:torch | echoworldstarter:pitchlight | alias | Vanilla-adjacent name; shared early light item/block. |
| echoopenlandsprotocol:torch_bundle | echoworldstarter:pitchlight_bundle | alias | Bundle follows pitchlight public identity. |
| echoopenlandsprotocol:copper_ore | echomaterialcore:cupral_vein | alias | Copper public identity replaced by cupral. |
| echoopenlandsprotocol:copper_ore_chunk | echomaterialcore:cupral_chunk | alias | Copper public identity replaced by cupral. |
| echoopenlandsprotocol:copper_ingot | echomaterialcore:cupral_bar | alias | Copper ingot replaced by cupral bar. |
| echoopenlandsprotocol:tin_ore | echomaterialcore:tinveil_vein | alias | Tin public identity replaced by tinveil. |
| echoopenlandsprotocol:tin_ore_chunk | echomaterialcore:tinveil_chunk | alias | Tin public identity replaced by tinveil. |
| echoopenlandsprotocol:tin_ingot | echomaterialcore:tinveil_bar | alias | Tin ingot replaced by tinveil bar. |
| echoopenlandsprotocol:bronze_ingot | echomaterialcore:bronze_cast | alias | Bronze ingot replaced by bronze cast. |
| echoopenlandsprotocol:iron_ore | echomaterialcore:ferrite_vein | alias | Iron public identity replaced by ferrite. |
| echoopenlandsprotocol:iron_ore_chunk | echomaterialcore:ferrite_chunk | alias | Iron public identity replaced by ferrite. |
| echoopenlandsprotocol:iron_ingot | echomaterialcore:ferrite_bar | alias | Iron ingot replaced by ferrite bar. |
| echoopenlandsprotocol:crude_axe | echotoolcore:crude_cutter | alias | Tool role name avoids vanilla axe identity. |
| echoopenlandsprotocol:crude_pick | echotoolcore:crude_breaker | alias | Tool role name avoids vanilla pick identity. |
| echoopenlandsprotocol:crude_spade | echotoolcore:crude_digger | alias | Tool role name avoids vanilla shovel/spade identity. |
| echoopenlandsprotocol:wooden_hammer | echotoolcore:field_hammer | alias | Generic hammer belongs in Foundation tool set. |
| echoopenlandsprotocol:copper_pick | echotoolcore:cupral_breaker | alias | Tool progression tied to cupral. |
| echoopenlandsprotocol:bronze_pick | echotoolcore:bronze_breaker | alias | Tool progression tied to bronze cast. |
| echoopenlandsprotocol:iron_pick | echotoolcore:ferrite_breaker | alias | Tool progression tied to ferrite. |
| echoopenlandsprotocol:raw_clay | echomaterialcore:clay_lump | alias | Generic clay item belongs in Material Core. |
| echoopenlandsprotocol:charcoal | echomaterialcore:charcoal_lump | alias | Generic fuel item belongs in Material Core. |
| echoopenlandsprotocol:pitch | echomaterialcore:pitch_resin | alias | Generic adhesive/fuel item belongs in Material Core. |
| echoopenlandsprotocol:bone | echomaterialcore:bone_shard | alias | Generic creature material belongs in Material Core. |
| echoopenlandsprotocol:hide | echomaterialcore:hide_strip | alias | Generic creature material belongs in Material Core. |
| echoopenlandsprotocol:forge | echostationcore:forge_hearth | alias | Shared processing station with distinct ECHO identity. |
| echoashfallprotocol:animal_bone | echomaterialcore:bone_shard | alias-or-tag | Ashfall duplicate baseline creature material. |
| echoashfallprotocol:animal_hide | echomaterialcore:hide_strip | alias-or-tag | Ashfall duplicate baseline creature material. |
| echoashfallprotocol:plant_fiber | echomaterialcore:reed_fiber | alias-or-tag | Ashfall duplicate baseline fiber material. |
| echoashfallprotocol:fiber_rope | echomaterialcore:fiber_binding | alias-or-tag | Ashfall duplicate baseline binding material. |
| echoashfallprotocol:map_table | echoashfallprotocol:survey_table | rename-needed | Map table is Openlands fantasy; Ashfall needs survey/evac identity. |
| echoashfallprotocol:riftstone | echoarcanadivisionprotocol:nexus_scar_stone | move-or-rename | Rift identity belongs to Arcana unless Ashfall makes it geological. |
| echoashfallprotocol:contaminated_redstone | echoashfallprotocol:charged_ash_circuit | blocked-rename | Vanilla public identity leak. |
| echoashfallprotocol:contaminated_lapis | echoashfallprotocol:blue_ash_salt | blocked-rename | Vanilla public identity leak. |
