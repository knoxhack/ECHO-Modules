package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.WildDog;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WildDogRenderer extends AshfallBoardQuadrupedRenderer<WildDog> {
    public WildDogRenderer(EntityRendererProvider.Context context) {
        super(context, "wild_dog", 0.45F);
    }
}
