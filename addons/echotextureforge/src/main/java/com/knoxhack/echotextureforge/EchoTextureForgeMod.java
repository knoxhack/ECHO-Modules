package com.knoxhack.echotextureforge;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.knoxhack.echotextureforge.common.TextureForgeService;
import com.knoxhack.echotextureforge.common.command.TextureForgeCommands;
import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoTextureForgeMod {
    public static final String MODID = "echotextureforge";
    public static final String CHAPTER_ID = "textureforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoTextureForgeMod(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        TextureForgeConfig.registerEchoConfig();
        EchoCommandRegistry.register(TextureForgeCommands.echoSubcommand());
        EchoBackendLifecycleBridge.registerGameEventHandler(this::registerAlias);
        EchoBackendLifecycleBridge.registerGameEventHandler(TextureForgeService.INSTANCE::onServerStarted);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            registerAddonChapter();
            TextureForgeService.INSTANCE.initialize();
            LOGGER.info("ECHO TextureForge online. Output root: {}",
                    TextureForgeService.INSTANCE.paths().outputRoot());
        });
    }

    private void registerAlias(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            TextureForgeCommands.registerAlias(dispatcher);
        }
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
                return "ECHO TextureForge";
            }

            @Override
            public String summary() {
                return "Development-only texture audit, spec, validation, and prompt export tooling.";
            }

            @Override
            public String statusLine(Player player) {
                return "TextureForge: dev texture scanner and prompt exporter"
                        + (TextureForgeConfig.enabled() ? " enabled." : " disabled.");
            }
        });
    }
}
