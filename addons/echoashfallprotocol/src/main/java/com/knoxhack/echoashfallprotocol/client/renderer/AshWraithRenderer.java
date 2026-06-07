package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.AshWraith;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AshWraithRenderer extends AshfallBoardWraithRenderer<AshWraith> {
    public AshWraithRenderer(EntityRendererProvider.Context context) {
        super(context, "ash_wraith", 0.5F);
    }
}
