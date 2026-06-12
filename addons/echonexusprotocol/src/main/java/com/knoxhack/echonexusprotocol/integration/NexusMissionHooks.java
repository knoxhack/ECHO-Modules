package com.knoxhack.echonexusprotocol.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NexusMissionHooks {
    private static final String PRIME_ROUTE_NAMESPACE = "echoprimecore";
    private static final String PRIME_ROUTE_MISSION_PATH = "mission/prime_route";

    private NexusMissionHooks() {
    }

    public static void registerCoverage() {
        register("the_signal_beneath", 0);
        register("dirty_charge", 0);
        register("stabilize_the_camp", 0);
        register("stabilize_the_camp", 1);
        register("the_tower_still_speaks", 0);
        register("deleted_history", 0);
        register("quarantine_failed", 0);
        register("the_monolith_remembers", 0);
        register("reality_forge", 0);
        register("the_core_door", 0);
        register("what_rebuilds_the_world", 0);
    }

    public static void recordScan(Player player, String target) {
        record(player, "the_signal_beneath", 0, MissionObjectiveType.SCAN_BLOCK, 1, "target", target);
    }

    public static void recordMachine(Player player, NexusMachineBlock.MachineKind kind) {
        if (kind == null) {
            return;
        }
        recordMachine(player, kind.getSerializedName());
    }

    public static void recordMachine(Player player, String machineId) {
        String id = machineId == null ? "" : machineId;
        switch (id) {
            case "nexus_recycler" -> record(player, "dirty_charge", 0, MissionObjectiveType.REPAIR_MACHINE, 1, "machine", id);
            case "nexus_field_stabilizer" -> record(player, "stabilize_the_camp", 0, MissionObjectiveType.REPAIR_MACHINE, 1, "machine", id);
            case "corruption_filter" -> record(player, "stabilize_the_camp", 1, MissionObjectiveType.REPAIR_MACHINE, 1, "machine", id);
            case "memory_decoder" -> record(player, "the_tower_still_speaks", 0, MissionObjectiveType.UNLOCK_RESEARCH, 1, "machine", id);
            case "reality_forge" -> record(player, "reality_forge", 0, MissionObjectiveType.CRAFT_ITEM, 1, "machine", id);
            default -> {
            }
        }
    }

    public static void recordBlackboxFragment(Player player) {
        record(player, "deleted_history", 0, MissionObjectiveType.OBTAIN_ITEM, 1, "item", "blackbox_fragment");
    }

    public static void recordWarden(Player player) {
        record(player, "quarantine_failed", 0, MissionObjectiveType.KILL_ENTITY, 1, "entity", "corruption_warden");
    }

    public static void recordMonolith(Player player) {
        record(player, "the_monolith_remembers", 0, MissionObjectiveType.SCAN_BLOCK, 1, "machine", "blackbox_monolith");
    }

    public static void recordCoreEntered(Player player) {
        record(player, "the_core_door", 0, MissionObjectiveType.ENTER_REGION, 1, "region", "nexus_core");
    }

    public static void recordEndingPath(Player player, String path) {
        record(player, "what_rebuilds_the_world", 0, MissionObjectiveType.UNLOCK_RESEARCH, 1, "path", path);
    }

    public static boolean startPrimeRoute(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return EchoCoreServices.startMission(serverPlayer, primeRouteMission());
    }

    public static Identifier primeRouteMission() {
        return Identifier.fromNamespaceAndPath(PRIME_ROUTE_NAMESPACE, PRIME_ROUTE_MISSION_PATH);
    }

    private static void register(String missionPath, int objectiveIndex) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoNexusProtocol.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoNexusProtocol.MODID, mission, objectiveIndex));
    }

    private static void record(Player player, String missionPath, int objectiveIndex, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoNexusProtocol.MODID, mission, objectiveIndex),
                amount,
                MissionHookTargets.context(EchoNexusProtocol.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, path);
    }
}
