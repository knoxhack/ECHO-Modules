package com.knoxhack.echoritualcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echoritualcore.integration.RitualCoreIntegrations;
import com.knoxhack.echoritualcore.registry.ModBlockEntities;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.registry.ModCreativeTabs;
import com.knoxhack.echoritualcore.registry.ModItems;
import com.knoxhack.echoritualcore.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class EchoRitualCore {
    public static final String MODID = "echoritualcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoRitualCore(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "ritualcore";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: RitualCore";
                }

                @Override
                public String summary() {
                    return "Shared ritual circuits, altar diagnostics, relic stabilization, and curse cleansing.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Ritual circuits armed. Check inputs before ignition.";
                }
            });
            RitualCoreIntegrations.registerOptional();
            LOGGER.info("ECHO: RitualCore online. Circles are contracts now.");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
