package com.knoxhack.echodeepreachprotocol.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/**
 * Shared mob renderer for Deep Reach creatures.
 */
public class DeepReachMobRenderer<T extends Mob> extends MobRenderer<T, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
    private final Identifier texture;
    private final float scale;

    public DeepReachMobRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layer,
            Function<ModelPart, EntityModel<LivingEntityRenderState>> modelFactory,
            Identifier texture,
            float shadowRadius,
            float scale) {
        super(context, modelFactory.apply(context.bakeLayer(layer)), shadowRadius);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return texture;
    }

    @Override
    protected void scale(LivingEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
