package com.knoxhack.echo.npcore.client.render;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobModelFactory;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogue;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueRuntime;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfile;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfileManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class EchoNpcRenderer extends MobRenderer<EchoNpcEntity, EchoNpcRenderState, EntityModel<EchoNpcRenderState>> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public EchoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, (EntityModel) EchoMobModelFactory.create(context, EchoMobFamily.SURVIVOR_NPC, "echo_npc"), 0.5F);
    }

    @Override
    public EchoNpcRenderState createRenderState() {
        return new EchoNpcRenderState();
    }

    @Override
    public void extractRenderState(EchoNpcEntity entity, EchoNpcRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        EchoNpcVisualProfile visual = EchoNpcVisualProfileManager.getOrFallback(entity.visualProfileId());
        state.texture = visual.texture();
        state.tint = 0xFFFFFFFF;
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(entity.npcProfileId());
        String title = entity.hasCustomName() ? entity.getCustomName().getString() : profile.displayName();
        EchoOverheadDialogCards.configure(state, entity, Math.min(8.0D, entity.interactionRange()), title,
                dialogBody(entity, profile), 0xFF66E8FF, 0.45F);
        if (state.overheadDialogVisible) {
            state.nameTag = null;
            state.scoreText = null;
        }
    }

    @Override
    public Identifier getTextureLocation(EchoNpcRenderState state) {
        return state.texture == null ? EchoNpcVisualProfile.FALLBACK_TEXTURE : state.texture;
    }

    @Override
    protected int getModelTint(EchoNpcRenderState state) {
        return state.tint;
    }

    @Override
    protected void scale(EchoNpcRenderState state, PoseStack poseStack) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    public void submit(EchoNpcRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);
        EchoOverheadDialogCards.submit(state, getFont(), poseStack, collector, cameraRenderState);
    }

    private static String dialogBody(EchoNpcEntity entity, EchoNpcProfile profile) {
        if (!profile.ambientLines().isEmpty()) {
            int index = profile.ambientLines().size() == 1 ? 0
                    : Math.floorMod(entity.tickCount / 120, profile.ambientLines().size());
            return profile.ambientLines().get(index);
        }
        EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
        return dialogue.nodeOrFallback(EchoNpcDialogueRuntime.safeStart(dialogue)).text();
    }
}
