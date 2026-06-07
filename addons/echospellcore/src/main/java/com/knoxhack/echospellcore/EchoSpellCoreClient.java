package com.knoxhack.echospellcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echospellcore.client.SpellHudOverlay;
import com.knoxhack.echospellcore.client.SpellProjectileRenderer;
import com.knoxhack.echospellcore.client.screen.SpellDeckScreen;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import com.knoxhack.echospellcore.registry.ModEntities;
import com.knoxhack.echospellcore.registry.ModMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class EchoSpellCoreClient {
    public EchoSpellCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoSpellCoreClient::registerScreens);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoSpellCoreClient::registerEntityRenderers);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoSpellCoreClient::onRenderGui);
    }

    private static void onRenderGui(Object event) {
        GuiGraphicsExtractor graphics = EchoBackendClientBridge.guiGraphics(event);
        if (graphics != null) {
            SpellHudOverlay.render(graphics);
        }
    }

    static void registerScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.SPELL_DECK.get(), SpellDeckScreen.class);
    }

    static void registerEntityRenderers(Object event) {
        EntityRendererProvider<SpellProjectileEntity> renderer = SpellProjectileRenderer::new;
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SPELL_PROJECTILE.get(), renderer);
    }
}
