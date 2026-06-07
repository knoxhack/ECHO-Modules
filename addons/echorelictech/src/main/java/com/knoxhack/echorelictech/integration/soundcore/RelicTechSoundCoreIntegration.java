package com.knoxhack.echorelictech.integration.soundcore;

import com.knoxhack.echorelictech.EchoRelicTech;
import net.minecraft.world.entity.player.Player;

public class RelicTechSoundCoreIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO SoundCore integration loaded for RelicTech.");
    }

    public static void playScan(Player player) {
        playStinger(player, "playSignalDetected");
    }

    public static void playGuardianLocated(Player player) {
        playStinger(player, "playGuardianLocated");
    }

    public static void playMachineComplete(Player player) {
        playStinger(player, "playObjectiveComplete");
    }

    public static void playRelicMalfunction(Player player) {
        playStinger(player, "playNexusStateChanged");
    }

    private static void playStinger(Player player, String methodName) {
        try {
            Class<?> apiClass = Class.forName("com.knoxhack.echosoundcore.api.SoundCoreApi");
            java.lang.reflect.Method method = apiClass.getMethod(methodName, Player.class);
            method.invoke(null, player);
        } catch (Exception | LinkageError ignored) {}
    }
}
