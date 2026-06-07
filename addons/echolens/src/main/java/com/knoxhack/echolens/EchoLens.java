package com.knoxhack.echolens;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echolens.command.LensCommands;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.integration.LensCoreIntegration;
import com.knoxhack.echolens.integration.LensMissionCoreIntegration;
import com.knoxhack.echolens.network.ModNetwork;
import com.knoxhack.echolens.provider.LensBuiltins;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class EchoLens {
    public static final String MODID = "echolens";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoLens(Object modEventBus, Object modContainer) {
        LensConfig.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(LensCommands::register);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            LensBuiltins.register();
            LensCoreIntegration.register();
            if (modulePresent("com.knoxhack.echomissioncore.EchoMissionCore")) {
                LensMissionCoreIntegration.register();
            }
            if (modulePresent("com.knoxhack.echomachinecore.EchoMachineCore")) {
                registerMachineCoreIntegration();
            }
            if (modulePresent("com.knoxhack.echoterminal.EchoTerminal")) {
                registerTerminalIntegration();
            }
            LOGGER.info("ECHO: Lens scanner HUD online with {} providers.",
                    com.knoxhack.echolens.registry.LensProviderRegistry.count());
            LOGGER.info("ECHO: Lens server-assisted Deep Scan online with {} server providers.",
                    com.knoxhack.echolens.registry.LensProviderRegistry.serverProviders().size());
        });
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echolens.integration.LensTerminalCommonIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("ECHO: Lens terminal integration could not be registered.", exception);
        }
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echolens.integration.MachineCoreLensIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Lens MachineCore integration could not be registered.", exception);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static boolean modulePresent(String className) {
        try {
            Class.forName(className, false, EchoLens.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
