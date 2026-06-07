package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.LensScanType;
import com.knoxhack.echoruntimeguard.api.RenderQuality;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import com.knoxhack.echoruntimeguard.api.TickPriority;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class IntegrationThrottleService {
    public static final IntegrationThrottleService INSTANCE = new IntegrationThrottleService();
    private final Map<UUID, Long> lensCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshTicks = new ConcurrentHashMap<>();
    private int lensScansRecorded;
    private int holomapRefreshSkips;

    private IntegrationThrottleService() {
    }

    public boolean shouldRefreshTerminalUi(Player player, BlockPos terminalPos) {
        if (!RuntimeGuardConfig.enabled()) {
            return true;
        }
        if (RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY) {
            return RuntimeProfilerService.INSTANCE.serverTick() % 20L == 0L;
        }
        return RuntimeProfilerService.INSTANCE.serverTick() % 5L == 0L;
    }

    public RenderQuality getTerminalAnimationLevel(Player player) {
        if (!RuntimeGuardConfig.enabled()) {
            return RenderQuality.FULL;
        }
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY || mode == RuntimeMode.POTATO) {
            return RenderQuality.SIMPLE;
        }
        return mode == RuntimeMode.CINEMATIC ? RenderQuality.FULL : RenderQuality.REDUCED;
    }

    public boolean shouldRefreshHoloMapMarkers(BlockPos holomapPos) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.HOLOMAP_GUARD_ENABLED, true)) {
            return true;
        }
        String key = holomapPos == null ? "global" : holomapPos.toShortString();
        long tick = RuntimeProfilerService.INSTANCE.serverTick();
        int interval = getHoloMapRefreshIntervalTicks();
        long previous = refreshTicks.getOrDefault("holomap:" + key, Long.MIN_VALUE);
        boolean allowed = tick - previous >= interval;
        if (allowed) {
            refreshTicks.put("holomap:" + key, tick);
        } else {
            holomapRefreshSkips++;
        }
        return allowed;
    }

    public int getHoloMapRefreshIntervalTicks() {
        int seconds = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.HOLOMAP_MARKER_REFRESH_SECONDS, 5);
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY) {
            seconds *= 4;
        } else if (mode == RuntimeMode.POTATO || mode == RuntimeMode.SERVER) {
            seconds *= 2;
        } else if (mode == RuntimeMode.CINEMATIC) {
            seconds = Math.max(1, seconds / 2);
        }
        return Math.max(1, seconds * 20);
    }

    public boolean shouldAnimateMarker(Identifier markerId, double distanceToPlayer) {
        if (!RuntimeGuardConfig.enabled()) {
            return true;
        }
        if (RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY) {
            return false;
        }
        if (RuntimeModeService.INSTANCE.mode() == RuntimeMode.POTATO && distanceToPlayer > 48.0D) {
            return false;
        }
        return distanceToPlayer <= 128.0D;
    }

    public boolean shouldSyncMarker(Identifier markerId, boolean dirty) {
        return !RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.HOLOMAP_SYNC_DIRTY_ONLY, true) || dirty;
    }

    public int getMaxAnimatedMarkers() {
        int configured = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.HOLOMAP_MAX_ANIMATED_MARKERS, 40);
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY) {
            return Math.max(0, configured / 8);
        }
        if (mode == RuntimeMode.POTATO) {
            return Math.max(0, configured / 3);
        }
        return mode == RuntimeMode.CINEMATIC ? configured * 2 : configured;
    }

    public boolean canRunLensScan(Player player, LensScanType type) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.LENS_GUARD_ENABLED, true)) {
            return true;
        }
        LensScanType safeType = type == null ? LensScanType.TARGET : type;
        if (safeType == LensScanType.DEBUG) {
            return RuntimeModeService.INSTANCE.mode() == RuntimeMode.DEBUG;
        }
        if (safeType == LensScanType.PASSIVE && RuntimeGuardConfig.safeBool(RuntimeGuardConfig.DISABLE_PASSIVE_SCANS_WHEN_INACTIVE, true)
                && RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY) {
            return false;
        }
        if (player == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        long previous = lensCooldowns.getOrDefault(player.getUUID(), 0L);
        int cooldown = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.LENS_SCAN_COOLDOWN_MS, 250);
        if (RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY) {
            cooldown *= 4;
        }
        boolean allowed = now - previous >= cooldown;
        if (allowed) {
            lensCooldowns.put(player.getUUID(), now);
        }
        return allowed;
    }

    public int getMaxBlocksPerScan(Player player, LensScanType type) {
        int base = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.LENS_MAX_BLOCKS_PER_SCAN, 128);
        return scaleLensBudget(base, type);
    }

    public int getMaxEntitiesPerScan(Player player, LensScanType type) {
        int base = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.LENS_MAX_ENTITIES_PER_SCAN, 32);
        return scaleLensBudget(base, type);
    }

    public int getDeepScanBudgetPerTick(Player player) {
        int budget = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.LENS_DEEP_SCAN_BUDGET_PER_TICK, 64);
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? Math.max(1, budget / 4) : budget;
    }

    public void recordLensScan(Player player, LensScanType type, int blocksScanned, int entitiesScanned) {
        lensScansRecorded++;
    }

    public boolean shouldTickIndustrialMachine(BlockPos pos) {
        return SmartTickService.INSTANCE.shouldRun("echoindustrialnexus:machine", pos, com.knoxhack.echoruntimeguard.api.TickPriority.ACTIVE_MACHINE);
    }

    public boolean shouldAnimateRoboticArm(BlockPos pos) {
        return RuntimeModeService.INSTANCE.mode() != RuntimeMode.EMERGENCY && RuntimeModeService.INSTANCE.mode() != RuntimeMode.SERVER;
    }

    public int getFactoryTaskTickRate(BlockPos pos) {
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? 80 : 20;
    }

    public boolean shouldSimulateConvoyRoute(Identifier routeId) {
        return RuntimeProfilerService.INSTANCE.serverTick() % getConvoySimulationInterval(routeId) == 0L;
    }

    public int getConvoySimulationInterval(Identifier routeId) {
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? 200 : 40;
    }

    public boolean shouldRefreshTelemetry(BlockPos pos) {
        return RuntimeProfilerService.INSTANCE.serverTick() % getOrbitalTelemetryInterval(pos) == 0L;
    }

    public int getOrbitalTelemetryInterval(BlockPos pos) {
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? 200 : 60;
    }

    public boolean shouldRunReclamationUpdate(BlockPos pos) {
        return RuntimeProfilerService.INSTANCE.serverTick() % Math.max(1, getReclamationBatchSize(pos)) == 0L;
    }

    public int getReclamationBatchSize(BlockPos pos) {
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? 4 : 16;
    }

    public boolean canSpawnNexusAmbientParticle(BlockPos pos) {
        return ParticleBudgetService.INSTANCE.canSpawnParticle(com.knoxhack.echoruntimeguard.api.ParticlePriority.AMBIENT, null);
    }

    public int getNexusStormTickInterval(Identifier regionId) {
        return RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY ? 100 : 20;
    }

    public boolean tryAcquireWork(Identifier systemId, RuntimeWorkType type, int cost) {
        return PerformanceBudgetService.INSTANCE.tryAcquire(type, cost);
    }

    public boolean shouldRunWork(Identifier systemId, Level level, BlockPos pos, RuntimeWorkType type,
            TickPriority priority, int cost) {
        if (!RuntimeGuardConfig.enabled()) {
            return true;
        }
        TickPriority safePriority = priority == null ? TickPriority.BACKGROUND : priority;
        RuntimeWorkType safeType = type == null ? RuntimeWorkType.BLOCK_ENTITY : type;
        return SmartTickService.INSTANCE.shouldRun(systemId == null ? "echoruntimeguard:generic" : systemId.toString(),
                level, pos, safePriority)
                && PerformanceBudgetService.INSTANCE.tryAcquire(safeType, Math.max(1, cost));
    }

    public void recordWork(Identifier systemId, RuntimeWorkType type, long nanos) {
        Identifier safeSystem = systemId == null
                ? Identifier.fromNamespaceAndPath("echoruntimeguard", "generic_work")
                : systemId;
        RuntimeGuardProfiler.record(safeSystem, nanos);
    }

    public String statusLine() {
        return "Lens scans " + lensScansRecorded + ", HoloMap refresh skips " + holomapRefreshSkips
                + ", HoloMap interval " + getHoloMapRefreshIntervalTicks() + " ticks";
    }

    public void reset() {
        lensCooldowns.clear();
        refreshTicks.clear();
        lensScansRecorded = 0;
        holomapRefreshSkips = 0;
    }

    private static int scaleLensBudget(int base, LensScanType type) {
        LensScanType safeType = type == null ? LensScanType.TARGET : type;
        int scaled = switch (safeType) {
            case TARGET -> base;
            case AREA -> base * 2;
            case DEEP -> base * 4;
            case PASSIVE -> Math.max(1, base / 4);
            case DEBUG -> base * 8;
        };
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY) {
            return Math.max(1, scaled / 4);
        }
        if (mode == RuntimeMode.POTATO) {
            return Math.max(1, scaled / 2);
        }
        if (mode == RuntimeMode.CINEMATIC) {
            return scaled * 2;
        }
        return scaled;
    }
}
