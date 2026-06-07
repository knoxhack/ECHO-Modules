package com.knoxhack.echoarcaneindex;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echoarcaneindex.integration.ArcaneIndexMissionIntegration;
import com.knoxhack.echoarcaneindex.integration.ArcaneIndexProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;

public final class EchoArcaneIndex {
    public static final String MODID = "echoarcaneindex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoArcaneIndex(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "arcane_index";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: Arcane Index";
                }

                @Override
                public String summary() {
                    return "Official magic recipe, research, ritual, spell, relic, curse, mob, and progression browser.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Arcane Index pages registered.";
                }
            });
            ArcaneIndexProvider.register();
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                ArcaneIndexMissionIntegration.register();
            }
            LOGGER.info("ECHO: Arcane Index online. JEI remains optional.");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
