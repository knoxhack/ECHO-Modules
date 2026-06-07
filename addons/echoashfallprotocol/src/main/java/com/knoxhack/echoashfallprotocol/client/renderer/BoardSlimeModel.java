package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echorendercore.client.NamedModelParts;
import com.knoxhack.echorendercore.client.RenderCorePartProvider;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BoardSlimeModel extends EntityModel<AshfallLivingRenderState> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_slime"), "main");

    private final ModelPart body;
    private final ModelPart innerCore;
    private final ModelPart puddle;
    private final ModelPart bubbles;
    private final ModelPart eyes;
    private final Map<String, ModelPart> renderCoreParts;

    public BoardSlimeModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.innerCore = body.getChild("inner_core");
        this.bubbles = body.getChild("bubbles");
        this.eyes = body.getChild("eyes");
        this.puddle = root.getChild("puddle");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("body", body)
                .put("torso", body)
                .put("inner_core", innerCore)
                .put("core", innerCore)
                .put("puddle", puddle)
                .put("bubbles", bubbles)
                .put("eyes", eyes)
                .put("scanner", eyes)
                .put("trail", puddle)
                .put("ground", puddle)
                .put("exhaust", bubbles)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.5F, -5.5F, -5.5F, 11.0F, 11.0F, 11.0F)
                        .texOffs(0, 24).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.18F)),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild("inner_core",
                CubeListBuilder.create().texOffs(40, 0).addBox(-2.5F, -2.5F, -5.95F, 5.0F, 5.0F, 1.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("bubbles",
                CubeListBuilder.create()
                        .texOffs(42, 8).addBox(-4.8F, -3.5F, -4.9F, 2.0F, 2.0F, 0.8F)
                        .addBox(2.5F, 1.2F, -4.9F, 1.8F, 1.8F, 0.8F)
                        .addBox(-1.0F, 3.2F, -5.0F, 1.6F, 1.6F, 0.8F),
                PartPose.ZERO);
        body.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 54).addBox(-3.3F, -1.5F, -6.25F, 6.6F, 1.2F, 0.55F),
                PartPose.ZERO);
        root.addOrReplaceChild("puddle",
                CubeListBuilder.create().texOffs(0, 50).addBox(-7.0F, -0.2F, -7.0F, 14.0F, 0.4F, 14.0F),
                PartPose.offset(0.0F, 23.2F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AshfallLivingRenderState state) {
        super.setupAnim(state);
        float pulse = 1.0F + Mth.sin(state.ageInTicks * 0.22F) * 0.08F;
        body.xScale = pulse;
        body.zScale = pulse;
        body.yScale = 1.0F + Mth.cos(state.ageInTicks * 0.22F) * 0.06F;
        innerCore.xScale = 1.0F + Mth.sin(state.ageInTicks * 0.32F) * 0.12F;
        bubbles.y = Mth.sin(state.ageInTicks * 0.16F) * 0.18F;
        puddle.xScale = 1.0F + Mth.sin(state.ageInTicks * 0.11F) * 0.04F;
        puddle.zScale = puddle.xScale;
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
