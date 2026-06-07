package com.knoxhack.echofamiliarcore.client;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ArcanaFamiliarRenderer<T extends ArcanaFamiliarEntity> extends EchoMobFamilyRenderer<T> {
    private final int tint;
    private final String label;

    public ArcanaFamiliarRenderer(EntityRendererProvider.Context context, String entityName, int tint, String label) {
        super(context, EchoFamiliarCore.MODID, entityName, EchoMobFamily.DRONE, 0.68F, 0.18F);
        this.tint = tint;
        this.label = label;
    }

    @Override
    public void extractRenderState(T entity, EchoMobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        EchoOverheadDialogCards.configure(state, entity, 8.0D, label, entity.statusLine(), tint, 0.3F);
    }

    @Override
    protected int tint(T entity, EchoMobRenderState state, float partialTick) {
        return tint;
    }
}
