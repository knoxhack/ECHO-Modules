package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

public class CombatTracker {

    public static void onLivingDamage(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }

        CombatData combatData = player.getData(ModAttachments.COMBAT_DATA);
        if (combatData != null) {
            combatData.onCombatTick(player.tickCount);
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
