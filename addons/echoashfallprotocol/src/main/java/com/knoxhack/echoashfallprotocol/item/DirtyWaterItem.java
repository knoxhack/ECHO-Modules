package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dirty Water Bottle - collected from water sources.
 * Drinking it gives brief nausea and emergency hydration.
 * Should be purified via Water Purifier machine.
 */
public class DirtyWaterItem extends WaterBottleItem {

    public DirtyWaterItem(Properties properties) {
        super(properties, 20, 1, 0.1f);
    }

    @Override
    protected NativeResult applyWaterEffects(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            return AshfallAdapterCoreEarlyEventRuntime.waterBottleDrunk(
                    serverPlayer,
                    stack,
                    hand,
                    runtimeHydrationGain(),
                    1,
                    0.1F,
                    true);
        }
        return super.applyWaterEffects(level, player, stack, hand);
    }
}
