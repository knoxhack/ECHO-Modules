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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoLens.MODID)
public class EchoLens {
    public static final String MODID = "echolens";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoLens(IEventBus modEventBus, ModContainer modContainer) {
        LensConfig.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(LensCommands::register);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echolens.test.ModGameTests");

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            new EchoLensClient(modEventBus);
        }
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

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Lens GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Lens GameTest instances could not be registered.", exception);
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
