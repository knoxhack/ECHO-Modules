package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.GlowingGhoul;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GlowingGhoulRenderer extends AshfallBoardHumanoidRenderer<GlowingGhoul> {
    public GlowingGhoulRenderer(EntityRendererProvider.Context context) {
        super(context, "glowing_ghoul", 0.5F);
    }
}
