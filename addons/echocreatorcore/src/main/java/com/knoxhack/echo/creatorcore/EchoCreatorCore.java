package com.knoxhack.echo.creatorcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.command.CreatorCoreCommands;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoCreatorCore {
    public static final String MODID = "echocreatorcore";
    public static final String CHAPTER_ID = "creatorcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoCreatorCore(Object modEventBus) {
        CreatorCoreApi.get().bootstrap();
        EchoCommandRegistry.register(CreatorCoreCommands.creatorCoreRoot());
        EchoCommandRegistry.register(CreatorCoreCommands.creatorAliasRoot());
        EchoBackendLifecycleBridge.registerGameEventHandler(CreatorCoreApi.get().pilot()::onServerTick);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echo.creatorcore.EchoCreatorCoreClient");
        LOGGER.info("ECHO: CreatorCore dashboard foundation loaded.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            CreatorCoreApi.get().bootstrap();
            registerAddonChapter();
            if (CreatorCoreConfig.bool(CreatorCoreConfig.LOG_ADAPTER_STATUS, true)) {
                CreatorCoreApi.get().adapters().adapters().forEach(adapter ->
                        LOGGER.info("CreatorCore adapter {} -> {}", adapter.id(), adapter.status()));
            }
        });
    }

    private static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: CreatorCore";
            }

            @Override
            public String summary() {
                return "In-game creator dashboard, validation center, adapter registry, draft store, and export foundation.";
            }

            @Override
            public String statusLine(Player player) {
                var api = CreatorCoreApi.get();
                long available = api.adapters().adapters().stream().filter(adapter -> adapter.isAvailable()).count();
                return "CreatorCore: " + available + "/" + api.adapters().adapters().size()
                        + " adapter(s), " + api.drafts().listDrafts().size() + " draft(s).";
            }
        });
    }
}
