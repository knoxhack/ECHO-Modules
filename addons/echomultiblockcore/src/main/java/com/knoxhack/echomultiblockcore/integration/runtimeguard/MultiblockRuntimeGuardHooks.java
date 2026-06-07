package com.knoxhack.echomultiblockcore.integration.runtimeguard;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class MultiblockRuntimeGuardHooks {
    private static boolean bridgeUnavailable;
    private static Method requestValidationMethod;
    private static Method markDirtyMethod;

    private MultiblockRuntimeGuardHooks() {
    }

    public static boolean requestValidation(MultiblockControllerBlockEntity controller, ServerLevel level,
            String priorityName, Runnable task) {
        if (!resolveBridge()) {
            return false;
        }
        try {
            Object result = requestValidationMethod.invoke(null, controller, level, priorityName, task);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoMultiblockCore.LOGGER.warn("MultiblockCore RuntimeGuard validation bridge failed; falling back to local validation.", exception);
            return false;
        }
    }

    public static void markDirty(Level level, BlockPos controllerPos, String reasonName) {
        if (!resolveBridge()) {
            return;
        }
        try {
            markDirtyMethod.invoke(null, level, controllerPos, reasonName);
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoMultiblockCore.LOGGER.warn("MultiblockCore RuntimeGuard dirty marker bridge failed.", exception);
        }
    }

    private static boolean resolveBridge() {
        if (bridgeUnavailable || !EchoRuntimeModules.isLoaded("echoruntimeguard")) {
            return false;
        }
        if (requestValidationMethod != null && markDirtyMethod != null) {
            return true;
        }
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echomultiblockcore.integration.runtimeguard.MultiblockRuntimeGuardBridge");
            requestValidationMethod = bridge.getMethod("requestValidation", MultiblockControllerBlockEntity.class,
                    ServerLevel.class, String.class, Runnable.class);
            markDirtyMethod = bridge.getMethod("markDirty", Level.class, BlockPos.class, String.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoMultiblockCore.LOGGER.warn("MultiblockCore RuntimeGuard bridge is unavailable.", exception);
            return false;
        }
    }
}
