package com.knoxhack.echopowergrid.integration.runtimeguard;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.config.PowerGridConfig;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardServices;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import net.minecraft.resources.Identifier;

public final class PowerGridRuntimeGuardIntegration {
    private PowerGridRuntimeGuardIntegration() {}

    public static void register() {
        EchoPowerGrid.LOGGER.info("ECHO PowerGrid RuntimeGuard integration registered.");
    }

    public static boolean canRunGridWork(String workId, int cost) {
        if (!PowerGridConfig.USE_RUNTIMEGUARD_IF_AVAILABLE.get()) {
            return true;
        }
        return RuntimeGuardServices.integrations().tryAcquireWork(id(workId), RuntimeWorkType.BLOCK_ENTITY, Math.max(1, cost));
    }

    public static void profile(String workId, Runnable task) {
        if (task == null) {
            return;
        }
        if (!PowerGridConfig.USE_RUNTIMEGUARD_IF_AVAILABLE.get()) {
            task.run();
            return;
        }
        RuntimeGuardProfiler.time(id(workId), task);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID,
                "runtimeguard/" + (path == null || path.isBlank() ? "grid_work" : path));
    }
}
