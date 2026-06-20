package com.knoxhack.echomissioncore.integration;

import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import java.lang.reflect.InvocationTargetException;

public final class MissionCoreTerminalIntegration {
    private static boolean registered;

    private MissionCoreTerminalIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        if (TerminalMissionRegistry.provider(MissionCoreTerminalProvider.CHAPTER_ID).isPresent()) {
            TerminalMissionActions.registerForTab(MissionCoreTerminalProvider.CHAPTER_ID);
            com.knoxhack.echomissioncore.EchoMissionCore.LOGGER.info(
                    "ECHO: MissionCore Terminal provider already registered with shared Terminal mission registry.");
            return;
        }
        TerminalMissionRegistry.register(MissionCoreTerminalProvider.INSTANCE);
        TerminalMissionActions.registerForTab(MissionCoreTerminalProvider.CHAPTER_ID);
        com.knoxhack.echomissioncore.EchoMissionCore.LOGGER.info(
                "ECHO: MissionCore Terminal provider registered with Terminal mission registry.");
    }

    public static void notifyMissionContentLoaded(int missionCount) {
        if (!registered) {
            register();
        }
        MainSurvivalQuestProvider.INSTANCE.invalidateRouteCache();
        TerminalMissionRegistry.ensureSorted();
        invalidateTerminalMissionData();
        invalidateScreenCoreData();
        com.knoxhack.echomissioncore.EchoMissionCore.LOGGER.info(
                "ECHO: MissionCore notified Terminal route views after loading {} missions. "
                        + "Terminal provider exposes {} missions; Survival Route exposes {} aggregate missions. Sources: {}",
                missionCount,
                TerminalMissionRegistry.provider(MissionCoreTerminalProvider.CHAPTER_ID)
                        .map(MissionCoreTerminalIntegration::safeMissionCount)
                        .orElse(-1),
                safeMissionCount(MainSurvivalQuestProvider.INSTANCE),
                com.knoxhack.echomissioncore.service.MissionCoreService.INSTANCE.sourceCounts());
    }

    private static void invalidateTerminalMissionData() {
        try {
            Class<?> dataProviders = Class.forName(
                    "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreDataProviders");
            dataProviders.getMethod("invalidateMissionData").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError ignored) {
        }
    }

    private static void invalidateScreenCoreData() {
        try {
            Class<?> screens = Class.forName("com.knoxhack.echoscreencore.api.EchoScreens");
            screens.getMethod("invalidateData").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError ignored) {
        }
    }

    private static int safeMissionCount(TerminalMissionProvider provider) {
        try {
            var missions = provider.missions(null);
            return missions == null ? 0 : missions.size();
        } catch (RuntimeException | LinkageError exception) {
            com.knoxhack.echomissioncore.EchoMissionCore.LOGGER.warn(
                    "ECHO: MissionCore could not resolve Terminal mission count after JSON content load: {}",
                    exception.toString());
            return -1;
        }
    }
}
