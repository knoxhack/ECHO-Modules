package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.RadZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RadZombieRenderer extends AshfallBoardHumanoidRenderer<RadZombie> {
    public RadZombieRenderer(EntityRendererProvider.Context context) {
        super(context, "rad_zombie", 0.5F);
    }
}
