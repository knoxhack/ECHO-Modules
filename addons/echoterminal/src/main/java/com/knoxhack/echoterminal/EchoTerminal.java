package com.knoxhack.echoterminal;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProgression;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.network.ModNetwork;
import com.knoxhack.echoterminal.registry.ModBlockEntities;
import com.knoxhack.echoterminal.registry.ModBlocks;
import com.knoxhack.echoterminal.registry.ModAttachments;
import com.knoxhack.echoterminal.registry.ModCreativeTabs;
import com.knoxhack.echoterminal.registry.ModItems;
import com.knoxhack.echoterminal.registry.ModMenus;
import com.knoxhack.echoterminal.service.EchoTerminalCoreServices;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoTerminal {
    public static final String MODID = "echoterminal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoTerminal(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        VanillaJourneyProgression.register();
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoTerminalCoreServices.register();
            BuiltinTerminalCommonIntegration.register();
            if (EchoRuntimeModules.isLoaded("echomachinecore")) {
                registerMachineCoreIntegration();
            }
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(VanillaJourneyProvider.INSTANCE);
            TerminalMissionActions.registerForTab(MainSurvivalQuestProvider.TAB_ID);
            TerminalMissionActions.registerForTab(VanillaJourneyProvider.TAB_ID);
            LOGGER.info("ECHO platform providers after Terminal setup: {}",
                    EchoCoreServices.platformProviderSummary());
        });
        LOGGER.info("ECHO: Terminal modular shell online.");
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoterminal.integration.MachineCoreTerminalIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Terminal MachineCore integration could not be registered.", exception);
        }
    }
}
