package com.knoxhack.echofamiliarcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echofamiliarcore.client.ArcanaFamiliarRenderer;
import com.knoxhack.echofamiliarcore.client.FamiliarCommandScreen;
import com.knoxhack.echofamiliarcore.entity.AetherWispEntity;
import com.knoxhack.echofamiliarcore.entity.SpiritDroneEntity;
import com.knoxhack.echofamiliarcore.registry.ModEntities;
import com.knoxhack.echofamiliarcore.registry.ModMenus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class EchoFamiliarCoreClient {
    public EchoFamiliarCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoFamiliarCoreClient::registerEntityRenderers);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoFamiliarCoreClient::registerScreens);
    }

    private static void registerEntityRenderers(Object event) {
        EntityRendererProvider<AetherWispEntity> wisp =
                context -> new ArcanaFamiliarRenderer<>(context, "aether_wisp", 0xFF8CFFE1, "Aether Wisp");
        EntityRendererProvider<SpiritDroneEntity> drone =
                context -> new ArcanaFamiliarRenderer<>(context, "spirit_drone", 0xFF86B8FF, "Spirit Drone");
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.AETHER_WISP.get(), wisp);
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SPIRIT_DRONE.get(), drone);
    }

    private static void registerScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.FAMILIAR_COMMAND.get(), FamiliarCommandScreen.class);
    }
}
