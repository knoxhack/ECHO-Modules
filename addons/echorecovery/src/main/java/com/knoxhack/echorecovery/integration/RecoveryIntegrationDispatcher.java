package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryIntegrationDispatcher {
    private RecoveryIntegrationDispatcher() {}

    public static void registerCommon() {
        try {
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                load("RecoveryTerminalIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echothemecore")) {
                load("RecoveryThemeCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                load("RecoveryMissionCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echotutorialcore")) {
                load("RecoveryTutorialCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echosoundcore")) {
                load("RecoverySoundCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoholomap")) {
                load("RecoveryHoloMapIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoindex")) {
                load("RecoveryIndexIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoworldcore")) {
                load("RecoveryWorldCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoruntimeguard")) {
                load("RecoveryRuntimeGuardIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoweathercore")) {
                load("RecoveryWeatherCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echolens")) {
                load("RecoveryLensIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoarmory")) {
                load("RecoveryArmoryIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echologisticsnetwork")) {
                load("RecoveryLogisticsIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoconvoyprotocol")) {
                load("RecoveryConvoyIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echopowergrid")) {
                load("RecoveryPowerGridIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echorelictech")) {
                load("RecoveryRelicTechIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echonexusprotocol")) {
                load("RecoveryNexusIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoblackboxprotocol")) {
                load("RecoveryBlackboxIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoashfallprotocol")) {
                load("RecoveryAshfallIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echorendercore")) {
                load("RecoveryRenderCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echodatacore")) {
                load("RecoveryDataCoreIntegration", "registerCommon");
            }
            if (EchoRuntimeModules.isLoaded("echoplayercore")) {
                load("RecoveryPlayerCoreIntegration", "registerCommon");
            }
        } catch (Exception e) {
            EchoRecovery.LOGGER.error("Error during integration dispatch", e);
        }
    }

    public static void onGraveCreated(ServerPlayer player, BlockPos pos, String graveId) {
        if (player == null || pos == null) {
            return;
        }
        try {
            if (EchoRuntimeModules.isLoaded("echoholomap")) {
                load("RecoveryHoloMapIntegration", "onGraveCreated", player, pos, graveId);
            }
        } catch (Exception e) {
            EchoRecovery.LOGGER.debug("HoloMap grave creation hook failed: {}", e.getMessage());
        }
    }

    public static void onGraveRecovered(ServerPlayer player, BlockPos pos, String graveId) {
        if (player == null || pos == null) {
            return;
        }
        try {
            if (EchoRuntimeModules.isLoaded("echoholomap")) {
                load("RecoveryHoloMapIntegration", "onGraveRecovered", player, pos, graveId);
            }
        } catch (Exception e) {
            EchoRecovery.LOGGER.debug("HoloMap grave recovery hook failed: {}", e.getMessage());
        }
    }

    public static void onGraveDeleted(ServerPlayer player, BlockPos pos, String graveId) {
        if (player == null || pos == null) {
            return;
        }
        try {
            if (EchoRuntimeModules.isLoaded("echoholomap")) {
                load("RecoveryHoloMapIntegration", "onGraveDeleted", player, pos, graveId);
            }
        } catch (Exception e) {
            EchoRecovery.LOGGER.debug("HoloMap grave deletion hook failed: {}", e.getMessage());
        }
    }

    private static void load(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Class.forName("com.knoxhack.echorecovery.integration." + className);
            if (args.length == 0) {
                clazz.getMethod(methodName).invoke(null);
            } else {
                clazz.getMethod(methodName, resolveParameterTypes(clazz, methodName, args)).invoke(null, args);
            }
        } catch (ReflectiveOperationException e) {
            EchoRecovery.LOGGER.warn("Integration {} not found or failed to register: {}", className, e.getMessage());
        }
    }

    private static Class<?>[] resolveParameterTypes(Class<?> clazz, String methodName, Object[] args)
        throws NoSuchMethodException {
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null || !parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return parameterTypes;
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
