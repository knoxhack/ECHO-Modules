package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndustrialMissionProvider implements TerminalMissionProvider {
   public static final IndustrialMissionProvider INSTANCE = new IndustrialMissionProvider();
   private static final String ACTION_SCAN = "scan_factory";
   private static final String ACTION_POI_HINT = "poi_hint";
   private static final String ACTION_CLAIM = "claim_cache";
   private static final List<Mission> MISSIONS = List.of(
      mission("reclaim_power", "Reclaim Power", "Generate 1,000 Thermal Flux and power the first ore line.", "Stage 1", 0, "scrap_dynamo", 1000, rewards("copper_flux_duct", 8, "scrap_fuel", 8)),
      mission("grind_wasteland", "Grind the Wasteland", "Process scrap, ore, and rust into usable industrial feedstock.", "Stage 1", 1, "salvage_shredder", 4, rewards("rust_dust", 8, "scrap_plate", 6)),
      mission("filters_survival", "Filters for Survival", "Automate basic gas mask filters through the Filter Press.", "Stage 2", 2, "filter_press", 8, rewards("gas_mask_filter", 8, "industrial_membrane", 3)),
      mission("dense_alloy", "Dense Alloy", "Build the Alloy Kiln path into Dense Alloy and reinforced frames.", "Stage 3", 3, "dense_alloy_plate", 1, rewards("dense_alloy_plate", 2, "heat_coil", 2)),
      mission("control_heat", "Control the Heat", "Install cooling support or survive a dangerous machine heat event.", "Stage 3", 4, "coolant_cell", 1, rewards("coolant_cell", 4, "heat_sink_upgrade", 1)),
      mission("clean_camp", "Clean the Camp", "Bring an Industrial Scrubber online and establish a safe-zone fallback.", "Stage 4", 5, "industrial_scrubber", 1, rewards("industrial_filter_core", 1, "coolant_cell", 2)),
      mission("reactor_waste", "Reactor Waste", "Process Rad Slag or install radiation shielding for hot reactor work.", "Stage 4", 6, "rad_slag", 1, rewards("radiation_shielding_upgrade", 1, "waste_canister", 4)),
      mission("hybrid_warning", "Hybrid Warning", "Scan hybrid cores and stabilize first Nexus-thermal processing.", "Stage 5", 7, "hybrid_thermal_core", 1, rewards("nexus_stabilizer_upgrade", 1, "field_relay", 2)),
      mission("factory_controller", "Factory Controller Online", "Connect five machines or ducts into a controller-visible factory.", "Stage 5", 8, "factory_controller", 5, rewards("factory_link_chip", 2, "smart_duct", 8)),
      mission("assembly_line", "Assembly Line", "Validate an Industrial Assembly Line and complete its first reinforced frame job.", "Stage 6", 9, "industrial_assembly_line_controller", 1, rewards("assembly_line_blueprint", 1, "conveyor_gear", 6)),
      mission("scrap_processor", "Scrap Processor", "Route scrap through a MultiblockCore processor and recover Scrap Plates.", "Stage 6", 10, "scrap_processor_controller", 4, rewards("scrap_processor_blueprint", 1, "scrap_plate", 8)),
      mission("plate_press", "Plate Press", "Press Scrap Plates into Refined Plates for durable factory frames.", "Stage 6", 11, "plate_press_controller", 4, rewards("plate_press_blueprint", 1, "refined_plate", 8)),
      mission("circuit_fabricator", "Circuit Fabricator", "Assemble Precision Circuits through robotic factory automation.", "Stage 7", 12, "circuit_fabricator_controller", 1, rewards("circuit_fabricator_blueprint", 1, "precision_circuit", 2)),
      mission("recipe_matrix_encoding", "Recipe Matrix Encoding", "Encode a Recipe Matrix Shard for advanced factory routing.", "Stage 7", 13, "recipe_matrix_core", 1, rewards("recipe_matrix_blueprint", 1, "recipe_matrix_shard", 1)),
      mission("nexus_furnace_array", "Nexus Furnace Array", "Form the Nexus Furnace Array and forge a Core Key Assembly through unstable factory automation.", "Stage 8", 14, "nexus_furnace_array_controller", 1, rewards("nexus_stabilizer_upgrade", 1, "field_relay", 2)),
      mission("logistics_auto_restock", "Logistics Auto-Restock", "Connect Logistics to an Industrial controller and dispatch an auto-restock.", "Stage 8", 15, "factory_link_chip", 1, rewards("factory_link_chip", 2, "smart_duct", 8)),
      mission("production_survived", "Production Survived", "Locate a Thermal Plant, defeat the Furnace Warden, and recover its core.", "Stage 9", 16, "warden_thermal_core", 1, rewards("warden_thermal_core", 1, "overclock_core", 1))
   );

   private IndustrialMissionProvider() {
   }

   public TerminalMissionChapter chapter() {
      return new TerminalMissionChapter(IndustrialTerminalIds.CHAPTER, "Industrial Nexus", "Where survival becomes infrastructure.", 70, 0xFFFF9F3D, true);
   }

   public List<TerminalMissionDefinition> missions(Player player) {
      return MISSIONS.stream().map(mission -> definition(player, mission)).toList();
   }

   static List<RouteMission> routeMissions() {
      return MISSIONS.stream()
         .map(mission -> new RouteMission(
            mission.id(),
            mission.key(),
            mission.title(),
            mission.briefing(),
            mission.phase(),
            "Factory",
            mission.poiHint()
         ))
         .toList();
   }

   public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
      Mission mission = mission(missionId);
      if (mission == null) {
         return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F, "LOCKED", "Industrial mission id not found.", "Rescan the Industrial Nexus chapter.", List.of());
      }
      float progress = player == null ? 0.0F : IndustrialProgress.progress(player, mission.key());
      boolean complete = progress >= 1.0F;
      boolean claimed = player != null && IndustrialProgress.claimed(player, mission.key());
      TerminalMissionStatus status = claimed ? TerminalMissionStatus.CLAIMED : complete ? TerminalMissionStatus.CLAIMABLE : TerminalMissionStatus.UNLOCKED;
      String detail = player == null ? "Telemetry offline." : mission.progressDetail(player, progress);
      List<TerminalMissionAction> actions = List.of(
         TerminalMissionAction.enabled(ACTION_SCAN, "SCAN FACTORY"),
         TerminalMissionAction.enabled(ACTION_POI_HINT, "POI HINT"),
         claimed ? TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Reward cache already claimed.") : complete ? TerminalMissionAction.enabled(ACTION_CLAIM, "CLAIM CACHE") : TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Complete mission objectives first.")
      );
      return new TerminalMissionSnapshot(mission.id(), status, progress, claimed ? "CLAIMED" : complete ? "CACHE READY" : "ACTIVE", detail,
         complete ? "Claim the Industrial support cache." : "Use SCAN FACTORY near your machines to update mission telemetry.", actions);
   }

   @Override
   public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      return TerminalMissionRole.OPTIONAL;
   }

   @Override
   public Optional<TerminalMissionRoutePlacement> routePlacement(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot,
      TerminalMissionRole role
   ) {
      int order = definition == null ? 0 : definition.missionOrder();
      int stage = definition == null ? 1 : stageFromPhase(definition.phaseTitle(), definition.phaseOrder());
      return Optional.of(TerminalMissionRoutePlacement.optional(routePhaseForStage(stage), order));
   }

   public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
      Mission mission = mission(missionId);
      if (mission == null) {
         return false;
      }
      if (ACTION_SCAN.equals(actionId)) {
         IndustrialProgress.FactoryScan scan = IndustrialProgress.scanFactory(player);
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Scan complete. Machines " + scan.machines() + ", ducts " + (scan.itemDucts() + scan.fluxDucts()) + ", stored " + scan.storedFlux() + " TF."));
         return true;
      }
      if (ACTION_POI_HINT.equals(actionId)) {
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // POI hint: " + IndustrialProgress.poiHint(player, mission.poiType(), mission.poiHint())));
         return true;
      }
      if (!ACTION_CLAIM.equals(actionId) || IndustrialProgress.progress(player, mission.key()) < 1.0F || IndustrialProgress.claimed(player, mission.key())) {
         return false;
      }
      List<ItemStack> rewards = mission.rewardStacks();
      if (!EchoCoreServices.storeTerminalRewards(player, mission.id().toString(), rewards)) {
         for (ItemStack stack : rewards) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
               player.drop(copy, false);
            }
         }
      }
      IndustrialProgress.claim(player, mission.key());
      player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Cache claimed for " + mission.title() + "."));
      return true;
   }

   private static TerminalMissionDefinition definition(Player player, Mission mission) {
      int have = player == null ? 0 : Math.round(IndustrialProgress.progress(player, mission.key()) * mission.need());
      boolean done = have >= mission.need();
      return new TerminalMissionDefinition(mission.id(), IndustrialTerminalIds.CHAPTER, mission.phase().toLowerCase(java.util.Locale.ROOT).replace(' ', '_'), mission.phase(), mission.order(), mission.order(), mission.title(), mission.briefing(), "Build and scan the relevant Industrial Nexus machinery. Optional compat stays soft when sister chapters are absent.", "Factory", "Production Complete", new ItemStack(mission.icon()), List.of(), List.of(TerminalMissionRequirement.custom(mission.title(), have + "/" + mission.need() + " telemetry", new ItemStack(mission.icon()), have, mission.need(), done)), mission.rewardStacks().stream().map(TerminalMissionReward::of).toList());
   }

   static int routePhaseForStage(int stage) {
      if (stage <= 4) {
         return 5;
      }
      if (stage <= 6) {
         return 10;
      }
      if (stage <= 8) {
         return 11;
      }
      return 12;
   }

   private static int stageFromPhase(String phase, int fallback) {
      if (phase != null) {
         String digits = phase.replaceAll("[^0-9]", "");
         if (!digits.isBlank()) {
            try {
               return Math.max(1, Integer.parseInt(digits));
            } catch (NumberFormatException ignored) {
            }
         }
      }
      return Math.max(1, fallback + 1);
   }

   private static Mission mission(String key, String title, String briefing, String phase, int order, String iconPath, int need, List<Reward> rewards) {
      return new Mission(IndustrialTerminalIds.id("mission/" + key), key, title, briefing, phase, order, iconPath, need, rewards);
   }

   private static Item resolveItem(String path) {
      Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", path));
      return item == null ? Items.BARRIER : item;
   }

   private static Mission mission(Identifier id) {
      return MISSIONS.stream().filter(mission -> mission.id().equals(id)).findFirst().orElse(null);
   }

   private static List<Reward> rewards(String a, int ac, String b, int bc) {
      return List.of(new Reward(a, ac), new Reward(b, bc));
   }

   private record Mission(Identifier id, String key, String title, String briefing, String phase, int order, String iconPath, int need, List<Reward> rewards) {
      Item icon() {
         return resolveItem(iconPath);
      }

      List<ItemStack> rewardStacks() {
         return rewards.stream().map(Reward::stack).toList();
      }

      String progressDetail(Player player, float progress) {
         int pct = Math.min(100, Math.round(progress * 100.0F));
         return switch (key) {
            case "reclaim_power" -> "Thermal Flux generated: " + IndustrialProgress.value(player, "thermal_flux_generated") + "/1000 TF (" + pct + "%).";
            case "factory_controller" -> "Controller scan target: " + IndustrialProgress.value(player, "machines") + "/5 linked machines or ducts.";
            case "clean_camp" -> IndustrialProgress.flag(player, "safe_zone") ? "Safe-zone fallback established." : "No active scrubber zone recorded yet.";
            case "hybrid_warning" -> IndustrialProgress.flag(player, "nexus_thermal_warning") ? "Nexus thermal warning recorded." : "No Nexus thermal event recorded.";
            case "production_survived" -> IndustrialProgress.flag(player, "furnace_warden_defeated") ? "Furnace Warden defeated." : "Thermal Plant boss not cleared.";
            case "assembly_line" -> IndustrialProgress.flag(player, "formed_industrial_assembly_line") ? "Assembly Line structure validated." : "No Assembly Line validation recorded.";
            case "scrap_processor" -> "Scrap processing completions: " + IndustrialProgress.value(player, "task_process_scrap_into_scrap_plate") + "/" + need + ".";
            case "plate_press" -> "Plate press completions: " + IndustrialProgress.value(player, "task_press_scrap_plate_into_refined_plate") + "/" + need + ".";
            case "circuit_fabricator" -> "Circuit fabrication completions: " + IndustrialProgress.value(player, "task_assemble_precision_circuit") + "/" + need + ".";
            case "recipe_matrix_encoding" -> IndustrialProgress.flag(player, "task_encode_recipe_matrix_shard_complete") ? "Recipe Matrix Shard encoded." : "Awaiting matrix encoding completion.";
            case "nexus_furnace_array" -> IndustrialProgress.flag(player, "task_forge_core_key_assembly_complete") ? "Core Key Assembly forged through the Nexus Furnace Array." : "Awaiting Nexus Furnace Array unstable processing completion.";
            case "logistics_auto_restock" -> IndustrialProgress.flag(player, "logistics_auto_restock_requested") ? "Logistics auto-restock dispatched." : "No auto-restock dispatch recorded.";
            default -> title + " telemetry: " + pct + "%.";
         };
      }

      String poiHint() {
         return switch (key) {
            case "production_survived" -> "Abandoned Thermal Plants contain the Warden arena and wake-core caches.";
            case "reactor_waste" -> "Reactor Cooling Stations favor radiation zones and shielded pipe fragments.";
            case "hybrid_warning" -> "Nexus Heat Exchanger Ruins carry purple-blue conduits and static fluid leaks.";
            case "nexus_furnace_array" -> "Use the Nexus Furnace Array blueprint after matrix encoding; it converts stabilized Nexus cores into late-game key assemblies.";
            case "factory_controller" -> "Rusted Factory Complexes contain controller schematics and duct caches.";
            case "assembly_line" -> "Industrial Assembly Line blueprints teach the first MultiblockCore factory loop.";
            case "logistics_auto_restock" -> "Enable auto-restock from a formed Industrial controller once Logistics routes are online.";
            default -> "Search newly generated wasteland chunks for factory lights, vents, and warning stripe pads.";
         };
      }

      String poiType() {
         return switch (key) {
            case "production_survived" -> "abandoned_thermal_plant";
            case "reactor_waste" -> "reactor_cooling_station";
            case "hybrid_warning", "nexus_furnace_array" -> "nexus_heat_exchanger_ruins";
            case "factory_controller", "assembly_line", "scrap_processor", "plate_press", "circuit_fabricator", "recipe_matrix_encoding", "logistics_auto_restock" -> "rusted_factory_complex";
            case "control_heat", "clean_camp" -> "geothermal_drill_site";
            default -> "";
         };
      }
   }

   static record RouteMission(Identifier id, String key, String title, String briefing, String phase, String category, String nextAction) {
   }

   private record Reward(String itemPath, int count) {
      ItemStack stack() {
         return new ItemStack(resolveItem(itemPath), count);
      }
   }
}
