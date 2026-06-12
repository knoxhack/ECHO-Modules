package com.knoxhack.echothemecore;

import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echothemecore.command.ThemeCoreCommands;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echothemecore.content.ThemeReloaders;
import com.knoxhack.echothemecore.integration.ThemeCoreRenderCoreBridge;
import com.knoxhack.echothemecore.integration.ThemeCoreTerminalBridge;
import com.knoxhack.echothemecore.network.ModNetwork;
import com.knoxhack.echothemecore.network.ThemeCoreServerSync;
import com.knoxhack.echothemecore.service.ThemeCoreService;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoThemeCore {
    public static final String MODID = "echothemecore";
    public static final String CHAPTER_ID = "themecore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoThemeCore() {
        ModNetwork.registerPayloads();
        ThemeCoreConfig.registerEchoConfig();
        commonSetup();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public void commonSetup() {
        registerAddonChapter();
        EchoCoreServices.registerThemeService(ThemeCoreService.INSTANCE);
        ThemeRegistry.setGlobalThemeChangeListener(ThemeCoreServerSync::broadcastGlobalTheme);
        ThemeRegistry.setPlayerThemeChangeListener(ThemeCoreServerSync::sendPlayerTheme);
        if (ThemeCoreRenderCoreBridge.registerIfAvailable()) {
            LOGGER.info("ECHO ThemeCore found RenderCore; theme provider bridge is available.");
        }
        ThemeCoreTerminalBridge.registerIfAvailable();
        LOGGER.info("ECHO ThemeCore 1.3.0 online. {} loaded theme(s). {}",
            ThemeRegistry.listThemes().size(),
            EchoCoreServices.platformProviderSummary());
    }

    public void registerCommands(
        com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
        CommandBuildContext buildContext,
        Commands.CommandSelection selection) {
        ThemeCoreCommands.register(dispatcher, buildContext, selection);
    }

    public List<ThemeReloaders.NativeReloadListenerRegistration> reloadListeners() {
        return ThemeReloaders.reloadListeners();
    }

    public void onServerStarted(MinecraftServer server) {
        ThemeCoreServerSync.onServerStarted(server);
    }

    public void onServerStopping(MinecraftServer server) {
        ThemeCoreServerSync.onServerStopping(server);
    }

    public void onPlayerLogin(ServerPlayer player) {
        ThemeCoreServerSync.onPlayerLogin(player);
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
                return "ECHO ThemeCore";
            }

            @Override
            public String summary() {
                return "Shared data-driven visual, audio, UI, and vanilla UI theme service for ECHO.";
            }

            @Override
            public String statusLine(Player player) {
                return "ThemeCore: " + ThemeRegistry.getThemeFor(player).displayName()
                    + " (" + ThemeRegistry.listThemes().size() + " theme(s)).";
            }
        });
    }
}
