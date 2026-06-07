package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FactionNpcRenderer extends AshfallBoardHumanoidRenderer<FactionNpcEntity> {
    public FactionNpcRenderer(EntityRendererProvider.Context context) {
        super(context, "faction_npc", "crash_survivor", 0.5F, 1.0F, entity -> 0xFFFFFFFF);
    }
}
