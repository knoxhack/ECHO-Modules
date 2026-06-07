package com.knoxhack.echorelictech.integration.powergrid;

import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class RelicTechPowerGridIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO PowerGrid integration loaded for RelicTech.");
    }

    public static void tryBackfire(ServerPlayer player, BlockPos pos) {
        if (!RelicTechConfig.ENABLE_POWERGRID_BACKFIRES.get()) return;
        try {
            Class<?> apiClass = Class.forName("com.knoxhack.echopowergrid.api.EchoPowerGridApi");
            java.lang.reflect.Method isPowered = apiClass.getMethod("isPowered", net.minecraft.world.level.Level.class, BlockPos.class);
            Object result = isPowered.invoke(null, player.level(), pos);
            if (result instanceof Boolean b && b) {
                Class<?> breakerClass = Class.forName("com.knoxhack.echopowergrid.block.BreakerBlock");
                java.lang.reflect.Method tryTrip = breakerClass.getMethod("tryTrip", net.minecraft.world.level.Level.class, BlockPos.class, Class.forName("com.knoxhack.echopowergrid.api.EchoPowerNetwork"));
                java.lang.reflect.Method getNetwork = apiClass.getMethod("getNetwork", net.minecraft.world.level.Level.class, BlockPos.class);
                Object network = getNetwork.invoke(null, player.level(), pos);
                if (network instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    tryTrip.invoke(null, player.level(), pos, opt.get());
                }
            }
        } catch (Exception | LinkageError ignored) {}
    }
}
