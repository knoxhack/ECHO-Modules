package com.knoxhack.echodeepreachprotocol.client.model;

import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BloaterModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "bloater"), "main");

    private final ModelPart body;
    private final ModelPart finLeft;
    private final ModelPart finRight;

    public BloaterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.finLeft = root.getChild("fin_left");
        this.finRight = root.getChild("fin_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("fin_left",
                CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, -2.0F, -1.0F, 5.0F, 4.0F, 2.0F),
                PartPose.offset(4.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("fin_right",
                CubeListBuilder.create().texOffs(32, 6).addBox(-5.0F, -2.0F, -1.0F, 5.0F, 4.0F, 2.0F),
                PartPose.offset(-4.0F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.3F;
        float pulse = 1.0F + Mth.sin(t * 0.5F) * 0.08F;
        body.xScale = pulse;
        body.yScale = pulse;
        body.zScale = pulse;
        finLeft.yRot = Mth.sin(t) * 0.3F;
        finRight.yRot = -Mth.sin(t) * 0.3F;
    }
}
