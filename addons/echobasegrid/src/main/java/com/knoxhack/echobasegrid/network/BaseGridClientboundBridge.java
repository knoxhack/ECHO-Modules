package com.knoxhack.echobasegrid.network;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import java.lang.reflect.Method;

public final class BaseGridClientboundBridge {
    private static final String HANDLER_CLASS = "com.knoxhack.echobasegrid.client.BaseGridClientPacketHandler";

    private BaseGridClientboundBridge() {
    }

    public static void applySnapshot(BaseGridSnapshotPacket packet) {
        try {
            Class<?> handler = Class.forName(HANDLER_CLASS);
            Method method = handler.getMethod("apply", BaseGridSnapshotPacket.class);
            method.invoke(null, packet);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoBaseGrid.LOGGER.warn("ECHO: Base Grid snapshot could not be applied on the client.", exception);
        }
    }
}
