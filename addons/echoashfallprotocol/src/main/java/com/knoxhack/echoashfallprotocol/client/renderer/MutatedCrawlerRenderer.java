package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.MutatedCrawler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MutatedCrawlerRenderer extends AshfallBoardCrawlerRenderer<MutatedCrawler> {
    public MutatedCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, "mutated_crawler", 0.3F);
    }
}
