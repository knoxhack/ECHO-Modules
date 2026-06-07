package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class IndustrialMissionHooks {
    private IndustrialMissionHooks() {
    }

    public static void registerCoverage() {
        for (String key : new String[] {
                "reclaim_power",
                "grind_wasteland",
                "filters_survival",
                "dense_alloy",
                "control_heat",
                "clean_camp",
                "reactor_waste",
                "hybrid_warning",
                "factory_controller",
                "assembly_line",
                "scrap_processor",
                "plate_press",
                "circuit_fabricator",
                "recipe_matrix_encoding",
                "nexus_furnace_array",
                "logistics_auto_restock",
                "production_survived"
        }) {
            register(key, 0);
        }
    }

    public static void recordFactoryScan(Player player, int machineDelta, int scrubberDelta, int hotDelta) {
        if (machineDelta > 0) {
            record(player, "factory_controller", MissionObjectiveType.SCAN_BLOCK, machineDelta, "machine", "factory_scan");
        }
        if (scrubberDelta > 0) {
            record(player, "clean_camp", MissionObjectiveType.REPAIR_MACHINE, 1, "machine", "industrial_scrubber");
        }
        if (hotDelta > 0) {
            record(player, "control_heat", MissionObjectiveType.REPAIR_MACHINE, 1, "machine", "heat_control");
        }
    }

    public static void recordFluxGenerated(Player player, int amount) {
        record(player, "reclaim_power", MissionObjectiveType.CUSTOM, amount, "machine", "thermal_flux");
    }

    public static void recordSafeZone(Player player, String mode) {
        record(player, "clean_camp", MissionObjectiveType.REPAIR_MACHINE, 1, "machine", mode == null ? "scrubber" : mode);
    }

    public static void recordHeatControl(Player player, boolean prevented) {
        record(player, "control_heat", MissionObjectiveType.REPAIR_MACHINE, 1, "machine", prevented ? "shutdown" : "overheat");
    }

    public static void recordNexusWarning(Player player) {
        record(player, "hybrid_warning", MissionObjectiveType.SCAN_BLOCK, 1, "machine", "nexus_thermal_warning");
    }

    public static void recordWardenDefeated(Player player) {
        record(player, "production_survived", MissionObjectiveType.KILL_ENTITY, 1, "entity", "furnace_warden");
    }

    public static void recordMultiblockFormed(Player player, Identifier definitionId) {
        if (definitionId != null && EchoIndustrialNexus.MODID.equals(definitionId.getNamespace())
                && "industrial_assembly_line".equals(definitionId.getPath())) {
            record(player, "assembly_line", MissionObjectiveType.SCAN_BLOCK, 1, "multiblock", definitionId.toString());
        }
    }

    public static void recordAutomationTask(Player player, Identifier taskId) {
        String mission = missionForTask(taskId);
        if (!mission.isBlank()) {
            record(player, mission, MissionObjectiveType.CUSTOM, 1, "task", taskId.toString());
        }
    }

    public static void recordLogisticsAutoRestock(Player player, String loadoutId) {
        record(player, "logistics_auto_restock", MissionObjectiveType.CUSTOM, 1, "loadout", loadoutId == null ? "auto_restock" : loadoutId);
    }

    public static void recordOutput(ServerLevel level, BlockPos pos, ItemStack output) {
        if (level == null || pos == null || output == null || output.isEmpty()) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(output.getItem()).getPath();
        String mission = switch (itemId) {
            case "rust_dust", "iron_dust" -> "grind_wasteland";
            case "gas_mask_filter" -> "filters_survival";
            case "dense_alloy_plate", "reinforced_machine_frame" -> "dense_alloy";
            case "coolant_cell" -> "control_heat";
            case "rad_slag", "radiation_shielding_upgrade" -> "reactor_waste";
            case "hybrid_thermal_core", "nexus_stabilizer_upgrade" -> "hybrid_warning";
            case "scrap_plate" -> "scrap_processor";
            case "refined_plate", "reinforced_plate" -> "plate_press";
            case "precision_circuit", "industrial_circuit" -> "circuit_fabricator";
            case "recipe_matrix_shard" -> "recipe_matrix_encoding";
            case "core_key_assembly", "protocol_extractor_coil" -> "nexus_furnace_array";
            default -> "";
        };
        if (mission.isBlank()) {
            return;
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(20.0D))) {
            record(player, mission, MissionObjectiveType.CRAFT_ITEM, Math.max(1, output.getCount()), "item", itemId);
        }
    }

    private static String missionForTask(Identifier taskId) {
        if (taskId == null || !EchoIndustrialNexus.MODID.equals(taskId.getNamespace())) {
            return "";
        }
        return switch (taskId.getPath()) {
            case "weld_reinforced_machine_frame" -> "assembly_line";
            case "process_scrap_into_scrap_plate" -> "scrap_processor";
            case "press_scrap_plate_into_refined_plate" -> "plate_press";
            case "assemble_precision_circuit" -> "circuit_fabricator";
            case "encode_recipe_matrix_shard" -> "recipe_matrix_encoding";
            case "stabilize_hybrid_thermal_core", "forge_core_key_assembly" -> "nexus_furnace_array";
            default -> "";
        };
    }

    private static void register(String missionKey, int objectiveIndex) {
        Identifier mission = mission(missionKey);
        EchoCoreServices.registerMissionHookCoverage(
                EchoIndustrialNexus.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoIndustrialNexus.MODID, mission, objectiveIndex));
    }

    private static void record(Player player, String missionKey, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionKey);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoIndustrialNexus.MODID, mission, 0),
                amount,
                MissionHookTargets.context(EchoIndustrialNexus.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String key) {
        return Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "mission/" + key);
    }
}
