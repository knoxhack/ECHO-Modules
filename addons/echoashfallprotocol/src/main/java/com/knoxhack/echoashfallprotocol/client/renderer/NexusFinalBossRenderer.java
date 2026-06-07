package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class NexusFinalBossRenderer extends HumanoidMobRenderer<NexusFinalBossEntity, NexusFinalBossRenderer.State, GuardianBossModel<NexusFinalBossRenderer.State>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "textures/entity/warden_boss.png");

    public NexusFinalBossRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardianBossModel<>(context.bakeLayer(GuardianBossModel.LAYER_LOCATION)), 0.86F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(NexusFinalBossEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.tint = entity.profile().accentColor();
        state.scale = switch (entity.path()) {
            case RESTORE -> 1.04F;
            case DESTROY -> 1.14F;
            case CONTROL -> 1.08F;
            case NONE -> 1.0F;
        };
    }

    @Override
    protected int getModelTint(State state) {
        return state.tint;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.scale, state.scale, state.scale);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    public static class State extends HumanoidRenderState {
        private int tint = 0xFFFFFFFF;
        private float scale = 1.0F;
    }
}
