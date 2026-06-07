package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class BiomeBossRenderer extends HumanoidMobRenderer<BiomeBossEntity, BiomeBossRenderer.State, GuardianBossModel<BiomeBossRenderer.State>> {
    private static final Identifier FALLBACK_TEXTURE = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/entity/warden_boss.png");

    public BiomeBossRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardianBossModel<>(context.bakeLayer(GuardianBossModel.LAYER_LOCATION)), 0.72f);
        this.addLayer(new GuardianGlowLayer(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BiomeBossEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BiomeGuardianProfiles.byBossType(entity.getType())
                .map(BiomeGuardianProfile::visual)
                .ifPresentOrElse(visual -> {
                    state.texture = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, visual.texturePath());
                    state.glowTexture = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, visual.glowTexturePath());
                    state.tint = visual.tint();
                    state.glowColor = visual.glowColor();
                    state.scale = visual.scale();
                    state.shadow = visual.shadow();
                    state.variant = visual.variant();
                }, () -> {
                    state.texture = FALLBACK_TEXTURE;
                    state.glowTexture = null;
                    state.tint = 0xFFFFFFFF;
                    state.glowColor = 0xFFFFFFFF;
                    state.scale = 1.0f;
                    state.shadow = 0.9f;
                    state.variant = BiomeGuardianProfile.VisualVariant.NONE;
                });
        this.shadowRadius = state.shadow;
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
        return state.texture;
    }

    public static class State extends HumanoidRenderState {
        private Identifier texture = FALLBACK_TEXTURE;
        Identifier glowTexture = null;
        private int tint = 0xFFFFFFFF;
        int glowColor = 0xFFFFFFFF;
        private float scale = 1.0f;
        private float shadow = 0.9f;
        BiomeGuardianProfile.VisualVariant variant = BiomeGuardianProfile.VisualVariant.NONE;
    }
}
