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

/**
 * Simple cube-based model for the Twilight Stalker.
 */
public class TwilightStalkerModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoDeepReachProtocol.MODID, "twilight_stalker"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public TwilightStalkerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 10.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(3.0F, 7.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-3.0F, 7.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(1.5F, 16.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(24, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-1.5F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.ageInTicks * 0.15F;
        leftArm.xRot = Mth.cos(swing) * 0.4F;
        rightArm.xRot = Mth.cos(swing + Mth.PI) * 0.4F;
        leftLeg.xRot = Mth.cos(swing + Mth.PI) * 0.4F;
        rightLeg.xRot = Mth.cos(swing) * 0.4F;
        head.yRot = state.yRot * ((float) Math.PI / 180.0F) * 0.6F;
    }
}
