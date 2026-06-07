package com.knoxhack.echorelictech.integration.nexus;

import com.knoxhack.echorelictech.EchoRelicTech;
import net.minecraft.server.level.ServerPlayer;

public class RelicTechNexusIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO NexusProtocol integration loaded for RelicTech.");
    }

    public static void recordHighInstability(ServerPlayer player) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echonexusprotocol.data.NexusPlayerData");
            Object data = dataClass.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            dataClass.getMethod("markMachineUsed", String.class).invoke(data, "echorelictech:relic_use");
            dataClass.getMethod("saveAndSync", ServerPlayer.class, dataClass).invoke(null, player, data);
        } catch (Exception | LinkageError ignored) {}
    }

    public static void recordRelicResearch(ServerPlayer player, String researchId) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echonexusprotocol.data.NexusPlayerData");
            Object data = dataClass.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            dataClass.getMethod("unlockResearch", String.class).invoke(data, researchId);
            dataClass.getMethod("saveAndSync", ServerPlayer.class, dataClass).invoke(null, player, data);
        } catch (Exception | LinkageError ignored) {}
    }
}
