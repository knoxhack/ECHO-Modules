package com.knoxhack.echocore.client.model;

import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class EchoMobFamilyRenderer<T extends Mob>
        extends MobRenderer<T, EchoMobRenderState, EntityModel<EchoMobRenderState>> {
    private final Identifier texture;
    private final float scale;
    private final float shadow;

    public EchoMobFamilyRenderer(EntityRendererProvider.Context context, String modId, String entityName,
            EchoMobFamily family, float scale, float shadow) {
        super(context, EchoMobModelFactory.create(context, family, entityName), shadow);
        this.texture = EchoMobRenderIds.baseTexture(modId, entityName);
        this.scale = scale;
        this.shadow = shadow;
    }

    @Override
    public EchoMobRenderState createRenderState() {
        return new EchoMobRenderState();
    }

    @Override
    public void extractRenderState(T entity, EchoMobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.shadowRadius = shadow;
        state.tint = tint(entity, state, partialTick);
    }

    @Override
    public Identifier getTextureLocation(EchoMobRenderState state) {
        return texture;
    }

    @Override
    public void submit(EchoMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);
        EchoOverheadDialogCards.submit(state, getFont(), poseStack, collector, cameraRenderState);
    }

    @Override
    protected int getModelTint(EchoMobRenderState state) {
        return state.tint;
    }

    @Override
    protected void scale(EchoMobRenderState state, PoseStack poseStack) {
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }

    protected int tint(T entity, EchoMobRenderState state, float partialTick) {
        return 0xFFFFFFFF;
    }
}
