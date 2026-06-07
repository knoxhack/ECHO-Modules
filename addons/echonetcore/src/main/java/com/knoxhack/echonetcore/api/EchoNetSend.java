package com.knoxhack.echonetcore.api;

import com.knoxhack.echocore.api.network.EchoPacketDirection;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EchoNetSend {
    private static volatile TestSendOverride testSendOverride;
    private static volatile RuntimeSendTransport runtimeSendTransport = RuntimeSendTransport.UNAVAILABLE;

    private EchoNetSend() {
    }

    public static boolean toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        return toPlayer(player, payload, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    public static boolean toPlayer(ServerPlayer player, CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null) {
            return false;
        }
        EchoPacketKind effectiveKind = kindOrDefault(kind);
        if (player == null) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, "", false, "missing-player");
            return false;
        }
        if (EchoRuntimeGuardNetworkBridge.shouldDropNonCriticalDuplicate(payload, effectiveKind)) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, player.getScoreboardName(), false, "runtimeguard-duplicate");
            return false;
        }
        TestSendOverride override = testSendOverride;
        if (override != null) {
            try {
                Optional<Boolean> result = override.send(player, payload, effectiveKind);
                if (result.isPresent()) {
                    boolean accepted = result.get();
                    if (accepted) {
                        EchoRuntimeGuardNetworkBridge.recordSend(payload, effectiveKind);
                    }
                    EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                            effectiveKind, player.getScoreboardName(), accepted,
                            accepted ? "test-send-accepted" : "test-send-rejected");
                    return accepted;
                }
            } catch (RuntimeException exception) {
                EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                        effectiveKind, player.getScoreboardName(), false, detail(exception));
                return false;
            }
        }
        if (EchoRuntimeGuardNetworkBridge.isNonCriticalOverBudget(payload, effectiveKind)) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, player.getScoreboardName(), true, "runtimeguard-over-budget-advisory");
        }
        try {
            if (!runtimeSendTransport.sendToPlayer(player, payload, effectiveKind)) {
                EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                        effectiveKind, player.getScoreboardName(), false, "native-transport-unavailable");
                return false;
            }
            EchoRuntimeGuardNetworkBridge.recordSend(payload, effectiveKind);
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, player.getScoreboardName(), true, "sent");
            return true;
        } catch (RuntimeException exception) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, player.getScoreboardName(), false, detail(exception));
            return false;
        }
    }

    public static int toPlayers(Collection<ServerPlayer> players, CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null) {
            return 0;
        }
        EchoPacketKind effectiveKind = kindOrDefault(kind);
        if (players == null) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, "", false, "missing-players");
            return 0;
        }
        int sent = 0;
        for (ServerPlayer player : players) {
            if (toPlayer(player, payload, effectiveKind)) {
                sent++;
            }
        }
        return sent;
    }

    public static int toPlayers(Collection<ServerPlayer> players, CustomPacketPayload payload) {
        return toPlayers(players, payload, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    public static int toAllPlayers(MinecraftServer server, CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null) {
            return 0;
        }
        EchoPacketKind effectiveKind = kindOrDefault(kind);
        if (server == null) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.CLIENTBOUND,
                    effectiveKind, "", false, "missing-server");
            return 0;
        }
        return toPlayers(server.getPlayerList().getPlayers(), payload, effectiveKind);
    }

    public static int toAllPlayers(MinecraftServer server, CustomPacketPayload payload) {
        return toAllPlayers(server, payload, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static EchoPacketKind kindOrDefault(EchoPacketKind kind) {
        return kind == null ? EchoPacketKind.CLIENTBOUND_SYNC : kind;
    }

    private static String detail(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public static TestSendOverrideHandle installSendOverrideForTests(TestSendOverride override) {
        TestSendOverride previous = testSendOverride;
        testSendOverride = override;
        return () -> testSendOverride = previous;
    }

    public static RuntimeSendTransportHandle installRuntimeSendTransport(RuntimeSendTransport transport) {
        RuntimeSendTransport previous = runtimeSendTransport;
        runtimeSendTransport = transport == null ? RuntimeSendTransport.UNAVAILABLE : transport;
        return () -> runtimeSendTransport = previous;
    }

    @FunctionalInterface
    public interface TestSendOverride {
        Optional<Boolean> send(ServerPlayer player, CustomPacketPayload payload, EchoPacketKind kind);
    }

    @FunctionalInterface
    public interface RuntimeSendTransport {
        RuntimeSendTransport UNAVAILABLE = (player, payload, kind) -> false;

        boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload, EchoPacketKind kind);
    }

    @FunctionalInterface
    public interface TestSendOverrideHandle extends AutoCloseable {
        @Override
        void close();
    }

    @FunctionalInterface
    public interface RuntimeSendTransportHandle extends AutoCloseable {
        @Override
        void close();
    }
}
