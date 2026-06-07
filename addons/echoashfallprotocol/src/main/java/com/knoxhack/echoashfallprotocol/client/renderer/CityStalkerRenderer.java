package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.CityStalker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CityStalkerRenderer extends AshfallBoardHumanoidRenderer<CityStalker> {
    public CityStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, "city_stalker", 0.5F);
    }
}
