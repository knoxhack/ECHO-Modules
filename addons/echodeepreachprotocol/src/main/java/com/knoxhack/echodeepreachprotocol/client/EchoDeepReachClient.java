package com.knoxhack.echodeepreachprotocol.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import com.knoxhack.echodeepreachprotocol.client.model.AbyssalLeviathanModel;
import com.knoxhack.echodeepreachprotocol.client.model.BloaterModel;
import com.knoxhack.echodeepreachprotocol.client.model.HadalWraithModel;
import com.knoxhack.echodeepreachprotocol.client.model.LatticeBoltModel;
import com.knoxhack.echodeepreachprotocol.client.model.LatticeSentinelModel;
import com.knoxhack.echodeepreachprotocol.client.model.RemoraModel;
import com.knoxhack.echodeepreachprotocol.client.model.TwilightStalkerModel;
import com.knoxhack.echodeepreachprotocol.client.model.VentCrabModel;
import com.knoxhack.echodeepreachprotocol.registry.ModEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Function;

/**
 * Client-side registrations for ECHO: Deep Reach Protocol.
 */
public final class EchoDeepReachClient {
    private static final String REGISTER_LAYER_DEFINITIONS_EVENT =
            "net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterLayerDefinitions";
    private static final String REGISTER_RENDERERS_EVENT =
            "net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers";

    private EchoDeepReachClient() {
    }

    public static void register(IEventBus modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(
                modEventBus,
                REGISTER_LAYER_DEFINITIONS_EVENT,
                EchoDeepReachClient::registerLayerDefinitions);
        EchoBackendLifecycleBridge.registerModListener(
                modEventBus,
                REGISTER_RENDERERS_EVENT,
                EchoDeepReachClient::registerRenderers);
    }

    private static void registerLayerDefinitions(Object event) {
        EchoBackendClientBridge.registerLayerDefinition(event, TwilightStalkerModel.LAYER_LOCATION, TwilightStalkerModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, VentCrabModel.LAYER_LOCATION, VentCrabModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, AbyssalLeviathanModel.LAYER_LOCATION, AbyssalLeviathanModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, LatticeSentinelModel.LAYER_LOCATION, LatticeSentinelModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, BloaterModel.LAYER_LOCATION, BloaterModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, HadalWraithModel.LAYER_LOCATION, HadalWraithModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, RemoraModel.LAYER_LOCATION, RemoraModel::createBodyLayer);
        EchoBackendClientBridge.registerLayerDefinition(event, LatticeBoltModel.LAYER_LOCATION, LatticeBoltModel::createBodyLayer);
    }

    private static void registerRenderers(Object event) {
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TWILIGHT_STALKER.get(),
                ctx -> mobRenderer(ctx, TwilightStalkerModel.LAYER_LOCATION, TwilightStalkerModel::new, "twilight_stalker", 0.5F, 1.0F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.VENT_CRAB.get(),
                ctx -> mobRenderer(ctx, VentCrabModel.LAYER_LOCATION, VentCrabModel::new, "vent_crab", 0.4F, 0.7F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ABYSSAL_LEVIATHAN.get(),
                ctx -> mobRenderer(ctx, AbyssalLeviathanModel.LAYER_LOCATION, AbyssalLeviathanModel::new, "abyssal_leviathan", 0.6F, 1.2F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.LATTICE_SENTINEL.get(),
                ctx -> mobRenderer(ctx, LatticeSentinelModel.LAYER_LOCATION, LatticeSentinelModel::new, "lattice_sentinel", 0.5F, 1.0F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.BLOATER.get(),
                ctx -> mobRenderer(ctx, BloaterModel.LAYER_LOCATION, BloaterModel::new, "bloater", 0.3F, 1.0F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.HADAL_WRAITH.get(),
                ctx -> mobRenderer(ctx, HadalWraithModel.LAYER_LOCATION, HadalWraithModel::new, "hadal_wraith", 0.5F, 1.0F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.REMORA_SUBMERSIBLE.get(), RemoraRenderer::new);
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.LATTICE_BOLT.get(), LatticeBoltRenderer::new);
    }

    private static <T extends Mob> DeepReachMobRenderer<T> mobRenderer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            ModelLayerLocation layer,
            Function<ModelPart, EntityModel<LivingEntityRenderState>> modelFactory,
            String textureName,
            float shadow,
            float scale) {
        Identifier texture = Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "textures/entity/" + textureName + ".png");
        return new DeepReachMobRenderer<>(context, layer, modelFactory, texture, shadow, scale);
    }
}
