package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.ScavengerBandit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ScavengerBanditRenderer extends AshfallBoardHumanoidRenderer<ScavengerBandit> {
    public ScavengerBanditRenderer(EntityRendererProvider.Context context) {
        super(context, "scavenger_bandit", 0.5F);
    }
}
