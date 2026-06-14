package com.knoxhack.echo.settlementcore.api;

import com.echoplatform.echocore.api.EchoServiceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Singleton settlement registry registered in {@link EchoServiceRegistry}.
 */
public final class SettlementService {
    private static SettlementService instance;

    private final Map<UUID, Settlement> settlements = new ConcurrentHashMap<>();

    private SettlementService() {
    }

    public static synchronized SettlementService getInstance() {
        if (instance == null) {
            instance = new SettlementService();
            EchoServiceRegistry.register(SettlementService.class, instance);
        }
        return instance;
    }

    public static SettlementService find() {
        return EchoServiceRegistry.find(SettlementService.class).orElseGet(SettlementService::getInstance);
    }

    public void registerSettlement(Settlement settlement) {
        if (settlement != null) {
            settlements.put(settlement.id(), settlement);
        }
    }

    public Optional<Settlement> getSettlement(UUID id) {
        return Optional.ofNullable(settlements.get(id));
    }

    public List<Settlement> settlements() {
        return List.copyOf(settlements.values());
    }

    public Optional<Settlement> getSettlementAt(BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        for (Settlement settlement : settlements.values()) {
            if (settlement.contains(pos)) {
                return Optional.of(settlement);
            }
        }
        return Optional.empty();
    }

    public boolean isPlayerInSafeHabitat(ServerPlayer player) {
        if (player == null || player.isRemoved()) {
            return false;
        }
        return getSettlementAt(player.blockPosition())
            .filter(s -> s.oxygenLevel() > 0.15f && s.pressureLevel() > 0.5f)
            .isPresent();
    }

    public void clear() {
        settlements.clear();
    }

    public static void resetForTests() {
        if (instance != null) {
            instance.clear();
        }
    }
}
