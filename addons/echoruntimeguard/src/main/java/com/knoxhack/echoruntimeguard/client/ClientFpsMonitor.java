package com.knoxhack.echoruntimeguard.client;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.ClientMetricsSnapshot;
import com.knoxhack.echoruntimeguard.runtime.RuntimeModeService;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;

public final class ClientFpsMonitor {
    private static final int WINDOW = 200;
    private static final Deque<Integer> FPS_WINDOW = new ArrayDeque<>();
    private static ClientMetricsSnapshot lastSnapshot =
            ClientMetricsSnapshot.unavailable(RuntimeModeService.INSTANCE.mode());

    private ClientFpsMonitor() {
    }

    public static void onClientTick(Object event) {
        if (!RuntimeGuardConfig.safeBool(RuntimeGuardConfig.CLIENT_FPS_GUARD_ENABLED, true)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int fps = Math.max(0, minecraft.getFps());
        FPS_WINDOW.addLast(fps);
        while (FPS_WINDOW.size() > WINDOW) {
            FPS_WINDOW.removeFirst();
        }
        double average = FPS_WINDOW.stream().mapToInt(Integer::intValue).average().orElse(fps);
        double frameTime = fps <= 0 ? -1.0D : 1000.0D / fps;
        boolean emergency = average > 0.0D && average <= RuntimeGuardConfig.safeInt(RuntimeGuardConfig.EMERGENCY_FPS, 30);
        lastSnapshot = new ClientMetricsSnapshot(fps, average, frameTime, emergency, RuntimeModeService.INSTANCE.mode());
        RuntimeModeService.INSTANCE.tickClient(lastSnapshot);
    }

    public static ClientMetricsSnapshot snapshot() {
        return lastSnapshot;
    }

    public static void resetForTests() {
        FPS_WINDOW.clear();
        lastSnapshot = ClientMetricsSnapshot.unavailable(RuntimeModeService.INSTANCE.mode());
    }
}
