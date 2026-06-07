package com.knoxhack.echostationfall.integration;

import com.knoxhack.echocore.api.EchoIntegrations;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class StationfallOrbitalCompat {
    public static final String ORBITAL_MODID = "echoorbitalremnants";
    private static final String SUIT_STATE = "com.knoxhack.echoorbitalremnants.suit.SuitState";
    private static final String SUIT_EVENTS = "com.knoxhack.echoorbitalremnants.suit.SuitEvents";
    private static final String ORBITAL_PROGRESS = "com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress";

    private StationfallOrbitalCompat() {
    }

    public static boolean orbitalLoaded() {
        return EchoIntegrations.has(ORBITAL_MODID);
    }

    public static boolean requiresOrbitalStaging() {
        return orbitalLoaded();
    }

    public static boolean isOrbitalExposure(Player player) {
        if (player == null || !orbitalLoaded()) {
            return true;
        }
        return invokeStaticBoolean(SUIT_EVENTS, "isOrbitalExposure", Player.class, player).orElse(true);
    }

    public static boolean orbitalGateOpen(Player player) {
        if (player == null || !orbitalLoaded()) {
            return false;
        }
        Optional<Object> progress = invokeStaticObject(ORBITAL_PROGRESS, "get", Player.class, player);
        if (progress.isEmpty()) {
            return false;
        }
        Object value = progress.get();
        return invokeBoolean(value, "stationCoordinatesRecovered").orElse(false)
                || invokeBoolean(value, "stationNetworkGateOpen").orElse(false)
                || invokeBoolean(value, "stationNetworkRestored").orElse(false);
    }

    public static void markLowOrbitReached(Player player) {
        if (player == null || !orbitalLoaded()) {
            return;
        }
        invokeStaticObject(ORBITAL_PROGRESS, "get", Player.class, player)
                .ifPresent(progress -> invokeVoid(progress, "markLowOrbitReached", Player.class, player));
    }

    public static ResourceKey<Level> dimensionKeyFromString(String id) {
        if (id == null || id.isBlank()) {
            return Level.OVERWORLD;
        }
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
        } catch (RuntimeException exception) {
            return Level.OVERWORLD;
        }
    }

    static Optional<Object> suitDelegate(Player player) {
        if (player == null || !orbitalLoaded()) {
            return Optional.empty();
        }
        return invokeStaticObject(SUIT_STATE, "get", Player.class, player);
    }

    static Optional<Integer> invokeInt(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LinkageError exception) {
            return Optional.empty();
        }
    }

    static void invokeVoid(Object target, String methodName, Class<?> parameterType, Object value) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LinkageError ignored) {
        }
    }

    static void invokeVoid(Object target, String methodName) {
        if (target == null) {
            return;
        }
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LinkageError ignored) {
        }
    }

    private static Optional<Boolean> invokeBoolean(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Boolean flag ? Optional.of(flag) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> invokeStaticBoolean(String className, String methodName, Class<?> parameterType, Object value) {
        try {
            Class<?> type = Class.forName(className);
            Object result = type.getMethod(methodName, parameterType).invoke(null, value);
            return result instanceof Boolean flag ? Optional.of(flag) : Optional.empty();
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                 | LinkageError exception) {
            return Optional.empty();
        }
    }

    private static Optional<Object> invokeStaticObject(String className, String methodName, Class<?> parameterType, Object value) {
        try {
            Class<?> type = Class.forName(className);
            return Optional.ofNullable(type.getMethod(methodName, parameterType).invoke(null, value));
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                 | LinkageError exception) {
            return Optional.empty();
        }
    }
}
