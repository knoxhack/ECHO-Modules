package com.knoxhack.echoblockworks.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BlockworksCatalog {
   private static final Set<BlockworksTheme> INDUSTRIAL_TAGS = Set.of(BlockworksTheme.INDUSTRIAL);
   private static final Set<BlockworksTheme> HAZARD_TAGS = Set.of(BlockworksTheme.INDUSTRIAL, BlockworksTheme.HAZARD);
   private static final Set<BlockworksTheme> RUIN_TAGS = Set.of(BlockworksTheme.RUINED);
   private static final Set<BlockworksTheme> TERMINAL_TAGS = Set.of(BlockworksTheme.TERMINAL, BlockworksTheme.ECHO_TECH);
   private static final Set<BlockworksTheme> CIRCUIT_TAGS = Set.of(BlockworksTheme.ECHO_TECH, BlockworksTheme.TERMINAL);
   private static final Set<BlockworksTheme> ORBITAL_TAGS = Set.of(BlockworksTheme.ORBITAL);
   private static final Set<BlockworksTheme> NEXUS_TAGS = Set.of(BlockworksTheme.NEXUS);
   private static final Set<BlockworksTheme> BLACKBOX_TAGS = Set.of(BlockworksTheme.BLACKBOX, BlockworksTheme.ARCHIVE);
   private static final Set<BlockworksTheme> RECLAMATION_TAGS = Set.of(BlockworksTheme.RECLAMATION, BlockworksTheme.LAB);

   private static final List<BlockworksFamily> FAMILIES = List.of(
      family("reinforced_metal", "Reinforced Metal", BlockworksTheme.INDUSTRIAL, "dark ECHO infrastructure metal", BlockworksUnlockTier.INDUSTRIAL, true,
         variant("panel", "Panel", INDUSTRIAL_TAGS),
         variant("riveted", "Riveted", INDUSTRIAL_TAGS),
         variant("grate", "Grate", INDUSTRIAL_TAGS),
         variant("frame", "Frame", INDUSTRIAL_TAGS),
         variant("cracked", "Cracked", RUIN_TAGS),
         variant("hazard_stripe", "Hazard Stripe", HAZARD_TAGS),
         lit("lit_panel", "Lit Panel", 10, INDUSTRIAL_TAGS),
         variant("pillar", "Pillar", INDUSTRIAL_TAGS)),
      family("rusted_metal", "Rusted Metal", BlockworksTheme.RUINED, "orange-brown ruined industrial metal", BlockworksUnlockTier.STARTER, true,
         variant("panel", "Panel", RUIN_TAGS),
         variant("riveted", "Riveted", RUIN_TAGS),
         variant("grate", "Grate", RUIN_TAGS),
         variant("pipe_wall", "Pipe Wall", Set.of(BlockworksTheme.RUINED, BlockworksTheme.INDUSTRIAL)),
         variant("cracked", "Cracked", RUIN_TAGS),
         variant("hazard_stripe", "Hazard Stripe", HAZARD_TAGS),
         variant("dark_plate", "Dark Plate", RUIN_TAGS),
         variant("pillar", "Pillar", RUIN_TAGS)),
      family("ashstone", "Ashstone", BlockworksTheme.RUINED, "ashy stone for Ashfall ruins", BlockworksUnlockTier.STARTER, true,
         variant("raw", "Raw", RUIN_TAGS),
         variant("brick", "Brick", RUIN_TAGS),
         variant("cracked_brick", "Cracked Brick", RUIN_TAGS),
         variant("chiseled", "Chiseled", RUIN_TAGS),
         variant("tile", "Tile", RUIN_TAGS),
         variant("pillar", "Pillar", RUIN_TAGS),
         variant("debris", "Debris", RUIN_TAGS),
         variant("smooth", "Smooth", RUIN_TAGS)),
      family("charred_concrete", "Charred Concrete", BlockworksTheme.RUINED, "scorched city concrete", BlockworksUnlockTier.STARTER, true,
         variant("smooth", "Smooth", RUIN_TAGS),
         variant("cracked", "Cracked", RUIN_TAGS),
         variant("tile", "Tile", RUIN_TAGS),
         variant("rebar", "Rebar", Set.of(BlockworksTheme.RUINED, BlockworksTheme.INDUSTRIAL)),
         variant("road_plate", "Road Plate", Set.of(BlockworksTheme.CONVOY, BlockworksTheme.RUINED)),
         variant("warning_stripe", "Warning Stripe", HAZARD_TAGS),
         variant("scorched", "Scorched", RUIN_TAGS),
         variant("broken", "Broken", RUIN_TAGS)),
      family("terminal_panel", "Terminal Panel", BlockworksTheme.TERMINAL, "command room panels and screens", BlockworksUnlockTier.TERMINAL_RESTORED, false,
         variant("wall_panel", "Wall Panel", TERMINAL_TAGS),
         lit("screen", "Screen", 7, TERMINAL_TAGS),
         variant("trim", "Trim", TERMINAL_TAGS),
         lit("cyan_lit", "Cyan Lit", 11, TERMINAL_TAGS),
         variant("dark_panel", "Dark Panel", TERMINAL_TAGS),
         lit("data_panel", "Data Panel", 6, TERMINAL_TAGS),
         variant("warning_panel", "Warning Panel", HAZARD_TAGS),
         variant("server_rack", "Server Rack", TERMINAL_TAGS)),
      family("echo_circuit", "ECHO Circuit", BlockworksTheme.ECHO_TECH, "cyan ECHO circuit surfaces", BlockworksUnlockTier.TERMINAL_RESTORED, false,
         variant("circuit_panel", "Circuit Panel", CIRCUIT_TAGS),
         variant("data_conduit", "Data Conduit", CIRCUIT_TAGS),
         lit("service_node", "Service Node", 6, CIRCUIT_TAGS),
         variant("matrix", "Matrix", CIRCUIT_TAGS),
         lit("glowing_circuit", "Glowing Circuit", 12, CIRCUIT_TAGS),
         variant("offline_circuit", "Offline Circuit", CIRCUIT_TAGS),
         variant("warning_circuit", "Warning Circuit", HAZARD_TAGS),
         variant("encrypted_circuit", "Encrypted Circuit", CIRCUIT_TAGS)),
      family("orbital_hull", "Orbital Hull", BlockworksTheme.ORBITAL, "orbital station hull plating", BlockworksUnlockTier.ORBITAL, true,
         variant("hull_panel", "Hull Panel", ORBITAL_TAGS),
         variant("thermal_tile", "Thermal Tile", ORBITAL_TAGS),
         variant("airlock_frame", "Airlock Frame", ORBITAL_TAGS),
         variant("docking_trim", "Docking Trim", ORBITAL_TAGS),
         lit("lit_strip", "Lit Strip", 9, ORBITAL_TAGS),
         variant("damaged_hull", "Damaged Hull", Set.of(BlockworksTheme.ORBITAL, BlockworksTheme.RUINED)),
         variant("white_hull", "White Hull", ORBITAL_TAGS),
         variant("black_hull", "Black Hull", ORBITAL_TAGS)),
      family("nexus_crystal", "Nexus Crystal", BlockworksTheme.NEXUS, "purple and cyan Nexus glass and crystal", BlockworksUnlockTier.NEXUS, false,
         glass("nexus_glass", "Nexus Glass", 4, NEXUS_TAGS),
         variant("nexus_frame", "Nexus Frame", NEXUS_TAGS),
         glass("glowing_crystal", "Glowing Crystal", 13, NEXUS_TAGS),
         glass("cracked_crystal", "Cracked Crystal", 3, NEXUS_TAGS),
         lit("energy_conduit", "Energy Conduit", 10, NEXUS_TAGS),
         variant("anomaly_tile", "Anomaly Tile", NEXUS_TAGS),
         variant("pillar", "Pillar", NEXUS_TAGS),
         lit("rift_panel", "Rift Panel", 12, NEXUS_TAGS)),
      family("blackbox_vault", "Blackbox Vault", BlockworksTheme.BLACKBOX, "secure archive alloy and warning glass", BlockworksUnlockTier.BLACKBOX, true,
         variant("vault_wall", "Vault Wall", BLACKBOX_TAGS),
         variant("locked_panel", "Locked Panel", BLACKBOX_TAGS),
         variant("archive_panel", "Archive Panel", BLACKBOX_TAGS),
         glass("memory_glass", "Memory Glass", 5, BLACKBOX_TAGS),
         lit("warning_light", "Warning Light", 9, HAZARD_TAGS),
         variant("dark_alloy", "Dark Alloy", BLACKBOX_TAGS),
         variant("secure_frame", "Secure Frame", BLACKBOX_TAGS),
         variant("cracked_vault", "Cracked Vault", BLACKBOX_TAGS)),
      family("reclamation_glass", "Reclamation Glass", BlockworksTheme.RECLAMATION, "greenhouse glass and reclamation panels", BlockworksUnlockTier.RECLAMATION, false,
         glass("clear_glass", "Clear Glass", 0, RECLAMATION_TAGS),
         glass("framed_glass", "Framed Glass", 0, RECLAMATION_TAGS),
         glass("green_glass", "Green Glass", 0, RECLAMATION_TAGS),
         glass("overgrown_glass", "Overgrown Glass", 0, RECLAMATION_TAGS),
         variant("hydroponic_panel", "Hydroponic Panel", RECLAMATION_TAGS),
         lit("lit_grow_panel", "Lit Grow Panel", 12, RECLAMATION_TAGS),
         glass("dome_panel", "Dome Panel", 0, RECLAMATION_TAGS),
         variant("irrigation_pipe", "Irrigation Pipe", RECLAMATION_TAGS))
   );

   private static final List<BlockworksDetailSpec> DETAILS = List.of(
      detail("echo_strip_light", "ECHO Strip Light", BlockworksDetailKind.CEILING_STRIP, BlockworksTheme.ECHO_TECH, BlockworksUnlockTier.INDUSTRIAL, 13, false),
      detail("warning_beacon", "Warning Beacon", BlockworksDetailKind.FULL, BlockworksTheme.HAZARD, BlockworksUnlockTier.INDUSTRIAL, 12, false),
      detail("flickering_warning_light", "Flickering Warning Light", BlockworksDetailKind.WALL_MOUNTED, BlockworksTheme.HAZARD, BlockworksUnlockTier.INDUSTRIAL, 9, true),
      detail("data_wall", "Data Wall", BlockworksDetailKind.DIRECTIONAL_FULL, BlockworksTheme.TERMINAL, BlockworksUnlockTier.TERMINAL_RESTORED, 4, false),
      detail("broken_monitor", "Broken Monitor", BlockworksDetailKind.WALL_MOUNTED, BlockworksTheme.RUINED, BlockworksUnlockTier.STARTER, 0, false),
      detail("server_cabinet", "Server Cabinet", BlockworksDetailKind.DIRECTIONAL_FULL, BlockworksTheme.TERMINAL, BlockworksUnlockTier.TERMINAL_RESTORED, 3, false),
      detail("cable_bundle", "Cable Bundle", BlockworksDetailKind.WALL_MOUNTED, BlockworksTheme.INDUSTRIAL, BlockworksUnlockTier.INDUSTRIAL, 0, false),
      detail("wall_pipe", "Wall Pipe", BlockworksDetailKind.WALL_MOUNTED, BlockworksTheme.INDUSTRIAL, BlockworksUnlockTier.INDUSTRIAL, 0, false),
      detail("ceiling_pipe", "Ceiling Pipe", BlockworksDetailKind.CEILING_STRIP, BlockworksTheme.INDUSTRIAL, BlockworksUnlockTier.INDUSTRIAL, 0, false),
      detail("steam_vent", "Steam Vent", BlockworksDetailKind.FLOOR_LOW, BlockworksTheme.INDUSTRIAL, BlockworksUnlockTier.INDUSTRIAL, 0, true),
      detail("sparking_cable_panel", "Sparking Cable Panel", BlockworksDetailKind.WALL_MOUNTED, BlockworksTheme.HAZARD, BlockworksUnlockTier.INDUSTRIAL, 7, true),
      detail("rubble_pile", "Rubble Pile", BlockworksDetailKind.FLOOR_LOW, BlockworksTheme.RUINED, BlockworksUnlockTier.STARTER, 0, false),
      detail("scattered_debris", "Scattered Debris", BlockworksDetailKind.FLOOR_LOW, BlockworksTheme.RUINED, BlockworksUnlockTier.STARTER, 0, false),
      detail("hanging_wire", "Hanging Wire", BlockworksDetailKind.CEILING_STRIP, BlockworksTheme.RUINED, BlockworksUnlockTier.STARTER, 0, false),
      detail("hologram_floor_projector", "Hologram Floor Projector", BlockworksDetailKind.FLOOR_LOW, BlockworksTheme.ECHO_TECH, BlockworksUnlockTier.TERMINAL_RESTORED, 10, true),
      detail("signal_dish_decorative", "Signal Dish Decorative", BlockworksDetailKind.DIRECTIONAL_FULL, BlockworksTheme.ORBITAL, BlockworksUnlockTier.ORBITAL, 0, false)
   );

   private static final List<BlockworksWorldgenSite> WORLDGEN_SITES = List.of(
      site("ashfall_street_ruin", "Ashfall Street Ruin", "ashfall_ruined_city", "showcase/ashfall_street_ruin", "Ashfall ruined city streets and survivor bases"),
      site("crash_zone_fragment", "Crash Zone Fragment", "crash_zone", "showcase/crash_zone_fragment", "crash zones, drop scars, and scattered wreck interiors"),
      site("terminal_bunker_alcove", "Terminal Bunker Alcove", "terminal_bunker", "showcase/terminal_bunker_alcove", "Terminal rooms, command bunkers, and restored tech alcoves"),
      site("orbital_airlock_remnant", "Orbital Airlock Remnant", "orbital_station", "showcase/orbital_airlock_remnant", "Orbital Remnants interiors and stationfall set pieces"),
      site("nexus_gate_shard", "Nexus Gate Shard", "nexus_gate", "showcase/nexus_gate_shard", "Nexus Protocol gate chambers and anomaly fragments"),
      site("blackbox_vault_breach", "Blackbox Vault Breach", "blackbox_vault", "showcase/blackbox_vault_breach", "Blackbox archive vault breaches and secure ruins"),
      site("reclamation_dome_remnant", "Reclamation Dome Remnant", "reclamation_dome", "showcase/reclamation_dome_remnant", "Agriculture Reclamation domes and greenhouse ruins"),
      site("convoy_depot_pullout", "Convoy Depot Pullout", "convoy_depot", "showcase/convoy_depot_pullout", "Convoy depots, road pullouts, and field repair pads")
   );

   private static final List<BlockworksPaletteKit> PALETTE_KITS = List.of(
      kit("ashfall_ruined_city", "Ashfall Ruined City",
         "Ashy stone, scorched concrete, rusted salvage, and ground debris for collapsed streets.",
         "Ruined Ashfall streets, survivor bases, and old civic blocks.",
         BlockworksTheme.RUINED,
         List.of("ashstone", "charred_concrete", "rusted_metal"),
         List.of("ashstone_brick", "ashstone_cracked_brick", "charred_concrete_cracked", "charred_concrete_broken"),
         List.of("rusted_metal_panel", "rusted_metal_cracked", "rubble_pile", "scattered_debris"),
         "ashfall_street_ruin"),
      kit("crash_zone", "Crash Zone",
         "Scorched concrete, exposed rebar, damaged hull, sparks, vents, and debris.",
         "Impact scars, wreck interiors, drop zones, and damaged landing paths.",
         BlockworksTheme.RUINED,
         List.of("charred_concrete", "rusted_metal", "orbital_hull"),
         List.of("charred_concrete_scorched", "charred_concrete_rebar", "rusted_metal_cracked", "orbital_hull_damaged_hull"),
         List.of("sparking_cable_panel", "steam_vent", "rubble_pile", "scattered_debris"),
         "crash_zone_fragment"),
      kit("terminal_bunker", "Terminal Bunker",
         "Terminal screens, ECHO circuitry, server cabinets, data walls, and cyan-lit command surfaces.",
         "Command bunkers, restored control rooms, diagnostics labs, and mission terminals.",
         BlockworksTheme.TERMINAL,
         List.of("terminal_panel", "echo_circuit", "reinforced_metal"),
         List.of("terminal_panel_wall_panel", "terminal_panel_screen", "terminal_panel_data_panel", "echo_circuit_matrix"),
         List.of("data_wall", "server_cabinet", "echo_strip_light", "reinforced_metal_frame"),
         "terminal_bunker_alcove"),
      kit("cyberglass_control_room", "CyberGlass Control Room",
         "Dark glass panels, cyan ECHO circuitry, thin neon borders, server hardware, and soft magenta-violet accents.",
         "ThemeCore CyberGlass rooms, holographic command centers, clean labs, and premium ECHO UI set dressing.",
         BlockworksTheme.CYBERGLASS,
         List.of("terminal_panel", "echo_circuit", "nexus_crystal", "reinforced_metal"),
         List.of("terminal_panel_cyan_lit", "echo_circuit_glowing_circuit", "echo_circuit_matrix", "nexus_crystal_nexus_glass"),
         List.of("hologram_floor_projector", "echo_strip_light", "server_cabinet", "nexus_crystal_energy_conduit"),
         "terminal_bunker_alcove"),
      kit("orbital_station", "Orbital Station",
         "Station hull plating, thermal tiles, airlock frames, white/black hull contrast, and signal hardware.",
         "Orbital interiors, airlock remnants, station corridors, and launch infrastructure.",
         BlockworksTheme.ORBITAL,
         List.of("orbital_hull", "reinforced_metal"),
         List.of("orbital_hull_hull_panel", "orbital_hull_thermal_tile", "orbital_hull_airlock_frame", "orbital_hull_lit_strip"),
         List.of("orbital_hull_white_hull", "orbital_hull_black_hull", "ceiling_pipe", "signal_dish_decorative"),
         "orbital_airlock_remnant"),
      kit("nexus_gate", "Nexus Gate",
         "Nexus glass, glowing crystal, rift panels, encrypted circuits, and anomaly accents.",
         "Nexus chambers, gate shards, anomaly containment rooms, and reality-breach set pieces.",
         BlockworksTheme.NEXUS,
         List.of("nexus_crystal", "echo_circuit"),
         List.of("nexus_crystal_nexus_glass", "nexus_crystal_glowing_crystal", "nexus_crystal_energy_conduit", "nexus_crystal_rift_panel"),
         List.of("nexus_crystal_anomaly_tile", "echo_circuit_encrypted_circuit", "hologram_floor_projector"),
         "nexus_gate_shard"),
      kit("blackbox_vault", "Blackbox Vault",
         "Dark archive alloy, locked panels, memory glass, secure frames, and orange warning light.",
         "Blackbox archives, breached vaults, sealed data rooms, and high-security interiors.",
         BlockworksTheme.BLACKBOX,
         List.of("blackbox_vault", "echo_circuit"),
         List.of("blackbox_vault_vault_wall", "blackbox_vault_locked_panel", "blackbox_vault_archive_panel", "blackbox_vault_memory_glass"),
         List.of("blackbox_vault_warning_light", "blackbox_vault_secure_frame", "echo_circuit_encrypted_circuit"),
         "blackbox_vault_breach"),
      kit("reclamation_dome", "Reclamation Dome",
         "Transparent greenhouse glass, overgrowth, hydroponic panels, grow lights, and irrigation detail.",
         "Agriculture domes, reclamation labs, greenhouse ruins, and restored food-route spaces.",
         BlockworksTheme.RECLAMATION,
         List.of("reclamation_glass", "echo_circuit"),
         List.of("reclamation_glass_clear_glass", "reclamation_glass_framed_glass", "reclamation_glass_green_glass", "reclamation_glass_overgrown_glass"),
         List.of("reclamation_glass_lit_grow_panel", "reclamation_glass_hydroponic_panel", "reclamation_glass_irrigation_pipe", "hologram_floor_projector"),
         "reclamation_dome_remnant"),
      kit("convoy_depot", "Convoy Depot",
         "Road plates, warning stripes, frames, rusted pipe walls, beacons, and wall pipes.",
         "Convoy depots, roadside pullouts, repair pads, checkpoints, and loading bays.",
         BlockworksTheme.CONVOY,
         List.of("charred_concrete", "reinforced_metal", "rusted_metal"),
         List.of("charred_concrete_road_plate", "charred_concrete_warning_stripe", "reinforced_metal_frame", "rusted_metal_pipe_wall"),
         List.of("warning_beacon", "wall_pipe", "signal_dish_decorative"),
         "convoy_depot_pullout"),
      kit("industrial_factory", "Industrial Factory",
         "Reinforced metal, rusted plates, hazard markings, grates, pipes, and steam vents.",
         "Factories, machine halls, depot interiors, and future MultiblockCore casing sets.",
         BlockworksTheme.INDUSTRIAL,
         List.of("reinforced_metal", "rusted_metal", "charred_concrete"),
         List.of("reinforced_metal_panel", "reinforced_metal_riveted", "reinforced_metal_grate", "reinforced_metal_hazard_stripe"),
         List.of("rusted_metal_pipe_wall", "rusted_metal_dark_plate", "ceiling_pipe", "steam_vent"),
         null),
      kit("starter_base", "Starter Base",
         "Cheap, sturdy starter palette with Ashstone, smooth charred concrete, rusted panels, and simple details.",
         "Early player bases, field shelters, safe rooms, and first-night Ashfall workshops.",
         BlockworksTheme.RUINED,
         List.of("ashstone", "charred_concrete", "rusted_metal", "reinforced_metal"),
         List.of("ashstone_raw", "ashstone_smooth", "charred_concrete_smooth", "rusted_metal_panel"),
         List.of("reinforced_metal_panel", "rubble_pile", "wall_pipe", "echo_strip_light"),
         null)
   );

   private static final List<BlockworksBlockInfo> BLOCKS = buildBlocks();
   private static final Map<String, BlockworksBlockInfo> BY_ID = BLOCKS.stream()
      .collect(LinkedHashMap::new, (map, info) -> map.put(info.blockId(), info), LinkedHashMap::putAll);
   private static final Map<String, BlockworksFamily> BY_FAMILY_ID = FAMILIES.stream()
      .collect(LinkedHashMap::new, (map, family) -> map.put(family.id(), family), LinkedHashMap::putAll);
   private static final Map<String, BlockworksPaletteKit> BY_KIT_ID = PALETTE_KITS.stream()
      .collect(LinkedHashMap::new, (map, kit) -> map.put(kit.id(), kit), LinkedHashMap::putAll);

   private BlockworksCatalog() {
   }

   public static List<BlockworksFamily> families() {
      return FAMILIES;
   }

   public static List<BlockworksDetailSpec> details() {
      return DETAILS;
   }

   public static List<BlockworksWorldgenSite> worldgenSites() {
      return WORLDGEN_SITES;
   }

   public static List<BlockworksPaletteKit> paletteKits() {
      return PALETTE_KITS;
   }

   public static List<BlockworksBlockInfo> blockInfos() {
      return BLOCKS;
   }

   public static Optional<BlockworksFamily> family(String id) {
      return Optional.ofNullable(BY_FAMILY_ID.get(normalize(id)));
   }

   public static Optional<BlockworksPaletteKit> paletteKit(String id) {
      return Optional.ofNullable(BY_KIT_ID.get(normalize(id)));
   }

   public static Optional<BlockworksBlockInfo> blockInfo(String blockId) {
      return Optional.ofNullable(BY_ID.get(normalize(blockId)));
   }

   public static List<BlockworksBlockInfo> conversionTargets(BlockworksBlockInfo source) {
      if (source == null) {
         return List.of();
      }
      return BLOCKS.stream()
         .filter(info -> info.family().id().equals(source.family().id()))
         .filter(info -> info.shape() == source.shape())
         .sorted(Comparator.comparing(info -> source.family().variants().indexOf(info.variant())))
         .toList();
   }

   public static List<BlockworksBlockInfo> conversionTargets(BlockworksBlockInfo source, BlockworksPaletteKit kit) {
      if (source == null || kit == null || !kit.includesFamily(source.family().id())) {
         return List.of();
      }
      return conversionTargets(source).stream()
         .filter(info -> kit.includesBlock(blockId(source.family().id(), info.variant().id(), BlockworksShapeKind.FULL)))
         .toList();
   }

   public static Optional<BlockworksBlockInfo> target(String familyId, String variantId, BlockworksShapeKind shape) {
      String id = blockId(normalize(familyId), normalize(variantId), shape);
      return blockInfo(id);
   }

   public static Optional<BlockworksBlockInfo> cycle(BlockworksBlockInfo source, boolean backwards) {
      List<BlockworksBlockInfo> targets = conversionTargets(source);
      if (targets.isEmpty()) {
         return Optional.empty();
      }
      int index = targets.indexOf(source);
      if (index < 0) {
         index = 0;
      }
      int next = backwards
         ? (index - 1 + targets.size()) % targets.size()
         : (index + 1) % targets.size();
      return Optional.of(targets.get(next));
   }

   public static String blockId(String familyId, String variantId, BlockworksShapeKind shape) {
      return normalize(familyId) + "_" + normalize(variantId) + shape.suffix();
   }

   private static List<BlockworksBlockInfo> buildBlocks() {
      List<BlockworksBlockInfo> blocks = new ArrayList<>();
      for (BlockworksFamily family : FAMILIES) {
         for (BlockworksVariant variant : family.variants()) {
            for (BlockworksShapeKind shape : BlockworksShapeKind.values()) {
               if (!variant.supports(shape)) {
                  continue;
               }
               String id = blockId(family.id(), variant.id(), shape);
               String displayName = displayName(family, variant, shape);
               blocks.add(new BlockworksBlockInfo(family, variant, shape, id, displayName));
            }
         }
      }
      return List.copyOf(blocks);
   }

   private static String displayName(BlockworksFamily family, BlockworksVariant variant, BlockworksShapeKind shape) {
      String base = switch (family.id()) {
         case "echo_circuit" -> variant.displayName().startsWith("Circuit") || variant.displayName().endsWith("Circuit")
            ? "ECHO " + variant.displayName()
            : "ECHO Circuit " + variant.displayName();
         case "terminal_panel" -> variant.id().equals("screen") ? "Terminal Screen Panel" : "Terminal " + variant.displayName();
         case "nexus_crystal" -> variant.id().startsWith("nexus_") ? "Nexus " + variant.displayName().replace("Nexus ", "") : "Nexus " + variant.displayName();
         case "blackbox_vault" -> "Blackbox " + variant.displayName();
         case "orbital_hull" -> "Orbital " + variant.displayName();
         case "reclamation_glass" -> variant.id().contains("glass") || variant.id().contains("dome")
            ? "Reclamation " + variant.displayName()
            : "Reclamation " + variant.displayName();
         default -> family.displayName() + " " + variant.displayName();
      };
      return switch (shape) {
         case FULL -> base;
         case SLAB -> base + " Slab";
         case STAIRS -> base + " Stairs";
         case WALL -> base.endsWith(" Wall") ? base + " Segment" : base + " Wall";
      };
   }

   private static BlockworksFamily family(String id, String displayName, BlockworksTheme theme, String style, BlockworksUnlockTier unlockTier, boolean structural, BlockworksVariant... variants) {
      List<BlockworksVariant> normalized = new ArrayList<>();
      for (BlockworksVariant variant : variants) {
         normalized.add(structural && !variant.supportsSlab()
            ? new BlockworksVariant(variant.id(), variant.displayName(), true, true, true, variant.light(), variant.animated(), variant.tags())
            : variant);
      }
      return new BlockworksFamily(id, displayName, theme, style, unlockTier, normalized);
   }

   private static BlockworksVariant variant(String id, String displayName, Set<BlockworksTheme> tags) {
      return new BlockworksVariant(id, displayName, false, false, false, 0, false, tags);
   }

   private static BlockworksVariant lit(String id, String displayName, int light, Set<BlockworksTheme> tags) {
      return new BlockworksVariant(id, displayName, false, false, false, light, true, tags);
   }

   private static BlockworksVariant glass(String id, String displayName, int light, Set<BlockworksTheme> tags) {
      return new BlockworksVariant(id, displayName, false, false, false, light, false, tags);
   }

   private static BlockworksDetailSpec detail(String id, String displayName, BlockworksDetailKind kind, BlockworksTheme theme, BlockworksUnlockTier unlockTier, int light, boolean animated) {
      return new BlockworksDetailSpec(id, displayName, kind, theme, unlockTier, light, animated);
   }

   private static BlockworksWorldgenSite site(String id, String displayName, String paletteId, String structureTemplate, String recommendedUsage) {
      return new BlockworksWorldgenSite(id, displayName, paletteId, structureTemplate, recommendedUsage);
   }

   private static BlockworksPaletteKit kit(String id, String displayName, String description, String recommendedUsage, BlockworksTheme theme,
         List<String> familyIds, List<String> featuredBlockIds, List<String> accentBlockIds, String worldgenSiteId) {
      return new BlockworksPaletteKit(id, displayName, description, recommendedUsage, theme, familyIds, featuredBlockIds, accentBlockIds,
         Optional.ofNullable(worldgenSiteId));
   }

   private static String normalize(String id) {
      return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
   }
}
