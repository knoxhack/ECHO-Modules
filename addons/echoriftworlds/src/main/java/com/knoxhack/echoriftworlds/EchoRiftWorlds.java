package com.knoxhack.echoriftworlds;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echoriftworlds.integration.RiftWorldsArcanaProvider;
import com.knoxhack.echoriftworlds.registry.ModBlocks;
import com.knoxhack.echoriftworlds.registry.ModCreativeTabs;
import com.knoxhack.echoriftworlds.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class EchoRiftWorlds {
    public static final String MODID = "echoriftworlds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoRiftWorlds(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "riftworlds";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: RiftWorlds";
                }

                @Override
                public String summary() {
                    return "Rift marker, pocket-world, ruin, and dimensional hazard foundation.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "RiftWorlds online: Rift Cracks and Pocket Rifts are field-readable.";
                }
            });
            ArcanaCoreServices.registerProvider(RiftWorldsArcanaProvider.INSTANCE);
            LOGGER.info("ECHO: RiftWorlds scaffold registered with Arcana Core.");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
