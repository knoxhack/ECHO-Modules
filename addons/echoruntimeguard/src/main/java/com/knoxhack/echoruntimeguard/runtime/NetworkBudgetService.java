package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.NetworkPriority;
import com.knoxhack.echoruntimeguard.api.NetworkSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class NetworkBudgetService {
    public static final NetworkBudgetService INSTANCE = new NetworkBudgetService();
    private final Map<Identifier, Integer> packetsByChannel = new ConcurrentHashMap<>();
    private final Map<Identifier, Integer> bytesByChannel = new ConcurrentHashMap<>();
    private final Map<PayloadKey, Long> duplicateWindow = new ConcurrentHashMap<>();
    private int packetsThisSecond;
    private int bytesThisSecond;
    private int warnings;
    private int duplicateDrops;
    private int tickCounter;

    private NetworkBudgetService() {
    }

    public boolean canSend(Identifier channel, NetworkPriority priority) {
        NetworkPriority safePriority = priority == null ? NetworkPriority.BACKGROUND_SYNC : priority;
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.NETWORK_GUARD_ENABLED, true)
                || safePriority.protectedSignal()) {
            return true;
        }
        return packetsThisSecond < RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARN_PACKETS_PER_SECOND, 300)
                && bytesThisSecond < RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARN_BYTES_PER_SECOND, 250000);
    }

    public void recordSend(Identifier channel, int bytes, NetworkPriority priority) {
        Identifier safeChannel = channel == null ? Identifier.fromNamespaceAndPath("echoruntimeguard", "unknown") : channel;
        packetsThisSecond++;
        bytesThisSecond += Math.max(0, bytes);
        packetsByChannel.merge(safeChannel, 1, Integer::sum);
        bytesByChannel.merge(safeChannel, Math.max(0, bytes), Integer::sum);
        if (!canSend(safeChannel, priority)) {
            warnings++;
        }
    }

    public boolean shouldBatch(Identifier channel) {
        return RuntimeGuardConfig.enabled()
                && RuntimeGuardConfig.safeBool(RuntimeGuardConfig.NETWORK_GUARD_ENABLED, true)
                && RuntimeGuardConfig.safeBool(RuntimeGuardConfig.BATCH_NONCRITICAL_PACKETS, true)
                && packetsThisSecond >= RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARN_PACKETS_PER_SECOND, 300) / 2;
    }

    public boolean shouldDropDuplicate(Identifier channel, int payloadHash) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.NETWORK_GUARD_ENABLED, true)
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.RATE_LIMIT_DUPLICATE_PAYLOADS, true)) {
            return false;
        }
        PayloadKey key = new PayloadKey(channel == null ? Identifier.fromNamespaceAndPath("echoruntimeguard", "unknown") : channel, payloadHash);
        long tick = RuntimeProfilerService.INSTANCE.serverTick();
        Long previous = duplicateWindow.put(key, tick);
        boolean duplicate = previous != null && tick - previous < 20L;
        if (duplicate) {
            duplicateDrops++;
        }
        return duplicate;
    }

    public void onServerTick(Object event) {
        tickCounter++;
        long tick = RuntimeProfilerService.INSTANCE.serverTick();
        duplicateWindow.entrySet().removeIf(entry -> tick - entry.getValue() > 40L);
        if (tickCounter >= 20) {
            packetsThisSecond = 0;
            bytesThisSecond = 0;
            packetsByChannel.clear();
            bytesByChannel.clear();
            tickCounter = 0;
        }
    }

    public NetworkSnapshot getSnapshot() {
        return new NetworkSnapshot(packetsThisSecond, bytesThisSecond, warnings, duplicateDrops,
                new LinkedHashMap<>(packetsByChannel), new LinkedHashMap<>(bytesByChannel));
    }

    public void reset() {
        packetsByChannel.clear();
        bytesByChannel.clear();
        duplicateWindow.clear();
        packetsThisSecond = 0;
        bytesThisSecond = 0;
        warnings = 0;
        duplicateDrops = 0;
        tickCounter = 0;
    }

    private record PayloadKey(Identifier channel, int payloadHash) {
    }
}
