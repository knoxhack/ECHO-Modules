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

public class VentCrabModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "vent_crab"), "main");

    private final ModelPart body;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;

    public VentCrabModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftClaw = root.getChild("left_claw");
        this.rightClaw = root.getChild("right_claw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 15).addBox(-1.0F, 0.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                PartPose.offset(3.0F, 21.0F, -2.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 19).addBox(-5.0F, 0.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                PartPose.offset(-3.0F, 21.0F, -2.0F));
        root.addOrReplaceChild("left_claw",
                CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(4.0F, 20.0F, 3.0F));
        root.addOrReplaceChild("right_claw",
                CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(-4.0F, 20.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks * 0.25F;
        leftLeg.xRot = Mth.sin(t) * 0.15F;
        rightLeg.xRot = Mth.sin(t + Mth.PI) * 0.15F;
        leftClaw.zRot = Mth.sin(t) * 0.1F;
        rightClaw.zRot = -Mth.sin(t) * 0.1F;
        body.y = 20.0F + Mth.sin(t * 0.5F) * 0.3F;
    }
}
