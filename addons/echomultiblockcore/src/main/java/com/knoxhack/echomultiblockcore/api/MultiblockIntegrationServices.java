package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.integration.DefaultMultiblockIntegrationProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class MultiblockIntegrationServices {
    private static final List<MultiblockTerminalProvider> TERMINAL_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<MultiblockScanProvider> SCAN_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<MultiblockDataCoreProvider> DATA_CORE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<MultiblockMapMarkerProvider> MAP_MARKER_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<MultiblockPowerProvider> POWER_PROVIDERS = new CopyOnWriteArrayList<>();

    private MultiblockIntegrationServices() {
    }

    public static void registerDefaultProviders() {
        registerTerminalProvider(DefaultMultiblockIntegrationProvider.TERMINAL);
        registerScanProvider(DefaultMultiblockIntegrationProvider.SCAN);
        registerDataCoreProvider(DefaultMultiblockIntegrationProvider.DATA_CORE);
        registerMapMarkerProvider(DefaultMultiblockIntegrationProvider.MAP_MARKERS);
    }

    public static boolean registerTerminalProvider(MultiblockTerminalProvider provider) {
        return register(TERMINAL_PROVIDERS, provider, provider == null ? null : provider.providerId(), "terminal");
    }

    public static boolean registerScanProvider(MultiblockScanProvider provider) {
        return register(SCAN_PROVIDERS, provider, provider == null ? null : provider.providerId(), "scan");
    }

    public static boolean registerDataCoreProvider(MultiblockDataCoreProvider provider) {
        return register(DATA_CORE_PROVIDERS, provider, provider == null ? null : provider.providerId(), "data core");
    }

    public static boolean registerMapMarkerProvider(MultiblockMapMarkerProvider provider) {
        return register(MAP_MARKER_PROVIDERS, provider, provider == null ? null : provider.providerId(), "map marker");
    }

    public static boolean registerPowerProvider(MultiblockPowerProvider provider) {
        return register(POWER_PROVIDERS, provider, provider == null ? null : provider.providerId(), "power");
    }

    public static List<MultiblockStatusSnapshot> terminalSnapshots(Player player) {
        Map<String, MultiblockStatusSnapshot> snapshots = new LinkedHashMap<>();
        for (MultiblockTerminalProvider provider : TERMINAL_PROVIDERS) {
            for (MultiblockStatusSnapshot snapshot : safeTerminalSnapshots(provider, player)) {
                if (snapshot != null) {
                    snapshots.putIfAbsent(statusKey(snapshot), snapshot);
                }
            }
        }
        return List.copyOf(snapshots.values());
    }

    public static Optional<LensMultiblockScan> scan(Player player, Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        for (MultiblockScanProvider provider : SCAN_PROVIDERS) {
            try {
                Optional<LensMultiblockScan> scan = provider.scan(player, level, pos);
                if (scan != null && scan.isPresent()) {
                    return scan;
                }
            } catch (RuntimeException exception) {
                warn("scan", provider, exception);
            }
        }
        return Optional.empty();
    }

    public static List<MultiblockRuntimeSnapshot> dataSnapshots(Player player) {
        Map<String, MultiblockRuntimeSnapshot> snapshots = new LinkedHashMap<>();
        for (MultiblockDataCoreProvider provider : DATA_CORE_PROVIDERS) {
            for (MultiblockRuntimeSnapshot snapshot : safeDataSnapshots(provider, player)) {
                if (snapshot != null) {
                    snapshots.putIfAbsent(runtimeKey(snapshot), snapshot);
                }
            }
        }
        return List.copyOf(snapshots.values());
    }

    public static List<MultiblockMapMarkerSnapshot> mapMarkers(Player player) {
        Map<Identifier, MultiblockMapMarkerSnapshot> markers = new LinkedHashMap<>();
        for (MultiblockMapMarkerProvider provider : MAP_MARKER_PROVIDERS) {
            for (MultiblockMapMarkerSnapshot marker : safeMapMarkers(provider, player)) {
                if (marker != null && marker.markerId() != null) {
                    markers.putIfAbsent(marker.markerId(), marker);
                }
            }
        }
        return List.copyOf(markers.values());
    }

    public static boolean refreshMapMarkers(ServerPlayer player, String reason) {
        boolean refreshed = false;
        for (MultiblockMapMarkerProvider provider : MAP_MARKER_PROVIDERS) {
            try {
                refreshed |= provider.refresh(player, reason == null ? "" : reason);
            } catch (RuntimeException exception) {
                warn("map refresh", provider, exception);
            }
        }
        return refreshed;
    }

    public static long availablePower(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return 0L;
        }
        long available = 0L;
        for (MultiblockPowerProvider provider : POWER_PROVIDERS) {
            try {
                available = saturatedAdd(available, Math.max(0L, provider.availablePower(level, controllerPos)));
            } catch (RuntimeException exception) {
                warn("power availability", provider, exception);
            }
        }
        return available;
    }

    public static long drawPower(Level level, BlockPos controllerPos, long ep, boolean simulate) {
        if (level == null || controllerPos == null || ep <= 0L) {
            return 0L;
        }
        long remaining = ep;
        long drawn = 0L;
        for (MultiblockPowerProvider provider : POWER_PROVIDERS) {
            if (remaining <= 0L) {
                break;
            }
            try {
                long providerDrawn = Math.max(0L, provider.drawPower(level, controllerPos, remaining, simulate));
                drawn = saturatedAdd(drawn, providerDrawn);
                remaining = Math.max(0L, remaining - providerDrawn);
            } catch (RuntimeException exception) {
                warn("power draw", provider, exception);
            }
        }
        return Math.min(ep, drawn);
    }

    public static int terminalProviderCount() {
        return TERMINAL_PROVIDERS.size();
    }

    public static int scanProviderCount() {
        return SCAN_PROVIDERS.size();
    }

    public static int dataCoreProviderCount() {
        return DATA_CORE_PROVIDERS.size();
    }

    public static int mapMarkerProviderCount() {
        return MAP_MARKER_PROVIDERS.size();
    }

    public static int powerProviderCount() {
        return POWER_PROVIDERS.size();
    }

    public static Identifier generatedProviderId(Object provider, String surface) {
        String type = provider == null ? "noop" : provider.getClass().getName();
        String safeSurface = surface == null || surface.isBlank() ? "provider" : surface;
        return EchoMultiblockCore.id("provider/" + sanitize(safeSurface) + "/" + sanitize(type));
    }

    public static void withClearedForTests(Runnable body) {
        List<MultiblockTerminalProvider> terminal = new ArrayList<>(TERMINAL_PROVIDERS);
        List<MultiblockScanProvider> scan = new ArrayList<>(SCAN_PROVIDERS);
        List<MultiblockDataCoreProvider> data = new ArrayList<>(DATA_CORE_PROVIDERS);
        List<MultiblockMapMarkerProvider> map = new ArrayList<>(MAP_MARKER_PROVIDERS);
        List<MultiblockPowerProvider> power = new ArrayList<>(POWER_PROVIDERS);
        TERMINAL_PROVIDERS.clear();
        SCAN_PROVIDERS.clear();
        DATA_CORE_PROVIDERS.clear();
        MAP_MARKER_PROVIDERS.clear();
        POWER_PROVIDERS.clear();
        try {
            body.run();
        } finally {
            TERMINAL_PROVIDERS.clear();
            SCAN_PROVIDERS.clear();
            DATA_CORE_PROVIDERS.clear();
            MAP_MARKER_PROVIDERS.clear();
            POWER_PROVIDERS.clear();
            TERMINAL_PROVIDERS.addAll(terminal);
            SCAN_PROVIDERS.addAll(scan);
            DATA_CORE_PROVIDERS.addAll(data);
            MAP_MARKER_PROVIDERS.addAll(map);
            POWER_PROVIDERS.addAll(power);
        }
    }

    private static <T> boolean register(List<T> providers, T provider, Identifier providerId, String surface) {
        if (provider == null || providerId == null) {
            return false;
        }
        for (T existing : providers) {
            Identifier existingId = providerId(existing);
            if (providerId.equals(existingId)) {
                if (existing != provider) {
                    EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore {} provider {} ignored because that id is already registered.",
                            surface, providerId);
                }
                return false;
            }
        }
        providers.add(provider);
        return true;
    }

    private static Identifier providerId(Object provider) {
        if (provider instanceof MultiblockTerminalProvider terminal) {
            return terminal.providerId();
        }
        if (provider instanceof MultiblockScanProvider scan) {
            return scan.providerId();
        }
        if (provider instanceof MultiblockDataCoreProvider data) {
            return data.providerId();
        }
        if (provider instanceof MultiblockMapMarkerProvider marker) {
            return marker.providerId();
        }
        if (provider instanceof MultiblockPowerProvider power) {
            return power.providerId();
        }
        return null;
    }

    private static List<MultiblockStatusSnapshot> safeTerminalSnapshots(MultiblockTerminalProvider provider, Player player) {
        try {
            List<MultiblockStatusSnapshot> snapshots = provider.snapshots(player);
            return snapshots == null ? List.of() : snapshots;
        } catch (RuntimeException exception) {
            warn("terminal", provider, exception);
            return List.of();
        }
    }

    private static List<MultiblockRuntimeSnapshot> safeDataSnapshots(MultiblockDataCoreProvider provider, Player player) {
        try {
            List<MultiblockRuntimeSnapshot> snapshots = provider.snapshots(player);
            return snapshots == null ? List.of() : snapshots;
        } catch (RuntimeException exception) {
            warn("data core", provider, exception);
            return List.of();
        }
    }

    private static List<MultiblockMapMarkerSnapshot> safeMapMarkers(MultiblockMapMarkerProvider provider, Player player) {
        try {
            List<MultiblockMapMarkerSnapshot> markers = provider.markers(player);
            return markers == null ? List.of() : markers;
        } catch (RuntimeException exception) {
            warn("map marker", provider, exception);
            return List.of();
        }
    }

    private static String statusKey(MultiblockStatusSnapshot snapshot) {
        return snapshot.definitionId() + "@" + snapshot.controllerPos().asLong();
    }

    private static String runtimeKey(MultiblockRuntimeSnapshot snapshot) {
        return snapshot.definitionId() + "@" + snapshot.dimension() + "@" + snapshot.controllerPos().asLong();
    }

    private static String sanitize(String raw) {
        String value = raw == null ? "provider" : raw.toLowerCase(Locale.ROOT);
        value = value.replace('\\', '/').replace('.', '/').replace('$', '/');
        value = value.replaceAll("[^a-z0-9_./-]", "_");
        while (value.contains("//")) {
            value = value.replace("//", "/");
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isBlank()) {
            value = "provider";
        }
        return value;
    }

    private static long saturatedAdd(long left, long right) {
        if (left <= 0L) {
            return Math.max(0L, right);
        }
        if (right <= 0L) {
            return left;
        }
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static void warn(String surface, Object provider, RuntimeException exception) {
        EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore {} provider {} failed; ignoring provider output.",
                surface, provider == null ? "<null>" : provider.getClass().getName(), exception);
    }
}
