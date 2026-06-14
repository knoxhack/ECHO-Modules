package com.knoxhack.echoaetherworks;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoaetherworks.integration.AetherWorksArcanaProvider;
import com.knoxhack.echoaetherworks.registry.ModBlockEntities;
import com.knoxhack.echoaetherworks.registry.ModBlocks;
import com.knoxhack.echoaetherworks.registry.ModCreativeTabs;
import com.knoxhack.echoaetherworks.registry.ModItems;
import com.knoxhack.echoaetherworks.registry.ModMenus;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;

@Mod(EchoAetherWorks.MODID)
public final class EchoAetherWorks {
    public static final String MODID = "echoaetherworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoAetherWorks(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoaetherworks.EchoAetherWorksClient");
}

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "aetherworks";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: AetherWorks";
                }

                @Override
                public String summary() {
                    return "Arcane automation foundation for Aether machines, storage, conduits, and contamination diagnostics.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "AetherWorks online: condenser, cell, and conduit circuits ready.";
                }
            });
            ArcanaCoreServices.registerProvider(AetherWorksArcanaProvider.INSTANCE);
            if (EchoRuntimeModules.isLoaded("echomachinecore")) {
                registerMachineCoreIntegration();
            }
            LOGGER.info("ECHO: AetherWorks beta module registered with Arcana Core.");
        });
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoaetherworks.integration.AetherWorksMachineCoreRuntimeProvider")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("ECHO: AetherWorks MachineCore integration could not be registered.", exception);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
