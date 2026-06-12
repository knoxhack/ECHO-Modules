package com.knoxhack.echogrimoire;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echogrimoire.integration.GrimoireMissionIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class EchoGrimoire {
    public static final String MODID = "echogrimoire";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoGrimoire(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "grimoire";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: Grimoire";
                }

                @Override
                public String summary() {
                    return "Terminal-based digital/mystic archive for Arcana Division lore and forbidden pages.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Grimoire archive shell online.";
                }
            });
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                registerTerminalIntegration();
            }
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                GrimoireMissionIntegration.register();
            }
            LOGGER.info("ECHO: Grimoire online. Archive voices are contained.");
        });
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echogrimoire.integration.GrimoireTerminalIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Grimoire Terminal integration could not be registered.", exception);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
