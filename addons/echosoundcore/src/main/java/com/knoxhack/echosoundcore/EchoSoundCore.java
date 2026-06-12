package com.knoxhack.echosoundcore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echosoundcore.client.config.SoundCoreConfig;
import com.knoxhack.echosoundcore.command.SoundCoreCommands;
import com.knoxhack.echosoundcore.data.SoundCoreDataReloadListener;
import com.knoxhack.echosoundcore.integration.SoundCoreCoreIntegration;
import com.knoxhack.echosoundcore.network.SoundCoreNetwork;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import com.knoxhack.echosoundcore.service.SoundCoreService;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.slf4j.Logger;

public class EchoSoundCore {
    public static final String MODID = "echosoundcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoSoundCore() {
        SoundCoreSounds.register();
        com.knoxhack.echosoundcore.integration.prime.SoundCorePrimeIntegration.register();
        SoundCoreNetwork.registerPayloads();
        commonSetup();
    }

    public void commonSetup() {
        LOGGER.info("ECHO: SoundCore online. Adaptive audio framework initializing.");
        EchoCoreServices.registerSoundService(SoundCoreService.INSTANCE);
        SoundCoreCoreIntegration.registerAddonChapter();
        registerOptionalIntegrations();
    }

    public void registerCommands(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        SoundCoreCommands.register(dispatcher, buildContext, selection);
    }

    public List<NativeReloadListenerRegistration> reloadListeners() {
        return List.of(new NativeReloadListenerRegistration(id("audio_profiles"), SoundCoreDataReloadListener.INSTANCE));
    }

    private static void registerOptionalIntegrations() {
        tryOptional("echoterminal", "com.knoxhack.echosoundcore.integration.terminal.SoundCoreTerminalIntegration");
        tryOptional("echomissioncore", "com.knoxhack.echosoundcore.integration.mission.SoundCoreMissionIntegration");
        tryOptional("echolens", "com.knoxhack.echosoundcore.integration.lens.SoundCoreLensIntegration");
        tryOptional("echoholomap", "com.knoxhack.echosoundcore.integration.holomap.SoundCoreHoloMapIntegration");
        tryOptional("echopowergrid", "com.knoxhack.echosoundcore.integration.powergrid.SoundCorePowerGridIntegration");
        tryOptional("signalos", "com.knoxhack.echosoundcore.integration.signalos.SoundCoreSignalOSIntegration");
        tryOptional("echoworldcore", "com.knoxhack.echosoundcore.integration.worldcore.SoundCoreWorldCoreIntegration");
        tryOptional("echonexusprotocol", "com.knoxhack.echosoundcore.integration.nexus.SoundCoreNexusIntegration");
        tryOptional("echoblackboxprotocol", "com.knoxhack.echosoundcore.integration.blackbox.SoundCoreBlackboxIntegration");
        tryOptional("echostationfall", "com.knoxhack.echosoundcore.integration.stationfall.SoundCoreStationfallIntegration");
    }

    private static void tryOptional(String modId, String className) {
        if (!EchoRuntimeModules.isLoaded(modId)) {
            LOGGER.debug("Optional integration {} skipped because {} is absent.", className, modId);
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Method m = clazz.getMethod("register");
            m.invoke(null);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }

    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, path);
    }

    public record NativeReloadListenerRegistration(
            Identifier id,
            PreparableReloadListener listener) {
    }
}
