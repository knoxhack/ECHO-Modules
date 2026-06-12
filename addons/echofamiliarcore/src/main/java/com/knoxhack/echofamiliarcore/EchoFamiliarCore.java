package com.knoxhack.echofamiliarcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import com.knoxhack.echofamiliarcore.integration.FamiliarCoreArcanaProvider;
import com.knoxhack.echofamiliarcore.registry.ModCreativeTabs;
import com.knoxhack.echofamiliarcore.registry.ModEntities;
import com.knoxhack.echofamiliarcore.registry.ModItems;
import com.knoxhack.echofamiliarcore.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class EchoFamiliarCore {
    public static final String MODID = "echofamiliarcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoFamiliarCore(Object modEventBus) {
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onPlayerTick);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "familiarcore";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: FamiliarCore";
                }

                @Override
                public String summary() {
                    return "Familiar registry, starter bonds, command, upgrade, and cursed companion foundation.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "FamiliarCore: " + FamiliarCoreApi.summary(player);
                }
            });
            ArcanaCoreServices.registerProvider(FamiliarCoreArcanaProvider.INSTANCE);
            LOGGER.info("ECHO: FamiliarCore scaffold registered with Arcana Core.");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void onPlayerTick(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
        if (player != null) {
            FamiliarCoreApi.tick(player);
        }
    }
}
