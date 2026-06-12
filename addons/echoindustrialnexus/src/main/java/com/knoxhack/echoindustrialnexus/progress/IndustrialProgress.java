package com.knoxhack.echoindustrialnexus.progress;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoindustrialnexus.block.IndustrialFluxDuctBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialItemDuctBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxStorage;
import com.knoxhack.echoindustrialnexus.integration.IndustrialMissionHooks;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class IndustrialProgress {
   public static final String ROOT = "echoindustrialnexus_progress";

   private IndustrialProgress() {
   }

   public static CompoundTag data(Player player) {
      if (player == null) {
         return new CompoundTag();
      }
      CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
      player.getPersistentData().put(ROOT, root);
      return root;
   }

   public static FactoryScan scanFactory(Player player) {
      int machines = 0;
      int itemDucts = 0;
      int fluxDucts = 0;
      int controllers = 0;
      int scrubbers = 0;
      int hot = 0;
      int stored = 0;
      int capacity = 0;
      Level level = player.level();
      BlockPos center = player.blockPosition();
      int radius = 12;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -5, -radius), center.offset(radius, 5, radius))) {
         if (level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine) {
            machines++;
            if (machine.kind().factoryController()) {
               controllers++;
            }
            if (machine.kind() == IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER) {
               scrubbers++;
            }
            if (machine.heatLevel() >= 85) {
               hot++;
            }
            if (machine instanceof ThermalFluxStorage storage) {
               stored += storage.getFluxStored();
               capacity += storage.getMaxFluxStored();
            }
         } else if (level.getBlockState(pos).getBlock() instanceof IndustrialFluxDuctBlock) {
            fluxDucts++;
         } else if (level.getBlockState(pos).getBlock() instanceof IndustrialItemDuctBlock) {
            itemDucts++;
         }
      }
      CompoundTag data = data(player);
      int previousMachines = data.getIntOr("machines", 0);
      int previousScrubbers = data.getIntOr("scrubbers", 0);
      int previousHot = data.getIntOr("hot_machines", 0);
      data.putInt("machines", Math.max(data.getIntOr("machines", 0), machines));
      data.putInt("item_ducts", Math.max(data.getIntOr("item_ducts", 0), itemDucts));
      data.putInt("flux_ducts", Math.max(data.getIntOr("flux_ducts", 0), fluxDucts));
      data.putInt("controllers", Math.max(data.getIntOr("controllers", 0), controllers));
      data.putInt("scrubbers", Math.max(data.getIntOr("scrubbers", 0), scrubbers));
      data.putInt("hot_machines", Math.max(data.getIntOr("hot_machines", 0), hot));
      data.putInt("stored_flux", Math.max(data.getIntOr("stored_flux", 0), stored));
      data.putInt("flux_capacity", Math.max(data.getIntOr("flux_capacity", 0), capacity));
      data.putBoolean("factory_scanned", true);
      if (player instanceof ServerPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress progress = IndustrialWorldProgress.get(serverLevel);
         progress.maxPlayerStat(player.getUUID(), "machines", machines);
         progress.maxPlayerStat(player.getUUID(), "item_ducts", itemDucts);
         progress.maxPlayerStat(player.getUUID(), "flux_ducts", fluxDucts);
         progress.maxPlayerStat(player.getUUID(), "controllers", controllers);
         progress.maxPlayerStat(player.getUUID(), "scrubbers", scrubbers);
         progress.maxPlayerStat(player.getUUID(), "hot_machines", hot);
         progress.maxPlayerStat(player.getUUID(), "stored_flux", stored);
         progress.maxPlayerStat(player.getUUID(), "flux_capacity", capacity);
         progress.setPlayerFlag(player.getUUID(), "factory_scanned", true);
         EchoCoreServices.discoverVisibleRouteRecords((ServerPlayer)player);
      }
      IndustrialMissionHooks.recordFactoryScan(
         player,
         Math.max(0, machines - previousMachines),
         Math.max(0, scrubbers - previousScrubbers),
         Math.max(0, hot - previousHot)
      );
      return new FactoryScan(machines, itemDucts, fluxDucts, controllers, scrubbers, hot, stored, capacity);
   }

   public static void recordFluxGeneratedNearby(Level level, BlockPos pos, int amount) {
      if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 20L != 0L || amount <= 0) {
         return;
      }
      AABB area = new AABB(pos).inflate(12.0);
      for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
         CompoundTag data = data(player);
         int previousFlux = data.getIntOr("thermal_flux_generated", 0);
         data.putInt("thermal_flux_generated", Math.min(2000000000, previousFlux + amount * 20));
         IndustrialWorldProgress.get(serverLevel).addPlayerStat(player.getUUID(), "thermal_flux_generated", amount * 20L);
         IndustrialMissionHooks.recordFluxGenerated(player, amount * 20);
         if (previousFlux <= 0) {
            EchoCoreServices.discoverVisibleRouteRecords(player);
         }
      }
      IndustrialWorldProgress.get(serverLevel).addWorldStat("thermal_flux_generated", amount * 20L);
   }

   public static void markScrubberZone(Level level, BlockPos pos, String mode, int storedFlux, int heat) {
      if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 40L != 0L) {
         return;
      }
      AABB area = new AABB(pos).inflate(16.0);
      IndustrialWorldProgress worldProgress = IndustrialWorldProgress.get(serverLevel);
      worldProgress.recordScrubberZone(pos, mode == null ? "Air Mode" : mode, storedFlux, heat);
      for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
         CompoundTag data = data(player);
         boolean wasSafe = data.getBoolean("safe_zone").orElse(false);
         data.putBoolean("safe_zone", true);
         data.putString("scrubber_mode", mode == null ? "Air Mode" : mode);
         data.putInt("scrubber_modes_seen", data.getIntOr("scrubber_modes_seen", 0) | scrubberModeBit(mode));
         data.putInt("scrubber_flux_seen", Math.max(data.getIntOr("scrubber_flux_seen", 0), storedFlux));
         data.putInt("scrubber_cooling_seen", Math.max(data.getIntOr("scrubber_cooling_seen", 0), Math.max(0, 100 - heat)));
         worldProgress.setPlayerFlag(player.getUUID(), "safe_zone", true);
         worldProgress.maxPlayerStat(player.getUUID(), "scrubber_modes_seen", data.getIntOr("scrubber_modes_seen", 0));
         worldProgress.maxPlayerStat(player.getUUID(), "scrubber_flux_seen", storedFlux);
         if (!wasSafe) {
            IndustrialMissionHooks.recordSafeZone(player, mode);
            EchoCoreServices.discoverVisibleRouteRecords(player);
         }
      }
   }

   public static void recordOverheatEvent(Level level, BlockPos pos, boolean prevented) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      AABB area = new AABB(pos).inflate(12.0);
      for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
         CompoundTag data = data(player);
         int previousEvents = data.getIntOr("overheating_events", 0);
         data.putInt("overheating_events", previousEvents + 1);
         if (prevented) {
            data.putInt("shutdowns_survived", data.getIntOr("shutdowns_survived", 0) + 1);
         } else {
            data.putInt("meltdowns_survived", data.getIntOr("meltdowns_survived", 0) + 1);
         }
         IndustrialWorldProgress progress = IndustrialWorldProgress.get(serverLevel);
         progress.addPlayerStat(player.getUUID(), "overheating_events", 1L);
         progress.addPlayerStat(player.getUUID(), prevented ? "shutdowns_survived" : "meltdowns_survived", 1L);
         if (previousEvents <= 0) {
            IndustrialMissionHooks.recordHeatControl(player, prevented);
            EchoCoreServices.discoverVisibleRouteRecords(player);
         }
      }
   }

   public static void recordNexusThermalWarning(Level level, BlockPos pos) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      AABB area = new AABB(pos).inflate(16.0);
      for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
         CompoundTag data = data(player);
         boolean alreadyWarned = data.getBoolean("nexus_thermal_warning").orElse(false);
         data.putBoolean("nexus_thermal_warning", true);
         data.putInt("nexus_scans", data.getIntOr("nexus_scans", 0) + 1);
         IndustrialWorldProgress progress = IndustrialWorldProgress.get(serverLevel);
         progress.setPlayerFlag(player.getUUID(), "nexus_thermal_warning", true);
         progress.addPlayerStat(player.getUUID(), "nexus_scans", 1L);
         if (!alreadyWarned) {
            IndustrialMissionHooks.recordNexusWarning(player);
            EchoCoreServices.discoverVisibleRouteRecords(player);
         }
      }
   }

   private static int scrubberModeBit(String mode) {
      if (mode == null) {
         return 1;
      }
      return switch (mode) {
         case "Radiation Mode" -> 2;
         case "Blight Mode" -> 4;
         case "Station Mode" -> 8;
         case "Cooling Mode" -> 16;
         default -> 1;
      };
   }

   public static void markWardenDefeated(Player player) {
      CompoundTag data = data(player);
      data.putBoolean("furnace_warden_defeated", true);
      data.putBoolean("thermal_plant_cleared", true);
      if (player instanceof ServerPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress progress = IndustrialWorldProgress.get(serverLevel);
         progress.setPlayerFlag(player.getUUID(), "furnace_warden_defeated", true);
         progress.setPlayerFlag(player.getUUID(), "thermal_plant_cleared", true);
         progress.addWorldStat("furnace_warden_defeats", 1L);
         IndustrialMissionHooks.recordWardenDefeated(player);
         EchoCoreServices.discoverVisibleRouteRecords((ServerPlayer)player);
      }
   }

   public static void markMultiblockFormed(Player player, Identifier definitionId) {
      if (player == null || definitionId == null || !EchoIndustrialNexus.MODID.equals(definitionId.getNamespace())) {
         return;
      }
      CompoundTag data = data(player);
      data.putBoolean("formed_" + definitionId.getPath(), true);
      if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress.get(serverLevel).setPlayerFlag(player.getUUID(), "formed_" + definitionId.getPath(), true);
         IndustrialMissionHooks.recordMultiblockFormed(player, definitionId);
         EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
      }
   }

   public static void recordAutomationTaskCompleted(ServerLevel level, BlockPos pos, Identifier taskId) {
      if (level == null || pos == null || taskId == null || !EchoIndustrialNexus.MODID.equals(taskId.getNamespace())) {
         return;
      }
      String taskKey = "task_" + taskId.getPath();
      String completeKey = taskKey + "_complete";
      AABB area = new AABB(pos).inflate(24.0D);
      IndustrialWorldProgress worldProgress = IndustrialWorldProgress.get(level);
      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
         CompoundTag data = data(player);
         data.putInt(taskKey, data.getIntOr(taskKey, 0) + 1);
         data.putBoolean(completeKey, true);
         worldProgress.addPlayerStat(player.getUUID(), taskKey, 1L);
         worldProgress.setPlayerFlag(player.getUUID(), completeKey, true);
         IndustrialMissionHooks.recordAutomationTask(player, taskId);
         EchoCoreServices.discoverVisibleRouteRecords(player);
      }
   }

   public static void markLogisticsAutoRestock(Player player, String loadoutId) {
      if (player == null) {
         return;
      }
      CompoundTag data = data(player);
      data.putBoolean("logistics_auto_restock_requested", true);
      if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress.get(serverLevel).setPlayerFlag(player.getUUID(), "logistics_auto_restock_requested", true);
         IndustrialMissionHooks.recordLogisticsAutoRestock(player, loadoutId);
         EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
      }
   }

   public static void markPoiLocated(ServerPlayer player, String id) {
      CompoundTag data = data(player);
      data.putBoolean("poi_located", true);
      data.putBoolean("poi_" + id, true);
      IndustrialWorldProgress progress = IndustrialWorldProgress.get(player.level());
      progress.setPlayerFlag(player.getUUID(), "poi_located", true);
      progress.setPlayerFlag(player.getUUID(), "poi_" + id, true);
      player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // POI locator tuned to " + id.replace('_', ' ') + ". Watch for thermal blocks and factory lights in newly explored terrain."));
      EchoCoreServices.discoverVisibleRouteRecords(player);
   }

   public static boolean claimed(Player player, String id) {
      if (player instanceof ServerPlayer && player.level() instanceof ServerLevel serverLevel
         && IndustrialWorldProgress.get(serverLevel).playerFlag(player.getUUID(), "claimed_" + id)) {
         return true;
      }
      return data(player).getBoolean("claimed_" + id).orElse(false);
   }

   public static void claim(Player player, String id) {
      data(player).putBoolean("claimed_" + id, true);
      if (player instanceof ServerPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress.get(serverLevel).claimReward(player.getUUID(), id);
         EchoCoreServices.discoverVisibleRouteRecords((ServerPlayer)player);
      }
   }

   public static int count(Player player, Item item) {
      int total = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            total += stack.getCount();
         }
      }
      return total;
   }

   public static int value(Player player, String key) {
      return data(player).getIntOr(key, 0);
   }

   public static boolean flag(Player player, String key) {
      return data(player).getBoolean(key).orElse(false);
   }

   public static float progress(Player player, String key) {
      CompoundTag data = data(player);
      if (player instanceof ServerPlayer && player.level() instanceof ServerLevel serverLevel) {
         IndustrialWorldProgress world = IndustrialWorldProgress.get(serverLevel);
         mirrorWorldProgress(player, data, world);
      }
      return switch (key) {
         case "reclaim_power" -> Math.min(1.0F, data.getIntOr("thermal_flux_generated", 0) / 1000.0F);
         case "grind_wasteland" -> Math.min(1.0F, (count(player, (Item)ModItems.RUST_DUST.get()) + count(player, (Item)ModItems.IRON_DUST.get())) / 4.0F);
         case "filters_survival" -> Math.min(1.0F, count(player, (Item)ModItems.GAS_MASK_FILTER.get()) / 8.0F);
         case "dense_alloy" -> count(player, (Item)ModItems.DENSE_ALLOY_PLATE.get()) > 0 || count(player, (Item)ModItems.REINFORCED_MACHINE_FRAME.get()) > 0 ? 1.0F : 0.0F;
         case "control_heat" -> data.getIntOr("hot_machines", 0) > 0 || data.getIntOr("overheating_events", 0) > 0 || count(player, (Item)ModItems.COOLANT_CELL.get()) > 0 ? 1.0F : 0.0F;
         case "clean_camp" -> data.getIntOr("scrubbers", 0) > 0 || data.getBoolean("safe_zone").orElse(false) ? 1.0F : 0.0F;
         case "reactor_waste" -> count(player, (Item)ModItems.RAD_SLAG.get()) > 0 || count(player, (Item)ModItems.RADIATION_SHIELDING_UPGRADE.get()) > 0 ? 1.0F : 0.0F;
         case "hybrid_warning" -> data.getBoolean("nexus_thermal_warning").orElse(false) || count(player, (Item)ModItems.HYBRID_THERMAL_CORE.get()) > 0 || count(player, (Item)ModItems.NEXUS_STABILIZER_UPGRADE.get()) > 0 ? 1.0F : 0.0F;
         case "factory_controller" -> Math.min(1.0F, data.getIntOr("machines", 0) / 5.0F);
         case "assembly_line" -> data.getBoolean("formed_industrial_assembly_line").orElse(false)
            || data.getIntOr("task_weld_reinforced_machine_frame", 0) > 0 ? 1.0F : 0.0F;
         case "scrap_processor" -> Math.min(1.0F,
            Math.max(data.getIntOr("task_process_scrap_into_scrap_plate", 0), count(player, (Item)ModItems.SCRAP_PLATE.get())) / 4.0F);
         case "plate_press" -> Math.min(1.0F,
            Math.max(data.getIntOr("task_press_scrap_plate_into_refined_plate", 0), count(player, (Item)ModItems.REFINED_PLATE.get())) / 4.0F);
         case "circuit_fabricator" -> data.getIntOr("task_assemble_precision_circuit", 0) > 0
            || count(player, (Item)ModItems.PRECISION_CIRCUIT.get()) > 0 ? 1.0F : 0.0F;
         case "recipe_matrix_encoding" -> data.getBoolean("task_encode_recipe_matrix_shard_complete").orElse(false)
            || count(player, (Item)ModItems.RECIPE_MATRIX_SHARD.get()) > 0 ? 1.0F : 0.0F;
         case "nexus_furnace_array" -> data.getBoolean("task_forge_core_key_assembly_complete").orElse(false)
            || data.getIntOr("task_forge_core_key_assembly", 0) > 0 ? 1.0F : 0.0F;
         case "logistics_auto_restock" -> data.getBoolean("logistics_auto_restock_requested").orElse(false) ? 1.0F : 0.0F;
         case "production_survived" -> data.getBoolean("furnace_warden_defeated").orElse(false) ? 1.0F : 0.0F;
         default -> 0.0F;
      };
   }

   public static void recordPoiGenerated(ServerLevel level, String type, BlockPos pos) {
      IndustrialWorldProgress.get(level).recordPoi(type, pos, "generated");
   }

   public static String poiHint(ServerPlayer player, String type, String fallback) {
      if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
         return fallback == null || fallback.isBlank() ? "No Industrial POI telemetry is available yet." : fallback;
      }
      IndustrialWorldProgress.PoiRecord record = IndustrialWorldProgress.get(serverLevel).nearestPoi(type, player.blockPosition());
      if (record == null) {
         return fallback == null || fallback.isBlank()
            ? "No generated Industrial POI coordinates are recorded yet. Explore new chunks to seed factory telemetry."
            : fallback + " No generated coordinates are recorded yet.";
      }
      BlockPos pos = record.pos();
      return readablePoi(record.type()) + " recorded at X " + pos.getX() + ", Y " + pos.getY() + ", Z " + pos.getZ() + ". " + fallback;
   }

   private static String readablePoi(String type) {
      if (type == null || type.isBlank()) {
         return "Industrial POI";
      }
      StringBuilder label = new StringBuilder();
      for (String part : type.split("_")) {
         if (part.isBlank()) {
            continue;
         }
         if (label.length() > 0) {
            label.append(' ');
         }
         label.append(Character.toUpperCase(part.charAt(0)));
         if (part.length() > 1) {
            label.append(part.substring(1));
         }
      }
      return label.isEmpty() ? "Industrial POI" : label.toString();
   }

   private static void mirrorWorldProgress(Player player, CompoundTag data, IndustrialWorldProgress world) {
      data.putInt("thermal_flux_generated", Math.max(data.getIntOr("thermal_flux_generated", 0), (int)Math.min(2000000000L, world.playerStat(player.getUUID(), "thermal_flux_generated"))));
      data.putInt("machines", Math.max(data.getIntOr("machines", 0), (int)world.playerStat(player.getUUID(), "machines")));
      data.putInt("scrubbers", Math.max(data.getIntOr("scrubbers", 0), (int)world.playerStat(player.getUUID(), "scrubbers")));
      data.putInt("hot_machines", Math.max(data.getIntOr("hot_machines", 0), (int)world.playerStat(player.getUUID(), "hot_machines")));
      data.putInt("task_process_scrap_into_scrap_plate", Math.max(data.getIntOr("task_process_scrap_into_scrap_plate", 0), (int)world.playerStat(player.getUUID(), "task_process_scrap_into_scrap_plate")));
      data.putInt("task_press_scrap_plate_into_refined_plate", Math.max(data.getIntOr("task_press_scrap_plate_into_refined_plate", 0), (int)world.playerStat(player.getUUID(), "task_press_scrap_plate_into_refined_plate")));
      data.putInt("task_assemble_precision_circuit", Math.max(data.getIntOr("task_assemble_precision_circuit", 0), (int)world.playerStat(player.getUUID(), "task_assemble_precision_circuit")));
      data.putInt("task_weld_reinforced_machine_frame", Math.max(data.getIntOr("task_weld_reinforced_machine_frame", 0), (int)world.playerStat(player.getUUID(), "task_weld_reinforced_machine_frame")));
      data.putInt("task_stabilize_hybrid_thermal_core", Math.max(data.getIntOr("task_stabilize_hybrid_thermal_core", 0), (int)world.playerStat(player.getUUID(), "task_stabilize_hybrid_thermal_core")));
      data.putInt("task_forge_core_key_assembly", Math.max(data.getIntOr("task_forge_core_key_assembly", 0), (int)world.playerStat(player.getUUID(), "task_forge_core_key_assembly")));
      data.putBoolean("safe_zone", data.getBoolean("safe_zone").orElse(false) || world.playerFlag(player.getUUID(), "safe_zone"));
      data.putBoolean("nexus_thermal_warning", data.getBoolean("nexus_thermal_warning").orElse(false) || world.playerFlag(player.getUUID(), "nexus_thermal_warning"));
      data.putBoolean("formed_industrial_assembly_line", data.getBoolean("formed_industrial_assembly_line").orElse(false) || world.playerFlag(player.getUUID(), "formed_industrial_assembly_line"));
      data.putBoolean("formed_nexus_furnace_array", data.getBoolean("formed_nexus_furnace_array").orElse(false) || world.playerFlag(player.getUUID(), "formed_nexus_furnace_array"));
      data.putBoolean("task_encode_recipe_matrix_shard_complete", data.getBoolean("task_encode_recipe_matrix_shard_complete").orElse(false) || world.playerFlag(player.getUUID(), "task_encode_recipe_matrix_shard_complete"));
      data.putBoolean("task_stabilize_hybrid_thermal_core_complete", data.getBoolean("task_stabilize_hybrid_thermal_core_complete").orElse(false) || world.playerFlag(player.getUUID(), "task_stabilize_hybrid_thermal_core_complete"));
      data.putBoolean("task_forge_core_key_assembly_complete", data.getBoolean("task_forge_core_key_assembly_complete").orElse(false) || world.playerFlag(player.getUUID(), "task_forge_core_key_assembly_complete"));
      data.putBoolean("logistics_auto_restock_requested", data.getBoolean("logistics_auto_restock_requested").orElse(false) || world.playerFlag(player.getUUID(), "logistics_auto_restock_requested"));
      data.putBoolean("furnace_warden_defeated", data.getBoolean("furnace_warden_defeated").orElse(false) || world.playerFlag(player.getUUID(), "furnace_warden_defeated"));
   }

   public record FactoryScan(int machines, int itemDucts, int fluxDucts, int controllers, int scrubbers, int hotMachines, int storedFlux, int capacity) {
   }
}
