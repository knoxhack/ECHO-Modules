package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ScoutDroneRenderer extends AshfallBoardDroneRenderer<ScoutDrone> {
    public ScoutDroneRenderer(EntityRendererProvider.Context context) {
        super(context, "scout_drone", 0.4F);
    }
}
