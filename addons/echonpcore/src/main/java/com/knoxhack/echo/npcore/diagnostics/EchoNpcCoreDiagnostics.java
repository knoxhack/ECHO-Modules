package com.knoxhack.echo.npcore.diagnostics;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementManager;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.faction.EchoNpcFactionManager;
import com.knoxhack.echo.npcore.data.NpcDataBridge;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.registry.ModEntities;
import com.knoxhack.echo.npcore.registry.ModItems;
import com.knoxhack.echo.npcore.service.EchoNpcServiceManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeManager;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfileManager;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.util.List;

public final class EchoNpcCoreDiagnostics {
    private EchoNpcCoreDiagnostics() {
    }

    public static void logStartup() {
        EchoNpcCore.LOGGER.info("NPCore initialized. Entity={}, spawnEgg={}, ScreenCore={}, vanillaReplacement={}",
                ModEntities.ECHO_NPC.id(), ModItems.ECHO_NPC_SPAWN_EGG.id(),
                EchoRuntimeModules.isLoaded("echoscreencore"),
                EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_VANILLA_VILLAGERS, true));
        EchoNpcCore.LOGGER.info("NPCore integrations. storageMode={}, DataCore={}, MissionCore={}",
                NpcDataBridge.storageMode(),
                NpcDataBridge.persistentBackendAvailable(),
                EchoCoreServices.missionCoreAvailable());
    }

    public static List<String> reportLines() {
        return List.of(
                "NPCore initialized: true",
                "registered entity type: " + ModEntities.ECHO_NPC.id(),
                "registered spawn egg: " + ModItems.ECHO_NPC_SPAWN_EGG.id(),
                "loaded profiles: " + EchoNpcProfileManager.count(),
                "loaded visual profiles: " + EchoNpcVisualProfileManager.count(),
                "loaded dialogues: " + EchoNpcDialogueManager.count(),
                "loaded trades: " + EchoNpcTradeManager.count(),
                "loaded services: " + EchoNpcServiceManager.count(),
                "loaded factions: " + EchoNpcFactionManager.count(),
                "loaded replacement mappings: " + EchoNpcReplacementManager.count(),
                "NPCore storage mode: " + NpcDataBridge.storageMode(),
                "DataCore persistent backend available: " + NpcDataBridge.persistentBackendAvailable(),
                "NPCore registered data keys: " + NpcDataBridge.registeredDataKeyCount(),
                "MissionCore available: " + EchoCoreServices.missionCoreAvailable(),
                "ScreenCore loaded: " + EchoRuntimeModules.isLoaded("echoscreencore"),
                "Terminal loaded: " + EchoRuntimeModules.isLoaded("echoterminal"),
                "vanilla replacement enabled: " + EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_VANILLA_VILLAGERS, true),
                "chunk-load replacement enabled: " + EchoNpcCoreConfig.bool(EchoNpcCoreConfig.REPLACE_ON_CHUNK_LOAD, true),
                "last reload warnings: " + EchoNpcProfileManager.warnings().size());
    }
}
