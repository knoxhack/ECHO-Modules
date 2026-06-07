package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BlackboxMissionHooks {
    private BlackboxMissionHooks() {
    }

    public static void registerCoverage() {
        register("decode_memories", 0);
        register("blackbox_vault", 0);
        register("blackbox_vault", 1);
        register("blackbox_vault", 2);
        register("blackbox_bunker", 0);
        register("blackbox_bunker", 1);
        register("blackbox_bunker", 2);
        register("memory_bosses", 0);
        register("memory_bosses", 1);
        register("blackbox_labyrinth", 0);
        register("blackbox_labyrinth", 1);
        register("blackbox_labyrinth", 2);
        register("blackbox_temple", 0);
        register("blackbox_temple", 1);
        register("blackbox_temple", 2);
        register("core_key", 0);
        register("core_chamber", 0);
        register("nexus_guardian", 0);
        register("truth_engine", 0);
    }

    public static void recordMemory(Player player, MemoryType type) {
        record(player, "decode_memories", 0, MissionObjectiveType.SCAN_ENTITY, 1, "memory", type == null ? "unknown" : type.getSerializedName());
        if (type == MemoryType.PERSONAL) {
            record(player, "blackbox_vault", 0, MissionObjectiveType.SCAN_ENTITY, 1, "memory", "personal");
        } else if (type == MemoryType.SECURITY) {
            record(player, "blackbox_vault", 1, MissionObjectiveType.SCAN_ENTITY, 1, "memory", "security");
        } else if (type == MemoryType.COMMAND) {
            record(player, "blackbox_bunker", 1, MissionObjectiveType.SCAN_ENTITY, 1, "memory", "command");
        } else if (type == MemoryType.ECHO) {
            record(player, "blackbox_labyrinth", 1, MissionObjectiveType.SCAN_ENTITY, 1, "memory", "echo");
        } else if (type == MemoryType.CORE) {
            record(player, "blackbox_temple", 1, MissionObjectiveType.SCAN_ENTITY, 1, "memory", "core");
        }
    }

    public static void recordBoss(Player player, String id) {
        if ("false_echo".equals(id)) {
            record(player, "memory_bosses", 0, MissionObjectiveType.KILL_ENTITY, 1, "entity", id);
            record(player, "blackbox_labyrinth", 0, MissionObjectiveType.KILL_ENTITY, 1, "entity", id);
        } else if ("command_remnant".equals(id)) {
            record(player, "memory_bosses", 1, MissionObjectiveType.KILL_ENTITY, 1, "entity", id);
            record(player, "blackbox_temple", 0, MissionObjectiveType.KILL_ENTITY, 1, "entity", id);
        } else if ("nexus_guardian".equals(id)) {
            record(player, "nexus_guardian", 0, MissionObjectiveType.KILL_ENTITY, 1, "entity", id);
        }
    }

    public static void recordDungeon(Player player, BlackboxDungeon dungeon) {
        if (dungeon == null) {
            return;
        }
        switch (dungeon) {
            case VAULT -> {
                record(player, "blackbox_vault", 2, MissionObjectiveType.ESTABLISH_ROUTE, 1, "dungeon", dungeon.getSerializedName());
                record(player, "blackbox_bunker", 0, MissionObjectiveType.ESTABLISH_ROUTE, 1, "dungeon", dungeon.getSerializedName());
            }
            case BUNKER -> record(player, "blackbox_bunker", 2, MissionObjectiveType.ESTABLISH_ROUTE, 1, "dungeon", dungeon.getSerializedName());
            case LABYRINTH -> record(player, "blackbox_labyrinth", 2, MissionObjectiveType.ESTABLISH_ROUTE, 1, "dungeon", dungeon.getSerializedName());
            case TEMPLE -> record(player, "blackbox_temple", 2, MissionObjectiveType.ESTABLISH_ROUTE, 1, "dungeon", dungeon.getSerializedName());
            case CORE_CHAMBER -> {
            }
        }
    }

    public static void recordCoreKey(Player player) {
        record(player, "core_key", 0, MissionObjectiveType.CRAFT_ITEM, 1, "item", "nexus_core_access_key");
        record(player, "core_chamber", 0, MissionObjectiveType.CRAFT_ITEM, 1, "item", "nexus_core_access_key");
    }

    public static void recordEnding(Player player, String ending) {
        record(player, "truth_engine", 0, MissionObjectiveType.UNLOCK_RESEARCH, 1, "ending", ending);
    }

    private static void register(String missionPath, int objectiveIndex) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoBlackboxProtocol.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoBlackboxProtocol.MODID, mission, objectiveIndex));
    }

    private static void record(Player player, String missionPath, int objectiveIndex, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoBlackboxProtocol.MODID, mission, objectiveIndex),
                amount,
                MissionHookTargets.context(EchoBlackboxProtocol.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
    }
}
