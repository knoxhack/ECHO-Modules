package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.item.ContaminatedItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Applies poison to players carrying contaminated resource items.
 */
public class ContaminatedItemTickHandler {

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 40 != 0) return;

        boolean hasContaminated = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof ContaminatedItem) {
                hasContaminated = true;
                break;
            }
        }

        if (hasContaminated) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
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
