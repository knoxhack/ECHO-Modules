package com.knoxhack.echomultiblockcore.integration.runtimeguard;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class MultiblockRuntimeGuardBridge {
    private MultiblockRuntimeGuardBridge() {
    }

    public static boolean requestValidation(MultiblockControllerBlockEntity controller, ServerLevel level,
            String priorityName, Runnable task) throws ReflectiveOperationException {
        if (controller == null || level == null || task == null) {
            return false;
        }
        if (!schedulerEnabled()) {
            return false;
        }
        Object scheduler = scheduler();
        Class<?> priorityType = Class.forName("com.knoxhack.echoruntimeguard.api.ValidationPriority");
        Method requestValidation = scheduler.getClass().getMethod("requestValidation", Identifier.class, Level.class,
                BlockPos.class, priorityType, Runnable.class);
        requestValidation.invoke(scheduler, EchoMultiblockCore.id("controller"), level, controller.getBlockPos(),
                enumValue(priorityType, priorityName, "SCHEDULED_IDLE"), task);
        return true;
    }

    public static void markDirty(Level level, BlockPos controllerPos, String reasonName) throws ReflectiveOperationException {
        Object scheduler = scheduler();
        Class<?> dirtyReasonType = Class.forName("com.knoxhack.echoruntimeguard.api.DirtyReason");
        Method markDirty = scheduler.getClass().getMethod("markDirty", Level.class, BlockPos.class, dirtyReasonType);
        markDirty.invoke(scheduler, level, controllerPos, enumValue(dirtyReasonType, reasonName, "DEBUG"));
    }

    private static Object scheduler() throws ReflectiveOperationException {
        Class<?> services = Class.forName("com.knoxhack.echoruntimeguard.api.RuntimeGuardServices");
        return services.getMethod("multiblocks").invoke(null);
    }

    private static boolean schedulerEnabled() {
        try {
            Class<?> config = Class.forName("com.knoxhack.echoruntimeguard.RuntimeGuardConfig");
            Object value = config.getField("MULTIBLOCK_SCHEDULER_ENABLED").get(null);
            for (Method method : config.getMethods()) {
                if ("safeBool".equals(method.getName()) && method.getParameterCount() == 2) {
                    Object result = method.invoke(null, value, true);
                    return result instanceof Boolean enabled ? enabled : true;
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoMultiblockCore.LOGGER.warn("MultiblockCore RuntimeGuard config bridge failed; using local validation.", exception);
            return false;
        }
        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object enumValue(Class<?> enumType, String name, String fallback) {
        Class<? extends Enum> safeType = enumType.asSubclass(Enum.class);
        String safeName = name == null || name.isBlank() ? fallback : name.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf((Class)safeType, safeName);
        } catch (IllegalArgumentException exception) {
            return Enum.valueOf((Class)safeType, fallback);
        }
    }
}
