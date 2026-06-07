package com.knoxhack.echo.npcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.npcore.client.render.EchoNpcRenderer;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EchoNpcCoreClient {
    public EchoNpcCoreClient() {
        this(null);
    }

    public EchoNpcCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoNpcCoreClient::onRegisterEntityRenderers);
    }

    static void onRegisterEntityRenderers(Object event) {
        EntityRendererProvider<EchoNpcEntity> renderer = EchoNpcRenderer::new;
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ECHO_NPC.get(), renderer);
    }
}
