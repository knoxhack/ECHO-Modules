
# Content Ownership Audit

This is the coding-ready ownership split for the Foundations refactor.

## Openlands Extraction Source

Openlands is frozen as the extraction source for baseline survival. Its MVP
registry currently contains baseline materials, tools, stations, loot,
first-hour progression, and creature pressure categories. Those contracts now
belong to Foundation modules.

## Foundation-Owned Content

echomaterialcore owns branchwood, fieldstone, reed fiber, flint, clay, glass,
hide, bone, pitch/resin, charcoal, cupral, tinveil, bronze cast, ferrite, and
generic wood construction blocks.

echotoolcore owns crude cutter, crude breaker, crude digger, flint knife, field
hammer, cupral breaker, bronze breaker, and ferrite breaker.

echostationcore owns handcrafting, field bench, field crate, kiln, forge hearth,
loom, cookpot, and mason table.

echoworldstarter owns campfire, pitchlight, pitchlight bundle, bedroll,
bedroll block, spawn safety, starter shelter score, and first-hour steps 1-6.

echocommonloot owns starter cache, traveler pack, ruined storage, material
scrap, generic block drops, and generic ore chunk drops.

echocreatureroles owns passive_small, passive_large, neutral_forager,
territorial_medium, hostile_small, hostile_large, aquatic_passive, and
night_stalker.

## Openlands-Only Content

Openlands keeps meadows, woodlands, stonehills, marshlands, pine construction,
thatch, old roads, waystones, map table, region rubbing, old road token, route
binding, glow crystal, homestead food, small pack, repair kit, copper fitting,
and Openlands creature identities.

## Ashfall Audit

Ashfall keeps storms, heat, ash exposure, scarcity, shelters, filtration,
atmospheric scrubbers, distillation, black rain, ash soil, scoria, basalt,
sulfur, and emberglass.

Ashfall duplicated baseline survival through animal_bone, animal_hide,
plant_fiber, fiber_rope, bone_knife, scrap_knife, and crude_spear. These should
resolve through Foundation tags or explicit Ashfall fantasy variants.

Ashfall rename blockers are map_table, riftstone, contaminated_redstone, and
contaminated_lapis.

## Arcana Split

Arcana modules currently referenced from the Ashfall product profile are
echoaetherworks, echoarcanacore, echoarcaneindex, echocursecore,
echofamiliarcore, echogrimoire, echoriftworlds, echoritualcore, and
echospellcore. Arcana Division becomes the owning experience protocol for these
surfaces.
