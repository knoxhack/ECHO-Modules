package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class EchoAtmosphereCoreEvents {
    private static volatile boolean levelTickHookAttached;

    private EchoAtmosphereCoreEvents() {
    }

    public static synchronized void attach() {
        if (levelTickHookAttached) {
            return;
        }
        levelTickHookAttached = true;
    }

    public static boolean levelTickHookAttached() {
        return levelTickHookAttached;
    }

    public static EchoAtmosphereRuntimeState.LiveAtmosphereTickState activeAtmosphereTick() {
        return EchoAtmosphereRuntimeState.activeAtmosphereTick();
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "EchoAtmosphereCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onLevelTick(Object event) {
        LevelTickSnapshot tick = LevelTickSnapshot.from(event);
        if (tick == null || tick.clientSide()) {
            return;
        }
        recordAgent7LiveHook(tick.gameTick(), "EchoAtmosphereCoreEvents.onLevelTick");
        EchoAtmosphereRuntimeState.materializeLevelTick(
                tick.gameTick(),
                "adaptercore.level_tick.post");
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echoatmospherecore",
                "level_tick.post",
                gameTick,
                sourceReason);
    }

    private record LevelTickSnapshot(long gameTick, boolean clientSide) {
        private static LevelTickSnapshot from(Object event) {
            if (event == null) {
                return null;
            }
            try {
                Object level = invoke(event, "getLevel");
                if (level == null) {
                    return null;
                }
                Object client = invoke(level, "isClientSide");
                Object time = invoke(level, "getGameTime");
                if (!(client instanceof Boolean clientSide) || !(time instanceof Number gameTime)) {
                    return null;
                }
                return new LevelTickSnapshot(Math.max(0L, gameTime.longValue()), clientSide);
            } catch (ReflectiveOperationException | IllegalArgumentException exception) {
                return null;
            }
        }

        private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
            Method method = target.getClass().getMethod(methodName);
            try {
                return method.invoke(target);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw exception;
            }
        }
    }
}
