package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echocore.client.model.EchoMobRenderIds;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echorendercore.client.RenderCoreMobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

abstract class AshfallBoardRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderCoreMobRenderer<T, S, M> {
    private final Identifier texture;

    protected AshfallBoardRenderer(EntityRendererProvider.Context context, M model, float shadowRadius, String entityName) {
        this(context, model, shadowRadius, entityName, entityName);
    }

    protected AshfallBoardRenderer(EntityRendererProvider.Context context, M model, float shadowRadius, String entityName, String fallbackEntityName) {
        super(context, model, shadowRadius, profile(entityName), generatedTexture(fallbackEntityName));
        this.texture = boardTexture(entityName);
    }

    @Override
    public Identifier getTextureLocation(S state) {
        return texture;
    }

    protected static Identifier boardTexture(String entityName) {
        return generatedTexture(entityName);
    }

    private static Identifier generatedTexture(String entityName) {
        return EchoMobRenderIds.baseTexture(EchoAshfallProtocol.MODID, entityName);
    }

    private static Identifier profile(String entityName) {
        return EchoMobRenderIds.profile(EchoAshfallProtocol.MODID, entityName);
    }
}

class AshfallBoardHumanoidRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallHumanoidRenderState, BoardHumanoidModel<AshfallHumanoidRenderState>> {
    private final float scale;
    private final TintProvider<T> tintProvider;

    protected AshfallBoardHumanoidRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        this(context, entityName, entityName, shadowRadius, 1.0F, entity -> 0xFFFFFFFF);
    }

    protected AshfallBoardHumanoidRenderer(EntityRendererProvider.Context context, String entityName, String fallbackEntityName,
            float shadowRadius, float scale, TintProvider<T> tintProvider) {
        super(context, new BoardHumanoidModel<>(context.bakeLayer(BoardHumanoidModel.LAYER_LOCATION)), shadowRadius, entityName, fallbackEntityName);
        this.scale = scale;
        this.tintProvider = tintProvider;
    }

    @Override
    public AshfallHumanoidRenderState createRenderState() {
        return new AshfallHumanoidRenderState();
    }

    @Override
    public void extractRenderState(T entity, AshfallHumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.tint = tintProvider.tint(entity);
    }

    @Override
    protected int getModelTint(AshfallHumanoidRenderState state) {
        return state.tint;
    }

    @Override
    protected void scale(AshfallHumanoidRenderState state, PoseStack poseStack) {
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}

class AshfallBoardHeavyBossRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallHumanoidRenderState, BoardHeavyBossModel<AshfallHumanoidRenderState>> {
    private final TintProvider<T> tintProvider;

    protected AshfallBoardHeavyBossRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        this(context, entityName, shadowRadius, entity -> 0xFFFFFFFF);
    }

    protected AshfallBoardHeavyBossRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius,
            TintProvider<T> tintProvider) {
        super(context, new BoardHeavyBossModel<>(context.bakeLayer(BoardHeavyBossModel.LAYER_LOCATION)), shadowRadius, entityName);
        this.tintProvider = tintProvider;
    }

    @Override
    public AshfallHumanoidRenderState createRenderState() {
        return new AshfallHumanoidRenderState();
    }

    @Override
    public void extractRenderState(T entity, AshfallHumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.tint = tintProvider.tint(entity);
    }

    @Override
    protected int getModelTint(AshfallHumanoidRenderState state) {
        return state.tint;
    }
}

class AshfallBoardQuadrupedRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallLivingRenderState, BoardQuadrupedModel> {
    protected AshfallBoardQuadrupedRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        super(context, new BoardQuadrupedModel(context.bakeLayer(BoardQuadrupedModel.LAYER_LOCATION)), shadowRadius, entityName);
    }

    @Override
    public AshfallLivingRenderState createRenderState() {
        return new AshfallLivingRenderState();
    }
}

class AshfallBoardCrawlerRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallLivingRenderState, BoardCrawlerModel> {
    private final TintProvider<T> tintProvider;

    protected AshfallBoardCrawlerRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        this(context, entityName, shadowRadius, entity -> 0xFFFFFFFF);
    }

    protected AshfallBoardCrawlerRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius,
            TintProvider<T> tintProvider) {
        super(context, new BoardCrawlerModel(context.bakeLayer(BoardCrawlerModel.LAYER_LOCATION)), shadowRadius, entityName);
        this.tintProvider = tintProvider;
    }

    @Override
    public AshfallLivingRenderState createRenderState() {
        return new AshfallLivingRenderState();
    }

    @Override
    public void extractRenderState(T entity, AshfallLivingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.tint = tintProvider.tint(entity);
    }

    @Override
    protected int getModelTint(AshfallLivingRenderState state) {
        return state.tint;
    }
}

class AshfallBoardWraithRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallLivingRenderState, BoardWraithModel> {
    protected AshfallBoardWraithRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        super(context, new BoardWraithModel(context.bakeLayer(BoardWraithModel.LAYER_LOCATION)), shadowRadius, entityName);
    }

    @Override
    public AshfallLivingRenderState createRenderState() {
        return new AshfallLivingRenderState();
    }
}

class AshfallBoardSlimeRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, AshfallLivingRenderState, BoardSlimeModel> {
    protected AshfallBoardSlimeRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        super(context, new BoardSlimeModel(context.bakeLayer(BoardSlimeModel.LAYER_LOCATION)), shadowRadius, entityName);
    }

    @Override
    public AshfallLivingRenderState createRenderState() {
        return new AshfallLivingRenderState();
    }
}

class AshfallBoardDroneRenderer<T extends Mob>
        extends AshfallBoardRenderer<T, DroneRenderState, DroneModel> {
    protected AshfallBoardDroneRenderer(EntityRendererProvider.Context context, String entityName, float shadowRadius) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION)), shadowRadius, entityName);
    }

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }
}

@FunctionalInterface
interface TintProvider<T extends Mob> {
    int tint(T entity);
}
