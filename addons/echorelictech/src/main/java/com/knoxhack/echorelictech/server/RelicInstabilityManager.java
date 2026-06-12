package com.knoxhack.echorelictech.server;

import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.integration.nexus.RelicTechNexusIntegration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class RelicInstabilityManager {

    public static int getInstability(ServerPlayer player) {
        if (!RelicTechConfig.ENABLE_RELIC_INSTABILITY.get()) return 0;
        return RelicInstabilitySavedData.get((ServerLevel) player.level()).get(player.getUUID()).value;
    }

    public static void addInstability(ServerPlayer player, int amount) {
        if (!RelicTechConfig.ENABLE_RELIC_INSTABILITY.get() || amount <= 0) return;
        var data = RelicInstabilitySavedData.get((ServerLevel) player.level());
        var inst = data.get(player.getUUID());
        int max = RelicTechConfig.MAX_INSTABILITY.get();
        inst.value = Math.min(max, inst.value + amount);
        inst.lastChange = ((ServerLevel) player.level()).getGameTime();
        inst.totalUses++;
        inst.recentUses++;
        inst.level = computeLevel(inst.value);
        if (inst.level > inst.highestLevel) inst.highestLevel = inst.level;
        data.set(player.getUUID(), inst);
        if (inst.level >= 3 && EchoRuntimeModules.isLoaded("echonexusprotocol")) {
            RelicTechNexusIntegration.recordHighInstability(player);
        }
    }

    public static void setInstability(ServerPlayer player, int amount) {
        if (!RelicTechConfig.ENABLE_RELIC_INSTABILITY.get()) return;
        var data = RelicInstabilitySavedData.get((ServerLevel) player.level());
        var inst = data.get(player.getUUID());
        int max = RelicTechConfig.MAX_INSTABILITY.get();
        inst.value = Math.max(0, Math.min(max, amount));
        inst.level = computeLevel(inst.value);
        inst.lastChange = ((ServerLevel) player.level()).getGameTime();
        if (inst.level > inst.highestLevel) inst.highestLevel = inst.level;
        data.set(player.getUUID(), inst);
    }

    public static int getInstabilityLevel(ServerPlayer player) {
        return computeLevel(getInstability(player));
    }

    public static int computeLevel(int value) {
        if (value >= RelicTechConfig.LEVEL5_THRESHOLD.get()) return 5;
        if (value >= RelicTechConfig.LEVEL4_THRESHOLD.get()) return 4;
        if (value >= RelicTechConfig.LEVEL3_THRESHOLD.get()) return 3;
        if (value >= RelicTechConfig.LEVEL2_THRESHOLD.get()) return 2;
        if (value >= RelicTechConfig.LEVEL1_THRESHOLD.get()) return 1;
        return 0;
    }

    public static void tickDecay(ServerLevel level) {
        if (!RelicTechConfig.ENABLE_RELIC_INSTABILITY.get() || !RelicTechConfig.INSTABILITY_DECAY_ENABLED.get()) return;
        long delay = RelicTechConfig.INSTABILITY_DECAY_DELAY_TICKS.get();
        int decay = RelicTechConfig.INSTABILITY_DECAY_AMOUNT.get();
        long now = level.getGameTime();
        var data = RelicInstabilitySavedData.get(level);
        for (var inst : new java.util.ArrayList<>(data.allEntries())) {
            if (inst.value > 0 && now - inst.lastChange >= delay) {
                inst.value = Math.max(0, inst.value - decay);
                inst.level = computeLevel(inst.value);
                inst.lastChange = now;
                data.set(inst.playerId, inst);
            }
        }
    }

    public static String levelName(int level) {
        return switch (level) {
            case 0 -> "STABLE";
            case 1 -> "ECHO_STATIC";
            case 2 -> "SYSTEM_DRIFT";
            case 3 -> "NEXUS_ATTENTION";
            case 4 -> "REALITY_BLEED";
            case 5 -> "CRITICAL_INSTABILITY";
            default -> "UNKNOWN";
        };
    }
}
