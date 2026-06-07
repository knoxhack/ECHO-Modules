package com.knoxhack.echoprimecore.progression;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.PrimeIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class PrimeProgressionService {
    private PrimeProgressionService() {
    }

    public static boolean hasFlag(ServerPlayer player, Identifier flag) {
        return PrimePlayerData.get(player).hasFlag(flag);
    }

    public static boolean unlock(ServerPlayer player, Identifier flag) {
        return unlock(player, flag, true);
    }

    public static boolean unlock(ServerPlayer player, Identifier flag, boolean notify) {
        if (player == null || flag == null) {
            return false;
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        boolean changed = data.unlockFlag(flag);
        if (changed) {
            applyStageForFlag(data, flag);
            PrimePlayerData.saveAndSync(player, data);
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, flag, 1,
                    Map.of("source", "echoprimecore:progression_flag"));
            if (notify) {
                player.sendSystemMessage(Component.literal("ECHO: Prime flag unlocked: " + flag)
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        return changed;
    }

    public static void setStage(ServerPlayer player, String stage, String objective) {
        if (player == null) {
            return;
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        data.setStage(stage);
        data.setObjective(objective);
        PrimePlayerData.saveAndSync(player, data);
    }

    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        data.resetFlags();
        data.setFirstJoinInitialized(false);
        data.setStage("Prime Survival: Begin");
        data.setObjective("Survive, explore, and bring ECHO systems online.");
        data.setRelayPos(null);
        PrimePlayerData.saveAndSync(player, data);
    }

    public static int worldSignalLevel(ServerPlayer player) {
        PrimePlayerData data = PrimePlayerData.get(player);
        int level = 0;
        if (data.hasFlag(EchoPrimeCore.id("first_signal"))) {
            level += 1;
        }
        if (data.hasFlag(EchoPrimeCore.id("first_ruin"))) {
            level += 1;
        }
        if (data.hasFlag(EchoPrimeCore.id("first_machine"))) {
            level += 1;
        }
        if (data.hasFlag(EchoPrimeCore.id("powergrid_online"))) {
            level += 2;
        }
        return level;
    }

    public static String currentObjective(ServerPlayer player) {
        return PrimePlayerData.get(player).objective();
    }

    public static String currentStage(ServerPlayer player) {
        return PrimePlayerData.get(player).stage();
    }

    private static void applyStageForFlag(PrimePlayerData data, Identifier flag) {
        if (flag.equals(EchoPrimeCore.id("started"))) {
            data.setStage("Prime Survival: Begin");
            data.setObjective("Find a Signal Shard, then craft a Crude Scanner.");
        } else if (flag.equals(EchoPrimeCore.id("lens_online"))) {
            data.setStage("First Signal");
            data.setObjective("Use the Crude Scanner to locate weak signal activity.");
        } else if (flag.equals(EchoPrimeCore.id("holomap_online"))) {
            data.setStage("First Ruin");
            data.setObjective("Follow the Prime Signals marker to the abandoned relay post.");
        } else if (flag.equals(EchoPrimeCore.id("first_ruin"))) {
            data.setStage("First Tech");
            data.setObjective("Loot Relay Fragment and Circuit Plate from the relay cache.");
        } else if (flag.equals(EchoPrimeCore.id("first_signal"))) {
            data.setStage("First Tech");
            data.setObjective("Use the revealed Index recipes to craft your first Prime circuit path.");
        } else if (flag.equals(EchoPrimeCore.id("first_machine"))) {
            data.setStage("PowerGrid Online");
            data.setObjective("Open a technology route: power, storage, base, agriculture, combat, arcana, or relics.");
        }
    }

    public static Identifier flag(String path) {
        return PrimeIds.id(path);
    }
}
