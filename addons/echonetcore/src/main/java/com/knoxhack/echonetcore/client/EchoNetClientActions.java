package com.knoxhack.echonetcore.client;

import com.knoxhack.echocore.api.network.EchoPacketDirection;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class EchoNetClientActions {
    private static volatile TestActionOverride testActionOverride;
    private static volatile RuntimeClientActionTransport runtimeClientActionTransport =
            RuntimeClientActionTransport.UNAVAILABLE;

    private EchoNetClientActions() {
    }

    public static void sendServerboundAction(CustomPacketPayload payload) {
        trySendServerboundAction(payload);
    }

    public static boolean trySendServerboundAction(CustomPacketPayload payload) {
        if (payload == null) {
            return false;
        }
        TestActionOverride override = testActionOverride;
        if (override != null) {
            try {
                Optional<Boolean> result = override.send(payload);
                if (result.isPresent()) {
                    boolean accepted = result.get();
                    EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.SERVERBOUND,
                            EchoPacketKind.SERVERBOUND_ACTION, "", accepted,
                            accepted ? "test-client-action-accepted" : "test-client-action-rejected");
                    return accepted;
                }
            } catch (RuntimeException exception) {
                EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.SERVERBOUND,
                        EchoPacketKind.SERVERBOUND_ACTION, "", false, detail(exception));
                return false;
            }
        }
        try {
            if (!runtimeClientActionTransport.sendToServer(payload)) {
                EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.SERVERBOUND,
                        EchoPacketKind.SERVERBOUND_ACTION, "", false, "native-client-transport-unavailable");
                return false;
            }
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.SERVERBOUND,
                    EchoPacketKind.SERVERBOUND_ACTION, "", true, "sent");
            return true;
        } catch (RuntimeException | LinkageError exception) {
            EchoNetDebug.emit(payload.type().id(), EchoPacketDirection.SERVERBOUND,
                    EchoPacketKind.SERVERBOUND_ACTION, "", false, exception.getMessage());
            return false;
        }
    }

    private static String detail(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public static TestActionOverrideHandle installActionOverrideForTests(TestActionOverride override) {
        TestActionOverride previous = testActionOverride;
        testActionOverride = override;
        return () -> testActionOverride = previous;
    }

    public static RuntimeClientActionTransportHandle installRuntimeClientActionTransport(RuntimeClientActionTransport transport) {
        RuntimeClientActionTransport previous = runtimeClientActionTransport;
        runtimeClientActionTransport = transport == null ? RuntimeClientActionTransport.UNAVAILABLE : transport;
        return () -> runtimeClientActionTransport = previous;
    }

    public static boolean hasActionOverrideForTests() {
        return testActionOverride != null;
    }

    @FunctionalInterface
    public interface TestActionOverride {
        Optional<Boolean> send(CustomPacketPayload payload);
    }

    @FunctionalInterface
    public interface RuntimeClientActionTransport {
        RuntimeClientActionTransport UNAVAILABLE = payload -> false;

        boolean sendToServer(CustomPacketPayload payload);
    }

    @FunctionalInterface
    public interface TestActionOverrideHandle extends AutoCloseable {
        @Override
        void close();
    }

    @FunctionalInterface
    public interface RuntimeClientActionTransportHandle extends AutoCloseable {
        @Override
        void close();
    }
}
