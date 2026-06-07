package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.FeralHuman;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FeralHumanRenderer extends AshfallBoardHumanoidRenderer<FeralHuman> {
    public FeralHumanRenderer(EntityRendererProvider.Context context) {
        super(context, "feral_human", 0.5F);
    }
}
