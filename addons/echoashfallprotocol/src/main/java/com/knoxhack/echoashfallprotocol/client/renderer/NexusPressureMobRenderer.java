package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class NexusPressureMobRenderer {
    private NexusPressureMobRenderer() {
    }

    public static EntityRendererProvider<NexusPressureMobEntity> humanoid(String entityName) {
        return context -> new Humanoid(context, entityName);
    }

    public static EntityRendererProvider<NexusPressureMobEntity> heavy(String entityName) {
        return context -> new Heavy(context, entityName);
    }

    public static EntityRendererProvider<NexusPressureMobEntity> crawler(String entityName) {
        return context -> new Crawler(context, entityName);
    }

    private static int tint(NexusPressureMobEntity entity) {
        return entity.profile().accentColor();
    }

    private static final class Humanoid extends AshfallBoardHumanoidRenderer<NexusPressureMobEntity> {
        private Humanoid(EntityRendererProvider.Context context, String entityName) {
            super(context, entityName, "glowing_ghoul", 0.55F, 1.0F, NexusPressureMobRenderer::tint);
        }
    }

    private static final class Heavy extends AshfallBoardHeavyBossRenderer<NexusPressureMobEntity> {
        private Heavy(EntityRendererProvider.Context context, String entityName) {
            super(context, entityName, 0.85F, NexusPressureMobRenderer::tint);
        }
    }

    private static final class Crawler extends AshfallBoardCrawlerRenderer<NexusPressureMobEntity> {
        private Crawler(EntityRendererProvider.Context context, String entityName) {
            super(context, entityName, 0.35F, NexusPressureMobRenderer::tint);
        }
    }
}
