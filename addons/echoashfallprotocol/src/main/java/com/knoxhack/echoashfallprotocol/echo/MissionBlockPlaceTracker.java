package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Feeds placed-block actions into AdapterCore so runtime-host mission updates
 * can evaluate block-placement requirements through one gameplay event path.
 */
public final class MissionBlockPlaceTracker {
    private MissionBlockPlaceTracker() {}

    public static void onPlace(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        if (!(eventValue(event, "getPlacedBlock") instanceof BlockState placedBlock)) return;
        if (!(eventValue(event, "getPos") instanceof BlockPos pos)) return;
        var key = BuiltInRegistries.BLOCK.getKey(placedBlock.getBlock());
        if (key == null) return;

        AshfallAdapterCoreEarlyEventRuntime.blockPlaced(player, key, pos);
        MissionRegistry.invalidateBlockProbeCache(player);
    }

    public static void onBreak(Object event) {
        if (eventValue(event, "getPlayer") instanceof ServerPlayer player) {
            MissionRegistry.invalidateBlockProbeCache(player);
        }
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
