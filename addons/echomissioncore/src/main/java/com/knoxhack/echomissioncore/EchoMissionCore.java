package com.knoxhack.echomissioncore;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echomissioncore.command.MissionCoreCommands;
import com.knoxhack.echomissioncore.content.MissionCoreReloaders;
import com.knoxhack.echomissioncore.integration.MissionCoreDiagnostics;
import com.knoxhack.echomissioncore.integration.MissionCoreIndexProvider;
import com.knoxhack.echomissioncore.integration.MissionCoreRuntimeSpineConsumer;
import com.knoxhack.echomissioncore.integration.MissionCoreTerminalIntegration;
import com.knoxhack.echomissioncore.integration.MissionCoreWorldCoreConsumer;
import com.knoxhack.echomissioncore.registry.ModAttachments;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;

public final class EchoMissionCore {
    public static final String MODID = "echomissioncore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoMissionCore() {
        ModAttachments.register();
        commonSetup();
    }

    public void commonSetup() {
        EchoCoreServices.registerMissionService(MissionCoreService.INSTANCE);
        MissionCoreService.INSTANCE.registerBuiltInContent();
        EchoCoreServices.registerDiagnosticService(MissionCoreDiagnostics.INSTANCE);
        EchoCoreServices.registerIndexContentProvider(MissionCoreIndexProvider.INSTANCE);
        MissionCoreRuntimeSpineConsumer.register();
        if (EchoRuntimeModules.isLoaded("echoworldcore")) {
            MissionCoreWorldCoreConsumer.register();
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            MissionCoreTerminalIntegration.register();
        }
        int missionCount = MissionCoreService.INSTANCE.missionDefinitions().size();
        if (missionCount == 0 && !EchoCoreServices.itemStackComponentsBound()) {
            LOGGER.info("ECHO: MissionCore online; built-in mission content deferred until item components are bound.");
        } else {
            LOGGER.info("ECHO: MissionCore online with {} missions.", missionCount);
        }
    }

    public void registerCommands(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        MissionCoreCommands.register(dispatcher, buildContext, selection);
    }

    public List<MissionCoreReloaders.NativeReloadListenerRegistration> reloadListeners() {
        return MissionCoreReloaders.reloadListeners();
    }
}
