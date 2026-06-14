package com.knoxhack.echowiki;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echowiki.command.GuideBookCommands;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.WikiContentRegistry;
import com.knoxhack.echowiki.content.WikiReloaders;
import com.knoxhack.echowiki.integration.GuideBookIndexProvider;
import com.knoxhack.echowiki.registry.ModCreativeTabs;
import com.knoxhack.echowiki.registry.ModDataComponents;
import com.knoxhack.echowiki.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

@Mod(EchoWiki.MODID)
public final class EchoWiki {
    public static final String MODID = "echowiki";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHAPTER_ID = "wiki";
    private static final String ADD_RELOAD_LISTENERS_EVENT =
            "net.neoforged.neoforge.event.AddServerReloadListenersEvent";
    private static final String REGISTER_COMMANDS_EVENT =
            "net.neoforged.neoforge.event.RegisterCommandsEvent";

    public EchoWiki(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        WikiContentRegistry.ensureDefaults();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(ADD_RELOAD_LISTENERS_EVENT, WikiReloaders::addServerReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT, GuideBookCommands::register);
        LOGGER.info("ECHO: Wiki is preparing the Survival Codex.");
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echowiki.EchoWikiClient");
}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            registerAddonChapter();
            EchoCoreServices.registerIndexContentProvider(GuideBookIndexProvider.INSTANCE);
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
                return "ECHO: Wiki";
            }

            @Override
            public String summary() {
                return "ScreenCore-powered ECHO Survival Codex for guide books, articles, discoveries, regions, hazards, missions, and addon knowledge.";
            }

            @Override
            public String statusLine(Player player) {
                return "Survival Codex online: " + WikiContentRegistry.articles().size()
                        + " article(s), " + GuideBookRegistry.visibleGuideBooks().size() + " guide book(s).";
            }
        });
    }
}
