package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class DroneCombatEventHandler {
    private static final double MARK_SCAN_RADIUS = 64.0D;
    private static final float MARK_DAMAGE_MULTIPLIER = 1.25F;

    private DroneCombatEventHandler() {
    }

    public static void onLivingDamage(Object event) {
        Object source = eventValue(event, "getSource");
        if (!(eventValue(source, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        if (!(eventValue(event, "getEntity") instanceof LivingEntity target)) {
            return;
        }

        if (target == player || target.level().isClientSide()) {
            return;
        }

        boolean markedByOwnedDrone = !target.level().getEntitiesOfClass(
                EchoCompanionDrone.class,
                target.getBoundingBox().inflate(MARK_SCAN_RADIUS),
                drone -> player.getUUID().equals(drone.getOwnerUUID()) && drone.hasMarkedTarget(target)
        ).isEmpty();

        if (markedByOwnedDrone) {
            setFloatValue(event, "setNewDamage", floatValue(eventValue(event, "getNewDamage"), 0.0F) * MARK_DAMAGE_MULTIPLIER);
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

    private static float floatValue(Object value, float fallback) {
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static void setFloatValue(Object event, String methodName, float value) {
        if (event == null) {
            return;
        }
        try {
            event.getClass().getMethod(methodName, float.class).invoke(event, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Native event views may expose mutation through a host-owned receipt.
        }
    }
}
