package com.knoxhack.echorecovery.integration;

import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.IDataKey;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryDataCoreIntegration {
    private static final IDataKey<Long> DEATHS = counter("deaths");
    private static final IDataKey<Long> GRAVES_CREATED = counter("graves_created");
    private static final IDataKey<Long> GRAVES_OPENED = counter("graves_opened");
    private static final IDataKey<Long> GRAVES_RECOVERED = counter("graves_recovered");
    private static final IDataKey<Long> PARTIAL_RECOVERED = counter("partial_recovered");
    private static final IDataKey<Long> GRAVES_EXPIRED = counter("graves_expired");
    private static final IDataKey<Long> GRAVES_DELETED = counter("graves_deleted");
    private static final IDataKey<Long> REMOTE_RECOVERED = counter("remote_recovered");
    private static final IDataKey<Long> GRAVES_SHARED = counter("graves_shared");
    private static final IDataKey<Long> ADMIN_RESTORED = counter("admin_restored");
    private static boolean registered;

    private RecoveryDataCoreIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDataKey(DEATHS);
        EchoCoreServices.registerDataKey(GRAVES_CREATED);
        EchoCoreServices.registerDataKey(GRAVES_OPENED);
        EchoCoreServices.registerDataKey(GRAVES_RECOVERED);
        EchoCoreServices.registerDataKey(PARTIAL_RECOVERED);
        EchoCoreServices.registerDataKey(GRAVES_EXPIRED);
        EchoCoreServices.registerDataKey(GRAVES_DELETED);
        EchoCoreServices.registerDataKey(REMOTE_RECOVERED);
        EchoCoreServices.registerDataKey(GRAVES_SHARED);
        EchoCoreServices.registerDataKey(ADMIN_RESTORED);
        EchoRecovery.LOGGER.info("Recovery DataCore counters registered.");
    }

    public static void recordDeath(ServerPlayer player) {
        increment(player, DEATHS);
    }

    public static void recordGraveCreated(ServerPlayer player) {
        increment(player, GRAVES_CREATED);
    }

    public static void recordGraveRecovered(ServerPlayer player) {
        increment(player, GRAVES_RECOVERED);
    }

    public static void recordGraveOpened(ServerPlayer player) {
        increment(player, GRAVES_OPENED);
    }

    public static void recordPartialRecovered(ServerPlayer player) {
        increment(player, PARTIAL_RECOVERED);
    }

    public static void recordExpired(ServerPlayer player) {
        increment(player, GRAVES_EXPIRED);
    }

    public static void recordDeleted(ServerPlayer player) {
        increment(player, GRAVES_DELETED);
    }

    public static void recordRemoteRecovered(ServerPlayer player) {
        increment(player, REMOTE_RECOVERED);
    }

    public static void recordShared(ServerPlayer player) {
        increment(player, GRAVES_SHARED);
    }

    public static void recordAdminRestored(ServerPlayer player) {
        increment(player, ADMIN_RESTORED);
    }

    private static void increment(ServerPlayer player, IDataKey<Long> key) {
        if (player == null || key == null) {
            return;
        }
        Long current = EchoCoreServices.playerData(player).get(key);
        EchoCoreServices.playerData(player).set(key, Math.max(0L, current == null ? 1L : current + 1L));
    }

    private static IDataKey<Long> counter(String path) {
        return IDataKey.counter(Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "counter/" + path),
                DataScope.PLAYER, 0L, true);
    }
}
