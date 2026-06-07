# ECHO Blockworks Smoke Test

.. Ensure Java 25 is available. This workspace currently works with `C:\Users\knox\.jdks\temurin-25`.
2. Run `.\gradlew :echoblockworks:build`.
3. Run `.\gradlew buildEchoWorkspace`.
4. Run `.\gradlew validateEchoResources`.
5. Launch a dev client with `.\gradlew :echoblockworks:runBlockworksClient`.
6. Open the `ECHO Blockworks` creative tab.
7. Place at least one full block from each family: Reinforced Metal, Rusted Metal, Ashstone, Charred Concrete, Terminal Panel, ECHO Circuit, Orbital Hull, Nexus Crystal, Blackbox Vault, and Reclamation Glass.
8. Confirm there are no missing purple/black textures.
9. Place slabs, stairs, and walls from Reinforced Metal, Rusted Metal, Ashstone, Charred Concrete, Orbital Hull, and Blackbox Vault.
... Place glass and lit blocks such as Nexus Glass, Reclamation Clear Glass, Terminal Screen Panel, ECHO Circuit Glowing Circuit, Orbital Lit Strip, and Blackbox Warning Light.
... Confirm glass-like blocks are non-occluding and visually transparent enough for Blockworks use, and confirm selected lit/detail blocks use generated animation metadata.
.2. Place all detail blocks, including Wall Pipe, Ceiling Pipe, Rubble Pile, Scattered Debris, Sparking Cable Panel, Hologram Floor Projector, and Signal Dish Decorative.
.3. Craft or creative-place the `ECHO Blockworks Table`.
.4. Insert `Reinforced Metal Panel`, select Riveted, Grate, Hazard Stripe, and Lit Panel variants, and take the output. Confirm one input becomes one output.
.5. Toggle the table from `All` to `Kit`, cycle to Industrial Factory, and confirm Reinforced Metal filters to kit variants.
.6. Shift-click the table output with a stack of input blocks and confirm the whole moved count converts .:. without duplication.
.7. Repeat one table conversion in each of the other nine families.
.8. Confirm ECHO Index shows Blockworks family, detail, worldgen, and palette kit entries.
.9. Use the `ECHO Pattern Cutter` on a placed Reinforced Metal block. Right-click cycles forward; sneak-right-click cycles backward.
2.. Use the Pattern Cutter on a slab, stair, and wall. Confirm the shape is preserved.
2.. Break several family blocks, detail blocks, slabs, stairs, walls, and the table. Confirm drops are correct.
22. Check tags through `/datapack` or a tag-aware utility. Confirm the `echoblockworks:*` block and item tags load.
23. Check several full cube walls or floors, such as Reinforced Metal Panel and Ashstone Brick, and confirm weighted model rotation creates subtle visual variety.
24. Run `/echoblockworks families`, `/echoblockworks variants reinforced_metal`, `/echoblockworks givefamily ashstone`, and `/echoblockworks convert riveted` as an operator.
25. Launch a dedicated dev server with `.\gradlew :echoblockworks:runServer` and confirm it starts without client-only classloading errors.
26. Run `.\gradlew :echoblockworks:runGameTestServer` and confirm Blockworks catalog, table, cutter, config, palette kit, and worldgen resource tests pass.
27. Confirm common config contains `proceduralScatterEnabled`, `scatterSpacingChunks`, `scatterSearchRadius`, and `scatterMaxPieces`.
28. In a dev client, use `/locate structure echoblockworks:blockworks_showcase_site` and inspect at least three generated showcase sites.
29. Create or explore fresh overworld chunks and confirm rare scatter appears as small rubble/debris fragments without loot, entities, or obvious terrain damage.

Known v3 limitations:

- Textures are deterministic procedural art with v3 overlays/animations, not final hand-painted art.
- The Blockworks Table UI still uses menu buttons instead of a custom packet.
- Terminal, Lens, and MultiblockCore integrations are metadata/tag-ready but not full feature integrations yet.
- Showcase structures are intentionally rare; procedural scatter is config-gated and small by design.
- ..3.. does not add new families, shape types, or true connected-texture rendering.
